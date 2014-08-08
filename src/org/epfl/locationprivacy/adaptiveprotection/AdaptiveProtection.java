package org.epfl.locationprivacy.adaptiveprotection;

import java.util.ArrayList;
import java.util.Random;

import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.databases.VenuesCondensedDBDataSource;
import org.epfl.locationprivacy.map.databases.VenuesDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.privacyestimation.PrivacyEstimator;
import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;
import org.epfl.locationprivacy.util.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.model.LatLng;

public class AdaptiveProtection implements AdaptiveProtectionInterface,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	private static final String LOGTAG = "AdaptiveProtection";
	PrivacyEstimator privacyEstimator;
	Context context;
	LocationClient locationClient;
	Random random;

	public static Location logCurrentLocation;
	public static MyPolygon logVenue;
	public static String logVenueDistance;
	public static Double logSensitivity;

	public AdaptiveProtection(Context context) {
		super();
		this.context = context;
		this.privacyEstimator = new PrivacyEstimator(context);
		this.locationClient = new LocationClient(context, this, this);
		locationClient.connect();
		random = new Random();
	}

	@Override
	public ArrayList<MyPolygon> getLocation() {

		//preparation
		Log.d(LOGTAG, "================================");
		long startGetLocation = System.currentTimeMillis();
		GridDBDataSource gridDBDataSource = GridDBDataSource.getInstance(context);
		VenuesCondensedDBDataSource venuesCondensedDBDataSource = VenuesCondensedDBDataSource
				.getInstance(context);
		SemanticLocationsDataSource semanticLocationsDataSource = SemanticLocationsDataSource
				.getInstance(context);

		//===========================================================================================
		// get current location
		Location location = locationClient.getLastLocation();
		if (location == null) {
			Toast.makeText(context, "Location is NULL", Toast.LENGTH_SHORT).show();
			return null;
		}
		Log.d(LOGTAG,
				"Current Location : " + location.getLatitude() + ":" + location.getLongitude());
		//--> overriding actual location with  random number
		double minLat = 46.508463;
		double maxLat = 46.529253;
		double minLng = 6.606302;
		double maxLng = 6.655912;

		double randomLatitude = minLat + (maxLat - minLat) * random.nextDouble();
		double randomLongitude = minLng + (maxLng - minLng) * random.nextDouble();
		location.setLatitude(randomLatitude);
		location.setLongitude(randomLongitude);
		logCurrentLocation = location;

		//===========================================================================================
		// get semantics of current location
		ArrayList<MyPolygon> currentLocationVenues = venuesCondensedDBDataSource
				.findVenuesContainingLocation(location.getLatitude(), location.getLongitude());
		String semantic = null;
		if (!currentLocationVenues.isEmpty()) {
			semantic = currentLocationVenues.get(0).getSemantic();
			logVenue = currentLocationVenues.get(0);
			logVenueDistance = "inside";
		}

		//--> what if no venues contains the current location ? get the nearest location
		if (currentLocationVenues.isEmpty()) {
			Pair<MyPolygon, Double> nearestVenueAndDistance = venuesCondensedDBDataSource
					.findNearestVenue(location.getLatitude(), location.getLongitude());
			semantic = nearestVenueAndDistance.first.getSemantic();
			logVenue = nearestVenueAndDistance.first;
			logVenueDistance = "nearest";
		}

		//===========================================================================================
		// get user sensitivity of current location semantic
		Double sensitivity = semanticLocationsDataSource.findSemanticSensitivity(semantic);
		logSensitivity = sensitivity;
		Log.d(LOGTAG, "Sensitivity: " + sensitivity);

		//===========================================================================================
		// Generate obfuscation Region
		//--> current Location ID
		long start = System.currentTimeMillis();
		MyPolygon currLocGridCell = gridDBDataSource.findGridCell(location.getLatitude(),
				location.getLongitude());
		int fineLocationID = Integer.parseInt(currLocGridCell.getName());
		Log.d(LOGTAG, "Getting fine Location ID took: " + (System.currentTimeMillis() - start)
				+ " ms");

		//--> obf Location IDS
		SharedPreferences prefs = context.getSharedPreferences("org.epfl.locationprivacy",
				context.MODE_PRIVATE);
		int ObfRegionHeightCells = prefs.getInt("ObfRegionHeightCells", 1);
		int ObfRegionWidthCells = prefs.getInt("ObfRegionWidthCells", 1);
		ArrayList<Integer> obfRegionCellIDs = generateRandomObfRegion(fineLocationID,
				ObfRegionHeightCells, ObfRegionWidthCells);
		Log.d(LOGTAG, "ObfRegionSize" + obfRegionCellIDs.size());

		//===========================================================================================
		// Get feedback from the privacy estimator
		long timeStamp = System.currentTimeMillis();
		//		double privacyEstimation = privacyEstimator.calculatePrivacyEstimation(fineLocationID,
		//				obfRegionCellIDs, timeStamp);

		//===========================================================================================
		// Convert cellIDs to the polygons forming the obfRegion
		ArrayList<MyPolygon> obfRegionPolygons = new ArrayList<MyPolygon>();
		for (Integer cellID : obfRegionCellIDs) {
			obfRegionPolygons.add(gridDBDataSource.findGridCell(cellID));
		}

		Log.d(LOGTAG, "Total Adaptive Protection Time: "
				+ (System.currentTimeMillis() - startGetLocation));

		return obfRegionPolygons;
	}

	private ArrayList<Integer> generateRandomObfRegion(int fineLocationID,
			int obfRegionHeightCells, int obfRegionWidthCells) {
		ArrayList<Integer> obfRegionCellIDs = new ArrayList<Integer>();

		//curr row and col
		int currRow = fineLocationID / Utils.LAUSSANE_GRID_WIDTH_CELLS;
		int currCol = fineLocationID % Utils.LAUSSANE_GRID_WIDTH_CELLS;

		// top left cell id
		int topLeftRow = currRow - (obfRegionHeightCells / 2);
		topLeftRow = topLeftRow < 0 ? 0 : topLeftRow;
		int topLeftCol = currCol - (obfRegionWidthCells / 2);
		topLeftCol = topLeftCol < 0 ? 0 : topLeftCol;

		// bottom right cell id
		int bottomRightRow = topLeftRow + obfRegionHeightCells - 1;
		bottomRightRow = bottomRightRow >= Utils.LAUSSANE_GRID_HEIGHT_CELLS ? Utils.LAUSSANE_GRID_HEIGHT_CELLS - 1
				: bottomRightRow;
		int bottomRightCol = topLeftCol + obfRegionWidthCells - 1;
		bottomRightCol = bottomRightCol >= Utils.LAUSSANE_GRID_WIDTH_CELLS ? Utils.LAUSSANE_GRID_WIDTH_CELLS - 1
				: bottomRightCol;

		// generate cell ids
		for (int r = topLeftRow; r <= bottomRightRow; r++)
			for (int c = topLeftCol; c <= bottomRightCol; c++)
				obfRegionCellIDs.add(r * Utils.LAUSSANE_GRID_WIDTH_CELLS + c);

		return obfRegionCellIDs;
	}

	//===========================================================================
	@Override
	public void onConnected(Bundle arg0) {
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
	}

	@Override
	public void onDisconnected() {
	}
}
