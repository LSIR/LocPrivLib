package org.epfl.locationprivacy.userhistory.databases;

import java.util.ArrayList;

import org.epfl.locationprivacy.userhistory.models.Transition;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

public class TransitionTableDataSource {
	private static final String LOGTAG = "TransitionTableDataSource";
	private static TransitionTableDataSource instance;

	SQLiteOpenHelper dbHelper;
	SQLiteDatabase db;
	Context context;

	private static final String[] allColums = {
			UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMLOCID,
			UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOLOCID,
			UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMTIMEID,
			UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOTIMEID,
			UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_COUNT };

	public static TransitionTableDataSource getInstance(Context context) {
		if (instance == null)
			instance = new TransitionTableDataSource(context);
		return instance;
	}

	private TransitionTableDataSource(Context context) {
		this.context = context;
		dbHelper = UserHistoryDBOpenHelper.getInstance(context);
		open();
	}

	private void open() {
		Log.i(LOGTAG, "DataBase userhistory opened");
		db = dbHelper.getWritableDatabase();
	}

	public void close() {
		Log.i(LOGTAG, "DataBase userhistory closed");
		dbHelper.close();
	}

	public void updateOrInsert(Transition transition) {

		// get previous transition count
		int count = getTransitionCount(transition.fromLocID, transition.toLocID);
		Log.d(LOGTAG, "Count : " + count);

		if (count > 0) { //update
			Log.d(LOGTAG, "Update");
			ContentValues values = new ContentValues();
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_COUNT, count + 1);
			db.update(UserHistoryDBOpenHelper.TABLE_TRANSITIONS, values,
					UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMLOCID + "=? AND "
							+ UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOLOCID + "=?",
					new String[] { transition.fromLocID + "", transition.toLocID + "" });

		} else { // insert
			Log.d(LOGTAG, "Insert");
			ContentValues values = new ContentValues();
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMLOCID, transition.fromLocID);
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOLOCID, transition.toLocID);
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMTIMEID, transition.fromTimeID);
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOTIMEID, transition.toTimeID);
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_COUNT, transition.count);

			db.insert(UserHistoryDBOpenHelper.TABLE_TRANSITIONS, null, values);
		}
	}

	public int countRows() {
		Cursor cursor = db.query(UserHistoryDBOpenHelper.TABLE_TRANSITIONS, allColums, null, null,
				null, null, null, null);
		Log.i(LOGTAG, "Returned " + cursor.getCount() + " rows");
		return cursor.getCount();
	}

	public ArrayList<Transition> findAll() {
		ArrayList<Transition> transitions = new ArrayList<Transition>();

		Cursor cursor = db.query(UserHistoryDBOpenHelper.TABLE_TRANSITIONS, allColums, null, null,
				null, null, null);
		Log.i(LOGTAG, "Returned " + cursor.getCount() + " rows");
		if (cursor.getCount() > 0)
			while (cursor.moveToNext())
				transitions.add(parseDBRow(cursor));
		return transitions;
	}

	public double getTransitionProbability(int fromLocID, int toLocID) {
		double eta = Math.pow(1, -5);
		int numerator = getTransitionCount(fromLocID, toLocID);
		Pair<Integer, Integer> transitionInfo = getTransitionCount(fromLocID);
		int totalTransitionCount = transitionInfo.first;
		int possibleDestinationsCount = transitionInfo.second;

		return ((double) numerator + eta)
				/ ((double) totalTransitionCount + possibleDestinationsCount * eta);
	}

	private Transition parseDBRow(Cursor cursor) {
		// Loc ID
		int fromLocID = cursor.getInt(cursor
				.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMLOCID));
		int toLocID = cursor.getInt(cursor
				.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOLOCID));

		// Time ID
		int fromTimeID = cursor.getInt(cursor
				.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMTIMEID));
		int toTimeID = cursor.getInt(cursor
				.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOTIMEID));

		// count
		int count = cursor.getInt(cursor
				.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_COUNT));

		Transition transition = new Transition(fromLocID, toLocID, fromTimeID, toTimeID, count);
		return transition;
	}

	private int getTransitionCount(int fromLocID, int toLocID) {
		Cursor cursor = db.query(UserHistoryDBOpenHelper.TABLE_TRANSITIONS, allColums,
				UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMLOCID + "=? AND "
						+ UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOLOCID + "=?", new String[] {
						fromLocID + "", toLocID + "" }, null, null, null, null);

		int rows = cursor.getCount();
		if (rows == 1) {
			cursor.moveToNext();
			return parseDBRow(cursor).count;
		} else
			return 0; // means this transition doesn't exist
	}

	private Pair<Integer, Integer> getTransitionCount(int fromID) {
		final Cursor cursor = db.rawQuery("SELECT sum("
				+ UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_COUNT + "), count("
				+ UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOLOCID + ")  FROM "
				+ UserHistoryDBOpenHelper.TABLE_TRANSITIONS + " where "
				+ UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMLOCID + " = " + fromID + " ;",
				null);
		int possibleDestinationsCount = 0;
		int totalTransitionCount = 0;
		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					totalTransitionCount = cursor.getInt(0);
					possibleDestinationsCount = cursor.getInt(1);
				}
			} finally {
				cursor.close();
			}
		}
		Pair<Integer, Integer> transitionInformation = new Pair<Integer, Integer>(
				totalTransitionCount, possibleDestinationsCount);
		return transitionInformation;
	}

}
