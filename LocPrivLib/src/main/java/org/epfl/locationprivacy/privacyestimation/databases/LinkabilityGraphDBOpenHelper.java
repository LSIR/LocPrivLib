package org.epfl.locationprivacy.privacyestimation.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LinkabilityGraphDBOpenHelper extends SQLiteOpenHelper {
	private static final String LOGTAG = "LinkabilityGraphDBOpenHelper";
	private static final String DATABASE_NAME = "linkabilitygraph.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_EVENTS = "events";
	public static final String TABLE_PARENTCHILDREN = "parentchildren";

	public static final String COLUMN_EVENTS_ID = "id";
	public static final String COLUMN_EVENTS_LEVELID = "levelid";
	public static final String COLUMN_EVENTS_LOCID = "locid";
	public static final String COLUMN_EVENTS_CELL = "cell";
	public static final String COLUMN_EVENTS_TIMESTAMP = "timestamp";
	public static final String COLUMN_EVENTS_TIMESTAMPID = "timestampid";
	public static final String COLUMN_EVENTS_PROBABILITY = "probability";
	public static final String COLUMN_EVENTS_CHILDRENTRANSPROBSUM = "childrenTransPorbSum";

	public static final String COLUMN_PARENTCHILDREN_PARENTID = "parentid";
	public static final String COLUMN_PARENTCHILDREN_CHILDID = "childid";
	public static final String COLUMN_PARENTCHILDREN_TRANSPROBABILITY = "transitionprobability";
	public static final String COLUMN_PARENTCHILDREN_LEVELID = "levelid";

	private static final String TABLE_EVENTS_CREATE = "CREATE TABLE " + TABLE_EVENTS + " ("
			+ COLUMN_EVENTS_ID + " INTEGER PRIMARY KEY  " + " , " + COLUMN_EVENTS_LEVELID
			+ " INTEGER  " + " , " + COLUMN_EVENTS_LOCID + " INTEGER " + " , "
			+ COLUMN_EVENTS_TIMESTAMP + " REAL " + " , " + COLUMN_EVENTS_TIMESTAMPID + " INTEGER "
			+ " , " + COLUMN_EVENTS_PROBABILITY + " REAL " + " , "
			+ COLUMN_EVENTS_CHILDRENTRANSPROBSUM + " REAL " + ")";

	private static final String TABLE_PARENTCHILDREN_CREATE = "CREATE TABLE "
			+ TABLE_PARENTCHILDREN + " (" + COLUMN_PARENTCHILDREN_PARENTID + " INTEGER NOT NULL"
			+ " , " + COLUMN_PARENTCHILDREN_CHILDID + " INTEGER NOT NULL" + " , "
			+ COLUMN_PARENTCHILDREN_LEVELID + " INTEGER NOT NULL" + " , "
			+ COLUMN_PARENTCHILDREN_TRANSPROBABILITY + " FLOAT " + " , PRIMARY KEY ("
			+ COLUMN_PARENTCHILDREN_PARENTID + ", " + COLUMN_PARENTCHILDREN_CHILDID + ") " + ")";

	private static LinkabilityGraphDBOpenHelper linkabilityGraphDBOpenHelper;

	public static LinkabilityGraphDBOpenHelper getInstance(Context context) {
		if (linkabilityGraphDBOpenHelper == null) {
			linkabilityGraphDBOpenHelper = new LinkabilityGraphDBOpenHelper(context);
		}
		return linkabilityGraphDBOpenHelper;
	}

	private LinkabilityGraphDBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(LOGTAG, "Helper onCreate, creating the database");
		db.execSQL(TABLE_EVENTS_CREATE);
		db.execSQL(TABLE_PARENTCHILDREN_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARENTCHILDREN);
		onCreate(db);
		Log.i(LOGTAG, "Helper onUpgrade, upgrading the database");
	}
}
