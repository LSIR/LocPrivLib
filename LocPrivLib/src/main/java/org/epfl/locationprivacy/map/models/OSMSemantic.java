package org.epfl.locationprivacy.map.models;

public abstract class OSMSemantic {

	protected String id;
	protected String name;
	protected String subtype;
	protected String version;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSubtype() {
		return subtype;
	}

	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "OSMSemantic{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", subtype='" + subtype + '\'' +
			", version='" + version + '\'' +
			'}';
	}
}
