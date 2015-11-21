package org.epfl.locationprivacy.baselineprotection.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.util.Utils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;

public class ObfRegionActivity extends ActionBarActivity implements
	GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	private static final int REQUEST_CODE = 100;
	private static final String TAG = "ObfRegionActivity";

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;
	GoogleMap googleMap;
	MapView mapView;
	Location location;
	ArrayList<Polyline> polylines = new ArrayList<Polyline>();
	Polygon polygon = null;
	Location currentLocation = null;
	// currGridHeightCells and currGridWidthCells are 2 times bigger than the obfuscation region
	// This is for drawing correctly the random area
	int currGridHeightCells = 1;
	int currGridWidthCells = 1;
	LatLng[][] mapGrid = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// read share preferences
		SharedPreferences prefs = getSharedPreferences("org.epfl.locationprivacy", MODE_PRIVATE);
		currGridHeightCells = prefs.getInt("ObfRegionHeightCells", 1) * 2 - 1;
		currGridWidthCells = prefs.getInt("ObfRegionWidthCells", 1) * 2 - 1;

		setContentView(R.layout.activity_baselineprotection_obfregion);
		mapView = (MapView) findViewById(R.id.currlocationmap);
		mapView.onCreate(savedInstanceState);

		if (initMap()) {
			// First we need to check availability of play services
			if (Utils.checkPlayServices(this, this.getApplicationContext())) {

				// Building the GoogleApi client
				buildGoogleApiClient();
			}

			location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
			if (location == null) {
				Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();
		}
	}

	private boolean initMap() {
		googleMap = mapView.getMap();
		return googleMap != null;
	}

	//=============================================================================
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
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
	}

	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
		Utils.checkPlayServices(this, this.getApplicationContext());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	//=============================================================================

	@Override
	protected void onStart() {
		super.onStart();
		if (mGoogleApiClient != null) {
			mGoogleApiClient.connect();
		}
	}

	/**
	 * Google api callback methods
	 */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
			+ result.getErrorCode());
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		mGoogleApiClient.connect();
	}


	@Override
	public void onConnected(Bundle arg0) {
		Toast.makeText(this, "Connected to location service", Toast.LENGTH_SHORT).show();

		MapsInitializer.initialize(this);

		currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (currentLocation != null) {

			//Animate
			LatLng latLng = new LatLng(currentLocation.getLatitude(),
				currentLocation.getLongitude());
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14);
			googleMap.moveCamera(cameraUpdate);

			//Adding Marker
			String timeStamp = dateFormat.format(new Date());
			String markerTitle = timeStamp + " " + latLng.toString();
			MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(latLng);
			googleMap.addMarker(markerOptions);

			// top Left corner
			LatLng centerPoint = new LatLng(currentLocation.getLatitude(),
				currentLocation.getLongitude());
			LatLng topLeftPoint = Utils.findGridTopLeftPoint(centerPoint, currGridHeightCells, currGridWidthCells);

			// generate Map Grid
			int arrRows = currGridHeightCells;
			int arrCols = currGridWidthCells;
			mapGrid = Utils.generateMapGrid(arrRows, arrCols, topLeftPoint);

			// obfuscation region size
			int obfuscationRegionHeightCells = currGridHeightCells / 2 + 1;
			int obfuscationRegionWidthCells = currGridWidthCells / 2 + 1;

			// top Left corner for the obfuscation region
			LatLng obfRegionTopLeftPoint = Utils.findGridTopLeftPoint(centerPoint, obfuscationRegionHeightCells, obfuscationRegionWidthCells);

			// refresh map
			refreshMapGrid(obfuscationRegionHeightCells, obfuscationRegionWidthCells,
				obfRegionTopLeftPoint);

		} else {
			Toast.makeText(this, "Current Location is not available, Can't Access GPS data", Toast.LENGTH_LONG).show();
		}
	}

	//=============================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_baselineprotection_obfregion, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (currentLocation != null) {

			int id = item.getItemId();
			if (id == R.id.action_obfuscate) {

				// obfuscation region size
				int obfuscationRegionHeightCells = currGridHeightCells / 2 + 1;
				int obfuscationRegionWidthCells = currGridWidthCells / 2 + 1;

				//generate random  top left point for the obfuscation region
				int randomRow = Utils.getRandom(0, currGridHeightCells / 2);
				int randomCol = Utils.getRandom(0, currGridWidthCells / 2);
				LatLng obfuscationRegionTopLeftPoint = new LatLng(mapGrid[randomRow][randomCol].latitude, mapGrid[randomRow][randomCol].longitude);

				//refresh map
				refreshMapGrid(obfuscationRegionHeightCells, obfuscationRegionWidthCells,
					obfuscationRegionTopLeftPoint);
				return true;

			} else if (id == R.id.action_settings) {
				Intent intent = new Intent(ObfRegionActivity.this, ObfRegionSettingActivity.class);
				startActivityForResult(intent, REQUEST_CODE);
				return true;
			}
		} else {
			Toast.makeText(this, "Sorry Can't Get the current location, Turn On the GPS",
				Toast.LENGTH_LONG).show();
		}
		return super.onOptionsItemSelected(item);
	}

	//========================================================================
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

			int obfuscationRegionHeightCells = data.getIntExtra("ObfRegionHeightCells", 1);
			int obfuscationRegionWidthCells = data.getIntExtra("ObfRegionWidthCells", 1);

			currGridHeightCells = obfuscationRegionHeightCells * 2 - 1;
			currGridWidthCells = obfuscationRegionWidthCells * 2 - 1;

			// top Left corner for the grid
			LatLng centerPoint = new LatLng(currentLocation.getLatitude(),
				currentLocation.getLongitude());
			LatLng gridTopLeftPoint = Utils.findGridTopLeftPoint(centerPoint, currGridHeightCells, currGridWidthCells);

			// generate Map Grid
			int arrRows = currGridHeightCells;
			int arrCols = currGridWidthCells;
			mapGrid = Utils.generateMapGrid(arrRows, arrCols, gridTopLeftPoint);

			// top Left corner for the obfuscation region
			LatLng obfRegionTopLeftPoint = Utils.findGridTopLeftPoint(centerPoint, obfuscationRegionHeightCells, obfuscationRegionWidthCells);

			// refresh map
			refreshMapGrid(obfuscationRegionHeightCells, obfuscationRegionWidthCells,
				obfRegionTopLeftPoint);

			Toast.makeText(this,
				"Returned " + obfuscationRegionHeightCells + " " + obfuscationRegionWidthCells,
				Toast.LENGTH_SHORT).show();
		}
	}

	//==============================================================================

	private void refreshMapGrid(int heightCells, int widthCells, LatLng topLeftPoint) {

		// generate Map Grid
		int arrRows = heightCells;
		int arrCols = widthCells;
		LatLng[][] mapGrid = Utils.generateMapGrid(arrRows, arrCols, topLeftPoint);

		//Remove old grid from map
		Utils.removeOldMapGrid(polylines, polygon);

		//Draw new grid on map
		polylines = Utils.drawMapGrid(mapGrid, googleMap);
		polygon = Utils.drawObfuscationArea(mapGrid, googleMap);
	}

	/**
	 * Creating google api client object
	 */
	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.addApi(LocationServices.API).build();
	}
}
