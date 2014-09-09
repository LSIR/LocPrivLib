package org.epfl.locationprivacy.adaptiveprotection;

import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

public interface AdaptiveProtectionInterface {

	Pair<LatLng, LatLng> getObfuscationLocation(LatLng location);

	Pair<LatLng, LatLng> getObfuscationLocation();
}
