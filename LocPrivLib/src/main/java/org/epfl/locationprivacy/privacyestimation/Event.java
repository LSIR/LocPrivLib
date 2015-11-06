package org.epfl.locationprivacy.privacyestimation;

import java.util.ArrayList;

import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

public class Event {
	public long id;
	public LatLng cell;
	public int timeStampID;
	public long timeStamp;
	public ArrayList<Event> children;
	public ArrayList<Pair<Event, Double>> parents;
	public double probability;
	public double childrenTransProbSum;

	public Event(long id, LatLng cell, int timeStampID, long timeStamp, double probability,
			double childrenTransProbSum) {
		super();
		this.id = id;
		this.cell = cell;
		this.timeStampID = timeStampID;
		this.timeStamp = timeStamp;
		this.probability = probability;
		this.childrenTransProbSum = childrenTransProbSum;
		children = new ArrayList<Event>();
		parents = new ArrayList<Pair<Event, Double>>();
	}

	public Event copy() {
		return new Event(this.id, this.cell, this.timeStampID, this.timeStamp, this.probability, this.childrenTransProbSum);
	}
}
