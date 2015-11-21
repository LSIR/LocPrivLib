package org.epfl.locationprivacy.thirdpartyemulator;

import java.util.ArrayList;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.adaptiveprotection.AdaptiveProtection;
import org.epfl.locationprivacy.adaptiveprotection.AdaptiveProtectionInterface;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.privacyestimation.databases.LinkabilityGraphDataSource;
import org.epfl.locationprivacy.util.Utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;

public class ThirdPartyActivity extends ActionBarActivity {

	private static final String LOGTAG = "ThirdPartyActivity";
	AdaptiveProtectionInterface adaptiveProtectionInterface;
	GoogleMap googleMap;
	MapView mapView;
	ArrayList<Polygon> polygons;
	ArrayList<Polyline> polylines = new ArrayList<Polyline>();
	Polygon polygon = null;
	ArrayList<Marker> markers;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Initialize adaptive protection
		adaptiveProtectionInterface = new AdaptiveProtection(this);

		// Initialize Google Maps
		setContentView(R.layout.activity_thirdparty);
		mapView = (MapView) findViewById(R.id.thirdpartymapview);
		mapView.onCreate(savedInstanceState);

		if (initMap()) {
			MapsInitializer.initialize(this);
		} else {
			Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();
		}

		// Initialize Polygons
		polygons = new ArrayList<Polygon>();
		markers = new ArrayList<Marker>();
	}

	private boolean initMap() {
		googleMap = mapView.getMap();
		return googleMap != null;
	}

	//==================================================
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

	//==================================================
	public void emulateThirdParty(View view) {

		// Mock Data1
		ArrayList<LatLng> mockLocations = new ArrayList<LatLng>();

		if (view.getId() == R.id.thirdpartytestlocations) {
			// cell ids : 10367, 17609, 11003
			mockLocations.add(new LatLng(46.533114299838836, 6.573491469025612));
			mockLocations.add(new LatLng(46.522422470139205, 6.585019938647747));
			mockLocations.add(new LatLng(46.53251253519775, 6.595497317612171));
		} else if (view.getId() == R.id.thirdpartytestsemantics) {
			mockLocations.add(new LatLng(46.5192, 6.5661)); // university
			mockLocations.add(new LatLng(46.5212, 6.6320)); // bar
			mockLocations.add(new LatLng(46.5253, 6.6421)); // hospital
		}

		// Clean the previous experiment if exists
		Utils.removePolygons(polygons);
		Utils.removeMarkers(markers);

		//Create Logging folder
		Utils.createNewLoggingFolder(view.getContext());

		long startExperiment = System.currentTimeMillis();
		for (int index = 0; index < mockLocations.size(); index++) {

			//create logging folder for this location
			Utils.createNewLoggingSubFolder(view.getContext());

			// get obf location
			LatLng mockLocation = mockLocations.get(index);

			Pair<LatLng, LatLng> obfRegionBoundaries = adaptiveProtectionInterface
				.getObfuscationLocation(mockLocation);

			// Draw obfuscation Region
			for (MyPolygon p : AdaptiveProtection.logObfRegion) {
				polygons.add(Utils.drawPolygon(p, googleMap, 0x00000000));
				polygons.add(Utils.drawPolygon(p, googleMap, 0x55FF0000));
			}

			// adding marker for the current Location
			MarkerOptions markerOptions = new MarkerOptions()
				.title("CurrentLocation")
				.snippet("ObfRegion Size: " + AdaptiveProtection.logObfRegSize)
				.position(
					new LatLng(AdaptiveProtection.logCurrentLocation.latitude,
						AdaptiveProtection.logCurrentLocation.longitude));
			markers.add(googleMap.addMarker(markerOptions));

			// draw nearest venue
			if (view.getId() == R.id.thirdpartytestsemantics && AdaptiveProtection.logVenue != null) {
				polygons.add(Utils.drawPolygon(AdaptiveProtection.logVenue, googleMap, 0x3300FF00));
				MarkerOptions markerOptions2 = new MarkerOptions()
					.title("Name: " + AdaptiveProtection.logVenue.getName())
					.snippet(
						"Tag: " + AdaptiveProtection.logVenue.getSemantic()
							+ " Sensitivity: " + AdaptiveProtection.logSensitivity)
					.position(
						new LatLng(AdaptiveProtection.logVenue.getPoints().get(0).latitude,
							AdaptiveProtection.logVenue.getPoints().get(0).longitude));
				markers.add(googleMap.addMarker(markerOptions2));
			}

			//testing
			Log.d(LOGTAG, "LG Events: "
				+ LinkabilityGraphDataSource.getInstance(this).countEventRows() + " LG Edges: "
				+ LinkabilityGraphDataSource.getInstance(this).countParentChildrenRows());

			Log.d(LOGTAG, "Finished mock location number: " + (index + 1));
		}
		Toast.makeText(
			this,
			"Finished experiment in " + (System.currentTimeMillis() - startExperiment) / 1000
				+ " sec", Toast.LENGTH_SHORT).show();
	}
}
