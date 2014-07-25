package org.epfl.locationprivacy.baselineprotection.util;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class Utils {

	public static String LOGTAG = "Utils";
	private static final int GPS_ERRORDIALOG_REQUEST = 9001;
	private static Random rand = new Random();
	private static final float GRID_CELL_SIZE = 0.05f; //50 meters 

	public static LatLng getLatLong(LatLng src, float distance, float bearing) {
		double dist = distance / 6371.0;
		double brng = Math.toRadians(bearing);
		double lat1 = Math.toRadians(src.latitude);
		double lon1 = Math.toRadians(src.longitude);

		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dist) + Math.cos(lat1) * Math.sin(dist)
				* Math.cos(brng));
		double a = Math.atan2(Math.sin(brng) * Math.sin(dist) * Math.cos(lat1), Math.cos(dist)
				- Math.sin(lat1) * Math.sin(lat2));
		double lon2 = lon1 + a;
		lon2 = (lon2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI;

		return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
	}

	public static int getRandom(int min, int max) {
		return rand.nextInt((max - min) + 1) + min;
	}

	public static LatLng findTopLeftPoint(LatLng centerPoint, int gridHeightCells,
			int gridWidthCells) {

		LatLng midLeftPoint = Utils.getLatLong(centerPoint, (gridWidthCells / 2 + 0.5f)
				* GRID_CELL_SIZE, -90f);
		LatLng topLeftPoint = Utils.getLatLong(midLeftPoint, (gridHeightCells / 2 + 0.5f)
				* GRID_CELL_SIZE, 0f);
		return topLeftPoint;
	}

	public static LatLng[][] generateMapGrid(int arrRows, int arrCols, LatLng topLeftPoint) {
		LatLng[][] grid = new LatLng[arrRows][arrCols];
		grid[0][0] = topLeftPoint;

		//fill first column
		for (int i = 1; i < arrRows; i++)
			grid[i][0] = Utils.getLatLong(grid[i - 1][0], GRID_CELL_SIZE, 180);

		//fill rows
		for (int i = 0; i < arrRows; i++)
			for (int j = 1; j < arrCols; j++)
				grid[i][j] = Utils.getLatLong(grid[i][j - 1], GRID_CELL_SIZE, 90);

		return grid;
	}

	public static void removeOldMapGrid(ArrayList<Polyline> polylines, Polygon polygon) {
		for (Polyline p : polylines) {
			p.remove();
		}
		polylines.clear();

		if (polygon != null)
			polygon.remove();
	}

	public static ArrayList<Polyline> drawMapGrid(LatLng[][] mapGrid, GoogleMap googleMap) {
		ArrayList<Polyline> polylines = new ArrayList<Polyline>();
		int arrRows = mapGrid.length;
		int arrCols = mapGrid[0].length;

		//draw horizontal polylines
		for (int i = 0; i < arrRows; i++) {
			PolylineOptions polylineOptions = new PolylineOptions().color(Color.BLUE).width(1)
					.add(mapGrid[i][0]).add(mapGrid[i][arrCols - 1]);
			polylines.add(googleMap.addPolyline(polylineOptions));
		}

		//draw vertical polylines
		for (int i = 0; i < arrCols; i++) {
			PolylineOptions polylineOptions = new PolylineOptions().color(Color.BLUE).width(1)
					.add(mapGrid[0][i]).add(mapGrid[arrRows - 1][i]);
			polylines.add(googleMap.addPolyline(polylineOptions));
		}

		return polylines;
	}

	public static Polygon drawObfuscationArea(LatLng[][] mapGrid, GoogleMap googleMap) {

		int arrRows = mapGrid.length;
		int arrCols = mapGrid[0].length;
		Log.d(LOGTAG, "Rows:"+arrRows+"  Cols:"+arrCols);

		PolygonOptions polygonOptions = new PolygonOptions().fillColor(0x330000FF)
				.strokeColor(Color.BLUE).strokeWidth(1);
		polygonOptions.add(mapGrid[0][0]).add(mapGrid[0][arrCols - 1])
				.add(mapGrid[arrRows - 1][arrCols - 1]).add(mapGrid[arrRows - 1][0]);
		return googleMap.addPolygon(polygonOptions);

	}

	public static boolean googlePlayServicesOK(Activity activity) {
		int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);

		if (isAvailable == ConnectionResult.SUCCESS) {
			return true;
		} else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, activity,
					GPS_ERRORDIALOG_REQUEST);
			dialog.show();
		} else {
			Toast.makeText(activity, "Can't connect to google play services", Toast.LENGTH_SHORT)
					.show();
		}
		return false;
	}
}
