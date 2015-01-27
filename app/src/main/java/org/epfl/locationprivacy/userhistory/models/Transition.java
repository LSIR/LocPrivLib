package org.epfl.locationprivacy.userhistory.models;

public class Transition {

	public int fromLocID;
	public int toLocID;
	public int fromTimeID;
	public int toTimeID;
	public int count;

	public Transition(int fromLocID, int toLocID, int fromTimeID, int toTimeID, int count) {
		super();
		this.fromLocID = fromLocID;
		this.toLocID = toLocID;
		this.fromTimeID = fromTimeID;
		this.toTimeID = toTimeID;
		this.count = count;
	}

	public String toString() {
		return "LocID: " + fromLocID + " -> " + toLocID + " , TimeID: " + fromTimeID + " -> "
				+ toTimeID + ", Count: " + count;
	}

}
