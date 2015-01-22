package org.epfl.locationprivacy.landingpage;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.baselineprotection.activities.ObfRegionActivity;
import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.databases.VenuesCondensedDBDataSource;
import org.epfl.locationprivacy.privacyestimation.databases.LinkabilityGraphDataSource;
import org.epfl.locationprivacy.privacyprofile.activities.PrivacyProfileActivity;
import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;
import org.epfl.locationprivacy.thirdpartyemulator.ThirdPartyActivity;
import org.epfl.locationprivacy.userhistory.activities.UserHistoryActivity;
import org.epfl.locationprivacy.userhistory.databases.LocationTableDataSource;
import org.epfl.locationprivacy.userhistory.databases.TransitionTableDataSource;
import org.epfl.locationprivacy.virtualtransitiongenerator.activities.VirtualTransitionGeneratorActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class LandingPageActivity extends Activity {

	private static final String LOGTAG = "LandingPageActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landingpage);

		// open dbs
		GridDBDataSource.getInstance(this);
		VenuesCondensedDBDataSource.getInstance(this);
		LocationTableDataSource.getInstance(this);
		TransitionTableDataSource.getInstance(this);
		SemanticLocationsDataSource.getInstance(this);
		LinkabilityGraphDataSource.getInstance(this);

		//mock sensitivities for unit tests
		//--> semantic sensitivities
		SemanticLocationsDataSource.getInstance(this).updateSemanticLocation("university", 25);
		SemanticLocationsDataSource.getInstance(this).updateSemanticLocation("bar", 25);
		SemanticLocationsDataSource.getInstance(this).updateSemanticLocation("hospital", 25);

		//--> semantic sensitivities
		GridDBDataSource.getInstance(this).updateGridCellSensititivity(10367, 20);
		GridDBDataSource.getInstance(this).updateGridCellSensititivity(17609, 20);
		GridDBDataSource.getInstance(this).updateGridCellSensititivity(11003, 20);
	}

	public void onClickPrivacySensitivity(View view) {
		Intent intent = new Intent(this, PrivacyProfileActivity.class);
		startActivity(intent);
	}

	public void onClickTrackEvents(View view) {
		//		Toast.makeText(this, "Not Implemented Yet", Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(this, UserHistoryActivity.class);
		startActivity(intent);
	}

	public void onClickBaselineProtection(View view) {
		Intent intent = new Intent(this, ObfRegionActivity.class);
		startActivity(intent);
	}

	public void onClickThirdPartyEmulator(View view) {
		Intent intent = new Intent(this, ThirdPartyActivity.class);
		startActivity(intent);
	}

	public void onClickVirtualRouteGenerator(View view) {
		Intent intent = new Intent(this, VirtualTransitionGeneratorActivity.class);
		startActivity(intent);
	}

}
