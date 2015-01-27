package org.epfl.locationprivacy.supplementary.dbpopulator;

import java.util.ArrayList;
import java.util.Vector;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import jsqlite.TableResult;

import org.epfl.locationprivacy.map.databases.VenuesCondensedDBDataSource;
import org.epfl.locationprivacy.map.databases.VenuesCondensedDBOpenHelper;
import org.epfl.locationprivacy.map.models.MyPolygon;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class VenuesDBDataSource {

	private static final String LOGTAG = "VenuesDBDataSource";
	private static VenuesDBDataSource instance;

	SQLiteOpenHelper dbHelper;
	SQLiteDatabase db; // this db is just used to copy the db from assets folder to the application data on the first time
	Database spatialdb;
	Context context;

	public static VenuesDBDataSource getInstance(Context context) {
		if (instance == null)
			instance = new VenuesDBDataSource(context);
		return instance;
	}

	private VenuesDBDataSource(Context context) {
		this.context = context;
		dbHelper = VenuesDBOpenHelper.getInstance(context);
		open();
	}

	private void open() {
		// on first time, the db will be copied to the application data from assets folder
		db = dbHelper.getWritableDatabase();

		// open spatial db
		spatialdb = openSpatialDB();

		Log.i(LOGTAG, "Venues DataBase opened");
	}

	public void close() {
		dbHelper.close();
		closeSpatialDB();
		Log.i(LOGTAG, "Venues DataBase closed");
	}

	private Database openSpatialDB() {
		Database spatialdb = new jsqlite.Database();
		try {
			spatialdb.open(context.getDatabasePath(VenuesDBOpenHelper.DATABASE_NAME).getPath(),
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

	public ArrayList<MyPolygon> findSampleFromPGAmenity() {
		ArrayList<MyPolygon> polygons = new ArrayList<MyPolygon>();
		//		String query = "select name, sub_type, GeometryType(Geometry) as type, AsText(Geometry) as locationgeometry from pg_amenity where name not NULL AND locationgeometry not NULL";
		String query = "select  " + VenuesDBOpenHelper.COLUMN_NAME + ", "
				+ VenuesDBOpenHelper.COLUMN_SUBTYPE + ", GeometryType("
				+ VenuesDBOpenHelper.COLUMN_GEOMETRY + ") as type ,   AsText("
				+ VenuesDBOpenHelper.COLUMN_GEOMETRY + ") as locationgeometry from "
				+ VenuesDBOpenHelper.TABLE_PG_AMENITY
				+ " where name not NULL AND locationgeometry not NULL";
		try {
			Stmt stmt = spatialdb.prepare(query);
			while (stmt.step()) {
				String name = stmt.column_string(0);
				String semantic = stmt.column_string(1);
				String geometry = stmt.column_string(3);
				MyPolygon polygon = new MyPolygon(name, semantic,
						MyPolygon.parseSpatialMulipolygon(geometry));
				Log.d(LOGTAG, polygon.toString());
				polygons.add(polygon);
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOGTAG, e.getMessage());
		}
		Log.d(LOGTAG, "....Rows: " + polygons.size() + "\n");

		return polygons;
	}

	public long findRowsPGAmenity() {
		long rowsCount = -1;
		try {
			String query = "select count(" + VenuesDBOpenHelper.COLUMN_GEOMETRY + ") from "
					+ VenuesDBOpenHelper.TABLE_PG_AMENITY;
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

	public void populateVenuesCondensedDB(VenuesCondensedDBDataSource venuesCondensedDBDataSource) {
		String[] points_tables = new String[] { "pt_addresses", "pt_aeroway", "pt_amenity",
				"pt_barrier", "pt_building", "pt_cycleway", "pt_highway", "pt_historic",
				"pt_landuse", "pt_leisure", "pt_man_made", "pt_natural", "pt_place", "pt_power",
				"pt_railway", "pt_service", "pt_shop", "pt_sport", "pt_tourism", "pt_waterway" };
		String[] lines_tables = new String[] { "ln_aeroway", "ln_amenity", "ln_barrier",
				"ln_boundary", "ln_building", "ln_highway", "ln_landuse", "ln_leisure",
				"ln_man_made", "ln_natural", "ln_power", "ln_railway", "ln_route", "ln_service",
				"ln_waterway" };
		String[] polygons_tables = { "pg_aeroway", "pg_amenity", "pg_barrier", "pg_building",
				"pg_highway", "pg_historic", "pg_landuse", "pg_leisure", "pg_natural", "pg_place",
				"pg_power", "pg_railway", "pg_shop", "pg_sport", "pg_tourism", "pg_waterway" };
		Database condensedSpatialDB = venuesCondensedDBDataSource.spatialdb;

		// populate polygons
		String desTable = "polygons";
		for (String srcTable : polygons_tables) {
			Log.d(LOGTAG, "Clonning table: " + srcTable);
			cloneTable(srcTable, desTable, condensedSpatialDB);
		}

		// populate lines
		desTable = "lines";
		for (String srcTable : lines_tables) {
			Log.d(LOGTAG, "Clonning table: " + srcTable);
			cloneTable(srcTable, desTable, condensedSpatialDB);
		}

		// populate points
		desTable = "points";
		for (String srcTable : points_tables) {
			Log.d(LOGTAG, "Clonning table: " + srcTable);
			cloneTable(srcTable, desTable, condensedSpatialDB);
		}

	}

	private void cloneTable(String srcTable, String desTable, Database condensedSpatialDB) {

		// query table
		String sql = "select * from " + srcTable + ";";
		TableResult tableResult = null;
		try {
			tableResult = spatialdb.get_table(sql);
		} catch (Exception e) {
			Log.e(LOGTAG, "Problem in cloneTable method: " + e.getMessage());
			e.printStackTrace();
		}

		// parse the results
		if (tableResult != null) {
			Vector<String[]> rows = tableResult.rows;
			int logCounter = 0;
			for (String[] row : rows) {
				Log.d(LOGTAG, "table: " + srcTable + " -> " + logCounter++ + "/" + rows.size());
				String id = row[0];
				String subType = row[1];
				String name = row[2];
				String geometry = row[3];

				// insert into the new table
				String query = "INSERT INTO " + desTable + " ("
						+ VenuesCondensedDBOpenHelper.COLUMN_ID + ","
						+ VenuesCondensedDBOpenHelper.COLUMN_SUBTYPE + ","
						+ VenuesCondensedDBOpenHelper.COLUMN_NAME + ","
						+ VenuesCondensedDBOpenHelper.COLUMN_GEOMETRY + ") VALUES ( " + id
						+ " , \"" + subType + "\" , \"" + name + "\" , " + geometry + ") ;";
				//				Log.d(LOGTAG, query);

				try {
					condensedSpatialDB.exec(query, null);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(LOGTAG, e.getMessage());
				}
			}
		}

	}
}
