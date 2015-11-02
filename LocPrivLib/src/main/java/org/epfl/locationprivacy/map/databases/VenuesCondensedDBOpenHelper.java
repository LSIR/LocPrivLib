package org.epfl.locationprivacy.map.databases;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class VenuesCondensedDBOpenHelper extends SQLiteAssetHelper {

	public static final String DATABASE_NAME = "lausannevenues.sqlite";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_POLYGONS = "polygons";
	public static final String TABLE_LINES = "lines";
	public static final String TABLE_POINTS = "points";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_SUBTYPE = "sub_type";
	public static final String COLUMN_GEOMETRY = "Geometry";

	private static VenuesCondensedDBOpenHelper venuesCondensedDBOpenHelper;

	public static VenuesCondensedDBOpenHelper getInstance(Context context) {
		if (venuesCondensedDBOpenHelper == null) {
			venuesCondensedDBOpenHelper = new VenuesCondensedDBOpenHelper(context);
		}
		return venuesCondensedDBOpenHelper;
	}

	private VenuesCondensedDBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
}
