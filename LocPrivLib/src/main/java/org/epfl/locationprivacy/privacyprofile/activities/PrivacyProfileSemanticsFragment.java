package org.epfl.locationprivacy.privacyprofile.activities;

import java.util.List;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.privacyprofile.adapters.SemanticLocationAdapter;
import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;
import org.epfl.locationprivacy.privacyprofile.models.SemanticLocation;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

public class PrivacyProfileSemanticsFragment extends Fragment {
	SemanticLocationsDataSource semanticLocationsDataSource;

	public PrivacyProfileSemanticsFragment() {
		super();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// Data
		semanticLocationsDataSource = SemanticLocationsDataSource.getInstance(getActivity());
		List<SemanticLocation> semanticLocations = semanticLocationsDataSource.finaAll();
		Toast.makeText(getActivity(),
				"Number of semantic locations are " + semanticLocations.size(), Toast.LENGTH_LONG)
				.show();

		// UI
		View rootView = inflater.inflate(R.layout.fragment_privacyprofile_semantics, container,
				false);
		ListView listView = (ListView) rootView.findViewById(R.id.semanticslist);
		SemanticLocationAdapter semanticLocationAdapter = new SemanticLocationAdapter(
				getActivity(), semanticLocations);
		listView.setAdapter(semanticLocationAdapter);

		return listView;
	}
}
