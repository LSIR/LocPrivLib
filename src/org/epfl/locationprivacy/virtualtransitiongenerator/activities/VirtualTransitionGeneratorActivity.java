package org.epfl.locationprivacy.virtualtransitiongenerator.activities;

import java.util.Random;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.databases.VenuesCondensedDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.util.Utils;

import android.app.Activity;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;

public class VirtualTransitionGeneratorActivity extends Activity {

	public static String LOGTAG = "VirtualRouteGeneratorActivity";
	GoogleMap googleMap;
	MapView mapView;
	Random rand = new Random();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Make sure that google play services are OK
		if (Utils.googlePlayServicesOK(this)) {
			setContentView(R.layout.activity_virtualtransitiongenerator);
			mapView = (MapView) findViewById(R.id.virtualtransitionmap);
			mapView.onCreate(savedInstanceState);

			if (initMap()) {
				//Move Camera to Laussane
				try {
					MapsInitializer.initialize(this);
				} catch (Exception e) {
					e.printStackTrace();
				}
				LatLng laussaneLocation = new LatLng(46.520912, 6.633983);
				CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(laussaneLocation, 13);
				googleMap.moveCamera(cameraUpdate);
			} else {
				Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();
			}

		} else {
			Toast.makeText(this, "Google Play Service Not Available", Toast.LENGTH_SHORT).show();
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
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	//=========================================

	public void generateVirtualTransition(View view) {
		Toast.makeText(this, "Calling Google API ...", Toast.LENGTH_LONG).show();

		// query 2 random venues
		VenuesCondensedDBDataSource venuesCondensedDBDataSource = VenuesCondensedDBDataSource
				.getInstance(this);
		Pair<MyPolygon, LatLng> originInfo = venuesCondensedDBDataSource
				.findRandomPolygonWithItsCentroid();
		Pair<MyPolygon, LatLng> destinationInfo = venuesCondensedDBDataSource
				.findRandomPolygonWithItsCentroid();

		// ask google direction api
		askGoogle(originInfo, destinationInfo, googleMap);
	}

	GoogleDirectionAPI lastGoogleRequest = null;

	private void askGoogle(Pair<MyPolygon, LatLng> originInfo,
			Pair<MyPolygon, LatLng> destinationInfo, GoogleMap map) {
		String key = "AIzaSyBLvMIGkdMW7-v_pEsM7NX3dVKfciAFTLw";

		LatLng originLatLng = originInfo.second;
		LatLng destinationLatLng = destinationInfo.second;

		String origin = originLatLng.latitude + "," + originLatLng.longitude;
		String destination = destinationLatLng.latitude + "," + destinationLatLng.longitude;

		String[] transportationArray = { "walking", "bicycling", "driving" };
		String randomTransportation = transportationArray[rand.nextInt(transportationArray.length)]; 
		
		String url = "https://maps.googleapis.com/maps/api/directions/json?" + "origin=" + origin
				+ "&destination=" + destination + "&mode="
				+ randomTransportation + "&key=" + key;

		if (lastGoogleRequest != null)
			lastGoogleRequest.clearMap();

		lastGoogleRequest = new GoogleDirectionAPI(map, GridDBDataSource.getInstance(this),
				originInfo, destinationInfo, randomTransportation);
		lastGoogleRequest.execute(url);

	}
}
