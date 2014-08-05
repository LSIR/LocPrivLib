package org.epfl.locationprivacy.landingpage;

import java.util.ArrayList;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.baselineprotection.activities.ObfRegionActivity;
import org.epfl.locationprivacy.privacyprofile.activities.PrivacyProfileActivity;
import org.epfl.locationprivacy.spatialitedb.SampleSpatialiteQueryActivity;
import org.epfl.locationprivacy.userhistory.activities.UserHistoryActivity;
import org.epfl.locationprivacy.userhistory.databases.TransitionTableDataSource;
import org.epfl.locationprivacy.userhistory.models.Transition;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class LandingPageActivity extends Activity {

	private static final String LOGTAG = "LandingPageActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landingpage);

		//Test GridDB
		//		GridDBDataSource gridDBDataSource = new GridDBDataSource(this);
		//		gridDBDataSource.open();
		//		Log.d(LOGTAG, "GridDB Rows: " + gridDBDataSource.findRowsCount());
		//		gridDBDataSource.close();

		//Test UserHistoryDB
		TransitionTableDataSource transitionTableDataSource = new TransitionTableDataSource(this);
		transitionTableDataSource.open();
		Log.d(LOGTAG, "UserHistoryDB Rows: " + transitionTableDataSource.countRows());
		ArrayList<Transition> transitions = transitionTableDataSource.findAll();
		for (Transition t : transitions)
			Log.d(LOGTAG, "" + t.toString());
		Log.d(LOGTAG, "tran prop  0 - > 1 "+ transitionTableDataSource.getTransitionProbability(0, 1) );
		Log.d(LOGTAG, "tran prop  0 - > 2 "+ transitionTableDataSource.getTransitionProbability(0, 2) );
		
		transitionTableDataSource.close();

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

	public void onClickSpatialiteDB(View view) {
		Intent intent = new Intent(this, SampleSpatialiteQueryActivity.class);
		startActivity(intent);
	}
}
