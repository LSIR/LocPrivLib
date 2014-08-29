package org.epfl.locationprivacy.privacyestimation;

import java.util.ArrayList;

import android.util.Pair;

public class Event {
	public long id;
	public int locID;
	public int timeStampID;
	public long timeStamp;
	public ArrayList<Event> children;
	public ArrayList<Pair<Event, Double>> parents;
	public double propability;

	public Event(long id, int locID, int timeStampID, long timeStamp) {
		super();
		this.id = id;
		this.locID = locID;
		this.timeStampID = timeStampID;
		this.timeStamp = timeStamp;
		children = new ArrayList<Event>();
		parents = new ArrayList<Pair<Event, Double>>();
	}
}
