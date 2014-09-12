package org.epfl.locationprivacy.supplementary.dbpopulator;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import android.content.Context;

public class VenuesDBOpenHelper extends SQLiteAssetHelper {
	private static final String LOGTAG = "VenuesDBOpenHelper";

	public static final String DATABASE_NAME = "laussane4.sqlite";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_PG_AMENITY = "pg_amenity";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_SUBTYPE = "sub_type";
	public static final String COLUMN_GEOMETRY = "Geometry";

	private static VenuesDBOpenHelper venuesDBOpenHelper;

	public static VenuesDBOpenHelper getInstance(Context context) {
		if (venuesDBOpenHelper == null) {
			venuesDBOpenHelper = new VenuesDBOpenHelper(context);
		}
		return venuesDBOpenHelper;
	}

	private VenuesDBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
}
