package org.epfl.locationprivacy.thirdpartyemulator;

import java.util.ArrayList;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.adaptiveprotection.AdaptiveProtection;
import org.epfl.locationprivacy.adaptiveprotection.AdaptiveProtectionInterface;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.util.Utils;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;

public class ThirdPartyActivity extends Activity {

	private static final String LOGTAG = "ThirdPartyActivity";
	AdaptiveProtectionInterface adaptiveProtectionInterface;
	GoogleMap googleMap;
	MapView mapView;
	ArrayList<Polygon> polygons;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Initialize adaptive protection
		adaptiveProtectionInterface = new AdaptiveProtection(this);

		// Initialize Google Maps
		if (Utils.googlePlayServicesOK(this)) {
			setContentView(R.layout.activity_thirdparty);
			mapView = (MapView) findViewById(R.id.thirdpartymapview);
			mapView.onCreate(savedInstanceState);

			if (initMap()) {
				MapsInitializer.initialize(this);
			} else {
				Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(this, "Google Play services Not OK", Toast.LENGTH_SHORT).show();
		}

		// Initialize Polygons
		polygons = new ArrayList<Polygon>();
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
	public void getLocation(View view) {

		ArrayList<MyPolygon> obfRegionCells = adaptiveProtectionInterface.getLocation();

		//testing
		Log.d(LOGTAG, "polygons returned: " + obfRegionCells.size());
		Utils.removePolygons(polygons);
		for (MyPolygon obfRegionCell : obfRegionCells) {
			polygons.add(Utils.drawPolygon(obfRegionCell, googleMap));
		}

	}
}
