package org.epfl.locationprivacy.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LocationDBOpenHelper extends SQLiteOpenHelper {

	private static final String LOGTAG = "LocationDBOpenHelper";

	private static final String DATABASE_NAME = "location.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_SEMANTICLOCATIONS = "semanticlocations";
	public static final String COLUMN_SEMANTICLOCATION_ID = "semanticLocationID";
	public static final String COLUMN_SEMANTICLOCATION_NAME = "semanticLocationName";
	public static final String COLUMN_SEMANTICLOCATION_USERSENSITIVITY = "semanticLocationUserSensitivity";

	private static final String TABLE_SEMANTICLOCATIONS_CREATE = "CREATE TABLE "
			+ TABLE_SEMANTICLOCATIONS + " (" + COLUMN_SEMANTICLOCATION_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT" + " , " + COLUMN_SEMANTICLOCATION_NAME
			+ " VARCHAR(50) " + " , " + COLUMN_SEMANTICLOCATION_USERSENSITIVITY + " INTEGER "
			+ ")";

	public static LocationDBOpenHelper getInstance(Context context) {
		if (locationDBOpenHelper == null) {
			locationDBOpenHelper = new LocationDBOpenHelper(context);
		}
		return locationDBOpenHelper;
	}

	private static LocationDBOpenHelper locationDBOpenHelper;

	private LocationDBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(LOGTAG, "Helper onCreate, creating the database");
		db.execSQL(TABLE_SEMANTICLOCATIONS_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(LOGTAG, "Helper onUpgrade, upgrading the database");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEMANTICLOCATIONS_CREATE);
		onCreate(db);
	}
}
