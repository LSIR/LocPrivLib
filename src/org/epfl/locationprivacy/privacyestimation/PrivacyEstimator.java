package org.epfl.locationprivacy.privacyestimation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.privacyestimation.databases.LinkabilityGraphDataSource;
import org.epfl.locationprivacy.userhistory.databases.TransitionTableDataSource;
import org.epfl.locationprivacy.util.Utils;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

public class PrivacyEstimator implements PrivacyEstimatorInterface {

	private static final String LOGTAG = "PrivacyEstimator";

	private static final int MAX_QUEUE_SIZE = 3;
	private static final int MAX_USER_SPEED_IN_KM_PER_HOUR = 30;
	private static final int MELLISECONDS_IN_HOUR = 3600000;
	private Queue<ArrayList<Event>> levels;
	private Context context;
	private LinkabilityGraphDataSource linkabilityGraphDataSource;
	private long currLevelID;
	private long currEventID;

	public PrivacyEstimator(Context c) {
		super();
		this.levels = new LinkedList<ArrayList<Event>>();
		this.context = c;
		linkabilityGraphDataSource = LinkabilityGraphDataSource.getInstance(context);

		//Load currLevelID
		currLevelID = linkabilityGraphDataSource.findMaxLevelID() + 1;
		logLG("currLevelID: " + currLevelID + "");

		//Load currEventID
		currEventID = linkabilityGraphDataSource.findMaxEventID() + 1;
		logLG("currEventID: " + currEventID + "");

		// TODO remove comments
		//load previously saved linkability graph
		//		if (currLevelID != 0) {
		//			long startLoading = System.currentTimeMillis();
		//			logLG("Loading LG from DB");
		//			loadLinkabilityGraphFromDB();
		//			Utils.createNewLoggingFolder();
		//			Utils.createNewLoggingSubFolder();
		//			Utils.logLinkabilityGraph(levels);
		//			logLG("Finished Loading LG from DB in " + (System.currentTimeMillis() - startLoading)
		//					+ " ms");
		//		}
	}

	private void loadLinkabilityGraphFromDB() {

		//--> phase One: load events
		HashMap<Long, Event> storedEvents = new HashMap<Long, Event>();
		for (long level = currLevelID - MAX_QUEUE_SIZE; level < currLevelID; level++) {
			logLG("Loading Level: " + level);
			ArrayList<Event> currLevelEvents = linkabilityGraphDataSource.findLevelEvents(level);
			levels.add(currLevelEvents);

			for (Event e : currLevelEvents)
				storedEvents.put(e.id, e);
		}

		//--> phase Two: load parent child relations
		Iterator<Entry<Long, Event>> it = storedEvents.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Event> pairs = (Map.Entry<Long, Event>) it.next();
			Long childID = pairs.getKey();
			Event child = pairs.getValue();

			//--> query parent child table
			ArrayList<Pair<Long, Double>> parentInformation = linkabilityGraphDataSource
					.findAllParents(childID);
			for (Pair<Long, Double> parentInfo : parentInformation) {
				Long parentID = parentInfo.first;
				Event parent = storedEvents.get(parentID);
				double transProp = parentInfo.second;

				parent.children.add(child);
				child.parents.add(new Pair<Event, Double>(parent, transProp));
			}

		}
	}

	@Override
	public void updateLinkabilityGraph(ArrayList<Event> currLevelEvents) {

		//--> update graph
		levels.add(currLevelEvents);

		//--> check graph size
		if (levels.size() > MAX_QUEUE_SIZE) {
			ArrayList<Event> toBeDeletedLevel = levels.poll();
			//TODO[Validate]: implement Event removal from graph
			removeLevel(toBeDeletedLevel);
		}

		// TODO remove comments 
		//--? Add currLevelEvents in DB
		//		long startSaving = System.currentTimeMillis();
		//		logLG("start saving");
		//		linkabilityGraphDataSource.saveLinkabilityGraphLevel(currLevelEvents, currLevelID);
		//		logLG("end saving " + currLevelEvents.size() + " events  in "
		//				+ (System.currentTimeMillis() - startSaving) + " ms");

		//--> update variables [must be after saving into the db]
		currEventID = maxEventID(currLevelEvents) + 1;
		currLevelID++;

		//--?Logging
		Utils.logLinkabilityGraph(levels);
	}

	public Pair<Double, ArrayList<Event>> calculatePrivacyEstimation(LatLng fineLocation,
			int fineLocationID, ArrayList<Integer> obfRegionCellIDs, long timeStamp) {

		long startPrivacyEstimation = System.currentTimeMillis();
		log("=========================");
		log("obfRegionSize: " + obfRegionCellIDs.size());
		log("Graph Levels: " + levels.size());

		// Phase 0: preparation
		TransitionTableDataSource userHistoryDBDataSource = TransitionTableDataSource
				.getInstance(context);
		GridDBDataSource gridDBDataSource = GridDBDataSource.getInstance(context);

		int timeStampID = Utils.findDayPortionID(timeStamp);
		ArrayList<Event> currLevelEvents = createNewEventList(obfRegionCellIDs, timeStampID,
				timeStamp);
		// TODO[NaiveImplementation]: what if levels.size = 0
		ArrayList<Event> previousLevelEvents = (!levels.isEmpty()) ? ((LinkedList<ArrayList<Event>>) levels)
				.getLast() : null;

		// Phase 1: transition probabilities
		long startPhaseOne = System.currentTimeMillis();
		if (previousLevelEvents != null) {
			ArrayList<Event> unReachableEvents = new ArrayList<Event>();
			for (Event e : currLevelEvents) {
				//TODO[DONE]: implement reachability
				ArrayList<Event> parentList = detectReachability(previousLevelEvents, e,
						gridDBDataSource);
				if (parentList.isEmpty()) {
					//TODO[Validate]: implement Event removal from graph
					unReachableEvents.add(e);
				} else {
					for (Event parent : parentList) {
						//TODO[Done+PopulateDATA]: How to get transition probability form DB
						double transitionPropability = userHistoryDBDataSource
								.getTransitionProbability(parent.locID, e.locID);
						parent.children.add(e);
						e.parents.add(new Pair<Event, Double>(parent, transitionPropability));
					}
				}
			}

			//--> remove unReachableEvents
			for (Event unReachableEvent : unReachableEvents)
				currLevelEvents.remove(unReachableEvent);
		}
		log("phase one took: " + (System.currentTimeMillis() - startPhaseOne) + " ms");

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
				//TODO remove comment
				//				for (Pair<Event, Double> parentRelation : e.parents) {
				//					Event parent = parentRelation.first;
				//					Double transitionProbability = parentRelation.second;
				//					e.propability += transitionProbability * parent.propability;
				//				}
				e.propability = 1.0 / (double) obfRegionCellIDs.size();
			}
		}

		// Phase 4: update Levels
		// [removed to another interface method to be called by adaptive mechanism after choosing the appropriate obf region]

		// Phase 5: calculate expected distortion
		long startPhase5 = System.currentTimeMillis();
		double expectedDistortion = 0;
		for (Event e : currLevelEvents)
			//TODO[Validate]: implement calculate Distance
			expectedDistortion += calculateDistance(fineLocation,
					gridDBDataSource.getCentroid(e.locID))
					* e.propability;
		log("Phase 5 took: " + (System.currentTimeMillis() - startPhase5) + " ms");

		log("Privacy Estimation took: " + (System.currentTimeMillis() - startPrivacyEstimation)
				+ " ms");

		return new Pair<Double, ArrayList<Event>>(expectedDistortion, currLevelEvents);
	}

	private double calculateDistance(LatLng fineLocation, LatLng coarseLocation) {
		return Utils.distance(fineLocation.latitude, fineLocation.longitude,
				coarseLocation.latitude, coarseLocation.longitude, 'K');
	}

	//	private double calculateEuclideanDistance(int fineLocationID, int obfLocID) {
	//		int row1 = fineLocationID / Utils.LAUSSANE_GRID_WIDTH_CELLS;
	//		int col1 = fineLocationID % Utils.LAUSSANE_GRID_WIDTH_CELLS;
	//
	//		int row2 = obfLocID / Utils.LAUSSANE_GRID_WIDTH_CELLS;
	//		int col2 = obfLocID % Utils.LAUSSANE_GRID_WIDTH_CELLS;
	//
	//		return Math.sqrt(Math.pow(Math.abs(row1 - row2), 2) + Math.pow(Math.abs(col1 - col2), 2));
	//	}

	private void removeLevel(ArrayList<Event> toBeDeletedLevel) {
		for (Event e : toBeDeletedLevel) {
			for (Event child : e.children) {
				child.parents = null;
			}
			e = null;
		}
	}

	private void removeEvent(Event e) {
	}

	private ArrayList<Event> detectReachability(ArrayList<Event> previousLevelEvents,
			Event currLevelEvent, GridDBDataSource gridDBDataSource) {
		//		ArrayList<Event> parents = new ArrayList<Event>();
		//
		//		LatLng centroid1 = gridDBDataSource.getCentroid(currLevelEvent.locID);
		//		for (Event previousLevelEvent : previousLevelEvents) {
		//			LatLng centroid2 = gridDBDataSource.getCentroid(previousLevelEvent.locID);
		//			double travelDistanceInKm = Utils.distance(centroid1.latitude, centroid1.longitude,
		//					centroid2.latitude, centroid2.longitude, 'K');
		//			double travelTimeInHr = (double) (currLevelEvent.timeStamp - previousLevelEvent.timeStamp)
		//					/ (double) MELLISECONDS_IN_HOUR;
		//			double travelSpeedInKmPerHr = travelDistanceInKm / travelTimeInHr;
		//			if (travelSpeedInKmPerHr <= MAX_USER_SPEED_IN_KM_PER_HOUR)
		//				parents.add(previousLevelEvent);
		//		}
		//		return parents;
		return previousLevelEvents;
	}

	private ArrayList<Event> createNewEventList(ArrayList<Integer> obfRegionCellIDs,
			int timeStampID, long timeStamp) {
		ArrayList<Event> eventList = new ArrayList<Event>();
		long tempCurrEventID = currEventID;
		for (int cellID : obfRegionCellIDs)
			eventList.add(new Event(tempCurrEventID++, cellID, timeStampID, timeStamp));
		return eventList;
	}

	private void log(String s) {
		Log.d(LOGTAG, s);
	}

	private void logLG(String s) {
		Log.d("LG", s);
	}

	private long maxEventID(ArrayList<Event> currLevelEvents) {
		long max = -1;
		for (Event e : currLevelEvents)
			max = Math.max(max, e.id);
		return max;
	}
}
