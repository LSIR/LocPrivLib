package org.epfl.locationprivacy.privacyestimation;

import java.util.ArrayList;

import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

public interface PrivacyEstimatorInterface {
	double calculatePrivacyEstimation(LatLng fineLocation, int fineLocationID,
			ArrayList<Integer> obfRegionCellIDs, long timeStamp);

	void saveLastLinkabilityGraphCopy();
}
