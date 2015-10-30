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
import android.app.Dialog;
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

	public static final int GRID_HEIGHT_CELLS = 101;
	public static final int GRID_WIDTH_CELLS = 101;
	public static final LatLng MAP_ORIGIN = new LatLng(0, 0);
	public static double INITIAL_CELL_SIZE = 0.0009; // in degrees (approx. 100 meters at MAP_ORIGIN)
	private static String LOGTAG = "Utils";
	private static final int GPS_ERRORDIALOG_REQUEST = 9001;
	private static Random rand = new Random();
	// FIXME : can change depending on position on earth
	private static final float GRID_CELL_SIZE = 0.05f; //50 meters
	private static DecimalFormat formatter = new DecimalFormat(".##E0");

	/**
	 * Compute the Harvesine formula
	 *
	 * @param src
	 * @param distance
	 * @param bearing
	 * @return
	 */
	public static LatLng getLatLong(LatLng src, double latDistance, double longDistance, float bearing) {
		// 6371 is the average radius of earth in km
		double latDist = latDistance / 6371.0;
		double longDist = longDistance / 6371.0;
		double brng = Math.toRadians(bearing);
		double lat1 = Math.toRadians(src.latitude);
		double lon1 = Math.toRadians(src.longitude);

		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(latDist) + Math.cos(lat1) * Math.sin(latDist)
				                                                             * Math.cos(brng));
		double a = Math.atan2(Math.sin(brng) * Math.sin(longDist) * Math.cos(lat1), Math.cos(longDist)
				                                                                            - Math.sin(lat1) * Math.sin(lat2));
		double lon2 = lon1 + a;
		lon2 = (lon2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI;

		return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
	}

	public static int getRandom(int min, int max) {
		return rand.nextInt((max - min) + 1) + min;
	}

	/**
	 * Compute the top Left position of a Cell given an other position
	 *
	 * @param position
	 * @return the top Left position of a Cell given an other position
	 */
	public static LatLng findTopLeftPoint(LatLng position) {
		int precision = 10000; // 4 decimals, precision of 11 meters
		// height in degrees
		double cellHeight = INITIAL_CELL_SIZE; // 99 meters
		double latitude = position.latitude + 90; // latitude is positive
		double longitude = position.longitude + 180; // longitude is positive
		// width in degrees
		double cellWidth = getDegreesFor100m(position.latitude, cellHeight);

		if (cellWidth == 0) {
		}

		// Truncate to 11 meters precision
		latitude = Math.floor(latitude * precision) / precision;

		double nbCellsLatFromSouthPole = Math.floor(latitude / cellHeight);
		double nbCellsLongFromOppositeOfOrigin = Math.floor(longitude / cellWidth);

		// Bottom Left corner of current cell
		latitude = cellHeight * nbCellsLatFromSouthPole;
		longitude = cellWidth * nbCellsLongFromOppositeOfOrigin;

		// Top Left corner of current cell
		if (position.latitude < 90) {
			latitude = latitude + cellHeight;
		}

		// Come back to real values
		latitude -= 90;
		longitude -= 180;

		// Truncate once to be sure to have the exact value
		latitude = Math.floor(latitude * precision) / precision;
		longitude = Math.floor(longitude * precision) / precision;

		return new LatLng(latitude, longitude);
	}

	/**
	 * Compute the width (longitude) of a cell to have in it approx. 100 meters
	 *
	 * @param latitude     between -90 and 90
	 * @param initialWidth in degrees
	 * @return the width of a cell to have in it approx. 100 meters
	 */
	public static double getDegreesFor100m(double latitude, double initialWidth) {

		// Find the percentage of distance lost between equinox and current circle of latitude
		double lat = Math.abs(latitude);
		double diffWithEquinox = differenceWithEquinox(lat);

		// Compute new width for cells
		double cellWidth = initialWidth + (initialWidth * diffWithEquinox);
		if (cellWidth == 0) {
			return 0;
		}
		// Compute nbOfCells needed
		double nbOfCellsForLong = Math.floor(360 / cellWidth);
		// Compute the new adapted size to fit the entire circle of latitude
		double oneCellSize = 360 / nbOfCellsForLong;

		return oneCellSize;
	}

	/**
	 * Compute the difference in percentage for lost distance between equinox and circle of latitude
	 *
	 * @param latitude in degrees
	 * @return the difference in percentage for lost distance between equinox and circle of latitude
	 */
	private static double differenceWithEquinox(double latitude) {
		double circleOfLatitude = Math.round(latitude);

		if (circleOfLatitude < 1) {
			return 0;
		} else if (circleOfLatitude < 5) {
			return 100 - 99.6;
		} else if (circleOfLatitude < 10) {
			return 100 - 98.5;
		} else if (circleOfLatitude < 15) {
			return 100 - 96.6;
		} else if (circleOfLatitude < 20) {
			return 100 - 94;
		} else if (circleOfLatitude < 25) {
			return 100 - 90.7;
		} else if (circleOfLatitude < 30) {
			return 100 - 86.7;
		} else if (circleOfLatitude < 35) {
			return 100 - 82;
		} else if (circleOfLatitude < 40) {
			return 100 - 76.7;
		} else if (circleOfLatitude < 45) {
			return 100 - 70.8;
		} else if (circleOfLatitude < 50) {
			return 100 - 64.4;
		} else if (circleOfLatitude < 55) {
			return 100 - 57.5;
		} else if (circleOfLatitude < 60) {
			return 100 - 50.1;
		} else if (circleOfLatitude < 65) {
			return 100 - 42.4;
		} else if (circleOfLatitude < 70) {
			return 100 - 34.3;
		} else if (circleOfLatitude < 75) {
			return 100 - 26;
		} else if (circleOfLatitude < 80) {
			return 100 - 17.4;
		} else if (circleOfLatitude < 90) {
			return 100 - 8.7;
		} else if (circleOfLatitude == 90) {
			return -1;
		} else {
			return 100;
		}
	}

	/**
	 * Compute the id of a given point on the map
	 * You should use this function for positions representing cells (top left corner)
	 *
	 * @param position
	 * @return the id of a given point on the map
	 */
	public static int computeCellIDFromPosition(LatLng position) {
		int precision = 1000; // 4 decimals, precision of 11 meters

		double latitude = position.latitude + 90;
		double longitude = position.longitude + 180;

		int integerLatitude = (int) Math.floor(latitude * precision);
		int integerLongitude = (int) Math.floor(longitude * precision);

		// Cantor pairing Function
		int id = 1 / 2 * (integerLatitude + integerLongitude) * (integerLatitude + integerLongitude + 1) + integerLongitude;

		return id;
	}

	/**
	 * Compute corners of a cell from the top left point of this cell
	 *
	 * @param topLeft the top left point of a cell
	 * @return list of a cell corners
	 */
	public static ArrayList<LatLng> computeCellCornerPoints(LatLng topLeft) {
		ArrayList<LatLng> corners = new ArrayList<LatLng>();
		corners.add(topLeft);

		double cellWidth = getDegreesFor100m(topLeft.latitude, Utils.INITIAL_CELL_SIZE);

		// Apparently the order of the corners in the arrayList is important
		// Top Right
		corners.add(new LatLng(topLeft.latitude, topLeft.longitude + cellWidth));
		// Bottom Right
		corners.add(new LatLng(topLeft.latitude - Utils.INITIAL_CELL_SIZE, topLeft.longitude + cellWidth));
		// Bottom Left
		corners.add(new LatLng(topLeft.latitude - Utils.INITIAL_CELL_SIZE, topLeft.longitude));

		return corners;
	}

	/*************************************** OBSOLETE *********************************************/
	/*public static LatLng findTopLeftPoint(LatLng centerPoint, int gridHeightCells,
	                                      int gridWidthCells) {

		LatLng midLeftPoint = Utils.getLatLong(centerPoint, (gridWidthCells / 2 + 0.5f)
				                                                    * GRID_CELL_SIZE, -90f);
		LatLng topLeftPoint = Utils.getLatLong(midLeftPoint, (gridHeightCells / 2 + 0.5f)
				                                                     * GRID_CELL_SIZE, 0f);
		return topLeftPoint;
	}*/

	/**********************************************************************************************/

	// FIXME : change GRID_CELL_SIZE with new values for a given cell
	public static LatLng[][] generateMapGrid(int arrRows, int arrCols, LatLng topLeftPoint) {
		LatLng[][] grid = new LatLng[arrRows][arrCols];
		grid[0][0] = topLeftPoint;

		//fill first column
		for (int i = 1; i < arrRows; i++)
			grid[i][0] = Utils.getLatLong(grid[i - 1][0], INITIAL_CELL_SIZE, Utils.getDegreesFor100m(grid[i - 1][0].latitude, INITIAL_CELL_SIZE), 180);

		//fill rows
		for (int i = 0; i < arrRows; i++)
			for (int j = 1; j < arrCols; j++)
				grid[i][j] = Utils.getLatLong(grid[i][j - 1], INITIAL_CELL_SIZE, Utils.getDegreesFor100m(grid[i][j - 1].latitude, INITIAL_CELL_SIZE), 90);

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

	public static void removePolygons(ArrayList<Polygon> polygons) {
		for (Polygon p : polygons) {
			p.remove();
		}
		polygons.clear();
	}

	public static void removeMarkers(ArrayList<Marker> markers) {
		for (Marker m : markers) {
			m.remove();
		}
		markers.clear();
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

	public static Polygon drawPolygon(MyPolygon polygon, GoogleMap googleMap, int fillColor) {
		PolygonOptions polygonOptions = new PolygonOptions().fillColor(fillColor)
				                                .strokeColor(Color.BLUE).strokeWidth(1);
		for (LatLng p : polygon.getPoints()) {
			polygonOptions.add(p);
		}
		return googleMap.addPolygon(polygonOptions);
	}

	public static Polygon drawObfuscationArea(LatLng[][] mapGrid, GoogleMap googleMap) {

		int arrRows = mapGrid.length;
		int arrCols = mapGrid[0].length;
		Log.d(LOGTAG, "Rows:" + arrRows + "  Cols:" + arrCols);

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
					String nodeLabel = "ID: " + e.id + " prob: " + formatter.format(e.propability);
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

	/* Checks if external storage is available for read and write */
	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}
}
