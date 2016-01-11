package org.epfl.locationprivacy.map.models;

import java.util.Map;

/**
 * A node from OpenStreetMap
 */
public class OSMNode extends OSMSemantic {

	private String lat;

	private String lon;

	private final Map<String, String> tags;

	private String version;

	public OSMNode(String id, String name, String subtype, String lat, String lon, Map<String, String> tags, String version) {
		super();
		this.id = id;
		this.name = name;
		this.subtype = subtype;
		this.lat = lat;
		this.lon = lon;
		this.tags = tags;
		this.version = version;
	}

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLong() {
		return lon;
	}

	public void setLong(String lon) {
		this.lon = lon;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Map<String, String> getTags() {
		return tags;
	}

}
