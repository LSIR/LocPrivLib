package org.epfl.locationprivacy.spatialitedb;

import java.io.File;
import java.util.ArrayList;

import jsqlite.Database;
import jsqlite.Stmt;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.baselineprotection.util.Utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
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
import com.google.android.gms.maps.model.PolylineOptions;

public class SampleSpatialiteQueryActivity extends Activity {

	public static String LOGTAG = "SampleSpatialiteQueryActivity";

	GoogleMap googleMap;
	MapView mapView;
	LocationClient locationClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
			// Open DB
			File sdcardDir = Environment.getExternalStorageDirectory();
			File spatialDbFile = new File(sdcardDir, "laussane4.sqlite");
			Database db = new jsqlite.Database();
			db.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);

			// Query DB
			results = queryDB(db);
			Log.d(LOGTAG, "Results Size: " + results.size());

			//Close DB
			db.close();
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
	public ArrayList<MyPolygon> queryDB(Database db) {
		ArrayList<MyPolygon> myPolygons = new ArrayList<MyPolygon>();
		String query = "select name, sub_type, GeometryType(Geometry) as type, AsText(Geometry)"
				+ " as locationgeometry from pg_amenity where name not NULL AND locationgeometry not NULL";
		int rows = 0;

		try {
			Stmt stmt = db.prepare(query);
			while (stmt.step()) {
				rows++;
				String name = stmt.column_string(0);
				String subType = stmt.column_string(1);
				String location = name + "\n" + subType + "\n" + stmt.column_string(2);
				Log.d(LOGTAG, location);
				myPolygons.add(MyPolygon.parseGeometry(stmt.column_string(3), name, subType));
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
		Log.d(LOGTAG, "....Rows: " + rows + "\n");
		Log.d(LOGTAG, "==============================");

		return myPolygons;
	}

	//===========================================================

	private void drawOnMap(MyPolygon myPolygon) {
		PolygonOptions polygonOptions = new PolygonOptions().fillColor(Color.BLUE).strokeWidth(1);
		for (int i = 0; i < myPolygon.points.size() - 1; i++) {
			polygonOptions.add(myPolygon.points.get(i));
		}
		googleMap.addPolygon(polygonOptions);

		MarkerOptions markerOptions = new MarkerOptions().position(myPolygon.points.get(0))
				.title(myPolygon.name).snippet(myPolygon.subType);
		googleMap.addMarker(markerOptions);

	}

}
