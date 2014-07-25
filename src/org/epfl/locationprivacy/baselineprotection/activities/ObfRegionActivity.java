package org.epfl.locationprivacy.baselineprotection.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.baselineprotection.util.Utils;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
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
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, OnSeekBarChangeListener {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	GoogleMap googleMap;
	MapView mapView;
	LocationClient locationClient;
	ArrayList<Polyline> polylines = new ArrayList<Polyline>();
	Polygon polygon = null;
	//	int currGridSize = 1;
	int currGridHeightCells = 1;
	int currGridWidthCells = 1;
	LatLng[][] mapGrid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Make sure that google play services are OK
		if (Utils.googlePlayServicesOK(this)) {
			setContentView(R.layout.activity_baselineprotection_obfregion);
			mapView = (MapView) findViewById(R.id.map);
			mapView.onCreate(savedInstanceState);

			if (initMap()) {
				locationClient = new LocationClient(this, this, this);
				locationClient.connect();
			} else {
				Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();
			}

		} else {
			Toast.makeText(this, "Google Play Service Not Available", Toast.LENGTH_SHORT).show();
		}

		//Seekbar
		SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);
		seekBar.setOnSeekBarChangeListener(this);
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
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	//=============================================================================
	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
	}

	@Override
	public void onDisconnected() {
	}

	@Override
	public void onConnected(Bundle arg0) {
		Toast.makeText(this, "Connected to location service", Toast.LENGTH_SHORT).show();

		MapsInitializer.initialize(this);

		Location currentLocation = locationClient.getLastLocation();
		if (currentLocation != null) {

			currentLocation.setLatitude(46.526092);
			currentLocation.setLongitude(6.584415);

			//Animate
			LatLng latLng = new LatLng(currentLocation.getLatitude(),
					currentLocation.getLongitude());
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 13);
			googleMap.animateCamera(cameraUpdate);

			//Adding Marker
			String timeStamp = dateFormat.format(new Date());
			String markerTitle = timeStamp + " " + latLng.toString();
			MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(latLng);
			googleMap.addMarker(markerOptions);

			//Draw Grid
			refreshMapGrid(currGridHeightCells, currGridWidthCells, currentLocation);

		} else {
			Toast.makeText(this, "Current Location is not available", Toast.LENGTH_SHORT).show();
		}
	}

	//=============================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_obfuscate) {
			Intent intent = new Intent(ObfRegionActivity.this, CoarseLocationActivity.class);
			intent.putExtra("fineLocationLat", locationClient.getLastLocation().getLatitude());
			intent.putExtra("fineLocationLng", locationClient.getLastLocation().getLongitude());

			int obfuscationRegionHeightCells = currGridHeightCells / 2 + 1;
			int obfuscationRegionWidthCells = currGridWidthCells / 2 + 1;
			intent.putExtra("obfuscationRegionHeightCells", obfuscationRegionHeightCells);
			intent.putExtra("obfuscationRegionWidthCells", obfuscationRegionWidthCells);

			//generate random obfuscation region
			int randomRow = Utils.getRandom(0, currGridHeightCells / 2);
			int randomCol = Utils.getRandom(0, currGridWidthCells / 2);
			intent.putExtra("obfuscationRegionTopLeftLat", mapGrid[randomRow][randomCol].latitude);
			intent.putExtra("obfuscationRegionTopLeftLng", mapGrid[randomRow][randomCol].longitude);

			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	//==============================================================================

	private void refreshMapGrid(int gridHeightCells, int gridWidthCells, Location currentLocation) {
		// top Left corner
		LatLng centerPoint = new LatLng(currentLocation.getLatitude(),
				currentLocation.getLongitude());
		LatLng topLeftPoint = Utils.findTopLeftPoint(centerPoint, gridHeightCells, gridWidthCells);

		// generate Map Grid
		int arrRows = gridHeightCells + 1;
		int arrCols = gridWidthCells + 1;
		mapGrid = Utils.generateMapGrid(arrRows, arrCols, topLeftPoint);

		//Remove old grid from map
		Utils.removeOldMapGrid(polylines, polygon);

		//Draw new grid on map
		polylines = Utils.drawMapGrid(mapGrid, googleMap);
		polygon = Utils.drawObfuscationArea(mapGrid, googleMap);
	}

	//====================================================================
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		//Toast.makeText(this, "Seek bar value " + seekBar.getProgress(), Toast.LENGTH_SHORT).show();
		currGridWidthCells = 2 * seekBar.getProgress() + 1;
		currGridHeightCells = 2 * seekBar.getProgress() + 1;
		refreshMapGrid(currGridHeightCells, currGridWidthCells, locationClient.getLastLocation());
	}
}
