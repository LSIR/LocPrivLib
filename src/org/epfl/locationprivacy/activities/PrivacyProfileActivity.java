package org.epfl.locationprivacy.activities;

import java.util.List;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.adapters.SemanticLocationAdapter;
import org.epfl.locationprivacy.databases.SemanticLocationsDataSource;
import org.epfl.locationprivacy.models.SemanticLocation;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class PrivacyProfileActivity extends Activity {

	SemanticLocationsDataSource semanticLocationsDataSource;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_privacyprofile);

		// Data
		semanticLocationsDataSource = new SemanticLocationsDataSource(this);
		semanticLocationsDataSource.open();
		List<SemanticLocation> semanticLocations = semanticLocationsDataSource.finaAll();
		if (semanticLocations.size() == 0) {
			semanticLocationsDataSource.populateDB();
			semanticLocations = semanticLocationsDataSource.finaAll();
		}
		Toast.makeText(this, "Number of semantic locations are " + semanticLocations.size(),
				Toast.LENGTH_LONG).show();

		// UI
		ListView listView = (ListView) findViewById(android.R.id.list);
		SemanticLocationAdapter semanticLocationAdapter = new SemanticLocationAdapter(this,
				semanticLocations);
		listView.setAdapter(semanticLocationAdapter);
		
		
		// listView.setOnItemClickListener(new OnItemClickListener() {
		// @Override
		// public void onItemClick(AdapterView parent, View view, int position,
		// long id) {
		// Toast.makeText(PrivacyProfileActivity.this, "You clicked : " +
		// position,
		// Toast.LENGTH_LONG).show();
		// }
		// });
	}
}
