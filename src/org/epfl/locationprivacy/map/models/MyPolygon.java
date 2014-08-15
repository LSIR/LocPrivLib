package org.epfl.locationprivacy.map.models;

import java.util.ArrayList;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class MyPolygon {

	String name;
	String semantic;
	ArrayList<LatLng> points;
	Double sensitivity;

	public MyPolygon(String name, String semantic, ArrayList<LatLng> points) {
		super();
		this.name = name;
		this.semantic = semantic;
		this.points = points;
	}

	public MyPolygon(String name, String semantic, ArrayList<LatLng> points,
			Double sensitivity) {
		super();
		this.name = name;
		this.semantic = semantic;
		this.points = points;
		this.sensitivity = sensitivity;
	}

	public static ArrayList<LatLng> parseSpatialPolygon(String geometry) {

		Log.d("parseSpatialPolygon", geometry);
		geometry = geometry.trim().replaceAll("POLYGON", "");

		while (geometry.startsWith("("))
			geometry = geometry.substring(1);
		while (geometry.endsWith(")"))
			geometry = geometry.substring(0, geometry.length() - 1);

		Log.d("parseSpatialPolygon", geometry);
		String[] points = geometry.split(",");
		ArrayList<LatLng> latLngPoints = new ArrayList<LatLng>();
		for (String point : points) {
			point = point.trim();
			LatLng latLngPoint = new LatLng(Double.parseDouble(point.split(" ")[1]),
					Double.parseDouble(point.split(" ")[0]));
			latLngPoints.add(latLngPoint);
		}
		return latLngPoints;
	}

	public static ArrayList<LatLng> parseSpatialMulipolygon(String geometry) {

		geometry = geometry.trim().replaceAll("MULTIPOLYGON", "");
		String[] polygons = geometry.split("[)], [(]");
		for (String polygon : polygons) {
			while (polygon.startsWith("("))
				polygon = polygon.substring(1);
			while (polygon.endsWith(")"))
				polygon = polygon.substring(0, polygon.length() - 1);

			String[] points = polygon.split(",");
			ArrayList<LatLng> latLngPoints = new ArrayList<LatLng>();
			for (String point : points) {
				point = point.trim();
				LatLng latLngPoint = new LatLng(Double.parseDouble(point.split(" ")[1]),
						Double.parseDouble(point.split(" ")[0]));
				latLngPoints.add(latLngPoint);
			}
			return latLngPoints;
		}
		return null;
	}

	public String convertToSpatialiteString() {

		String ret = "'POLYGON((";
		for (LatLng point : points)
			ret += point.longitude + " " + point.latitude + ", ";

		//add first point again to be closed polygon
		ret += points.get(0).longitude + " " + points.get(0).latitude;

		ret += "))'";

		return ret;
	}

	public String toString() {
		String ret = "";
		ret += "name: " + name;
		ret += " semantic: " + semantic;
		ret += " sensit "+ sensitivity;
		return ret;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSemantic() {
		return semantic;
	}

	public void setSemantic(String semantic) {
		this.semantic = semantic;
	}

	public ArrayList<LatLng> getPoints() {
		return points;
	}

	public void setPoints(ArrayList<LatLng> points) {
		this.points = points;
	}

	public Double getSensitivity() {
		return sensitivity;
	}

	public void setSensitivity(Double sensitivity) {
		this.sensitivity = sensitivity;
	}

}
