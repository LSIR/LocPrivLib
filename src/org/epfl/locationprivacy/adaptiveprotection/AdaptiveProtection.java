package org.epfl.locationprivacy.adaptiveprotection;

import java.util.ArrayList;

import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.privacyestimation.PrivacyEstimator;
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

	public AdaptiveProtection(Context context) {
		super();
		this.context = context;
		this.privacyEstimator = new PrivacyEstimator(context);
		this.locationClient = new LocationClient(context, this, this);
		locationClient.connect();
	}

	@Override
	public ArrayList<MyPolygon> getLocation() {

		//preparation
		Log.d(LOGTAG, "================================");
		long startGetLocation = System.currentTimeMillis();
		GridDBDataSource gridDBDataSource = new GridDBDataSource(context);
		gridDBDataSource.open();

		// get current location
		Location location = locationClient.getLastLocation();
		if (location == null) {
			Toast.makeText(context, "Location is NULL", Toast.LENGTH_SHORT).show();
			return null;
		}
		Log.d(LOGTAG,
				"Current Location : " + location.getLatitude() + ":" + location.getLongitude());

		// Generate obfuscation Region
		//--> current Location ID
		long start = System.currentTimeMillis();
		MyPolygon currLocPolygon = gridDBDataSource.findGridCell(location.getLatitude(),
				location.getLongitude());
		int fineLocationID = Integer.parseInt(currLocPolygon.getName());
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

		// Get feedback from the privacy estimator
		long timeStamp = System.currentTimeMillis();
		//		double privacyEstimation = privacyEstimator.calculatePrivacyEstimation(fineLocationID,
		//				obfRegionCellIDs, timeStamp);

		// Convert cellIDs to the polygons forming the obfRegion
		ArrayList<MyPolygon> obfRegionPolygons = new ArrayList<MyPolygon>();
		for (Integer cellID : obfRegionCellIDs) {
			obfRegionPolygons.add(gridDBDataSource.findGridCell(cellID));
		}

		//free resources
		gridDBDataSource.close();
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
