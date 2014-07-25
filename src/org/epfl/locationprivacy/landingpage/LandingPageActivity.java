package org.epfl.locationprivacy.landingpage;

import java.util.ArrayList;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.baselineprotection.activities.ObfRegionActivity;
import org.epfl.locationprivacy.baselineprotection.activities.ObfRegionSettingActivity;
import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.privacyprofile.activities.PrivacyProfileActivity;
import org.epfl.locationprivacy.spatialitedb.SampleSpatialiteQueryActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class LandingPageActivity extends Activity {

	private static final String LOGTAG = "LandingPageActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landingpage);

		//Test GridDB
		GridDBDataSource gridDBDataSource = new GridDBDataSource(this);
		gridDBDataSource.open();
		Log.d(LOGTAG, "GridDB Rows: " + gridDBDataSource.findRowsCount());
		gridDBDataSource.close();
	}

	public void onClickPrivacySensitivity(View view) {
		Intent intent = new Intent(this, PrivacyProfileActivity.class);
		startActivity(intent);
	}

	public void onClickTrackEvents(View view) {
		Toast.makeText(this, "Not Implemented Yet", Toast.LENGTH_SHORT).show();
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
