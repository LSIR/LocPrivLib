package org.epfl.locationprivacy.privacyprofile.databases;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.epfl.locationprivacy.privacyprofile.models.SemanticLocation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SemanticLocationsDataSource {
	private static final String LOGTAG = "SemanticLocationsDataSource";
	private static final int DEFAULT_USERSENSITIVITY = 0;

	private static SemanticLocationsDataSource instance = null;

	SQLiteOpenHelper dbHelper;
	SQLiteDatabase db;
	Context context;

	private static final String[] allColums = { LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_ID,
			LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_NAME,
			LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_USERSENSITIVITY };

	public static SemanticLocationsDataSource getInstance(Context context) {
		if (instance == null)
			instance = new SemanticLocationsDataSource(context);
		return instance;
	}

	private SemanticLocationsDataSource(Context context) {
		this.context = context;
		dbHelper = LocationDBOpenHelper.getInstance(context);
		open();

		List<SemanticLocation> semanticLocations = finaAll();
		if (semanticLocations.size() == 0) {
			populateDB();
		}
	}

	private void open() {
		Log.i(LOGTAG, "DataBase opened");
		db = dbHelper.getWritableDatabase();
	}

	public void close() {
		Log.i(LOGTAG, "DataBase closed");
		dbHelper.close();
	}

	public SemanticLocation create(SemanticLocation semanticLocation) {

		ContentValues values = new ContentValues();
		values.put(LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_NAME, semanticLocation.name);
		values.put(LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_USERSENSITIVITY,
				semanticLocation.userSentivity);

		long semanticLocationId = db.insert(LocationDBOpenHelper.TABLE_SEMANTICLOCATIONS, null,
				values);
		semanticLocation.id = semanticLocationId;
		return semanticLocation;
	}

	public List<SemanticLocation> finaAll() {
		List<SemanticLocation> semanticLocations = new ArrayList<SemanticLocation>();

		Cursor cursor = db.query(LocationDBOpenHelper.TABLE_SEMANTICLOCATIONS, allColums, null,
				null, null, null, LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_USERSENSITIVITY+" DESC", null);
		Log.i(LOGTAG, "Returned " + cursor.getCount() + " rows");
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {

				long semanticLocationID = cursor.getLong(cursor
						.getColumnIndex(LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_ID));
				String semanticLocationName = cursor.getString(cursor
						.getColumnIndex(LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_NAME));
				int semanticLocationUserSensitivity = cursor
						.getInt(cursor
								.getColumnIndex(LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_USERSENSITIVITY));

				SemanticLocation semanticLocation = new SemanticLocation(semanticLocationID,
						semanticLocationName, semanticLocationUserSensitivity);
				semanticLocations.add(semanticLocation);
			}
		}
		return semanticLocations;
	}

	public Double findSemanticSensitivity(String semanticTag) {


		Cursor cursor = db.query(LocationDBOpenHelper.TABLE_SEMANTICLOCATIONS, allColums,
				LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_NAME + " ='" + semanticTag + "'",
				null, null, null, null, null);
		Log.i(LOGTAG, "Returned " + cursor.getCount() + " rows");
		if (cursor.getCount() > 0 && cursor.moveToNext()) {
			int semanticLocationUserSensitivity = cursor.getInt(cursor
					.getColumnIndex(LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_USERSENSITIVITY));
			return (double) semanticLocationUserSensitivity / 100.0;
		}
		return null; // problem
	}

	public void populateDB() {
		// read file
		HashSet<String> semanticLocations = new HashSet<String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(context.getAssets().open(
					"semantic_locations.txt")));
			String mLine = reader.readLine();
			while (mLine != null) {
				semanticLocations.add(mLine);
				mLine = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			Log.e(LOGTAG, "Failed While reading semantic_locations.txt");
		}

		// add data to DB
		for (String semanticLocationName : semanticLocations) {
			SemanticLocation sl = new SemanticLocation(semanticLocationName,
					DEFAULT_USERSENSITIVITY);
			create(sl);
		}
	}

	public void updateSemanticLocation(long id, int progress) {
		String strSQL = "UPDATE " + LocationDBOpenHelper.TABLE_SEMANTICLOCATIONS + " SET "
				+ LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_USERSENSITIVITY + " = " + progress
				+ " WHERE " + LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_ID + " = " + id;

		db.execSQL(strSQL);
	}
	public void updateSemanticLocation(String name, int progress) {
		String strSQL = "UPDATE " + LocationDBOpenHelper.TABLE_SEMANTICLOCATIONS + " SET "
				+ LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_USERSENSITIVITY + " = " + progress
				+ " WHERE " + LocationDBOpenHelper.COLUMN_SEMANTICLOCATION_NAME + " = '" + name+"'";

		db.execSQL(strSQL);
	}
}
