package org.epfl.locationprivacy.privacyestimation;

import java.util.ArrayList;

import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

public interface PrivacyEstimatorInterface {
	double calculatePrivacyEstimation(LatLng fineLocation, LatLng[][] mapGrid, long timeStamp);

	void saveLastLinkabilityGraphCopy();
}
