package org.epfl.locationprivacy.userhistory.databases;

import java.util.ArrayList;

import org.epfl.locationprivacy.userhistory.models.Location;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LocationTableDataSource {
	private static final String LOGTAG = "LocationTableDataSource";

	SQLiteOpenHelper dbHelper;
	SQLiteDatabase db;
	Context context;

	private static final String[] allColums = { UserHistoryDBOpenHelper.COLUMN_LOCATIONS_UID,
			UserHistoryDBOpenHelper.COLUMN_LOCATIONS_LATITUDE,
			UserHistoryDBOpenHelper.COLUMN_LOCATIONS_LONGITUDE,
			UserHistoryDBOpenHelper.COLUMN_LOCATIONS_TIMESTAMP };

	public LocationTableDataSource(Context context) {
		this.context = context;
		dbHelper = UserHistoryDBOpenHelper.getInstance(context);
	}

	public void open() {
		Log.i(LOGTAG, "DataBase userhistory opened");
		db = dbHelper.getWritableDatabase();
	}

	public void close() {
		Log.i(LOGTAG, "DataBase userhistory closed");
		dbHelper.close();
	}

	public Location create(Location location) {

		ContentValues values = new ContentValues();
		values.put(UserHistoryDBOpenHelper.COLUMN_LOCATIONS_LATITUDE, location.latitude);
		values.put(UserHistoryDBOpenHelper.COLUMN_LOCATIONS_LONGITUDE, location.longitude);
		values.put(UserHistoryDBOpenHelper.COLUMN_LOCATIONS_TIMESTAMP, location.timestamp);

		db.insert(UserHistoryDBOpenHelper.TABLE_LOCATIONS, null, values);
		return location;
	}

	public int countRows() {
		Cursor cursor = db.query(UserHistoryDBOpenHelper.TABLE_LOCATIONS, allColums, null, null,
				null, null, null, null);
		Log.i(LOGTAG, "Returned " + cursor.getCount() + " rows");
		return cursor.getCount();
	}

	public ArrayList<Location> findAll() {
		ArrayList<Location> locations = new ArrayList<Location>();

		Cursor cursor = db.query(UserHistoryDBOpenHelper.TABLE_LOCATIONS, allColums, null, null,
				null, null, null);
		Log.i(LOGTAG, "Returned " + cursor.getCount() + " rows");
		if (cursor.getCount() > 0)
			while (cursor.moveToNext())
				locations.add(parseDBRow(cursor));
		return locations;
	}

	private Location parseDBRow(Cursor cursor) {
		double latitude = cursor.getDouble(cursor
				.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_LOCATIONS_LATITUDE));
		double longitude = cursor.getDouble(cursor
				.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_LOCATIONS_LONGITUDE));
		long timestamp = cursor.getLong(cursor
				.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_LOCATIONS_TIMESTAMP));
		Location location = new Location(latitude, longitude, timestamp);
		return location;
	}
}
