package org.epfl.locationprivacy.map.databases;

import java.util.ArrayList;
import java.util.Vector;

import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;

import com.google.android.gms.maps.model.LatLng;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import jsqlite.TableResult;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

public class VenuesCondensedDBDataSource {
	private static final String LOGTAG = "VenuesCondensedDBDataSource";
	private static VenuesCondensedDBDataSource instance;

	SQLiteOpenHelper dbHelper;
	SQLiteDatabase db; // this db is just used to copy the db from assets folder to the application data on the first time
	Database spatialdb;
	Context context;

	public static VenuesCondensedDBDataSource getInstance(Context context) {
		if (instance == null)
			instance = new VenuesCondensedDBDataSource(context);
		return instance;
	}

	private VenuesCondensedDBDataSource(Context context) {
		this.context = context;
		dbHelper = VenuesCondensedDBOpenHelper.getInstance(context);
		open();
	}

	private void open() {
		// on first time, the db will be copied to the application data from assets folder
		db = dbHelper.getWritableDatabase();

		// open spatial db
		spatialdb = openSpatialDB();

		Log.i(LOGTAG, "Venues Condensed DataBase opened");
	}

	public void close() {
		dbHelper.close();
		closeSpatialDB();
		Log.i(LOGTAG, "Venues DataBase closed");
	}

	private Database openSpatialDB() {
		Database spatialdb = new jsqlite.Database();
		try {
			spatialdb.open(context.getDatabasePath(VenuesCondensedDBOpenHelper.DATABASE_NAME)
					.getPath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
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

	public long findRowsCount(String tableName) {
		long rowsCount = -1;
		try {
			String query = "select count(" + VenuesCondensedDBOpenHelper.COLUMN_GEOMETRY
					+ ") from " + tableName;
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

	public ArrayList<MyPolygon> findVenuesContainingLocation(double latitude, double longitude) {
		Log.d(LOGTAG, "================");
		long startTime = System.currentTimeMillis();
		ArrayList<MyPolygon> polygons = new ArrayList<MyPolygon>();
		String table = VenuesCondensedDBOpenHelper.TABLE_POLYGONS;
		// query table
		String sql = "select " + VenuesCondensedDBOpenHelper.COLUMN_NAME + ","
				+ VenuesCondensedDBOpenHelper.COLUMN_SUBTYPE + ",AsText("
				+ VenuesCondensedDBOpenHelper.COLUMN_GEOMETRY + ") from " + table
				+ " where sub_type!='yes' and MBRContains( Geometry,  BuildMBR( " + longitude
				+ " , " + latitude + " , " + longitude + " , " + latitude + " ));";
		//			Log.d(LOGTAG, "SQL: " + sql);

		TableResult tableResult = null;
		try {
			tableResult = spatialdb.get_table(sql);
		} catch (Exception e) {
			Log.e(LOGTAG, "Problem in findVenuesContainingLocation method: " + e.getMessage());
			e.printStackTrace();
		}

		// parse the results
		if (tableResult != null) {
			Vector<String[]> rows = tableResult.rows;
			for (String[] row : rows) {
				String name = row[0];
				String semantic = row[1];
				ArrayList<LatLng> points = MyPolygon.parseSpatialMulipolygon(row[2]);
				MyPolygon polygon = new MyPolygon(name, semantic, points);
				polygons.add(polygon);
				Log.d(LOGTAG, "table: " + table + " --> " + polygon.toString());
			}
		}

		Log.d(LOGTAG,
				"Polygons returned: " + polygons.size() + " rows in "
						+ (System.currentTimeMillis() - startTime) + " ms");
		return polygons;
	}

	public Pair<MyPolygon, Double> findNearestVenue(double latitude, double longitude) {
		Log.d(LOGTAG, "================");
		long startTime = System.currentTimeMillis();
		Double minDistance = Double.MAX_VALUE;
		MyPolygon minPolygon = null;

		String[] tables = { VenuesCondensedDBOpenHelper.TABLE_POLYGONS };
		for (String table : tables) {
			Log.d(LOGTAG, "table:" + table);
			Pair<MyPolygon, Double> nearest = findNearestVenueInTable(latitude, longitude, table);
			if (nearest != null && nearest.second < minDistance) {
				minDistance = nearest.second;
				minPolygon = nearest.first;
			}
		}

		Log.d(LOGTAG, "Time of NearestVenue Query: " + (System.currentTimeMillis() - startTime)
				+ " ms");

		return new Pair<MyPolygon, Double>(minPolygon, minDistance);
	}

	private Pair<MyPolygon, Double> findNearestVenueInTable(double latitude, double longitude,
			String table) {

		Pair<MyPolygon, Double> polygonWithDistance = null;
		String distanceFromLocation = (table.equals(VenuesCondensedDBOpenHelper.TABLE_POLYGONS)) ? "ExteriorRing(Geometry)"
				: "Geometry";
		String distanceToLocation = "BuildMBR( " + longitude + " , " + latitude + " , " + longitude
				+ " , " + latitude + " )";
		// query table
		String sql = "select name,sub_type,Distance (" + distanceFromLocation + " , "
				+ distanceToLocation + " ) as distance,AsText(Geometry)  " + "from " + table
				+ " where sub_type != 'yes' " + "Order By distance asc " + "Limit 1;";
		TableResult tableResult = null;
		try {
			tableResult = spatialdb.get_table(sql);
		} catch (Exception e) {
			Log.e(LOGTAG, "Problem in findNearestVenueInTable method: " + e.getMessage());
			e.printStackTrace();
		}

		// parse the results
		if (tableResult != null) {
			Vector<String[]> rows = tableResult.rows;
			for (String[] row : rows) {
				String name = row[0];
				String semantic = row[1];
				Double distance = Double.parseDouble(row[2]);
				ArrayList<LatLng> points = MyPolygon.parseSpatialMulipolygon(row[3]);
				MyPolygon polygon = new MyPolygon(name, semantic, points);
				polygonWithDistance = new Pair<MyPolygon, Double>(polygon, distance);
			}
		}
		return polygonWithDistance;
	}
}
