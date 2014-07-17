package org.epfl.locationprivacy.landingpage;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.baselineprotection.activities.ObfRegionActivity;
import org.epfl.locationprivacy.privacyprofile.activities.PrivacyProfileActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class LandingPageActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landingpage);
	}

	public void onClickPrivacySensitivity(View view) {
		Intent intent = new Intent(this, PrivacyProfileActivity.class);
		startActivity(intent);
	}

	public void onClickTrackEvents(View view) {
		Toast.makeText(this, "onClickTrackEvents", Toast.LENGTH_SHORT).show();
	}

	public void onClickBaselineProtection(View view) {
		Intent intent = new Intent(this, ObfRegionActivity.class);
		startActivity(intent);
	}
}
