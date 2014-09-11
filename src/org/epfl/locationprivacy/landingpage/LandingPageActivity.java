package org.epfl.locationprivacy.landingpage;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.baselineprotection.activities.ObfRegionActivity;
import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.databases.VenuesCondensedDBDataSource;
import org.epfl.locationprivacy.map.databases.VenuesDBDataSource;
import org.epfl.locationprivacy.privacyestimation.databases.LinkabilityGraphDataSource;
import org.epfl.locationprivacy.privacyprofile.activities.PrivacyProfileActivity;
import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;
import org.epfl.locationprivacy.spatialitedb.SampleSpatialiteQueryActivity;
import org.epfl.locationprivacy.thirdpartyemulator.ThirdPartyActivity;
import org.epfl.locationprivacy.userhistory.activities.UserHistoryActivity;
import org.epfl.locationprivacy.userhistory.databases.LocationTableDataSource;
import org.epfl.locationprivacy.userhistory.databases.TransitionTableDataSource;
import org.epfl.locationprivacy.util.Utils;
import org.epfl.locationprivacy.virtualtransitiongenerator.activities.VirtualTransitionGeneratorActivity;

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

		// open dbs
		GridDBDataSource.getInstance(this);
		VenuesCondensedDBDataSource.getInstance(this);
		VenuesDBDataSource.getInstance(this);
		LocationTableDataSource.getInstance(this);
		TransitionTableDataSource.getInstance(this);
		SemanticLocationsDataSource.getInstance(this);
		LinkabilityGraphDataSource.getInstance(this);

		//mock sensitivities
		//--> semantic sensitivities
		SemanticLocationsDataSource.getInstance(this).updateSemanticLocation("university", 25);
		SemanticLocationsDataSource.getInstance(this).updateSemanticLocation("bar", 25);
		SemanticLocationsDataSource.getInstance(this).updateSemanticLocation("hospital", 25);

		//--> semantic sensitivities
		GridDBDataSource.getInstance(this).updateGridCellSensititivity(10367, 20);
		GridDBDataSource.getInstance(this).updateGridCellSensititivity(17609, 20);
		GridDBDataSource.getInstance(this).updateGridCellSensititivity(11003, 20);

		//Test GridDB
		//		GridDBDataSource gridDBDataSource = new GridDBDataSource(this);
		//		gridDBDataSource.open();
		//		Log.d(LOGTAG, "GridDB Rows: " + gridDBDataSource.findRowsCount());
		//		gridDBDataSource.close();

		//Test UserHistoryDB
		//		TransitionTableDataSource transitionTableDataSource = new TransitionTableDataSource(this);
		//		transitionTableDataSource.open();
		//		Log.d(LOGTAG, "UserHistoryDB Rows: " + transitionTableDataSource.countRows());
		//		ArrayList<Transition> transitions = transitionTableDataSource.findAll();
		//		for (Transition t : transitions)
		//			Log.d(LOGTAG, "" + t.toString());
		//		Log.d(LOGTAG, "tran prop  0 - > 1 "+ transitionTableDataSource.getTransitionProbability(0, 1) );
		//		Log.d(LOGTAG, "tran prop  0 - > 2 "+ transitionTableDataSource.getTransitionProbability(0, 2) );
		//		
		//		transitionTableDataSource.close();

		//Test case 1
		// open
		//		VenuesDBDataSource venuesDBDataSource = new VenuesDBDataSource(this);
		//		SemanticLocationsDataSource semanticLocationsDataSource = SemanticLocationsDataSource
		//				.getInstance(this);
		//		venuesDBDataSource.open();
		//		semanticLocationsDataSource.open();
		//
		//		// current location
		//
		//		double lat = 46.514631;
		//		double lng = 6.62084;
		//
		//		// get semantics of current location
		//		ArrayList<MyPolygon> currentLocationVenues = new ArrayList<MyPolygon>();
		//		Pair<MyPolygon, Double> nearest = venuesDBDataSource.findNearestVenue(lat, lng);
		//		Log.d(LOGTAG, nearest.first+"");
		//		currentLocationVenues.add(nearest.first);
		//
		//		ArrayList<String> semantics = new ArrayList<String>();
		//		for (MyPolygon p : currentLocationVenues) {
		//			semantics.add(p.getSemantic());
		//			Log.d(LOGTAG, p.toString());
		//		}
		//
		//		// get user sensitivity of current location semantic
		//		if (!semantics.isEmpty()) {
		//			Double sensitivity = semanticLocationsDataSource.findSemanticSensitivity(semantics
		//					.get(0));
		//			Log.d(LOGTAG, "Sensitivity: " + sensitivity);
		//		}
		//
		//		//free resources
		//		venuesDBDataSource.close();
		//		semanticLocationsDataSource.close();

		//test case 2
		//		long linesCount = VenuesCondensedDBDataSource.getInstance(this).findRowsCount("lines");
		//		long polygonsCount = VenuesCondensedDBDataSource.getInstance(this)
		//				.findRowsCount("polygons");
		//		long pointsCount = VenuesCondensedDBDataSource.getInstance(this).findRowsCount("points");
		//		Log.d(LOGTAG, "polygons: " + polygonsCount);
		//		Log.d(LOGTAG, "lines: " + linesCount);
		//		Log.d(LOGTAG, "points: " + pointsCount);

		//test case 3
		//				LinkabilityGraphDataSource.getInstance(this).clearDB();
		//		Log.d(LOGTAG, "Events: " + LinkabilityGraphDataSource.getInstance(this).countEventRows());
		//		Log.d(LOGTAG, "ParentChilred: "
		//				+ LinkabilityGraphDataSource.getInstance(this).countParentChildrenRows());
		//		Log.d(LOGTAG, "Max Level: " + LinkabilityGraphDataSource.getInstance(this).findMaxLevelID());
		//		Log.d(LOGTAG, "Max Event: " + LinkabilityGraphDataSource.getInstance(this).findMaxEventID());

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

	public void onClickThirdPartyEmulator(View view) {
		Intent intent = new Intent(this, ThirdPartyActivity.class);
		startActivity(intent);
	}

	public void onClickVirtualRouteGenerator(View view) {
		Intent intent = new Intent(this, VirtualTransitionGeneratorActivity.class);
		startActivity(intent);
	}

	//	@Override
	//	protected void onDestroy() {
	//		//stop all dbs
	//		Toast.makeText(this, "LandingPage: OnDestroy", Toast.LENGTH_LONG).show();
	//		GridDBDataSource.getInstance(this).close();
	//		VenuesCondensedDBDataSource.getInstance(this).close();
	//		VenuesDBDataSource.getInstance(this).close();
	//		LocationTableDataSource.getInstance(this).close();
	//		TransitionTableDataSource.getInstance(this).close();
	//		SemanticLocationsDataSource.getInstance(this).close();
	//		super.onDestroy();
	//	}

}
