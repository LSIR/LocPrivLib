package org.epfl.locationprivacy.spatialitedb;

import java.util.ArrayList;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.map.databases.VenuesDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.util.Utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

public class SampleSpatialiteQueryActivity extends Activity {

	public static String LOGTAG = "SampleSpatialiteQueryActivity";

	GoogleMap googleMap;
	MapView mapView;
	LocationClient locationClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		VenuesDBDataSource venuesDBDataSource = VenuesDBDataSource.getInstance(this);

		// Make sure that google play services are OK
		if (Utils.googlePlayServicesOK(this)) {
			setContentView(R.layout.activity_spatialitedb_samplespatialitequery);
			mapView = (MapView) findViewById(R.id.spatialiteresultsmap);
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

		//Data base query
		ArrayList<MyPolygon> results = null;
		try {
			// Query DB
			results = venuesDBDataSource.findSampleFromPGAmenity();
			Log.d(LOGTAG, "Results Size: " + results.size());

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}

		// Draw on Map
		for (MyPolygon myPolygon : results) {
			drawOnMap(myPolygon);
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

	//=================================================================================

	private void drawOnMap(MyPolygon myPolygon) {
		PolygonOptions polygonOptions = new PolygonOptions().fillColor(Color.BLUE).strokeWidth(1);
		for (int i = 0; i < myPolygon.getPoints().size() - 1; i++) {
			polygonOptions.add(myPolygon.getPoints().get(i));
		}
		googleMap.addPolygon(polygonOptions);

		MarkerOptions markerOptions = new MarkerOptions().position(myPolygon.getPoints().get(0))
				.title(myPolygon.getName()).snippet(myPolygon.getSemantic());
		googleMap.addMarker(markerOptions);

	}

}
