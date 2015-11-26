package org.epfl.locationprivacy.privacyestimation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;

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

	/* Library Parameters */
	private static final boolean SAVE_LINKABILITYGRAPH_IN_DB = false;
	private static final boolean TEST_UPDATES_PROPAGATION = false; //IMPORTANT NOTE: if you enable this boolean, you should set the ALPHA=1 in AdaptiveProtection class
	private static final boolean ACTIVATE_REACHABILITY_FORMULA = false; // IMPORTANT NOTE: not test if true
	private static final int MAX_INMEMORY_GRAPH_LEVELS = 3;
	private static final int MAX_INDB_GRAPH_LEVELS = 3;
	private static final int MAX_USER_SPEED_IN_KM_PER_HOUR = 30;
	private static final int MILLISECONDS_IN_HOUR = 3600000;

	private Queue<ArrayList<Event>> linkabilityGraphLevels;
	private Queue<ArrayList<Event>> lastLinkabilityGraphCopy;
	private Context context;
	private LinkabilityGraphDataSource linkabilityGraphDataSource;
	private long currLevelID;
	private long currEventID;
	private static DecimalFormat formatter = new DecimalFormat(".##E0");
	private static DecimalFormat formatter2 = new DecimalFormat("#.###");
	private static Random rand = new Random();

	public PrivacyEstimator(Context c) {
		super();
		this.linkabilityGraphLevels = new LinkedList<ArrayList<Event>>();
		this.context = c;
		linkabilityGraphDataSource = LinkabilityGraphDataSource.getInstance(context);

		//Load currLevelID
		currLevelID = linkabilityGraphDataSource.findMaxLevelID() + 1;
		logLG("currLevelID: " + currLevelID + "");

		//Load currEventID
		currEventID = linkabilityGraphDataSource.findMaxEventID() + 1;
		logLG("currEventID: " + currEventID + "");

		//load previously saved linkability graph
		if (SAVE_LINKABILITYGRAPH_IN_DB) {
			if (currLevelID != 0) {
				long startLoading = System.currentTimeMillis();
				logLG("Loading LG from DB");
				loadLinkabilityGraphFromDB();
				Utils.createNewLoggingFolder(context, LOGTAG);
				Utils.createNewLoggingSubFolder(context);
				Utils.logLinkabilityGraph(linkabilityGraphLevels, "nodes.txt", "edges.txt", context);
				logLG("Finished Loading LG from DB in "
					+ (System.currentTimeMillis() - startLoading) + " ms");
			}
		}
	}

	private void loadLinkabilityGraphFromDB() {

		//--> phase One: load events
		HashMap<Long, Event> storedEvents = new HashMap<Long, Event>();
		for (long level = currLevelID - MAX_INMEMORY_GRAPH_LEVELS; level < currLevelID; level++) {
			logLG("Loading Level: " + level);
			ArrayList<Event> currLevelEvents = linkabilityGraphDataSource.findLevelEvents(level);
			linkabilityGraphLevels.add(currLevelEvents);

			for (Event e : currLevelEvents) {
				storedEvents.put(e.id, e);
			}
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
				// special case for first level
				if (parent == null)
					continue;
				double transProp = parentInfo.second;

				parent.children.add(child);
				child.parents.add(new Pair<Event, Double>(parent, transProp));
			}

		}
	}

	@Override
	public void saveLastLinkabilityGraphCopy() {

		//--> override original linkability graph with the last copy generated
		linkabilityGraphLevels = lastLinkabilityGraphCopy;

		//--> check in-memory graph size
		if (linkabilityGraphLevels.size() > MAX_INMEMORY_GRAPH_LEVELS) {
			ArrayList<Event> toBeDeletedLevel = linkabilityGraphLevels.poll();
			removeLevel(toBeDeletedLevel);
		}

		//--> check in-db graph size
		if (SAVE_LINKABILITYGRAPH_IN_DB) {
			linkabilityGraphDataSource.removeGraphEventsWithLevelLowerThanOrEqual(currLevelID
				- MAX_INDB_GRAPH_LEVELS);
			linkabilityGraphDataSource.removeGraphEdgesWithLevelLowerThanOrEqual(currLevelID
				- MAX_INDB_GRAPH_LEVELS);
		}

		//--> Add currLevelEvents in DB
		ArrayList<Event> lastLevelEvents = ((LinkedList<ArrayList<Event>>) linkabilityGraphLevels)
			.getLast();
		if (SAVE_LINKABILITYGRAPH_IN_DB) {
			long startSaving = System.currentTimeMillis();
			logLG("start saving");
			linkabilityGraphDataSource.saveLinkabilityGraphLevel(lastLevelEvents, currLevelID);
			logLG("end saving " + lastLevelEvents.size() + " events  in "
				+ (System.currentTimeMillis() - startSaving) + " ms");
		}

		//--> update variables [must be after saving into the db]
		currEventID = maxEventID(lastLevelEvents) + 1;
		currLevelID++;

		//--?Logging
		Utils.logLinkabilityGraph(linkabilityGraphLevels, "nodes.txt", "edges.txt", context);
	}

	public double calculatePrivacyEstimation(LatLng fineLocation, LatLng[][] mapGrid, long timeStamp) {

		//--> logging
		long startPrivacyEstimation = System.currentTimeMillis();
		log("=========================");
		log("obfRegionSize: " + mapGrid.length);
		log("Graph Levels: " + linkabilityGraphLevels.size());

		// Phase 0: preparation
		TransitionTableDataSource userHistoryDBDataSource = TransitionTableDataSource
			.getInstance(context);
		GridDBDataSource gridDBDataSource = GridDBDataSource.getInstance(context);

		int timeStampID = Utils.findDayPortionID(timeStamp);
		LinkedList<ArrayList<Event>> linkabilityGraphCopy = getLinkabilityGraphCopy();
		ArrayList<Event> previousLevelEvents = (!linkabilityGraphCopy.isEmpty()) ? linkabilityGraphCopy
			.getLast() : null;
		ArrayList<Event> currLevelEvents = createNewEventList(mapGrid, timeStampID,
			timeStamp);

		// Phase 1: transition probabilities
		long startPhaseOne = System.currentTimeMillis();
		if (previousLevelEvents != null) {
			Iterator<Event> currLevelEventsIterator = currLevelEvents.iterator();
			while (currLevelEventsIterator.hasNext()) {
				Event e = currLevelEventsIterator.next();
				ArrayList<Event> parentList = detectReachability(previousLevelEvents, e,
					gridDBDataSource);

				if (parentList.isEmpty()) {
					//--> remove unreachable event from current level
					currLevelEventsIterator.remove();
				} else {
					for (Event parent : parentList) {
						//--> transition probability
						double transitionProbability = userHistoryDBDataSource
							.getTransitionProbability(parent.id, e.id);
						parent.childrenTransProbSum += transitionProbability;
						parent.children.add(e);
						e.parents.add(new Pair<>(parent, transitionProbability));
					}
				}
			}
		}
		log("phase one took: " + (System.currentTimeMillis() - startPhaseOne) + " ms");

		// Phase 2: Add last level events to the linkability graph
		linkabilityGraphCopy.add(currLevelEvents);

		//--> testing update propagation
		if (TEST_UPDATES_PROPAGATION) {
			Utils.logLinkabilityGraph(linkabilityGraphCopy,
				"ObfRegionSize" + mapGrid.length + "_BeforePropagation_nodes.txt",
				"ObfRegionSize" + mapGrid.length + "_BeforePropagation_edges.txt",
				context);

		}

		// Phase 3: propagate graph updates
		if (previousLevelEvents != null)
			propagateGraphUpdates(linkabilityGraphCopy);

		// Phase 4: event probabilities
		for (Event e : currLevelEvents) {
			e.probability = 0;
			if (previousLevelEvents == null) {
				e.probability = 1.0 / (double) currLevelEvents.size();
			} else {
				for (Pair<Event, Double> parentRelation : e.parents) {
					Event parent = parentRelation.first;
					Double transitionProbability = parentRelation.second;
					Double normalizedTransitionProbability = transitionProbability
						/ parent.childrenTransProbSum;
					e.probability += normalizedTransitionProbability * parent.probability;

				}
			}
		}

		//--> testing update propagation
		if (TEST_UPDATES_PROPAGATION) {
			Utils.logLinkabilityGraph(linkabilityGraphCopy,
				"ObfRegionSize" + mapGrid.length + "_AfterPropagation_nodes.txt",
				"ObfRegionSize" + mapGrid.length + "_AfterPropagation_edges.txt",
				context);
		}

		// Phase 5: calculate expected distortion
		long startPhase5 = System.currentTimeMillis();
		double expectedDistortion = 0;
		StringBuilder distanceLogString = new StringBuilder("");
		double distanceSum = 0;
		for (Event e : currLevelEvents) {

			double distance;
			if (fineLocation == Utils.computePostitionFromCellID(e.cellID)) {
				distance = 0;
			} else {
				LatLng centroid = gridDBDataSource.getCentroid(Utils.computePostitionFromCellID(e.cellID));
				if (centroid == null) {
					centroid = Utils.findCellCenter(Utils.computePostitionFromCellID(e.cellID));
				}
				distance = calculateDistance(fineLocation, centroid);
			}
			expectedDistortion += distance * e.probability;

			//--> for logging
			distanceLogString.append(formatter2.format(distance) + ", ");
			distanceSum += distance;
		}

		//--> logging
		if (!currLevelEvents.isEmpty()) {
			log("Prob of first event: " + formatter.format(currLevelEvents.get(0).probability));
		}
		log("Distances: " + distanceLogString.toString() + " SUM: "
			+ formatter2.format(distanceSum));
		log("Expected Distortion: " + expectedDistortion);
		log("Phase 5 took: " + (System.currentTimeMillis() - startPhase5) + " ms");

		// Phase 6: save linkability graph copy
		/*
		 * so that if adaptive protection is satisfied with the ED of this
		 * obfuscation region, then
		 * the privacy Estimator uses replaces the original linkability graph
		 * with this copy
		 */
		lastLinkabilityGraphCopy = linkabilityGraphCopy;

		//--> logging time
		log("Privacy Estimation took: " + (System.currentTimeMillis() - startPrivacyEstimation)
			+ " ms");

		return expectedDistortion;
	}

	private double calculateDistance(LatLng fineLocation, LatLng coarseLocation) {
		return Utils.distance(fineLocation.latitude, fineLocation.longitude,
			coarseLocation.latitude, coarseLocation.longitude, 'K');
	}

	private void removeLevel(ArrayList<Event> toBeDeletedLevel) {
		for (Event e : toBeDeletedLevel) {
			for (Event child : e.children) {
				child.parents = new ArrayList<Pair<Event, Double>>(); // empty list (no parents)
			}
			e = null;
		}
	}

	private ArrayList<Event> detectReachability(ArrayList<Event> previousLevelEvents,
												Event currLevelEvent, GridDBDataSource gridDBDataSource) {

		if (TEST_UPDATES_PROPAGATION)
			return pickRandomParents(previousLevelEvents);

		if (ACTIVATE_REACHABILITY_FORMULA) {
			ArrayList<Event> parents = new ArrayList<Event>();

			LatLng centroid1 = gridDBDataSource.getCentroid(Utils.computePostitionFromCellID(currLevelEvent.cellID));
			if (centroid1 == null) {
				centroid1 = Utils.findCellCenter(Utils.computePostitionFromCellID(currLevelEvent.cellID));
			}
			for (Event previousLevelEvent : previousLevelEvents) {
				LatLng centroid2 = gridDBDataSource.getCentroid(Utils.computePostitionFromCellID(previousLevelEvent.cellID));
				if (centroid2 == null) {
					centroid2 = Utils.findCellCenter(Utils.computePostitionFromCellID(previousLevelEvent.cellID));
				}
				double travelDistanceInKm = Utils.distance(centroid1.latitude, centroid1.longitude,
					centroid2.latitude, centroid2.longitude, 'K');
				double travelTimeInHr = (double) (currLevelEvent.timeStamp - previousLevelEvent.timeStamp)
					/ (double) MILLISECONDS_IN_HOUR;
				double travelSpeedInKmPerHr = travelDistanceInKm / travelTimeInHr;
				if (travelSpeedInKmPerHr <= MAX_USER_SPEED_IN_KM_PER_HOUR)
					parents.add(previousLevelEvent);
			}
			return parents;
		}

		//--> If the reachability formula is not activated, so return all the previous level events as parents to the currrent event
		return previousLevelEvents;
	}

	private ArrayList<Event> pickRandomParents(ArrayList<Event> previousLevelEvents) {
		int numberOfRandomParents = 1;
		int possibleParents = previousLevelEvents.size();
		ArrayList<Event> chosenParents = new ArrayList<Event>();
		HashSet<Long> ids = new HashSet<Long>();
		for (int i = 0; i < numberOfRandomParents; ) {
			Event e = previousLevelEvents.get(rand.nextInt(possibleParents));

			if (!ids.contains(e.id)) {
				chosenParents.add(e);
				ids.add(e.id);
				i++;
			}
		}
		return chosenParents;
	}

	private ArrayList<Event> createNewEventList(LatLng[][] mapGrid,
												int timeStampID, long timeStamp) {
		ArrayList<Event> eventList = new ArrayList<Event>();
		long tempCurrEventID = currEventID;
		for (LatLng[] positions : mapGrid) {
			for (LatLng position : positions) {
				eventList.add(new Event(tempCurrEventID++, Utils.computeCellIDFromPosition(position), timeStampID, timeStamp, 0, 0));
			}
		}
		return eventList;
	}

	private void log(String s) {
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, s);
			Utils.appendLog(LOGTAG + ".txt", s, context);
		}
	}

	private void logLG(String s) {
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d("LG", s);
		}
	}

	private long maxEventID(ArrayList<Event> currLevelEvents) {
		long max = -1;
		for (Event e : currLevelEvents)
			max = Math.max(max, e.id);
		return max;
	}

	private LinkedList<ArrayList<Event>> getLinkabilityGraphCopy() {
		long startCopying = System.currentTimeMillis();

		//--> copy events
		HashMap<Long, Event> copiedEvents = new HashMap<Long, Event>();
		LinkedList<ArrayList<Event>> originalLevels = ((LinkedList<ArrayList<Event>>) linkabilityGraphLevels);
		for (ArrayList<Event> level : originalLevels)
			for (Event e : level) {
				Event copiedEvent = e.copy();
				copiedEvents.put(copiedEvent.id, copiedEvent);
			}

		//--> copy parent-child relationships
		LinkedList<ArrayList<Event>> copiedLinkabilityGraph = new LinkedList<ArrayList<Event>>();
		for (ArrayList<Event> originalLevel : originalLevels) {
			ArrayList<Event> copiedLevel = new ArrayList<Event>();
			for (Event originalChild : originalLevel) {
				long childID = originalChild.id;
				Event copiedChild = copiedEvents.get(childID);
				copiedLevel.add(copiedChild);
				ArrayList<Pair<Event, Double>> parentsInfo = originalChild.parents;
				for (Pair<Event, Double> parentInfo : parentsInfo) {
					long parentID = parentInfo.first.id;
					Event copiedParent = copiedEvents.get(parentID);
					double transProp = parentInfo.second;

					copiedChild.parents.add(new Pair<Event, Double>(copiedParent, transProp));
					copiedParent.children.add(copiedChild);
				}
			}
			copiedLinkabilityGraph.add(copiedLevel);
		}

		Log.d(LOGTAG, "Copying took: " + (System.currentTimeMillis() - startCopying) + " ms");

		return copiedLinkabilityGraph;
	}

	private void propagateGraphUpdates(LinkedList<ArrayList<Event>> linkabilityGraphCopy) {

		//Phase 0: get events with no children
		ArrayList<Event> beforeLastLevelEvents = linkabilityGraphCopy.get(linkabilityGraphCopy
			.size() - 2);
		Queue<Event> eventsHaveNoChildren = new LinkedList<Event>();
		for (Event e : beforeLastLevelEvents) {
			if (e.children.isEmpty()) {
				eventsHaveNoChildren.add(e);
			}
		}

		//Phase 1: propagate events removals up through the linkability graph
		HashSet<Long> eventsToBeRemoved = new HashSet<Long>();
		HashSet<Long> eventsLostSomeChildren = new HashSet<Long>();
		while (!eventsHaveNoChildren.isEmpty()) {
			Event e = eventsHaveNoChildren.poll();
			eventsToBeRemoved.add(e.id);
			Iterator<Pair<Event, Double>> parentsIterator = e.parents.iterator();
			while (parentsIterator.hasNext()) {
				Pair<Event, Double> parentInfo = parentsIterator.next();
				Event parent = parentInfo.first;
				Double transProb = parentInfo.second;
				parent.children.remove(e);
				parent.childrenTransProbSum -= transProb;

				//--> remove parent from parents list
				parentsIterator.remove();

				//--> check if parent now has no children
				if (parent.children.isEmpty()) {
					eventsHaveNoChildren.add(parent);
				} else {
					eventsLostSomeChildren.add(parent.id);
				}
			}
		}

		//Phase 2: traverse graph levels downwards and update probabilities if needed
		int levelNumber = 1;
		HashSet<Long> eventsToBeUpdated = new HashSet<Long>();
		for (ArrayList<Event> level : linkabilityGraphCopy) {
			boolean levelHasEventsRemoved = false;
			Iterator<Event> levelEventsIterator = level.iterator();
			while (levelEventsIterator.hasNext()) {
				Event e = levelEventsIterator.next();

				if (eventsToBeRemoved.contains(e.id)) {
					levelEventsIterator.remove();
					levelHasEventsRemoved = true;
				} else {

					if (eventsLostSomeChildren.contains(e.id))
						for (Event child : e.children)
							eventsToBeUpdated.add(child.id);

					if (eventsToBeUpdated.contains(e.id)) {
						//--> re-calculate the probability
						e.probability = 0;
						for (Pair<Event, Double> parentInfo : e.parents) {
							Event parent = parentInfo.first;
							Double transProp = parentInfo.second;
							double normalizedTransProp = transProp / parent.childrenTransProbSum;
							e.probability += normalizedTransProp * parent.probability;
						}
						//--> mark children as to be updated
						for (Event child : e.children)
							eventsToBeUpdated.add(child.id);
					}
				}
			}

			//--> special case: the first level
			if (levelNumber == 1 && levelHasEventsRemoved)
				for (Event e : level) {
					//--> re-calculate probability
					e.probability = 1.0 / (double) level.size();

					//--> mark children as to be updated
					for (Event child : e.children)
						eventsToBeUpdated.add(child.id);
				}

			levelNumber++;
		}

	}
}
