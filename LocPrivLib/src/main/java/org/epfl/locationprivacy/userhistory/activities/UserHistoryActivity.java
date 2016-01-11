package org.epfl.locationprivacy.userhistory.activities;

import java.util.ArrayList;
import java.util.Random;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.userhistory.databases.LocationTableDataSource;
import org.epfl.locationprivacy.userhistory.databases.TransitionTableDataSource;
import org.epfl.locationprivacy.userhistory.services.LocationTrackingService;
import org.epfl.locationprivacy.util.Utils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class UserHistoryActivity extends ActionBarActivity implements
		GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	private static final String LOGTAG = "UserHistoryActivity";

	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;
	GoogleMap googleMap;
	MapView mapView;
	Random random;
	ArrayList<Marker> markers;

	int previousLocID = -1;
	int previousTimeID = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Random Number
		random = new Random();

		// markers
		markers = new ArrayList<Marker>();

		setContentView(R.layout.activity_userhistory);
		mapView = (MapView) findViewById(R.id.userhistorymap);
		mapView.onCreate(savedInstanceState);

		if (initMap()) {
			// First we need to check availability of play services
			if (Utils.checkPlayServices(this, this.getApplicationContext())) {

				// Building the GoogleApi client
				buildGoogleApiClient();
			}
		} else {
			Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();
		}

		//====================================================================

		// read pereference
		SharedPreferences prefs = getSharedPreferences("org.epfl.locationprivacy", MODE_PRIVATE);
		boolean locationtracking = prefs.getBoolean("locationtracking", true);

		// set toggle button
		ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton1);
		toggle.setChecked(locationtracking);

	}

	public void toggleButtonClicked(View view) {
		ToggleButton toggle = (ToggleButton) view.findViewById(R.id.toggleButton1);

		SharedPreferences prefs = getSharedPreferences("org.epfl.locationprivacy", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("locationtracking", toggle.isChecked()).apply();

		if (toggle.isChecked()) {
			startService(new Intent(this, LocationTrackingService.class));
		} else {
			stopService(new Intent(this, LocationTrackingService.class));
		}
	}

	private boolean initMap() {
		googleMap = mapView.getMap();
		return googleMap != null;
	}

	//==================================================================
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
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	//==================================================================

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
		Log.i(LOGTAG, "Connection failed: ConnectionResult.getErrorCode() = "
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

		refreshMap();
	}

	private void refreshMap() {

		// open dbs
		TransitionTableDataSource transitionTableDataSource = TransitionTableDataSource
				                                                      .getInstance(this);
		LocationTableDataSource locationTableDataSource = LocationTableDataSource.getInstance(this);

		// remove old markers
		for (Marker m : markers) {
			m.remove();
		}
		markers.clear();

		// adding new markers
		ArrayList<org.epfl.locationprivacy.userhistory.models.Location> locations = locationTableDataSource
				                                                                            .findAll();
		LatLng previousPoint = null;
		for (org.epfl.locationprivacy.userhistory.models.Location l : locations) {
			//Adding Marker
			String markerTitle = l.latitude + ":" + l.longitude;
			LatLng currPoint = new LatLng(l.latitude, l.longitude);
			MarkerOptions markerOptions = new MarkerOptions().title(markerTitle)
					                              .position(currPoint);
			markers.add(googleMap.addMarker(markerOptions));

			// adding line
			if (previousPoint != null) {
				Polyline line = googleMap.addPolyline(new PolylineOptions()
						                                      .add(previousPoint, currPoint).width(5).color(Color.BLUE));
				markers.add(Utils.DrawArrowHead(googleMap, previousPoint, currPoint));
			}
			previousPoint = currPoint;
		}
	}

	//=============================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_userhistory, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_refresh) {
			refreshMap();
		}
		return super.onOptionsItemSelected(item);
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
