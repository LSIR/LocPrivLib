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
	public double childrenTransProbSum;

	public Event(long id, int locID, int timeStampID, long timeStamp, double propability,
			double childrenTransProbSum) {
		super();
		this.id = id;
		this.locID = locID;
		this.timeStampID = timeStampID;
		this.timeStamp = timeStamp;
		this.propability = propability;
		this.childrenTransProbSum = childrenTransProbSum;
		children = new ArrayList<Event>();
		parents = new ArrayList<Pair<Event, Double>>();
	}

	public Event copy() {
		return new Event(this.id, this.locID, this.timeStampID, this.timeStamp, this.propability, this.childrenTransProbSum);
	}
}
