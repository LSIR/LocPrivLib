package org.epfl.locationprivacy.spatialitedb;

import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;

public class MyPolygon {

	ArrayList<LatLng> points;
	String name;
	String subType;



	public MyPolygon(ArrayList<LatLng> points, String name, String subType) {
		super();
		this.points = points;
		this.name = name;
		this.subType = subType;
	}



	public static MyPolygon parseGeometry(String multiPolygon, String name, String subType) {

		multiPolygon = multiPolygon.trim().replaceAll("MULTIPOLYGON", "");
		String[] polygons = multiPolygon.split("[)], [(]");
		for (String polygon : polygons) {
			while (polygon.startsWith("("))
				polygon = polygon.substring(1);
			while (polygon.endsWith(")"))
				polygon = polygon.substring(0, polygon.length() - 1);
			System.out.println(polygon);

			String[] points = polygon.split(",");
			ArrayList<LatLng> latLngPoints = new ArrayList<LatLng>();
			for (String point : points) {
				point = point.trim();
				LatLng latLngPoint = new LatLng(Double.parseDouble(point.split(" ")[1]),
						Double.parseDouble(point.split(" ")[0]));
				latLngPoints.add(latLngPoint);
			}
			return new MyPolygon(latLngPoints, name, subType);
		}
		return null;
	}

}
