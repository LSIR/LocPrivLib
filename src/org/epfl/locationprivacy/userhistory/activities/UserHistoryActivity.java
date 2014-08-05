package org.epfl.locationprivacy.userhistory.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.privacyestimation.MyPrivacyEstimator;
import org.epfl.locationprivacy.privacyestimation.PrivacyEstimator;
import org.epfl.locationprivacy.userhistory.databases.LocationTableDataSource;
import org.epfl.locationprivacy.userhistory.databases.TransitionTableDataSource;
import org.epfl.locationprivacy.userhistory.models.Transition;
import org.epfl.locationprivacy.util.Utils;

import android.app.Activity;
import android.app.Dialog;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class UserHistoryActivity extends Activity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	private static final String LOGTAG = "UserHistoryActivity";
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	private static final int GPS_ERRORDIALOG_REQUEST = 9001;

	GoogleMap mMap;
	MapView mapView;
	LocationClient locationClient;
	Random random;
	GridDBDataSource gridDBDataSource;
	TransitionTableDataSource transitionTableDataSource;
	LocationTableDataSource locationTableDataSource;
	PrivacyEstimator privacyEstimator;

	int previousLocID = -1;
	int previousTimeID = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Random Number
		random = new Random();

		// grid DB
		gridDBDataSource = new GridDBDataSource(this);
		gridDBDataSource.open();

		// Transition Table
		transitionTableDataSource = new TransitionTableDataSource(this);
		transitionTableDataSource.open();

		// Location Table;
		locationTableDataSource = new LocationTableDataSource(this);
		locationTableDataSource.open();

		//privacy estimator
		privacyEstimator = new MyPrivacyEstimator(this);

		// Make sure that google play services are OK
		if (servicesOK()) {
			setContentView(R.layout.activity_userhistory);
			mapView = (MapView) findViewById(R.id.map);
			mapView.onCreate(savedInstanceState);

			if (initMap()) {
				locationClient = new LocationClient(this, this, this);
				locationClient.connect();
			} else {
				Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();
			}

		} else {
			Toast.makeText(this, "Google Play services Not OK", Toast.LENGTH_SHORT).show();
		}
	}

	public boolean servicesOK() {
		int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

		if (isAvailable == ConnectionResult.SUCCESS) {
			return true;
		} else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, this,
					GPS_ERRORDIALOG_REQUEST);
			dialog.show();
		} else {
			Toast.makeText(this, "Can't connect to google play services", Toast.LENGTH_SHORT)
					.show();
		}
		return false;
	}

	private boolean initMap() {
		mMap = mapView.getMap();
		return mMap != null;
	}

	//==================================================================
	@Override
	protected void onDestroy() {

		super.onDestroy();
		mapView.onDestroy();
		Toast.makeText(this, "On Destroy", Toast.LENGTH_SHORT).show();
		locationClient.removeLocationUpdates(this);
		gridDBDataSource.close();
		transitionTableDataSource.close();
		locationTableDataSource.close();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mapView.onPause();
		Toast.makeText(this, "On Pause", Toast.LENGTH_SHORT).show();
		locationClient.removeLocationUpdates(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	//==================================================================
	@Override
	public void onConnectionFailed(ConnectionResult arg0) {

	}

	@Override
	public void onDisconnected() {

	}

	@Override
	public void onConnected(Bundle arg0) {
		Toast.makeText(this, "Connected to location service", Toast.LENGTH_SHORT).show();

		// draw grid
		// top Left corner
		int gridHeightCells = 101;//must be odd number [1,3,5,...,15]
		int gridWidthCells = 301; //must be odd number [1,3,5,...,15]
		LatLng centerPoint = new LatLng(46.526092, 6.584415);
		LatLng topLeftPoint = Utils.findTopLeftPoint(centerPoint, gridHeightCells, gridWidthCells);
		refreshMapGrid(gridHeightCells, gridWidthCells, topLeftPoint);

		MapsInitializer.initialize(this);

		LocationRequest locationRequest = LocationRequest.create();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locationRequest.setInterval(30000);
		locationRequest.setFastestInterval(30000);
		locationClient.requestLocationUpdates(locationRequest, this);
	}

	//==================================================================
	@Override
	public void onLocationChanged(Location location) {

		// random number
		double minLat = 46.508463;
		double maxLat = 46.529253;
		double minLng = 6.606302;
		double maxLng = 6.655912;

		double randomLatitude = minLat + (maxLat - minLat) * random.nextDouble();
		double randomLongitude = minLng + (maxLng - minLng) * random.nextDouble();
		location.setLatitude(randomLatitude);
		location.setLongitude(randomLongitude);

		//Output current Location
		String msg = "Location : " + location.getLatitude() + "," + location.getLongitude();

		//Animate to current Location
		LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12);
		mMap.moveCamera(cameraUpdate);

		// Query Grid
		MyPolygon currLocPolygon = gridDBDataSource.findGridCell(randomLatitude, randomLongitude);
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		Utils.drawPolygon(currLocPolygon, mMap);

		// Query nearby locations

		// Adding location sample to Location table
		long currTime = System.currentTimeMillis();
		org.epfl.locationprivacy.userhistory.models.Location newLocation = new org.epfl.locationprivacy.userhistory.models.Location(
				location.getLatitude(), location.getLongitude(), currTime);
		locationTableDataSource.create(newLocation);

		//==== Testing
		Log.d(LOGTAG, "No of locations: " + locationTableDataSource.countRows() + " Rows");
		for (org.epfl.locationprivacy.userhistory.models.Location l : locationTableDataSource
				.findAll())
			Log.d(LOGTAG, l.toString());

		// Adding transition to transition table
		int currLocID = Integer.parseInt(currLocPolygon.getName());
		int currTimeID = Utils.findDayPortionID(currTime);
		if (previousLocID != -1) {
			Transition newTransition = new Transition(previousLocID, currLocID, previousTimeID,
					currTimeID, 1);
			transitionTableDataSource.updateOrInsert(newTransition);

			//==== Testing
			Log.d(LOGTAG, "No of transitios: " + transitionTableDataSource.countRows() + " Rows");
			for (Transition t : transitionTableDataSource.findAll())
				Log.d(LOGTAG, t.toString());
		}
		previousLocID = currLocID;
		previousTimeID = currTimeID;

		//Adding Marker
		String timeStamp = dateFormat.format(new Date());
		String markerTitle = timeStamp + " " + latLng.toString();
		MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(latLng);
		mMap.addMarker(markerOptions);

	}

	private void refreshMapGrid(int heightCells, int widthCells, LatLng topLeftPoint) {

		// Generate Map Grid
		int arrRows = heightCells + 1;
		int arrCols = widthCells + 1;
		LatLng[][] mapGrid = Utils.generateMapGrid(arrRows, arrCols, topLeftPoint);

		//Draw new grid on map
		Utils.drawMapGrid(mapGrid, mMap);
		Utils.drawObfuscationArea(mapGrid, mMap);
	}
}
