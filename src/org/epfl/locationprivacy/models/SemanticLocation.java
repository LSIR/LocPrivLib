package org.epfl.locationprivacy.models;

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
}
