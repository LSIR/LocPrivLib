package org.epfl.locationprivacy.privacyestimation;

import java.util.ArrayList;

import android.content.Context;

public interface PrivacyEstimatorInterface {
	double calculatePrivacyEstimation(int fineLocationID, ArrayList<Integer> obfRegionCellIDs,
			long timeStamp);
}
