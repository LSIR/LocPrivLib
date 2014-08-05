package org.epfl.locationprivacy.privacyestimation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.userhistory.databases.TransitionTableDataSource;
import org.epfl.locationprivacy.util.Utils;

import android.content.Context;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

public class MyPrivacyEstimator implements PrivacyEstimator {

	private static final int MAX_QUEUE_SIZE = 20;
	private static final int MAX_USER_SPEED_IN_KM_PER_HOUR = 30;
	private static final int MELLISECONDS_IN_HOUR = 3600000;
	Queue<ArrayList<Event>> levels;
	TransitionTableDataSource userHistoryDBDataSource;
	GridDBDataSource gridDBDataSource;
	Context context;

	public MyPrivacyEstimator(Context c) {
		super();
		this.levels = new LinkedList<ArrayList<Event>>();
		this.context = context;
	}

	public double calculatePrivacyEstimation(int fineLocationID,
			ArrayList<Integer> obfRegionCellIDs, long timeStamp) {

		// Phase 0: preparation
		userHistoryDBDataSource = new TransitionTableDataSource(context);
		userHistoryDBDataSource.open();
		gridDBDataSource = new GridDBDataSource(context);
		gridDBDataSource.open();
		int timeStampID = Utils.findDayPortionID(timeStamp);
		ArrayList<Event> currLevelEvents = createNewEventList(obfRegionCellIDs, timeStampID,
				timeStamp);
		// TODO[NaiveImplementation]: what if levels.size = 0
		ArrayList<Event> previousLevelEvents = (!levels.isEmpty()) ? ((LinkedList<ArrayList<Event>>) levels)
				.getLast() : null;

		// Phase 1: transition probabilities
		if (previousLevelEvents != null)
			for (Event e : currLevelEvents) {
				//TODO[DONE]: implement reachability
				ArrayList<Event> parentList = detectReachability(previousLevelEvents, e);
				if (parentList.isEmpty()) {
					//TODO: implement Event removal from graph
					removeEvent(e);
					continue;
				}
				for (Event parent : parentList) {
					//TODO[Done+Question]: How to get transition probability form DB
					double transitionPropability = userHistoryDBDataSource
							.getTransitionProbability(parent.locID, e.locID);
					parent.children.add(e);
					e.parents.add(new Pair<Event, Double>(parent, transitionPropability));
				}
			}

		// Phase 2: propagate graph updates
		if (previousLevelEvents != null)
			for (Event e : previousLevelEvents)
				if (e.children.isEmpty()) {
					//TODO: implement Event removal from graph
					removeEvent(e);
				}

		// Phase 3: event probabilities
		for (Event e : currLevelEvents) {
			if (previousLevelEvents == null) {
				e.propability = 1.0 / (double) obfRegionCellIDs.size();
			} else {
				for (Pair<Event, Double> parentRelation : e.parents) {
					Event parent = parentRelation.first;
					Double transitionProbability = parentRelation.second;
					e.propability += transitionProbability * parent.propability;
				}
			}
		}

		// Phase 4: update Levels
		levels.add(currLevelEvents);
		if (levels.size() > MAX_QUEUE_SIZE) {
			ArrayList<Event> toBeDeleted = levels.poll();
			//TODO[Validate]: implement Event removal from graph
			removeEvents(toBeDeleted);
		}

		// Phase 5: calculate expected distortion
		double expectedDistortion = 0;
		for (Event e : currLevelEvents)
			//TODO[Done]: implement calculate Distance
			expectedDistortion += calculateEuclideanDistance(fineLocationID, e.locID)
					* e.propability;

		// Phase 6: close DB
		userHistoryDBDataSource.close();
		gridDBDataSource.close();

		return expectedDistortion;
	}

	private double calculateEuclideanDistance(int fineLocationID, int obfLocID) {
		int row1 = fineLocationID / Utils.LAUSSANE_GRID_WIDTH_CELLS;
		int col1 = fineLocationID % Utils.LAUSSANE_GRID_WIDTH_CELLS;

		int row2 = obfLocID / Utils.LAUSSANE_GRID_WIDTH_CELLS;
		int col2 = obfLocID % Utils.LAUSSANE_GRID_WIDTH_CELLS;

		return Math.sqrt(Math.pow(Math.abs(row1 - row2), 2) + Math.pow(Math.abs(col1 - col2), 2));
	}

	private void removeEvents(ArrayList<Event> toBeDeleted) {
		for(Event e : toBeDeleted){
			for(Event child : e.children){
				child.parents.remove(e);
			}
		}
	}

	private void removeEvent(Event e) {
	}

	private ArrayList<Event> detectReachability(ArrayList<Event> previousLevelEvents,
			Event currLevelEvent) {
		ArrayList<Event> parents = new ArrayList<Event>();

		LatLng centroid1 = gridDBDataSource.getCentroid(currLevelEvent.locID);
		for (Event previousLevelEvent : previousLevelEvents) {
			LatLng centroid2 = gridDBDataSource.getCentroid(previousLevelEvent.locID);
			double travelDistanceInKm = Utils.distance(centroid1.latitude, centroid1.longitude,
					centroid2.latitude, centroid2.longitude, 'K');
			double travelTimeInHr = (double) (currLevelEvent.timeStamp - previousLevelEvent.timeStamp)
					/ (double) MELLISECONDS_IN_HOUR;
			double travelSpeedInKmPerHr = travelDistanceInKm / travelTimeInHr;
			if (travelSpeedInKmPerHr <= MAX_USER_SPEED_IN_KM_PER_HOUR)
				parents.add(previousLevelEvent);
		}
		return parents;
	}

	private ArrayList<Event> createNewEventList(ArrayList<Integer> obfRegionCellIDs,
			int timeStampID, long timeStamp) {
		ArrayList<Event> eventList = new ArrayList<Event>();
		for (int cellID : obfRegionCellIDs)
			eventList.add(new Event(cellID, timeStampID, timeStamp));
		return eventList;
	}

	private class Event {
		int locID;
		int timeStampID;
		long timeStamp;
		ArrayList<Event> children;
		ArrayList<Pair<Event, Double>> parents;
		double propability;

		public Event(int locID, int timeStampID, long timeStamp) {
			super();
			this.locID = locID;
			this.timeStampID = timeStampID;
			this.timeStamp = timeStamp;
		}
	}
}
