package org.epfl.locationprivacy.adaptiveprotection;

import java.util.ArrayList;

import org.epfl.locationprivacy.map.models.MyPolygon;

import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

public interface AdaptiveProtectionInterface {

	ArrayList<MyPolygon> getLocation(LatLng mockLocation);
}
