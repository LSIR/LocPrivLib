package org.epfl.locationprivacy.baselineprotection.activities;

import java.util.ArrayList;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.baselineprotection.util.Utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;

public class CoarseLocationActivity extends Activity {

	GoogleMap googleMap;
	MapView mapView;
	ArrayList<Polyline> polylines = new ArrayList<Polyline>();
	Polygon polygon = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Get intent data
		Intent intent = getIntent();
		double fineLocationLat = intent.getDoubleExtra("fineLocationLat", 0.0);
		double fineLocationLng = intent.getDoubleExtra("fineLocationLng", 0.0);
		LatLng fineLocation = new LatLng(fineLocationLat, fineLocationLng);

		int obfuscationRegionHeightCells = intent.getIntExtra("obfuscationRegionHeightCells", 1);
		int obfuscationRegionWidthCells = intent.getIntExtra("obfuscationRegionWidthCells", 1);

		double obfuscationRegionTopLeftLat = intent.getDoubleExtra("obfuscationRegionTopLeftLat",
				0.0);
		double obfuscationRegionTopLeftLng = intent.getDoubleExtra("obfuscationRegionTopLeftLng",
				0.0);
		LatLng obfuscationRegionTopLeftPoint = new LatLng(obfuscationRegionTopLeftLat,
				obfuscationRegionTopLeftLng);

		//Rendering Obfuscation Map
		if (Utils.googlePlayServicesOK(this)) {
			setContentView(R.layout.activity_baselineprotection_coarselocation);
			mapView = (MapView) findViewById(R.id.obfuscationmap);
			mapView.onCreate(savedInstanceState);

			if (initMap()) {
				//Animate
				CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(fineLocation, 13);
				googleMap.moveCamera(cameraUpdate);

				//Adding Marker
				MarkerOptions markerOptions = new MarkerOptions().position(fineLocation);
				googleMap.addMarker(markerOptions);

				// generate Map Grid
				int arrRows = obfuscationRegionHeightCells + 1;
				int arrCols = obfuscationRegionWidthCells + 1;
				LatLng[][] mapGrid = Utils.generateMapGrid(arrRows, arrCols,
						obfuscationRegionTopLeftPoint);

				//Remove old grid from map
				Utils.removeOldMapGrid(polylines, polygon);

				//Draw new grid on map
				polylines = Utils.drawMapGrid(mapGrid, googleMap);
				polygon = Utils.drawObfuscationArea(mapGrid, googleMap);
			} else {
				Toast.makeText(this, "googleMap is null", Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(this, "google play services are not OK", Toast.LENGTH_SHORT).show();
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
}
