package org.epfl.locationprivacy.privacyprofile.listeners;

import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;
import org.epfl.locationprivacy.privacyprofile.models.SemanticLocation;

import android.content.Context;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class mySeekBarChangeListener implements OnSeekBarChangeListener {

	private static final String LOGTAG = "mySeekBarChangeListener";
	private SemanticLocation semanticLocation;
	private Context context;

	public mySeekBarChangeListener(Context context, SemanticLocation semanticLocation) {
		super();
		this.semanticLocation = semanticLocation;
		this.context = context;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		Log.d(LOGTAG, "ID: " + semanticLocation.id + " Name:" + semanticLocation.name + " seekbar:"
				+ seekBar.getProgress());

		// update DB with new sensitivity value
		SemanticLocationsDataSource.getInstance(context).updateSemanticLocation(
				semanticLocation.id, seekBar.getProgress());

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}
}
