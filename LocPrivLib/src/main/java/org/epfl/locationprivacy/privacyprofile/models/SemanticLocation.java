package org.epfl.locationprivacy.privacyprofile.models;

public class SemanticLocation {

	public long id;
	public String name;
	public int userSentivity;

	public SemanticLocation(String name, int userSentivity) {
		super();
		this.name = name;
		this.userSentivity = userSentivity;
	}

	public SemanticLocation(long id, String name, int userSentivity) {
		super();
		this.id = id;
		this.name = name;
		this.userSentivity = userSentivity;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SemanticLocation that = (SemanticLocation) o;

		return name.equals(that.name);

	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
