package org.epfl.locationprivacy.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Queue;
import java.util.Random;

import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.privacyestimation.Event;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
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

	public static final int LAUSSANE_GRID_HEIGHT_CELLS = 101;
	public static final int LAUSSANE_GRID_WIDTH_CELLS = 301;
	private static String LOGTAG = "Utils";
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

	private static String filePathPart1 = "sdcard/LocationPrivacyLibrary";
	private static String filePathPart2;
	private static String filePathPart3;

	public static void createNewLoggingFolder() {
		filePathPart2 = System.currentTimeMillis() + "";
		File dir = new File(filePathPart1 + "/" + filePathPart2);
		dir.mkdir();
	}

	public static void createNewLoggingSubFolder() {
		filePathPart3 = System.currentTimeMillis() + "";
		File dir = new File(filePathPart1 + "/" + filePathPart2 + "/" + filePathPart3);
		dir.mkdir();
	}

	public static void appendLog(String fileName, String text) {
		File logFile = new File(filePathPart1 + "/" + filePathPart2 + "/" + filePathPart3 + "/"
				+ fileName);
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
			e.printStackTrace();
		}
	}

	public static void logLinkabilityGraph(Queue<ArrayList<Event>> levels) {
		try {
			// open nodes and edges files
			File nodesFile = new File(filePathPart1 + "/" + filePathPart2 + "/" + filePathPart3
					+ "/" + "nodes.txt");
			File edgesFile = new File(filePathPart1 + "/" + filePathPart2 + "/" + filePathPart3
					+ "/" + "edges.txt");
			nodesFile.createNewFile();
			edgesFile.createNewFile();
			BufferedWriter bufNodes = new BufferedWriter(new FileWriter(nodesFile, true));
			BufferedWriter bufEdges = new BufferedWriter(new FileWriter(edgesFile, true));

			// logging
			//--> Nodes
			bufNodes.append("Id,Label\n");
			for (ArrayList<Event> level : levels) {
				for (Event e : level) {
					String nodeLabel = "cellID " + e.locID + " prop " + e.propability;
					String nodeID = e.id + "";
					bufNodes.append(nodeID + "," + nodeLabel + "\n");
				}
			}
			//--> Edges
			bufEdges.append("Source,Target,Label\n");
			for (ArrayList<Event> level : levels) {
				for (Event e : level) {
					ArrayList<Pair<Event, Double>> parents = e.parents;
					if (parents != null)
						for (Pair<Event, Double> parent : parents) {
							String source = parent.first.id + "";
							String target = e.id + "";
							String transitionProp = parent.second + "";
							bufEdges.append(source + "," + target + "," + transitionProp + "\n");
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
}
