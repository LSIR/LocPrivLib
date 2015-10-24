package org.epfl.locationprivacy.map.databases;

import java.util.ArrayList;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.util.Utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class GridDBDataSource {
	private static final String LOGTAG = "GridDBDataSource";
	private static GridDBDataSource instance;

	SQLiteOpenHelper dbHelper;
	SQLiteDatabase db; // this db is just used to copy the db from assets folder to the application data on the first time
	Database spatialDB;
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
		// Only for testing purpose
		db = dbHelper.getWritableDatabase();

		// open spatial db
		spatialDB = openSpatialDB();

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
			spatialDB.close();
		} catch (jsqlite.Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
	}

	public ArrayList<MyPolygon> findAllData() {

		ArrayList<MyPolygon> polygons = new ArrayList<MyPolygon>();
		try {
			String query = "select  " + GridDBOpenHelper.COLUMN_ID + ", "
					               + GridDBOpenHelper.COLUMN_SEMANTIC + ", asText("
					               + GridDBOpenHelper.COLUMN_GEOMETRY + ") , "
					               + GridDBOpenHelper.COLUMN_SENSITIVITY + " from "
					               + GridDBOpenHelper.TABLE_GRIDCELLS;
			Stmt stmt = spatialDB.prepare(query);
			while (stmt.step()) {
				int id = stmt.column_int(0);
				String semantic = stmt.column_string(1);
				String geometry = stmt.column_string(2);
				String sensitivityString = stmt.column_string(3);
				Integer sensitivity = sensitivityString == null ? null : Integer
						                                                         .parseInt(sensitivityString);
				MyPolygon polygon = new MyPolygon(id + "", semantic, MyPolygon.parseSpatialPolygon(
						                                                                                  "POLYGON", geometry), sensitivity);
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
			spatialDB.exec(query, null);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
	}

	/**
	 * Save a grid into the DB
	 * @param mapGrid the grid to save
	 * @param gridHeightCells the height of the grid
	 * @param gridWidthCells the width of the grid
	 */
	public void saveGrid(LatLng[][] mapGrid, int gridHeightCells, int gridWidthCells) {
		int arrRows = gridHeightCells + 1;
		int arrCols = gridWidthCells + 1;
		for (int i = 0; i < arrRows - 1; i++) {
			for (int j = 0; j < arrCols - 1; j++) {
				ArrayList<LatLng> cornerPoints = new ArrayList<LatLng>();
				cornerPoints.add(mapGrid[i][j]);
				cornerPoints.add(mapGrid[i][j + 1]);
				cornerPoints.add(mapGrid[i + 1][j + 1]);
				cornerPoints.add(mapGrid[i + 1][j]);
				// FIXME : make ID unique
				int gridCellId = i * gridWidthCells + j;
				MyPolygon polygon = new MyPolygon(gridCellId + "", "", cornerPoints);
				insertPolygonIntoDB(polygon);
			}
			Log.d(LOGTAG, "Inserting Row:" + i);
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
			spatialDB.exec(query, null);
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
			Stmt stmt = spatialDB.prepare(query);
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

	// FIXME : useful ?
	public void repopulateGridDB() {
		// top Left corner
		int gridHeightCells = Utils.LAUSANNE_GRID_HEIGHT_CELLS;//must be odd number [1,3,5,...,15]
		int gridWidthCells = Utils.LAUSANNE_GRID_WIDTH_CELLS; //must be odd number [1,3,5,...,15]
		// FIXME : take the current point
		LatLng centerPoint = new LatLng(46.526092, 6.584415);
		LatLng topLeftPoint = Utils.findTopLeftPoint(centerPoint, gridHeightCells, gridWidthCells);

		// generate Map Grid
		int arrRows = gridHeightCells + 1;
		int arrCols = gridWidthCells + 1;
		LatLng[][] mapGrid = Utils.generateMapGrid(arrRows, arrCols, topLeftPoint);

		//delete all data
		// FIXME : do not remove all data but simply do not create redundant data
		deleteAllData();

		saveGrid(mapGrid, gridHeightCells, gridWidthCells);
	}

	public MyPolygon findGridCell(Integer cellID) {
		MyPolygon polygon = null;
		try {
			String query = "select  " + GridDBOpenHelper.COLUMN_ID + ", "
					               + GridDBOpenHelper.COLUMN_SEMANTIC + ", asText("
					               + GridDBOpenHelper.COLUMN_GEOMETRY + "), "
					               + GridDBOpenHelper.COLUMN_SENSITIVITY + " from "
					               + GridDBOpenHelper.TABLE_GRIDCELLS + " where " + GridDBOpenHelper.COLUMN_ID
					               + " = " + cellID + " ;";

			Stmt stmt = spatialDB.prepare(query);
			while (stmt.step()) {
				int id = stmt.column_int(0);
				String semantic = stmt.column_string(1);
				String geometry = stmt.column_string(2);
				String sensitivityString = stmt.column_string(3);
				Integer sensitivity = sensitivityString == null ? null : Integer
						                                                         .parseInt(sensitivityString);
				if (polygon != null)
					throw new Exception("Multiple results for cellID:" + cellID);
				polygon = new MyPolygon(id + "", semantic, MyPolygon.parseSpatialPolygon("POLYGON",
						                                                                        geometry), sensitivity);

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
					               + GridDBOpenHelper.COLUMN_SEMANTIC + ", asText("
					               + GridDBOpenHelper.COLUMN_GEOMETRY + ") , "
					               + GridDBOpenHelper.COLUMN_SENSITIVITY
					               + " FROM gridcells  WHERE MBRContains( geometry, BuildMBR( " + longitude
					               + "  ," + latitude + ", " + longitude + "  , " + latitude + " ) );";
			Log.d(LOGTAG, query);

			long s1 = System.currentTimeMillis();
			Stmt stmt = spatialDB.prepare(query);
			Log.d(LOGTAG, "prepare time: " + (System.currentTimeMillis() - s1) + "ms ");

			long s2 = System.currentTimeMillis();
			while (stmt.step()) {
				Log.d(LOGTAG, "step time: " + (System.currentTimeMillis() - s2) + "ms ");
				int id = stmt.column_int(0);
				String semantic = stmt.column_string(1);
				String geometry = stmt.column_string(2);
				String sensitivityString = stmt.column_string(3);
				Integer sensitivity = sensitivityString == null ? null : Integer
						                                                         .parseInt(sensitivityString);

				if (polygon != null) {
					throw new Exception("Multiple results for Lat/Lng:" + latitude + "/"
							                    + longitude);
				}
				polygon = new MyPolygon(id + "", semantic, MyPolygon.parseSpatialPolygon("POLYGON",
						                                                                        geometry), sensitivity);
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
			Stmt stmt = spatialDB.prepare(query);
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

	public void updateGridCellSensitivity(int gridId, Integer sensitivity) {
		try {
			String sensitivityString = sensitivity == null ? "NULL" : sensitivity + "";
			String query = "UPDATE " + GridDBOpenHelper.TABLE_GRIDCELLS + " SET "
					               + GridDBOpenHelper.COLUMN_SENSITIVITY + "=" + sensitivityString + " WHERE "
					               + GridDBOpenHelper.COLUMN_ID + "=" + gridId + ";";
			spatialDB.exec(query, null);

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
	}

	public ArrayList<MyPolygon> findSensitiveGridCells() {
		ArrayList<MyPolygon> polygons = new ArrayList<MyPolygon>();
		try {
			String query = "select  " + GridDBOpenHelper.COLUMN_ID + ", "
					               + GridDBOpenHelper.COLUMN_SEMANTIC + ", asText("
					               + GridDBOpenHelper.COLUMN_GEOMETRY + ") , "
					               + GridDBOpenHelper.COLUMN_SENSITIVITY + " from "
					               + GridDBOpenHelper.TABLE_GRIDCELLS + " where "
					               + GridDBOpenHelper.COLUMN_SENSITIVITY + " IS NOT NULL ";
			Stmt stmt = spatialDB.prepare(query);
			while (stmt.step()) {
				int id = stmt.column_int(0);
				String semantic = stmt.column_string(1);
				String geometry = stmt.column_string(2);
				String sensitivityString = stmt.column_string(3);
				Integer sensitivity = sensitivityString == null ? null : Integer
						                                                         .parseInt(sensitivityString);
				MyPolygon polygon = new MyPolygon(id + "", semantic, MyPolygon.parseSpatialPolygon(
						                                                                                  "POLYGON", geometry), sensitivity);
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
