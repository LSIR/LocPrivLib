package org.epfl.locationprivacy.map.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class GridDBOpenHelper extends SQLiteOpenHelper {

	private static final String LOGTAG = "GridDBOpenHelper";

	public static final String DATABASE_NAME = "grid.sqlite";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_GRIDCELLS = "gridcells";
	public static final String COLUMN_AUTO_ID = "id";
	public static final String COLUMN_ID = "cellid";
	public static final String COLUMN_GEOMETRY = "geometry";
	public static final String COLUMN_SEMANTIC = "semantic";
	public static final String COLUMN_SENSITIVITY = "sensitivity";
	private static final String TABLE_GRIDCELLS_CREATE = "CREATE TABLE "
		+ TABLE_GRIDCELLS + " ("
		+ COLUMN_AUTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + " , "
		+ COLUMN_ID + " UNSIGNED BIG INT" + " , "
		+ COLUMN_GEOMETRY + " VARCHAR(50)" + " , "
		+ COLUMN_SEMANTIC + " VARCHAR(50)" + " , "
		+ COLUMN_SENSITIVITY + " INTEGER"
		+ ")";


	private static GridDBOpenHelper gridDBOpenHelper;

	public static GridDBOpenHelper getInstance(Context context) {
		if (gridDBOpenHelper == null) {
			gridDBOpenHelper = new GridDBOpenHelper(context);
		}
		return gridDBOpenHelper;
	}

	private GridDBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(LOGTAG, "Helper onCreate, creating the database");
		db.execSQL(TABLE_GRIDCELLS_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(LOGTAG, "Helper onUpgrade, upgrading the database");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_GRIDCELLS);
		onCreate(db);
	}

}
