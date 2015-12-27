package org.epfl.locationprivacy.map.databases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import jsqlite.TableResult;

import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.map.models.OSMNode;
import org.epfl.locationprivacy.map.models.OSMRelation;
import org.epfl.locationprivacy.map.models.OSMSemantic;
import org.epfl.locationprivacy.map.models.OSMWay;
import org.epfl.locationprivacy.util.Utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

public class VenuesCondensedDBDataSource {
	private static final String LOGTAG = "VenuesCondensedDBDataSource";
	private static VenuesCondensedDBDataSource instance;

	SQLiteOpenHelper dbHelper;
	SQLiteDatabase db; // this db is just used to copy create the database on the sdcard
	public Database spatialdb;
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
		// on first time, the db will be created
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
			spatialdb.open(Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/Android/data/tinygsn/" + VenuesCondensedDBOpenHelper.DATABASE_NAME, jsqlite.Constants.SQLITE_OPEN_READWRITE);
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

	/**
	 * Insert a polygon into the DB
	 *
	 * @param element
	 * @param polygon
	 */
	public void insertPolygonIntoDB(OSMSemantic element, MyPolygon polygon) {
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, "Insert " + element.toString() + " into DB");
		}
		String spatialitePolygon = polygon.convertPolygonToSpatialiteString();
		String geometry = "GeomFromText";
		String table = VenuesCondensedDBOpenHelper.TABLE_POLYGONS;
		if (element.getClass().equals(OSMNode.class)) {
			geometry = "PointFromText";
			spatialitePolygon = polygon.convertPointToSpatialiteString();
			table = VenuesCondensedDBOpenHelper.TABLE_POINTS;
		}
		String query = "INSERT INTO " + table + " ("
			+ VenuesCondensedDBOpenHelper.COLUMN_LOCATION_ID + ", " + VenuesCondensedDBOpenHelper.COLUMN_NAME + ", "
			+ VenuesCondensedDBOpenHelper.COLUMN_SUBTYPE + ", " + VenuesCondensedDBOpenHelper.COLUMN_VERSION + ", " + VenuesCondensedDBOpenHelper.COLUMN_GEOMETRY
			+ ") VALUES (" + element.getId() + ", " + "\"" + element.getName() + "\"" + ", " + "'" + element.getSubtype() + "'" + "," + element.getVersion() + "," + geometry + "(" + spatialitePolygon
			+ ", 4326));";
		try {
			spatialdb.exec(query, null);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
	}

	/**
	 * Delete an OSMSemantic location
	 *
	 * @param semanticLocation
	 */
	public void delete(OSMSemantic semanticLocation) {
		String table;
		if (semanticLocation.getClass().equals(OSMNode.class)) {
			table = VenuesCondensedDBOpenHelper.TABLE_POINTS;
		} else {
			table = VenuesCondensedDBOpenHelper.TABLE_POLYGONS;
		}

		String strSQL = "DELETE FROM " + table + " WHERE "
			+ VenuesCondensedDBOpenHelper.COLUMN_LOCATION_ID + " = '" + semanticLocation.getId() + "'";

		try {
			spatialdb.exec(strSQL, null);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
	}

	/**
	 * @return incomplete OSM objects with only ID, name, subtype and version
	 */
	public HashMap<String, OSMSemantic> findAllSemanticLocationsInDB() {
		HashMap<String, OSMSemantic> elements = new HashMap<>();

		ArrayList<String> tables = new ArrayList<>();
		tables.add(VenuesCondensedDBOpenHelper.TABLE_POINTS);
		tables.add(VenuesCondensedDBOpenHelper.TABLE_POLYGONS);

		String[] columns = {VenuesCondensedDBOpenHelper.COLUMN_LOCATION_ID,
			VenuesCondensedDBOpenHelper.COLUMN_NAME,
			VenuesCondensedDBOpenHelper.COLUMN_SUBTYPE,
			VenuesCondensedDBOpenHelper.COLUMN_VERSION};
		for (String table : tables) {
			Cursor cursor = db.query(table, columns, null,
				null, null, null, null, null);
			if (cursor.getCount() > 0) {
				while (cursor.moveToNext()) {
					String id = cursor.getString(cursor.getColumnIndex(VenuesCondensedDBOpenHelper.COLUMN_LOCATION_ID));
					String name = cursor.getString(cursor.getColumnIndex(VenuesCondensedDBOpenHelper.COLUMN_NAME));
					String subtype = cursor.getString(cursor.getColumnIndex(VenuesCondensedDBOpenHelper.COLUMN_SUBTYPE));
					String version = cursor.getString(cursor.getColumnIndex(VenuesCondensedDBOpenHelper.COLUMN_VERSION));
					if (subtype.equals("node")) {
						elements.put(id, new OSMNode(id, name, subtype, null, null, null, version));
					} else if (subtype.equals("way")) {
						elements.put(id, new OSMWay(id, name, subtype, null, null, version));
					} else {
						elements.put(id, new OSMRelation(id, name, subtype, null, null, version));
					}
				}
			}
		}
		return elements;
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
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, "================");
		}
		long startTime = System.currentTimeMillis();
		ArrayList<MyPolygon> polygons = new ArrayList<>();
		String table = VenuesCondensedDBOpenHelper.TABLE_POLYGONS;
		// query table
		String sql = "select " + VenuesCondensedDBOpenHelper.COLUMN_NAME + ","
			+ VenuesCondensedDBOpenHelper.COLUMN_SUBTYPE + ",AsText("
			+ VenuesCondensedDBOpenHelper.COLUMN_GEOMETRY + ") from " + table
			+ " where sub_type!='yes' and MBRContains( Geometry,  BuildMBR( " + longitude
			+ " , " + latitude + " , " + longitude + " , " + latitude + " ));";

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
				if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
					Log.d(LOGTAG, "table: " + table + " --> " + polygon.toString());
				}
			}
		}

		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG,
				"Polygons returned: " + polygons.size() + " rows in "
					+ (System.currentTimeMillis() - startTime) + " ms");
		}
		return polygons;
	}

	public Pair<MyPolygon, Double> findNearestVenue(double latitude, double longitude) {
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, "================");
		}
		long startTime = System.currentTimeMillis();
		Double minDistance = Double.MAX_VALUE;
		MyPolygon minPolygon = null;

		String[] tables = {VenuesCondensedDBOpenHelper.TABLE_POLYGONS, VenuesCondensedDBOpenHelper.TABLE_POINTS};
		for (String table : tables) {
			if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
				Log.d(LOGTAG, "table:" + table);
			}
			Pair<MyPolygon, Double> nearest = findNearestVenueInTable(latitude, longitude, table);
			if (nearest != null && nearest.second < minDistance) {
				minDistance = nearest.second;
				minPolygon = nearest.first;
			}
		}

		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, "Time of NearestVenue Query: " + (System.currentTimeMillis() - startTime)
				+ " ms");
		}

		return new Pair<>(minPolygon, minDistance);
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
				String geometry = row[3].trim().replaceAll("[\\(]*([0-9]+.[0-9]+| |,|\\))+", "");
				ArrayList<LatLng> points = MyPolygon.parseSpatialPolygon(geometry, row[3]);
				MyPolygon polygon = new MyPolygon(name, semantic, points);
				polygonWithDistance = new Pair<>(polygon, distance);
			}
		}
		return polygonWithDistance;
	}

	public Pair<MyPolygon, LatLng> findRandomPolygonWithItsCentroid() {
		// query table
		String sql = "SELECT name,sub_type,Y(Centroid(ExteriorRing(Geometry))),X(Centroid(ExteriorRing(Geometry))),"
			+ "AsText(ExteriorRing(Geometry)) FROM polygons where sub_type !='yes' ORDER BY RANDOM() LIMIT 1";

		TableResult tableResult = null;
		try {
			tableResult = spatialdb.get_table(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// parse the results
		if (tableResult != null) {
			Vector<String[]> rows = tableResult.rows;
			String[] row = rows.get(0);
			String name = row[0];
			String semantic = row[1];
			LatLng centroid = new LatLng(Double.parseDouble(row[2]), Double.parseDouble(row[3]));
			ArrayList<LatLng> points = MyPolygon.parseSpatialPolygon("LINESTRING", row[4]);
			MyPolygon polygon = new MyPolygon(name, semantic, points);
			return new Pair<>(polygon, centroid);
		}
		return null;
	}
}