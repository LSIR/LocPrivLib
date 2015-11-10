package org.epfl.locationprivacy.privacyestimation.databases;

import java.util.ArrayList;

import org.epfl.locationprivacy.privacyestimation.Event;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

public class LinkabilityGraphDataSource {
	private static final String LOGTAG = "LinkabilityGraphDataSource";
	private static LinkabilityGraphDataSource instance;

	SQLiteOpenHelper dbHelper;
	SQLiteDatabase db;
	Context context;

	public static LinkabilityGraphDataSource getInstance(Context context) {
		if (instance == null)
			instance = new LinkabilityGraphDataSource(context);
		return instance;
	}

	private LinkabilityGraphDataSource(Context context) {
		this.context = context;
		dbHelper = LinkabilityGraphDBOpenHelper.getInstance(context);
		open();
	}

	private void open() {
		Log.i(LOGTAG, "DataBase linkability graph opened");
		db = dbHelper.getWritableDatabase();
	}

	public void close() {
		Log.i(LOGTAG, "DataBase linkability graph closed");
		dbHelper.close();
	}

	private void createEvent(Event e, long levelID) {
		ContentValues values = new ContentValues();
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_ID, e.id);
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_LEVELID, levelID);
		// FIXME : to check this value
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_LOCID, e.cellID);
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_TIMESTAMP, e.timeStamp);
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_TIMESTAMPID, e.timeStampID);
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_PROBABILITY, e.probability);
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_CHILDRENTRANSPROBSUM,
				e.childrenTransProbSum);

		db.insert(LinkabilityGraphDBOpenHelper.TABLE_EVENTS, null, values);
	}

	private void createParentChildRelation(long parentID, long childID, double transitionProp,
			long levelID) {

		ContentValues values = new ContentValues();
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_PARENTCHILDREN_PARENTID, parentID);
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_PARENTCHILDREN_CHILDID, childID);
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_PARENTCHILDREN_TRANSPROBABILITY,
				transitionProp);
		values.put(LinkabilityGraphDBOpenHelper.COLUMN_PARENTCHILDREN_LEVELID, levelID);
		db.insert(LinkabilityGraphDBOpenHelper.TABLE_PARENTCHILDREN, null, values);
	}

	public void saveLinkabilityGraphLevel(ArrayList<Event> currLevelEvents, long levelID) {
		try {
			db.beginTransaction();
			for (Event e : currLevelEvents) {
				createEvent(e, levelID);

				for (Pair<Event, Double> parentRelation : e.parents) {
					long parentID = parentRelation.first.id;
					double transProp = parentRelation.second;
					createParentChildRelation(parentID, e.id, transProp, levelID - 1);
				}

			}
			db.setTransactionSuccessful();
		} catch (Exception exeption) {
			exeption.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

	public int findMaxLevelID() {
		final Cursor cursor = db.rawQuery("SELECT MAX("
				+ LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_LEVELID + ")  FROM "
				+ LinkabilityGraphDBOpenHelper.TABLE_EVENTS + ";", null);
		int maxLevelID = -1;
		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					String value = cursor.getString(0) + "";
					if (value.equalsIgnoreCase("null"))
						return -1;
					maxLevelID = cursor.getInt(0);
				}
			} finally {
				cursor.close();
			}
		}
		return maxLevelID;
	}

	public int findMaxEventID() {
		final Cursor cursor = db.rawQuery("SELECT MAX("
				+ LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_ID + ")  FROM "
				+ LinkabilityGraphDBOpenHelper.TABLE_EVENTS + ";", null);
		int maxEventID = -1;
		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					String value = cursor.getString(0) + "";
					if (value.equalsIgnoreCase("null"))
						return -1;
					maxEventID = cursor.getInt(0);
				}
			} finally {
				cursor.close();
			}
		}
		return maxEventID;
	}

	private static final String[] eventTableColums = {
			LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_ID,
			LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_LOCID,
			LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_PROBABILITY,
			LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_CHILDRENTRANSPROBSUM,
			LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_TIMESTAMP,
			LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_TIMESTAMPID };

	public ArrayList<Event> findLevelEvents(long level) {
		Cursor cursor = db.query(LinkabilityGraphDBOpenHelper.TABLE_EVENTS, eventTableColums,
				LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_LEVELID + " = " + level, null, null,
				null, null, null);

		ArrayList<Event> events = new ArrayList<Event>();
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				events.add(parseDBRow(cursor));
			}
		}
		return events;
	}

	private static final String[] parentChildrenTableColums = {
			LinkabilityGraphDBOpenHelper.COLUMN_PARENTCHILDREN_PARENTID,
			LinkabilityGraphDBOpenHelper.COLUMN_PARENTCHILDREN_TRANSPROBABILITY };

	public ArrayList<Pair<Long, Double>> findAllParents(Long childID) {
		Cursor cursor = db.query(LinkabilityGraphDBOpenHelper.TABLE_PARENTCHILDREN,
				parentChildrenTableColums,
				LinkabilityGraphDBOpenHelper.COLUMN_PARENTCHILDREN_CHILDID + " = " + childID, null,
				null, null, null, null);

		ArrayList<Pair<Long, Double>> parentsInfo = new ArrayList<Pair<Long, Double>>();
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				long parentID = cursor
						.getLong(cursor
								.getColumnIndex(LinkabilityGraphDBOpenHelper.COLUMN_PARENTCHILDREN_PARENTID));
				double transProp = cursor
						.getDouble(cursor
								.getColumnIndex(LinkabilityGraphDBOpenHelper.COLUMN_PARENTCHILDREN_TRANSPROBABILITY));
				parentsInfo.add(new Pair<>(parentID, transProp));
			}
		}
		return parentsInfo;
	}

	private Event parseDBRow(Cursor cursor) {
		long eventID = cursor.getLong(cursor
				.getColumnIndex(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_ID));
		int locID = cursor.getInt(cursor
				.getColumnIndex(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_LOCID));
		double prop = cursor.getDouble(cursor
				.getColumnIndex(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_PROBABILITY));
		double childrenTransProbSum = cursor.getDouble(cursor
				.getColumnIndex(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_CHILDRENTRANSPROBSUM));
		long timeStamp = cursor.getLong(cursor
				.getColumnIndex(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_TIMESTAMP));
		int timeStampID = cursor.getInt(cursor
				.getColumnIndex(LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_TIMESTAMPID));

		// FIXME : find a way to store the position in DB
		Event event = new Event(eventID, locID, null, timeStampID, timeStamp, prop, childrenTransProbSum);
		return event;
	}

	public int countEventRows() {
		Cursor cursor = db.query(LinkabilityGraphDBOpenHelper.TABLE_EVENTS, eventTableColums, null,
				null, null, null, null, null);
		return cursor.getCount();
	}

	public int countParentChildrenRows() {
		Cursor cursor = db.query(LinkabilityGraphDBOpenHelper.TABLE_PARENTCHILDREN,
				parentChildrenTableColums, null, null, null, null, null, null);
		return cursor.getCount();
	}

	public void clearDB() {
		db.delete(LinkabilityGraphDBOpenHelper.TABLE_EVENTS, null, null);
		db.delete(LinkabilityGraphDBOpenHelper.TABLE_PARENTCHILDREN, null, null);
	}

	public void removeGraphEventsWithLevelLowerThanOrEqual(long levelID) {
		String whereClause = LinkabilityGraphDBOpenHelper.COLUMN_EVENTS_LEVELID + " <= " + levelID;
		db.delete(LinkabilityGraphDBOpenHelper.TABLE_EVENTS, whereClause, null);
	}

	public void removeGraphEdgesWithLevelLowerThanOrEqual(long levelID) {
		String whereClause = LinkabilityGraphDBOpenHelper.COLUMN_PARENTCHILDREN_LEVELID + " <= "
				+ levelID;
		db.delete(LinkabilityGraphDBOpenHelper.TABLE_PARENTCHILDREN, whereClause, null);
	}
}
