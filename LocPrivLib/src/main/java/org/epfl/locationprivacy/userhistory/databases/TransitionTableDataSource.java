package org.epfl.locationprivacy.userhistory.databases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.epfl.locationprivacy.userhistory.models.Transition;
import org.epfl.locationprivacy.util.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TransitionTableDataSource {
	private static final String LOGTAG = "TransitionTableDataSource";
	private static TransitionTableDataSource instance;
	private HashMap<Integer, HashMap<Integer, Integer>> inMemoryTransitionTable;
	private double eta = Math.pow(10, -5);
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

		//load transitions table in memory for fast access
		inMemoryTransitionTable = new HashMap<Integer, HashMap<Integer, Integer>>();
		loadTransitionTableInMemory();
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

			//--> update db
			ContentValues values = new ContentValues();
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_COUNT, count + 1);
			db.update(UserHistoryDBOpenHelper.TABLE_TRANSITIONS, values,
					UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMLOCID + "=? AND "
							+ UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOLOCID + "=?",
					new String[] { transition.fromLocID + "", transition.toLocID + "" });

			//--> update in-memory
			HashMap<Integer, Integer> destinationsMap = inMemoryTransitionTable
					.get(transition.fromLocID);
			destinationsMap.remove(transition.toLocID);
			destinationsMap.put(transition.toLocID, count + 1);
			inMemoryTransitionTable.put(transition.fromLocID, destinationsMap);

		} else { // insert
			Log.d(LOGTAG, "Insert");

			//--> insert into db
			ContentValues values = new ContentValues();
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMLOCID, transition.fromLocID);
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOLOCID, transition.toLocID);
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMTIMEID, transition.fromTimeID);
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOTIMEID, transition.toTimeID);
			values.put(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_COUNT, transition.count);
			db.insert(UserHistoryDBOpenHelper.TABLE_TRANSITIONS, null, values);

			//--> insert into memory
			HashMap<Integer, Integer> destinationsMap = inMemoryTransitionTable
					.get(transition.fromLocID);
			if (destinationsMap == null)
				destinationsMap = new HashMap<Integer, Integer>();
			destinationsMap.put(transition.toLocID, transition.count);
			inMemoryTransitionTable.put(transition.fromLocID, destinationsMap);
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

		int transitionCountFromLocToLoc = getTransitionCount(fromLocID, toLocID);
		int transitionCountFromLoc = getTransitionCount(fromLocID);
		int numberOfGridCells = Utils.GRID_HEIGHT_CELLS * Utils.GRID_WIDTH_CELLS;
		return ((double) (transitionCountFromLocToLoc + eta))
				/ ((double) (transitionCountFromLoc + (numberOfGridCells * eta)));
	}

	private int getTransitionCount(int fromLocID, int toLocID) {

		if (!inMemoryTransitionTable.containsKey(fromLocID))
			return 0;
		HashMap<Integer, Integer> destinationsMap = inMemoryTransitionTable.get(fromLocID);
		if (!destinationsMap.containsKey(toLocID))
			return 0;
		return destinationsMap.get(toLocID);
	}

	private Integer getTransitionCount(int fromID) {

		if (!inMemoryTransitionTable.containsKey(fromID))
			return 0;

		//iterate over the destinations map
		HashMap<Integer, Integer> destinationsMap = inMemoryTransitionTable.get(fromID);
		int totalTransitionCount = 0;
		Iterator<Entry<Integer, Integer>> it = destinationsMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Integer> pairs = (Map.Entry<Integer, Integer>) it.next();
			totalTransitionCount += pairs.getValue();
			it.remove(); // avoids a ConcurrentModificationException
		}
		return totalTransitionCount;
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

	private void loadTransitionTableInMemory() {

		// query all transitions whose count > 0
		Cursor cursor = db.query(UserHistoryDBOpenHelper.TABLE_TRANSITIONS, allColums,
				UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_COUNT + " > 0 ", null, null, null, null,
				null);

		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {

				int transitionFromID = cursor.getInt(cursor
						.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_FROMLOCID));
				int transitionToID = cursor.getInt(cursor
						.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_TOLOCID));
				int transitionCount = cursor.getInt(cursor
						.getColumnIndex(UserHistoryDBOpenHelper.COLUMN_TRANSITIONS_COUNT));

				//update the in-memory data structure
				HashMap<Integer, Integer> destinationsMap = inMemoryTransitionTable
						.get(transitionFromID);
				if (destinationsMap == null)
					destinationsMap = new HashMap<Integer, Integer>();
				destinationsMap.put(transitionToID, transitionCount);
				inMemoryTransitionTable.put(transitionFromID, destinationsMap);

			}
		}
	}

}
