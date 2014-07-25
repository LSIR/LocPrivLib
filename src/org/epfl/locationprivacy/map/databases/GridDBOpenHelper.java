package org.epfl.locationprivacy.map.databases;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class GridDBOpenHelper extends SQLiteAssetHelper {

	private static final String LOGTAG = "GridDBOpenHelper";

	public static final String DATABASE_NAME = "laussanegrid.sqlite";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_GRIDCELLS = "gridcells";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_GEOMETRY = "geometry";
	public static final String COLUMN_Semantic = "semantic";

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
}
