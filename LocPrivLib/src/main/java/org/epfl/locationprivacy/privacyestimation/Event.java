package org.epfl.locationprivacy.privacyestimation;

import java.util.ArrayList;

import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

public class Event {
	public long id;
	public LatLng cell;
	public long cellID;
	public int timeStampID;
	public long timeStamp;
	public ArrayList<Event> children;
	public ArrayList<Pair<Event, Double>> parents;
	public double probability;
	public double childrenTransProbSum;

	public Event(long id, long cellID, LatLng cell, int timeStampID, long timeStamp, double probability,
			double childrenTransProbSum) {
		super();
		this.id = id;
		this.cellID = cellID;
		this.cell = cell;
		this.timeStampID = timeStampID;
		this.timeStamp = timeStamp;
		this.probability = probability;
		this.childrenTransProbSum = childrenTransProbSum;
		children = new ArrayList<>();
		parents = new ArrayList<>();
	}

	public Event copy() {
		return new Event(this.id, this.cellID, this.cell, this.timeStampID, this.timeStamp, this.probability, this.childrenTransProbSum);
	}
}
