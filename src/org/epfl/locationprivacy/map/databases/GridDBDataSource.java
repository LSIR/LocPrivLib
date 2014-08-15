package org.epfl.locationprivacy.map.databases;

import java.util.ArrayList;
import java.util.Vector;

import jsqlite.Callback;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import jsqlite.TableResult;

import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;
import org.epfl.locationprivacy.util.Utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.GeomagneticField;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class GridDBDataSource {
	private static final String LOGTAG = "GridDBDataSource";
	private static GridDBDataSource instance;

	SQLiteOpenHelper dbHelper;
	SQLiteDatabase db; // this db is just used to copy the db from assets folder to the application data on the first time
	Database spatialdb;
	Context context;

	public static GridDBDataSource getInstance(Context context) {
		if (instance == null)
			instance = new GridDBDataSource(context);
		return instance;
	}

	private GridDBDataSource(Context context) {
		this.context = context;
		dbHelper = GridDBOpenHelper.getInstance(context);
		open();
	}

	private void open() {
		// on first time, the db will be copied to the application data from assets folder
		db = dbHelper.getWritableDatabase();

		// open spatial db
		spatialdb = openSpatialDB();

		Log.i(LOGTAG, "Grid DataBase opened");
	}

	public void close() {
		dbHelper.close();
		closeSpatialDB();
		Log.i(LOGTAG, "Grid DataBase closed");
	}

	private Database openSpatialDB() {
		Database spatialdb = new jsqlite.Database();
		try {
			spatialdb.open(context.getDatabasePath(GridDBOpenHelper.DATABASE_NAME).getPath(),
					jsqlite.Constants.SQLITE_OPEN_READWRITE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return spatialdb;
	}

	private void closeSpatialDB() {
		try {
			spatialdb.close();
		} catch (jsqlite.Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
	}

	public ArrayList<MyPolygon> findAllData() {

		ArrayList<MyPolygon> polygons = new ArrayList<MyPolygon>();
		try {
			String query = "select  " + GridDBOpenHelper.COLUMN_ID + ", "
					+ GridDBOpenHelper.COLUMN_Semantic + ", asText("
					+ GridDBOpenHelper.COLUMN_GEOMETRY + ") , "
					+ GridDBOpenHelper.COLUMN_SENSITIVITY + " from "
					+ GridDBOpenHelper.TABLE_GRIDCELLS;
			Stmt stmt = spatialdb.prepare(query);
			while (stmt.step()) {
				int id = stmt.column_int(0);
				String semantic = stmt.column_string(1);
				String geometry = stmt.column_string(2);
				Double sensitivity = stmt.column_double(3);
				MyPolygon polygon = new MyPolygon(id + "", semantic,
						MyPolygon.parseSpatialPolygon(geometry), sensitivity);
				polygons.add(polygon);
				//				Log.d(LOGTAG, polygon.toString());
			}
			stmt.close();

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
		Log.d(LOGTAG, "ROWs " + polygons.size());
		return polygons;
	}

	private void deleteAllData() {
		String query = "delete from " + GridDBOpenHelper.TABLE_GRIDCELLS;
		Log.d(LOGTAG, query);
		try {
			spatialdb.exec(query, null);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
	}

	private void insertPolygonIntoDB(MyPolygon polygon) {
		String spatialitePolygon = polygon.convertToSpatialiteString();
		String query = "INSERT INTO " + GridDBOpenHelper.TABLE_GRIDCELLS + " ("
				+ GridDBOpenHelper.COLUMN_ID + ", " + GridDBOpenHelper.COLUMN_GEOMETRY
				+ ") VALUES (" + polygon.getName() + ",GeomFromText(" + spatialitePolygon
				+ ", 4326));";
		//Log.d(LOGTAG, query);
		try {
			spatialdb.exec(query, null);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
	}

	public long findRowsCount() {
		long rowsCount = -1;
		try {
			String query = "select count(" + GridDBOpenHelper.COLUMN_GEOMETRY + ") from "
					+ GridDBOpenHelper.TABLE_GRIDCELLS;
			Stmt stmt = spatialdb.prepare(query);
			if (stmt.step()) {
				rowsCount = stmt.column_int(0);
				return rowsCount;
			}
			stmt.close();

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}

		return rowsCount;
	}

	public void repopulateGridDB() {
		// top Left corner
		int gridHeightCells = Utils.LAUSSANE_GRID_HEIGHT_CELLS;//must be odd number [1,3,5,...,15]
		int gridWidthCells = Utils.LAUSSANE_GRID_WIDTH_CELLS; //must be odd number [1,3,5,...,15]
		LatLng centerPoint = new LatLng(46.526092, 6.584415);
		LatLng topLeftPoint = Utils.findTopLeftPoint(centerPoint, gridHeightCells, gridWidthCells);

		// generate Map Grid
		int arrRows = gridHeightCells + 1;
		int arrCols = gridWidthCells + 1;
		LatLng[][] mapGrid = Utils.generateMapGrid(arrRows, arrCols, topLeftPoint);

		//delete all data
		deleteAllData();

		// Insert GridCells Into DB
		for (int i = 0; i < arrRows - 1; i++) {
			for (int j = 0; j < arrCols - 1; j++) {
				ArrayList<LatLng> cornerPoints = new ArrayList<LatLng>();
				cornerPoints.add(mapGrid[i][j]);
				cornerPoints.add(mapGrid[i][j + 1]);
				cornerPoints.add(mapGrid[i + 1][j + 1]);
				cornerPoints.add(mapGrid[i + 1][j]);
				int gridCellId = i * gridWidthCells + j;
				MyPolygon polygon = new MyPolygon(gridCellId + "", "", cornerPoints);
				insertPolygonIntoDB(polygon);
			}
			Log.d(LOGTAG, "Inserting Row:" + i);
		}
	}

	public MyPolygon findGridCell(Integer cellID) {
		MyPolygon polygon = null;
		try {
			String query = "select  " + GridDBOpenHelper.COLUMN_ID + ", "
					+ GridDBOpenHelper.COLUMN_Semantic + ", asText("
					+ GridDBOpenHelper.COLUMN_GEOMETRY + "), "
					+ GridDBOpenHelper.COLUMN_SENSITIVITY + " from "
					+ GridDBOpenHelper.TABLE_GRIDCELLS + " where " + GridDBOpenHelper.COLUMN_ID
					+ " = " + cellID + " ;";

			Stmt stmt = spatialdb.prepare(query);
			while (stmt.step()) {
				int id = stmt.column_int(0);
				String semantic = stmt.column_string(1);
				String geometry = stmt.column_string(2);
				Double sensitivity = stmt.column_double(3);
				if (polygon != null)
					throw new Exception("Multiple results for cellID:" + cellID);
				polygon = new MyPolygon(id + "", semantic, MyPolygon.parseSpatialPolygon(geometry),
						sensitivity);

			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
		Log.d(LOGTAG, "ROWs 1");
		return polygon;
	}

	// find a grid cell for specific Location 
	public MyPolygon findGridCell(double latitude, double longitude) {
		MyPolygon polygon = null;
		Log.d(LOGTAG, "start query");
		try {

			String query = "SELECT " + GridDBOpenHelper.COLUMN_ID + ", "
					+ GridDBOpenHelper.COLUMN_Semantic + ", asText("
					+ GridDBOpenHelper.COLUMN_GEOMETRY + ") , "
					+ GridDBOpenHelper.COLUMN_SENSITIVITY
					+ " FROM gridcells  WHERE MBRContains( geometry, BuildMBR( " + longitude
					+ "  ," + latitude + ", " + longitude + "  , " + latitude + " ) );";
			Log.d(LOGTAG, query);

			long s1 = System.currentTimeMillis();
			Stmt stmt = spatialdb.prepare(query);
			Log.d(LOGTAG, "prepare time: " + (System.currentTimeMillis() - s1) + "ms ");

			long s2 = System.currentTimeMillis();
			while (stmt.step()) {
				Log.d(LOGTAG, "step time: " + (System.currentTimeMillis() - s2) + "ms ");
				int id = stmt.column_int(0);
				String semantic = stmt.column_string(1);
				String geometry = stmt.column_string(2);
				Double sensitivity = stmt.column_double(3);

				if (polygon != null) {
					throw new Exception("Multiple results for Lat/Lng:" + latitude + "/"
							+ longitude);
				}
				polygon = new MyPolygon(id + "", semantic, MyPolygon.parseSpatialPolygon(geometry),
						sensitivity);
			}
			stmt.close();

			Log.d(LOGTAG, "end query");
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
		Log.d(LOGTAG, "ROWs 1");
		return polygon;
	}

	public LatLng getCentroid(int locID) {
		LatLng centroid = null;
		try {
			String query = "select X(Centroid(" + GridDBOpenHelper.COLUMN_GEOMETRY
					+ ")), Y(Centroid(" + GridDBOpenHelper.COLUMN_GEOMETRY + ")) from "
					+ GridDBOpenHelper.TABLE_GRIDCELLS + " where id =" + locID + " ;";
			Stmt stmt = spatialdb.prepare(query);
			while (stmt.step()) {
				double longitude = stmt.column_double(0);
				double latitude = stmt.column_double(1);
				return new LatLng(latitude, longitude);
			}
			stmt.close();

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
		return centroid;
	}

	public void updateGridCellSensititivity(int gridId, Double sensitivity) {
		try {
			String query = "UPDATE " + GridDBOpenHelper.TABLE_GRIDCELLS + " SET "
					+ GridDBOpenHelper.COLUMN_SENSITIVITY + "=" + sensitivity + " WHERE "
					+ GridDBOpenHelper.COLUMN_ID + "=" + gridId + ";";
			spatialdb.exec(query, null);

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
	}

	public ArrayList<MyPolygon> findSensitiveGridCells() {
		ArrayList<MyPolygon> polygons = new ArrayList<MyPolygon>();
		try {
			String query = "select  " + GridDBOpenHelper.COLUMN_ID + ", "
					+ GridDBOpenHelper.COLUMN_Semantic + ", asText("
					+ GridDBOpenHelper.COLUMN_GEOMETRY + ") , "
					+ GridDBOpenHelper.COLUMN_SENSITIVITY + " from "
					+ GridDBOpenHelper.TABLE_GRIDCELLS + " where "
					+ GridDBOpenHelper.COLUMN_SENSITIVITY + " IS NOT NULL and "
					+ GridDBOpenHelper.COLUMN_SENSITIVITY + " > 0";
			Stmt stmt = spatialdb.prepare(query);
			while (stmt.step()) {
				int id = stmt.column_int(0);
				String semantic = stmt.column_string(1);
				String geometry = stmt.column_string(2);
				Double sensitivity = stmt.column_double(3);
				MyPolygon polygon = new MyPolygon(id + "", semantic,
						MyPolygon.parseSpatialPolygon(geometry), sensitivity);
				polygons.add(polygon);
				//				Log.d(LOGTAG, polygon.toString());
			}
			stmt.close();

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
		Log.d(LOGTAG, "ROWs " + polygons.size());
		return polygons;
	}
}
