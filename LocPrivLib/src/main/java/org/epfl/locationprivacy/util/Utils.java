package org.epfl.locationprivacy.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Queue;
import java.util.Random;

import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.privacyestimation.Event;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class Utils {

	// Kept only for testing purpose
	public static final int LAUSANNE_GRID_HEIGHT_CELLS = 101;
	public static final int LAUSANNE_GRID_WIDTH_CELLS = 301;

	public static final int GRID_HEIGHT_CELLS = 31;
	public static final int GRID_WIDTH_CELLS = 31;
	public static final LatLng MAP_ORIGIN = new LatLng(0, 0);
	public static double INITIAL_DEGREES_CELL_SIZE = 0.0009; // in degrees (approx. 100 meters at MAP_ORIGIN)
	private static int MAP_PRECISION = 1000; // 4 decimals, precision of 11 meters
	private static String LOGTAG = "Utils";
	private static final int GPS_ERRORDIALOG_REQUEST = 9001;
	private static Random rand = new Random();
	private static DecimalFormat formatter = new DecimalFormat(".##E0");
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

	/**
	 * Compute the Haversine formula to get a point from another point, a distance and a direction
	 *
	 * @param src      The source point
	 * @param distance The distance between src and the wanted point in km
	 * @param bearing  The orientation where we want the new point
	 * @return a point in a direction bearing with distance from src
	 */
	public static LatLng getLatLong(LatLng src, double distance, float bearing) {
		// 6371 is the average radius of earth in km
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

	/**
	 * Compute the distance in km for a distance in degrees between two points
	 * Uses the Haversine formula
	 *
	 * @param first
	 * @param second
	 * @return the distance in km for a distance in degrees between two points
	 */
	public static double getDistance(LatLng first, LatLng second) {
		int R = 6371; // km
		double lat1 = first.latitude;
		double lat2 = second.longitude;
		double lon1 = first.latitude;
		double lon2 = second.longitude;

		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
				           Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
						           Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.asin(Math.sqrt(a));
		double d = R * c;
		return d / 10000;
	}

	/**
	 * Get a random integer between min and max
	 *
	 * @param min
	 * @param max
	 * @return
	 */
	public static int getRandom(int min, int max) {
		return rand.nextInt((max - min) + 1) + min;
	}

	/**
	 * Compute the top Left position of a Cell given another position
	 *
	 * @param position
	 * @return the top Left position of a Cell given another position
	 */
	public static LatLng findCellTopLeftPoint(LatLng position) {
		int precision = 10000; // 4 decimals, precision of 11 meters
		// height in degrees
		double cellHeight = INITIAL_DEGREES_CELL_SIZE; // 99 meters
		double latitude = position.latitude + 90; // latitude is positive
		double longitude = position.longitude + 180; // longitude is positive

		// Truncate to 11 meters precision
		latitude = Math.floor(latitude * precision) / precision;

		double nbCellsLatFromSouthPole = Math.floor(latitude / cellHeight);

		// Bottom of current cell
		latitude = cellHeight * nbCellsLatFromSouthPole;

		// Top of current cell
		if (position.latitude < 90) {
			latitude = latitude + cellHeight;
		}

		// Come back to real values
		latitude -= 90;

		// width in degrees
		double cellWidth = getDegreesFor100m(new LatLng(latitude, position.longitude), 90);
		cellWidth = Math.floor(cellWidth * precision * 1000) / (precision * 1000);
		double nbCellsLongFromOppositeOfOrigin = Math.floor(longitude / cellWidth);
		longitude = cellWidth * nbCellsLongFromOppositeOfOrigin;
		longitude -= 180;

		return new LatLng(latitude, longitude);
	}

	/**
	 * Compute the center of cell given its top left and bottom right points
	 *
	 * @param topLeft
	 * @return
	 */
	public static LatLng findCellCenter(LatLng topLeft) {
		double topLeftLatitude = topLeft.latitude + 90;
		double topLeftLongitude = topLeft.longitude + 180;

		double bottomRightLatitude = topLeftLatitude - INITIAL_DEGREES_CELL_SIZE;
		double bottomRightLongitude = bottomRightLatitude + getDegreesFor100m(topLeft, 90);

		double latDistance = topLeftLatitude - bottomRightLatitude;
		double longDistance = topLeftLongitude - bottomRightLongitude;

		return new LatLng((topLeftLatitude + latDistance / 2) - 90, topLeftLongitude + longDistance / 2 + 180);
	}

	/**
	 * Compute the width (longitude) of a cell in degrees to have in it approx. 100 meters
	 *
	 * @param position
	 * @param bearing
	 * @return
	 */
	public static double getDegreesFor100m(LatLng position, float bearing) {

		// Get a point at 100 meters from position
		LatLng other = getLatLong(position, 0.1, bearing);
		// Compute the distance in degrees
		double cellWidth = Math.abs(position.longitude - other.longitude);
		return cellWidth;
	}

	/**
	 * Compute the id of a given point on the map
	 * You should use this function for positions representing cells (top left corner)
	 *
	 * @param position
	 * @return the id of a given point on the map
	 */
	public static long computeCellIDFromPosition(LatLng position) {
		int precision = MAP_PRECISION;

		double latitude = position.latitude + 90;
		double longitude = position.longitude + 180;

		long integerLatitude = (long) Math.floor(latitude * precision);
		long integerLongitude = (long) Math.floor(longitude * precision);

		// Cantor pairing Function
		long id = ((integerLatitude + integerLongitude) * (integerLatitude + integerLongitude + 1)) / 2
				          + integerLongitude;

		return id;
	}

	/**
	 * Compute the position of a cell given its id.
	 *
	 * @param id
	 * @return the position of a cell given its id.
	 */
	public static LatLng computePostitionFromCellID(long id) {

		int precision = MAP_PRECISION;
		double w = Math.floor((Math.sqrt(8 * id + 1) - 1) / 2);
		double t = (w * w + w) / 2;
		double longitudeTemp = id - t;
		double latitudeTemp = w - longitudeTemp;


		double latitude = latitudeTemp / precision - 90;
		double longitude = longitudeTemp / precision - 180;

		// FIXME : be sure it is ok in any cases
		return findCellTopLeftPoint(new LatLng(latitude, longitude));
	}

	/**
	 * Compute corners of a cell from the top left point of this cell
	 *
	 * @param topLeft the top left point of a cell
	 * @return list of a cell corners
	 */
	public static ArrayList<LatLng> computeCellCornerPoints(LatLng topLeft) {
		ArrayList<LatLng> corners = new ArrayList<>();
		corners.add(topLeft);

		// The order of the corners in the arrayList is important for drawing the cell
		// Top Right
		LatLng topRight = getLatLong(topLeft, 0.1, 90);
		corners.add(topRight);
		// Bottom Right
		corners.add(getLatLong(topRight, 0.1, 180));
		// Bottom Left
		corners.add(getLatLong(topLeft, 0.1, 180));

		return corners;
	}

	/**
	 * Find the top left point of a grid given then central cell
	 *
	 * @param centerPoint     the center of the grid
	 * @param gridHeightCells the height of the grid
	 * @param gridWidthCells  the width of the grid
	 * @return the top left point of a grid given then central cell
	 */
	public static LatLng findGridTopLeftPoint(LatLng centerPoint, int gridHeightCells,
	                                          int gridWidthCells) {

		// Top left point of a cell the central cell
		LatLng cell = findCellTopLeftPoint(centerPoint);

		double latitude = cell.latitude + (gridHeightCells - 1) / 2 * INITIAL_DEGREES_CELL_SIZE;
		double longitude = cell.longitude - (gridWidthCells - 1) / 2 * getDegreesFor100m(new LatLng(latitude, cell.longitude), -90);
		LatLng topLeftPoint = findCellTopLeftPoint(new LatLng(latitude, longitude));

		return topLeftPoint;
	}

	/**
	 * Find the bottom point right of a grid given the top left point and sizes of grid
	 *
	 * @param topLeft        the top left point of the grid (the top left point of the top left cell of the grid)
	 * @param gridHeightCell the height of the grid
	 * @param gridWidthCell  the width of the grid
	 * @return the bottom point right of a grid given the top left point and sizes of grid
	 */
	public static LatLng findGridBottomRightPoint(LatLng topLeft, int gridHeightCell, int gridWidthCell) {
		double latitude = topLeft.latitude + gridHeightCell * INITIAL_DEGREES_CELL_SIZE + INITIAL_DEGREES_CELL_SIZE / 2;
		double longitude = topLeft.longitude + (gridWidthCell + 1 / 2) * getDegreesFor100m(new LatLng(latitude, topLeft.longitude), 90);
		return findCellTopLeftPoint(new LatLng(latitude, longitude));
	}

	/**
	 * Generate the grid given its size and the top left cell
	 * <p/>
	 * the grid is not a nice square because of different sizes due to different latitudes
	 *
	 * @param arrRows
	 * @param arrCols
	 * @param topLeftPoint
	 * @return
	 */
	public static LatLng[][] generateMapGrid(int arrRows, int arrCols, LatLng topLeftPoint) {
		LatLng[][] grid = new LatLng[arrRows][arrCols];
		grid[0][0] = topLeftPoint;
		double defaultDist = 0.1001;

		//fill first column
		for (int i = 1; i < arrRows; i++) {
			// for each cell, we need to take a point in the cell to have the top left corner
			grid[i][0] = findCellTopLeftPoint(getLatLong(grid[0][0], defaultDist * i, 180));
		}

		//fill rows
		for (int i = 0; i < arrRows; i++) {
			for (int j = 1; j < arrCols; j++) {
				// Compute horizontal distance with cell on the left
				grid[i][j] = getLatLong(grid[i][j - 1], 0.1, 90);
			}
		}
		return grid;
	}

	/**
	 * Remove lines created for a grid
	 *
	 * @param polylines
	 * @param polygon
	 */
	public static void removeOldMapGrid(ArrayList<Polyline> polylines, Polygon polygon) {
		for (Polyline p : polylines) {
			p.remove();
		}
		polylines.clear();

		if (polygon != null)
			polygon.remove();
	}

	/**
	 * Remove polygons out of the map
	 *
	 * @param polygons
	 */
	public static void removePolygons(ArrayList<Polygon> polygons) {
		for (Polygon p : polygons) {
			p.remove();
		}
		polygons.clear();
	}

	/**
	 * Remove markers
	 *
	 * @param markers
	 */
	public static void removeMarkers(ArrayList<Marker> markers) {
		for (Marker m : markers) {
			m.remove();
		}
		markers.clear();
	}

	/**
	 * Draw the grid on the map
	 *
	 * @param mapGrid
	 * @param googleMap
	 * @return
	 */
	public static ArrayList<Polyline> drawMapGrid(LatLng[][] mapGrid, GoogleMap googleMap) {
		ArrayList<Polyline> polylines = new ArrayList<>();
		PolylineOptions polylineOptions;
		int arrRows = mapGrid.length;
		int arrCols = mapGrid[0].length;

		LatLng[][] grid = new LatLng[arrRows + 1][arrCols + 1];
		for (int i = 0; i < arrRows + 1; i++) {
			for (int j = 0; j < arrCols; j++) {
				if (i < arrRows) {
					grid[i][j] = mapGrid[i][j];
				} else {
					grid[i][j] = getLatLong(grid[i - 1][j], 0.1, 180);
				}
			}
			grid[i][arrCols] = getLatLong(grid[i][arrCols - 1], 0.1, 90);
		}
		grid[arrRows][arrCols] = getLatLong(grid[arrRows][arrCols - 1], 0.1, 90);

		arrCols++;
		arrRows++;

		//draw horizontal polylines
		for (int i = 0; i < arrRows; i++) {
			polylineOptions = new PolylineOptions().color(Color.BLUE).width(1)
					                  .add(grid[i][0]).add(grid[i][arrCols - 1]);
			polylines.add(googleMap.addPolyline(polylineOptions));
		}

		//draw vertical polylines
		for (int i = 0; i < arrCols; i++) {
			polylineOptions = new PolylineOptions().color(Color.BLUE).width(1);
			for (int j = 0; j < arrRows - 1; j++) {
				LatLng bottomLeftJ = new LatLng(grid[j + 1][i].latitude, grid[j][i].longitude);
				polylineOptions.add(grid[j][i]).add(bottomLeftJ);
				polylineOptions.add(bottomLeftJ).add(grid[j + 1][i]);
			}

			polylines.add(googleMap.addPolyline(polylineOptions));
		}

		return polylines;
	}

	/**
	 * Draw a Polygon
	 *
	 * @param polygon
	 * @param googleMap
	 * @param fillColor
	 * @return
	 */
	public static Polygon drawPolygon(MyPolygon polygon, GoogleMap googleMap, int fillColor) {
		PolygonOptions polygonOptions = new PolygonOptions().fillColor(fillColor)
				                                .strokeColor(Color.BLUE).strokeWidth(1);
		for (LatLng p : polygon.getPoints()) {
			polygonOptions.add(p);
		}
		return googleMap.addPolygon(polygonOptions);
	}

	/**
	 * Draw obfuscation area
	 *
	 * @param mapGrid
	 * @param googleMap
	 * @return
	 */
	public static Polygon drawObfuscationArea(LatLng[][] mapGrid, GoogleMap googleMap) {

		int arrRows = mapGrid.length;
		int arrCols = mapGrid[0].length;
		Log.d(LOGTAG, "Rows:" + arrRows + "  Cols:" + arrCols);

		LatLng[][] grid = new LatLng[arrRows + 1][arrCols + 1];
		for (int i = 0; i < arrRows + 1; i++) {
			for (int j = 0; j < arrCols; j++) {
				if (i < arrRows) {
					grid[i][j] = mapGrid[i][j];
				} else {
					grid[i][j] = getLatLong(grid[i - 1][j], 0.1, 180);
				}
			}
			grid[i][arrCols] = getLatLong(grid[i][arrCols - 1], 0.1, 90);
		}
		grid[arrRows][arrCols] = getLatLong(grid[arrRows][arrCols - 1], 0.1, 90);

		arrCols++;
		arrRows++;

		PolygonOptions polygonOptions = new PolygonOptions().fillColor(0x330000FF)
				                                .strokeColor(Color.BLUE).strokeWidth(1);

		// Left side
		for (int j = 0; j < arrRows - 1; j++) {
			// Bottom Right of cell j
			LatLng bottomLeftJ = new LatLng(grid[j + 1][0].latitude, grid[j][0].longitude);
			polygonOptions.add(grid[j][0]).add(bottomLeftJ);
			polygonOptions.add(bottomLeftJ).add(grid[j + 1][0]);
		}

		// Bottom
		polygonOptions.add(grid[arrRows - 1][0]).add(grid[arrRows - 1][arrCols - 1]);

		// Right side
		for (int j = arrRows - 1; j > 0; j--) {
			// Bottom Right of cell j - 1
			LatLng bottomRightJ = new LatLng(grid[j][arrCols - 1].latitude, grid[j - 1][arrCols - 1].longitude);
			polygonOptions.add(grid[j][arrCols - 1]).add(bottomRightJ);
			polygonOptions.add(bottomRightJ).add(grid[j - 1][arrCols - 1]);
		}

		// Top is done automatically

		return googleMap.addPolygon(polygonOptions);

	}

	public static int findDayPortionID(long currTime) {
		Calendar c = Calendar.getInstance();
		long now = c.getTimeInMillis();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millisecondsSinceMidnight = now - c.getTimeInMillis();
		long millisecondsInHalfHour = 1800000;
		int dayPortionID = (int) (millisecondsSinceMidnight / millisecondsInHalfHour);
		return dayPortionID;
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function calculates the distance between 2 lat/lng points : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	public static double distance(double lat1, double lon1, double lat2, double lon2, char unit) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1))
				                                                                  * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		if (unit == 'K') {
			dist = dist * 1.609344;
		} else if (unit == 'N') {
			dist = dist * 0.8684;
		}
		return (dist);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts decimal degrees to radians : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts radians to decimal degrees : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private static double rad2deg(double rad) {
		return (rad * 180.0 / Math.PI);
	}

	//=============================================================
	private final static double degreesPerRadian = 180.0 / Math.PI;

	public static Marker DrawArrowHead(GoogleMap mMap, LatLng from, LatLng to) {
		// obtain the bearing between the last two points
		double bearing = GetBearing(from, to);

		// round it to a multiple of 3 and cast out 120s
		double adjBearing = Math.round(bearing / 3) * 3;
		while (adjBearing >= 120) {
			adjBearing -= 120;
		}

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Get the corresponding triangle marker from Google
		URL url;
		Bitmap image = null;

		try {
			url = new URL("http://www.google.com/intl/en_ALL/mapfiles/dir_"
					              + String.valueOf((int) adjBearing) + ".png");
			try {
				image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (image != null) {

			// Anchor is ratio in range [0..1] so value of 0.5 on x and y will center the marker image on the lat/long
			float anchorX = 0.5f;
			float anchorY = 0.5f;

			int offsetX = 0;
			int offsetY = 0;

			// images are 24px x 24px
			// so transformed image will be 48px x 48px

			//315 range -- 22.5 either side of 315
			if (bearing >= 292.5 && bearing < 335.5) {
				offsetX = 24;
				offsetY = 24;
			}
			//270 range
			else if (bearing >= 247.5 && bearing < 292.5) {
				offsetX = 24;
				offsetY = 12;
			}
			//225 range
			else if (bearing >= 202.5 && bearing < 247.5) {
				offsetX = 24;
				offsetY = 0;
			}
			//180 range
			else if (bearing >= 157.5 && bearing < 202.5) {
				offsetX = 12;
				offsetY = 0;
			}
			//135 range
			else if (bearing >= 112.5 && bearing < 157.5) {
				offsetX = 0;
				offsetY = 0;
			}
			//90 range
			else if (bearing >= 67.5 && bearing < 112.5) {
				offsetX = 0;
				offsetY = 12;
			}
			//45 range
			else if (bearing >= 22.5 && bearing < 67.5) {
				offsetX = 0;
				offsetY = 24;
			}
			//0 range - 335.5 - 22.5
			else {
				offsetX = 12;
				offsetY = 24;
			}

			Bitmap wideBmp;
			Canvas wideBmpCanvas;
			Rect src, dest;

			// Create larger bitmap 4 times the size of arrow head image
			wideBmp = Bitmap.createBitmap(image.getWidth() * 2, image.getHeight() * 2,
					                             image.getConfig());

			wideBmpCanvas = new Canvas(wideBmp);

			src = new Rect(0, 0, image.getWidth(), image.getHeight());
			dest = new Rect(src);
			dest.offset(offsetX, offsetY);

			wideBmpCanvas.drawBitmap(image, src, dest, null);

			return mMap.addMarker(new MarkerOptions().position(to)
					                      .icon(BitmapDescriptorFactory.fromBitmap(wideBmp)).anchor(anchorX, anchorY));
		}
		return null;
	}

	private static double GetBearing(LatLng from, LatLng to) {
		double lat1 = from.latitude * Math.PI / 180.0;
		double lon1 = from.longitude * Math.PI / 180.0;
		double lat2 = to.latitude * Math.PI / 180.0;
		double lon2 = to.longitude * Math.PI / 180.0;

		// Compute the angle.
		double angle = -Math.atan2(
				                          Math.sin(lon1 - lon2) * Math.cos(lat2),
				                          Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2)
						                                                            * Math.cos(lon1 - lon2));

		if (angle < 0.0)
			angle += Math.PI * 2.0;

		// And convert result to degrees.
		angle = angle * degreesPerRadian;

		return angle;
	}

	//===========================================================================
	//========================= Logging Methods =================================
	//===========================================================================

	public static String filePathPart1 = "LocationPrivacyLibrary";
	public static String filePathPart2;
	public static String filePathPart3;

	public static void createNewLoggingFolder(Context context) {
		filePathPart2 = System.currentTimeMillis() + "";
		File path;
		if (isExternalStorageWritable()) {
			path = context.getExternalFilesDir(null);
		} else {
			path = context.getFilesDir();
		}
		File completePath = new File(path, filePathPart1 + File.separator + filePathPart2);
		completePath.mkdirs();
	}

	public static void createNewLoggingSubFolder(Context context) {
		filePathPart3 = System.currentTimeMillis() + "";
		File path;
		if (isExternalStorageWritable()) {
			path = context.getExternalFilesDir(null);
		} else {
			path = context.getFilesDir();
		}
		File completePath = new File(path, filePathPart1 + File.separator + filePathPart2 + File.separator + filePathPart3);
		completePath.mkdirs();
	}

	public static void appendLog(String fileName, String text, Context context) {
		File path;
		if (isExternalStorageWritable()) {
			path = context.getExternalFilesDir(null);
		} else {
			path = context.getFilesDir();
		}
		File completePath = new File(path, filePathPart1 + File.separator + filePathPart2 + File.separator + filePathPart3);
		File logFile = new File(completePath, fileName);
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Toast.makeText(context, "Problem with logging", Toast.LENGTH_SHORT);
				e.printStackTrace();
			}
		}
		try {
			//BufferedWriter for performance, true to set append to file flag
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
			buf.append(text);
			buf.newLine();
			buf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Toast.makeText(context, "Problem with logging", Toast.LENGTH_SHORT);
			e.printStackTrace();
		}
	}

	public static void logLinkabilityGraph(Queue<ArrayList<Event>> levels, String nodesFileName,
	                                       String edgesFileName, Context context) {
		try {
			// open nodes and edges files
			File pathNodes;
			if (isExternalStorageWritable()) {
				pathNodes = context.getExternalFilesDir(null);
			} else {
				pathNodes = context.getFilesDir();
			}
			File completeNodesPath = new File(pathNodes, filePathPart1 + File.separator + filePathPart2 + File.separator + filePathPart3);
			File nodesFile = new File(completeNodesPath, nodesFileName);

			File pathEdges;
			if (isExternalStorageWritable()) {
				pathEdges = context.getExternalFilesDir(null);
			} else {
				pathEdges = context.getFilesDir();
			}
			File completeEdgesPath = new File(pathEdges, filePathPart1 + File.separator + filePathPart2 + File.separator + filePathPart3);
			File edgesFile = new File(completeEdgesPath, edgesFileName);

			nodesFile.createNewFile();
			edgesFile.createNewFile();
			BufferedWriter bufNodes = new BufferedWriter(new FileWriter(nodesFile, true));
			BufferedWriter bufEdges = new BufferedWriter(new FileWriter(edgesFile, true));

			// logging
			//--> Nodes
			bufNodes.append("Id,Level,Label\n");
			int currLevel = 0;
			for (ArrayList<Event> level : levels) {
				currLevel++;
				for (Event e : level) {
					String nodeLabel = "ID: " + e.id + " prob: " + formatter.format(e.probability);
					String nodeID = e.id + "";
					bufNodes.append(nodeID + "," + currLevel + "," + nodeLabel + "\n");
				}
			}
			//--> Edges
			bufEdges.append("Source,Target,Label\n");
			for (ArrayList<Event> level : levels) {
				for (Event e : level) {
					ArrayList<Pair<Event, Double>> parents = e.parents;

					for (Pair<Event, Double> parentInfo : parents) {
						Event parent = parentInfo.first;
						double transProp = parentInfo.second;
						double normalizedTransProp = transProp / parent.childrenTransProbSum;
						String source = parent.id + "";
						String target = e.id + "";
						String label = formatter.format(normalizedTransProp);
						bufEdges.append(source + "," + target + "," + label + "\n");
					}
				}
			}

			// close nodes and edges files
			bufNodes.close();
			bufEdges.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks if external storage is available for read and write
	 **/
	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	/**
	 * Method to verify google play services on the device
	 */
	public static boolean checkPlayServices(Activity activity, Context ctx) {
		int resultCode = GooglePlayServicesUtil
				                 .isGooglePlayServicesAvailable(ctx);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
						                                     PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Toast.makeText(ctx.getApplicationContext(),
						              "This device is not supported.", Toast.LENGTH_LONG)
						.show();
				activity.finish();
			}
			return false;
		}
		return true;
	}
}
