package org.epfl.locationprivacy.map.models;

import java.util.List;
import java.util.Map;

public class OSMWay extends OSMSemantic {

	private List<String> nodes;

	private final Map<String, String> tags;

	private String version;

	public OSMWay(String id, String name, String subtype, List<String> nodes, Map<String, String> tags, String version) {
		super();
		this.id = id;
		this.name = name;
		this.subtype = subtype;
		this.nodes = nodes;
		this.tags = tags;
		this.version = version;
	}

	public List<String> getNodes() {
		return nodes;
	}

	public void setNodes(List<String> nodes) {
		this.nodes = nodes;
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
