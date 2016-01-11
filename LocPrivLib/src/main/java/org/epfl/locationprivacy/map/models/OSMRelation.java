package org.epfl.locationprivacy.map.models;

import java.util.List;
import java.util.Map;

public class OSMRelation extends OSMSemantic {

	private List<String> ways;

	private final Map<String, String> tags;

	private String version;

	public OSMRelation(String id, String name, String subtype, List<String> ways, Map<String, String> tags, String version) {
		super();
		this.id = id;
		this.name = name;
		this.subtype = subtype;
		this.ways = ways;
		this.tags = tags;
		this.version = version;
	}

	public List<String> getWays() {
		return ways;
	}

	public void setWays(List<String> ways) {
		this.ways = ways;
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
