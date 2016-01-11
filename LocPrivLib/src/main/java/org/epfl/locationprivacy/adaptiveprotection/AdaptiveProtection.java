package org.epfl.locationprivacy.adaptiveprotection;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import org.epfl.locationprivacy.map.OSMWrapperAPI;
import org.epfl.locationprivacy.map.activities.SemanticMapFragment;
import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.databases.VenuesCondensedDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.privacyestimation.PrivacyEstimator;
import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;
import org.epfl.locationprivacy.userhistory.databases.TransitionTableDataSource;
import org.epfl.locationprivacy.userhistory.models.Transition;
import org.epfl.locationprivacy.util.Utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class AdaptiveProtection implements AdaptiveProtectionInterface,
	GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	private static final String LOGTAG = "AdaptiveProtection";

	/* Library Parameters */
	private static final double THETA = 0.2; //200 meters
	private static final int ALPHA = 2; // try 2 different obf regions with same size before enlarging the obf region
	private static final int MAX_OBF_REG_AREA = 81; // 81 grid cells (9X9)

	private PrivacyEstimator privacyEstimator;
	private Context context;
	private Random random;
	private long totalLoggingTime;
	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;

	private long previousLocID = -1;
	private int previousTimeID = -1;

	// All the next variables which have the prefix log* are only used by ThirdPartyActivity for testing purposes, because
	// the library returns only the obfuscation region. So, these variables can be removed safely without affecting the
	// adaptive protection mechanism
	public static LatLng logCurrentLocation;
	public static MyPolygon logVenue;
	public static String logVenueDistance;
	public static Double logSensitivity;
	public static Double logPrivacyEstimation;
	public static String logObfRegSize;
	public static ArrayList<MyPolygon> logObfRegion = new ArrayList<>();

	public AdaptiveProtection(Context context) {
		super();
		this.context = context;
		this.privacyEstimator = new PrivacyEstimator(context);
		random = new Random();
	}

	@Override
	public Pair<LatLng, LatLng> getObfuscationLocation() {
		// get current location
		if (mGoogleApiClient != null) {
			mGoogleApiClient.connect();
		}
		Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (location == null) {
			Toast.makeText(context, "Location is NULL", Toast.LENGTH_SHORT).show();
			return null;
		}

		return getObfuscationLocation(new LatLng(location.getLatitude(), location.getLongitude()));
	}

	@Override
	public Pair<LatLng, LatLng> getObfuscationLocation(LatLng location) {
		return getObfuscationLocation(location, System.currentTimeMillis());
	}

	@Override
	public Pair<LatLng, LatLng> getObfuscationLocation(LatLng location, long time) {

		// Logging
		long startGetLocationTimeStamp = System.currentTimeMillis();
		totalLoggingTime = 0;
		log("================================");
		log("Current Location : " + location.latitude + "," + location.longitude);
		logCurrentLocation = location;
		logSensitivity("Exact:" + location.latitude + "," + location.longitude);

		// DBs preparation
		GridDBDataSource gridDBDataSource = GridDBDataSource.getInstance(context);
		VenuesCondensedDBDataSource venuesCondensedDBDataSource = VenuesCondensedDBDataSource
			.getInstance(context);
		SemanticLocationsDataSource semanticLocationsDataSource = SemanticLocationsDataSource
			.getInstance(context);
		TransitionTableDataSource transitionTableDataSource = TransitionTableDataSource
			.getInstance(context);

		//===========================================================================================
		//current Location ID
		long start = System.currentTimeMillis();
		LatLng cell = Utils.findCellTopLeftPoint(location);
		long cellID = Utils.computeCellIDFromPosition(cell);
		MyPolygon currLocGridCell = gridDBDataSource.findGridCell(cellID);
		if (currLocGridCell == null) {
			currLocGridCell = new MyPolygon(cellID + "", "", Utils.computeCellCornerPoints(cell));
		}
		long fineLocationID = Long.parseLong(currLocGridCell.getName());
		log("Getting fine Location ID took: " + (System.currentTimeMillis() - start) + " ms");
		log("Current CellID: " + fineLocationID);

		//===========================================================================================
		// Checking and loading semantic locations
		boolean inLoadedArea = false;
		VenuesCondensedDBDataSource dbDataSource = VenuesCondensedDBDataSource.getInstance(context);
		ArrayList<Pair<LatLng, LatLng>> pairs = dbDataSource.findAllStoredSemanticArea();
		for (Pair<LatLng, LatLng> p : pairs) {
			if (p.first.latitude >= location.latitude && p.first.longitude >= location.longitude &&
				p.second.latitude <= location.latitude && p.second.longitude <= location.longitude) {
				inLoadedArea = true;
			}
		}
		log("Current position in loaded semantic area : " + inLoadedArea);
		if (!inLoadedArea) {
			// Load an area around current position
			Pair<LatLng, LatLng> corners;

			ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (connManager.getActiveNetworkInfo() == null) {
				log("No Network for loading new area");
			} else {
				log("Loading new area");
				if (!mWifi.isConnected()) {
					double distance = 1;
					corners = SemanticMapFragment.getCorners(Utils.getLatLong(location, distance, 45), Utils.getLatLong(location, distance, 225));
				} else {
					double distance = 3;
					corners = SemanticMapFragment.getCorners(Utils.getLatLong(location, distance, 45), Utils.getLatLong(location, distance, 225));
				}
				new DownloadSemanticMapAsyncTask().execute(corners);
			}
		}

		//===========================================================================================
		// Create a new transition with the new position
		long currTime = time;
		long currLocID = cellID;
		int currTimeID = Utils.findDayPortionID(currTime);
		if (previousLocID != -1) {
			Transition newTransition = new Transition(previousLocID, currLocID, previousTimeID,
				currTimeID, 1);
			transitionTableDataSource.updateOrInsert(newTransition);
		}
		// FIXME : is it ok to have as init value -1 ?
		previousLocID = currLocID;
		previousTimeID = currTimeID;


		//===========================================================================================
		// getting sensitivity preference of location, if not existing then sensitivity of semantic of nearest venue
		Double sensitivity = currLocGridCell.getSensitivityAsDouble();
		log("Current Cell Sensitivity: " + sensitivity);
		if (sensitivity == null) { // current grid cell is not sensitive,then get nearest venue semantic sensitivity

			//--> get semantics of current location
			long startGetNearVenues = System.currentTimeMillis();
			ArrayList<MyPolygon> currentLocationVenues = venuesCondensedDBDataSource
				.findVenuesContainingLocation(cell.latitude, cell.longitude);
			String semantic = null;
			if (!currentLocationVenues.isEmpty()) {
				semantic = currentLocationVenues.get(0).getSemantic();
				logVenue = currentLocationVenues.get(0);
				logVenueDistance = "inside";
			}

			//--> what if no venues contains the current location ? get the nearest location
			if (currentLocationVenues.isEmpty()) {
				Pair<MyPolygon, Double> nearestVenueAndDistance = venuesCondensedDBDataSource
					.findNearestVenue(cell.latitude, cell.longitude);
				if (nearestVenueAndDistance.first != null) {
					semantic = nearestVenueAndDistance.first.getSemantic();
					logVenue = nearestVenueAndDistance.first;
					logVenueDistance = "nearest";
				} else {
					logVenue = null;
					logVenueDistance = "nearest";
					semantic = null;
				}
			}

			//--> get user sensitivity of current location semantic
			if (semantic != null) {
				sensitivity = semanticLocationsDataSource.findSemanticSensitivity(semantic);
			} else {
				sensitivity = 0.0;
			}

			//--> logging
			log("No Location Sensitivity");
			if (logVenue != null) {
				log("Nearest Venue: " + logVenue.getName());
			}
			log("Relationship: " + logVenueDistance);
			log("Nearest Venue Query took " + (System.currentTimeMillis() - startGetNearVenues)
				+ " ms");
			log("Semantic: " + semantic);
			log("Semantic Sensitivity: " + sensitivity);
		}
		logSensitivity = sensitivity;

		log("Theta =  " + THETA);
		log("Theta * Sen =  " + (THETA * sensitivity));

		//===========================================================================================
		boolean finished = false;
		int lambda = 0;
		int ObfRegionHeightCells = getObfRegionHeightCells(lambda);
		int ObfRegionWidthCells = getObfRegionWidthCells(lambda);
		int numOfTrialsWithSameObfSize = 0;
		Pair<LatLng, LatLng> gridEnds = null;
		while (!finished) {

			log("----------------------------");
			long startLoopTime = System.currentTimeMillis();

			//--> Phase 1:
			// Generate obfuscation Region
			gridEnds = generateRandomObfRegion(location, ObfRegionHeightCells,
				ObfRegionWidthCells);
			log("Lambda: " + lambda);
			log("ObfRegionSize: " + ObfRegionHeightCells + "X" + ObfRegionWidthCells);
			logObfRegSize = ObfRegionHeightCells + "X" + ObfRegionWidthCells;

			//--> Phase 2:
			// Get feedback from the privacy estimator
			long timeStamp = time;
			LatLng[][] mapGrid = Utils.generateMapGrid(ObfRegionHeightCells, ObfRegionWidthCells, gridEnds.first);
			double privacyEstimation = privacyEstimator.calculatePrivacyEstimation(cell, mapGrid, timeStamp);
			log("Expected Distortion = " + privacyEstimation);

			//--> Phase3:
			// Comparison
			if (privacyEstimation > (THETA * sensitivity)) {
				finished = true;
			} else {
				numOfTrialsWithSameObfSize++;
				if (numOfTrialsWithSameObfSize >= ALPHA) {
					numOfTrialsWithSameObfSize = 0;
					lambda++;
					ObfRegionHeightCells = getObfRegionHeightCells(lambda);
					ObfRegionWidthCells = getObfRegionWidthCells(lambda);
				}
				if (ObfRegionHeightCells * ObfRegionWidthCells > MAX_OBF_REG_AREA) {
					finished = true;
					log("Terminating because obf region is too large ");
				}
			}

			//--> Phase4:
			// update likability graph
			if (finished) {
				privacyEstimator.saveLastLinkabilityGraphCopy();
			}

			//--> Logging
			logPrivacyEstimation = privacyEstimation;
			log("This loop took: " + (System.currentTimeMillis() - startLoopTime) + " ms");
		}
		logSensitivity("obfuscatedRegion:" + gridEnds.first.latitude + "," + gridEnds.first.longitude + "," + gridEnds.second.latitude + "," + gridEnds.second.longitude);
		logSensitivity("Theta*sensitivity:" + (THETA * sensitivity));
		logSensitivity("privacyEstimation:" + logPrivacyEstimation);

		//===========================================================================================

		LatLng obfRegionTopLeft = gridEnds.first;
		LatLng obfRegionBottomRight = gridEnds.second;

		//-->test
		logObfRegion = new ArrayList<>();
		LatLng[][] mapGrid = Utils.generateMapGrid(ObfRegionHeightCells, ObfRegionWidthCells, obfRegionTopLeft);
		for (LatLng[] positions : mapGrid) {
			for (LatLng position : positions) {
				MyPolygon currentPolygon = GridDBDataSource.getInstance(context).findGridCell(Utils.computeCellIDFromPosition(position));
				if (currentPolygon != null) {
					logObfRegion.add(currentPolygon);
				} else {
					logObfRegion.add(new MyPolygon(Utils.computeCellIDFromPosition(position) + "", "", Utils.computeCellCornerPoints(position)));
				}
			}
		}

		// Logging
		log("Total Adaptive Protection Time : "
			+ (System.currentTimeMillis() - startGetLocationTimeStamp) + " ms");
		log("Total logging time: " + totalLoggingTime + "ms");
		log("Total Adaptive Protection Time without logging : "
			+ (System.currentTimeMillis() - startGetLocationTimeStamp - totalLoggingTime)
			+ " ms");

		return new Pair<>(obfRegionTopLeft, obfRegionBottomRight);

	}

	private int getObfRegionWidthCells(int lambda) {
		//sx = 1+ Ceil(lambda/2)
		return 1 + (int) Math.ceil(lambda / 2.0);
	}

	private int getObfRegionHeightCells(int lambda) {
		//sy = 1+ Floor(lambda/2)
		return 1 + (int) Math.floor(lambda / 2.0);
	}

	private void log(String s) {
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			long startlogging = System.currentTimeMillis();
			Log.d(LOGTAG, s);
			Utils.createNewLoggingFolder(context, LOGTAG);
			Utils.appendLog(LOGTAG + ".txt", s, context);
			totalLoggingTime += (System.currentTimeMillis() - startlogging);
		}
	}

	private void logSensitivity(String s) {
		String tag = "SensitivityEstimation";
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			long startlogging = System.currentTimeMillis();
			Log.d(tag, s);
			Utils.appendLog(tag + ".txt", s, context);
			totalLoggingTime += (System.currentTimeMillis() - startlogging);
		}
	}

	private Pair<LatLng, LatLng> generateRandomObfRegion(LatLng cell,
														 int obfRegionHeightCells, int obfRegionWidthCells) {
		ArrayList<Integer> obfRegionCellIDs = new ArrayList<Integer>();

		int topLeftRow = random.nextInt(obfRegionHeightCells);
		int topLeftCol = random.nextInt(obfRegionWidthCells);

		LatLng topLeftPoint = Utils.findGridTopLeftPoint(cell, topLeftRow * 2, topLeftCol * 2);

		LatLng bottomRightPoint = Utils.findGridBottomRightPoint(topLeftPoint, obfRegionHeightCells, obfRegionWidthCells);

		return new Pair<>(topLeftPoint, bottomRightPoint);
	}

	//===========================================================================
	//--> GPS methods

	/**
	 * Google api callback methods
	 */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.i(LOGTAG, "Connection failed: ConnectionResult.getErrorCode() = "
			+ result.getErrorCode());
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		mGoogleApiClient.connect();
	}

	@Override
	public void onConnected(Bundle arg0) {

	}

	/**
	 * Creating google api client object
	 */
	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this.context)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.addApi(LocationServices.API).build();
	}

	/**
	 * Async Task to automatically download semantic informations
	 */
	private class DownloadSemanticMapAsyncTask extends AsyncTask<Pair<LatLng, LatLng>, Void, Pair<LatLng, LatLng>> {

		private boolean retValue;
		
		//The code to be executed in a background thread.
		@Override
		protected Pair<LatLng, LatLng> doInBackground(Pair<LatLng, LatLng>... params) {
			long start = System.currentTimeMillis();
			Pair<LatLng, LatLng> corners = params[0];
			//Get the current thread's token
			synchronized (this) {
				retValue = OSMWrapperAPI.updateSemanticLocations(context, corners.first, corners.second);
			}
			long end = System.currentTimeMillis();
			if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
				Log.d(LOGTAG, "Time to save automatic upload semantic area : " + (end - start) + " ms.");
			}
			return params[0];
		}

		//after executing the code in the thread
		@Override
		protected void onPostExecute(Pair<LatLng, LatLng> corners) {
			if (retValue) {
				// Remove area included in new area
				VenuesCondensedDBDataSource dbDataSource = VenuesCondensedDBDataSource.getInstance(context);
				ArrayList<Pair<LatLng, LatLng>> pairs = dbDataSource.findAllStoredSemanticArea();
				double latFirst = corners.first.latitude;
				double longFirst = corners.first.longitude;
				double latSecond = corners.second.latitude;
				double longSecond = corners.second.longitude;
				for (Pair<LatLng, LatLng> p : pairs) {
					double pLatFirst = p.first.latitude;
					double pLongFirst = p.first.longitude;
					double pLatSecond = p.second.latitude;
					double pLongSecond = p.second.longitude;
					if (pLatFirst <= latFirst && pLongFirst <= longFirst && pLatSecond >= latSecond && pLongSecond >= longSecond) {
						VenuesCondensedDBDataSource.getInstance(context).deleteSemanticArea(p);
					}
				}
				// Save new area into db
				dbDataSource.insertSemanticArea(corners.first, corners.second);
			}
			log("Loading semantic area finished");
		}
	}
}
