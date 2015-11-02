package org.epfl.locationprivacy.userhistory.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UserHistoryDBOpenHelper extends SQLiteOpenHelper {
	private static final String LOGTAG = "UserHistoryDBOpenHelper";

	private static final String DATABASE_NAME = "userhistory.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_TRANSITIONS = "transitions";
	public static final String TABLE_LOCATIONS = "locations";

	public static final String COLUMN_TRANSITIONS_FROMLOCID = "fromLocID";
	public static final String COLUMN_TRANSITIONS_TOLOCID = "toLocID";
	public static final String COLUMN_TRANSITIONS_FROMTIMEID = "fromTimeID";
	public static final String COLUMN_TRANSITIONS_TOTIMEID = "toTimeID";
	public static final String COLUMN_TRANSITIONS_COUNT = "count";

	public static final String COLUMN_LOCATIONS_UID = "uID";
	public static final String COLUMN_LOCATIONS_LATITUDE = "latitude";
	public static final String COLUMN_LOCATIONS_LONGITUDE = "longitude";
	public static final String COLUMN_LOCATIONS_TIMESTAMP = "timestamp";

	private static final String TABLE_TRANSITIONS_CREATE = "CREATE TABLE " + TABLE_TRANSITIONS
			+ " (" + COLUMN_TRANSITIONS_FROMLOCID + " INTEGER NOT NULL" + " , "
			+ COLUMN_TRANSITIONS_TOLOCID + " INTEGER NOT NULL" + " , "
			+ COLUMN_TRANSITIONS_FROMTIMEID + " INTEGER " + " , " + COLUMN_TRANSITIONS_TOTIMEID
			+ " INTEGER " + " , " + COLUMN_TRANSITIONS_COUNT + " INTEGER , PRIMARY KEY ("
			+ COLUMN_TRANSITIONS_FROMLOCID + ", " + COLUMN_TRANSITIONS_TOLOCID + ") " + ")";

	private static final String TABLE_LOCATIONS_CREATE = "CREATE TABLE " + TABLE_LOCATIONS + " ("
			+ COLUMN_LOCATIONS_UID + " INTEGER PRIMARY KEY AUTOINCREMENT " + " , "
			+ COLUMN_LOCATIONS_LATITUDE + " REAL " + " , " + COLUMN_LOCATIONS_LONGITUDE + " REAL "
			+ " , " + COLUMN_LOCATIONS_TIMESTAMP + " INTEGER " + ")";

	private static UserHistoryDBOpenHelper userHistoryDBOpenHelper;

	public static UserHistoryDBOpenHelper getInstance(Context context) {
		if (userHistoryDBOpenHelper == null) {
			userHistoryDBOpenHelper = new UserHistoryDBOpenHelper(context);
		}
		return userHistoryDBOpenHelper;
	}

	private UserHistoryDBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(LOGTAG, "Helper onCreate, creating the database");
		db.execSQL(TABLE_TRANSITIONS_CREATE);
		db.execSQL(TABLE_LOCATIONS_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSITIONS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
		onCreate(db);
		Log.i(LOGTAG, "Helper onUpgrade, upgrading the database");
	}
}
