package org.epfl.locationprivacy.userhistory.models;

public class Location {

	public double latitude;
	public double longitude;
	public long timestamp;
	public Location(double latitude, double longitude, long timestamp) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.timestamp = timestamp;
	}
	
	public String toString(){
		return "Location: "+latitude+"/"+longitude+" Time: "+timestamp;
	}
}
