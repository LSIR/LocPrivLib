package org.epfl.locationprivacy.map.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class VenuesCondensedDBOpenHelper extends SQLiteOpenHelper {

	private static final String LOGTAG = "VenuesCondensedDBOpenHelper";

	public static final String DATABASE_NAME = "semantic_locations.sqlite";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_POLYGONS = "polygons";
	public static final String TABLE_LINES = "lines";
	public static final String TABLE_POINTS = "points";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_LOCATION_ID = "location_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_SUBTYPE = "sub_type";
	public static final String COLUMN_GEOMETRY = "geometry";
	public static final String COLUMN_VERSION = "version";

	public static final String TABLE_SEMANTIC_AREA = "area";
	public static final String COLUMN_FIRST_CORNER_LAT = "first_corner_lat";
	public static final String COLUMN_FIRST_CORNER_LONG = "first_corner_long";
	public static final String COLUMN_SECOND_CORNER_LAT = "second_corner_lat";
	public static final String COLUMN_SECOND_CORNER_LONG = "second_corner_long";
	public static final String COLUMN_DATE = "date";

	private static final String TABLE_POLYGONS_CREATE = "CREATE TABLE "
		+ TABLE_POLYGONS + " ("
		+ COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + " , "
		+ COLUMN_LOCATION_ID + " BIG INT NOT NULL" + " , "
		+ COLUMN_NAME + " VARCHAR(50) NOT NULL" + " , "
		+ COLUMN_SUBTYPE + " VARCHAR(50) NOT NULL" + " , "
		+ COLUMN_VERSION + " INTEGER NOT NULL" + " , "
		+ COLUMN_GEOMETRY + " BLOB NOT NULL"
		+ ")";

	private static final String TABLE_LINES_CREATE = "CREATE TABLE "
		+ TABLE_LINES + " ("
		+ COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + " , "
		+ COLUMN_LOCATION_ID + " BIG INT NOT NULL" + " , "
		+ COLUMN_NAME + " VARCHAR(50) NOT NULL" + " , "
		+ COLUMN_SUBTYPE + " VARCHAR(50) NOT NULL" + " , "
		+ COLUMN_VERSION + " INTEGER NOT NULL" + " , "
		+ COLUMN_GEOMETRY + " BLOB NOT NULL"
		+ ")";

	private static final String TABLE_POINTS_CREATE = "CREATE TABLE "
		+ TABLE_POINTS + " ("
		+ COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + " , "
		+ COLUMN_LOCATION_ID + " BIG INT NOT NULL" + " , "
		+ COLUMN_NAME + " VARCHAR(50) NOT NULL" + " , "
		+ COLUMN_SUBTYPE + " VARCHAR(50) NOT NULL" + " , "
		+ COLUMN_VERSION + " INTEGER NOT NULL" + " , "
		+ COLUMN_GEOMETRY + " BLOB NOT NULL"
		+ ")";

	private static final String TABLE_SEMANTIC_AREA_CREATE = "CREATE TABLE "
		+ TABLE_SEMANTIC_AREA + " ("
		+ COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + " , "
		+ COLUMN_FIRST_CORNER_LAT + " DOUBLE NOT NULL" + " , "
		+ COLUMN_FIRST_CORNER_LONG + " DOUBLE NOT NULL" + " , "
		+ COLUMN_SECOND_CORNER_LAT + " DOUBLE NOT NULL" + " , "
		+ COLUMN_SECOND_CORNER_LONG + " DOUBLE NOT NULL" + " , "
		+ COLUMN_DATE + " DATETIME NOT NULL"
		+ ")";

	private static VenuesCondensedDBOpenHelper venuesCondensedDBOpenHelper;

	public static VenuesCondensedDBOpenHelper getInstance(Context context) {
		if (venuesCondensedDBOpenHelper == null) {
			venuesCondensedDBOpenHelper = new VenuesCondensedDBOpenHelper(context);
		}
		return venuesCondensedDBOpenHelper;
	}

	private VenuesCondensedDBOpenHelper(Context context) {
		super(context, Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/Android/data/tinygsn/" + DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(LOGTAG, "Helper onCreate, creating the database");
		db.execSQL(TABLE_POLYGONS_CREATE);
		db.execSQL(TABLE_POINTS_CREATE);
		db.execSQL(TABLE_LINES_CREATE);
		db.execSQL(TABLE_SEMANTIC_AREA_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(LOGTAG, "Helper onUpgrade, upgrading the database");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_POLYGONS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_POINTS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEMANTIC_AREA);
		onCreate(db);
	}
}
