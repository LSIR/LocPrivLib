package org.epfl.locationprivacy.map.databases;

import java.util.ArrayList;

import org.epfl.locationprivacy.map.models.MyPolygon;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class VenuesDBDataSource {

	private static final String LOGTAG = "VenuesDBDataSource";

	SQLiteOpenHelper dbHelper;
	SQLiteDatabase db; // this db is just used to copy the db from assets folder to the application data on the first time
	Database spatialdb;
	Context context;

	public VenuesDBDataSource(Context context) {
		this.context = context;
		dbHelper = VenuesDBOpenHelper.getInstance(context);
	}

	public void open() {
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
}
