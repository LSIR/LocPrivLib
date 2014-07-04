package org.epfl.locationprivacy.adapters;

import java.util.List;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.listeners.mySeekBarChangeListener;
import org.epfl.locationprivacy.models.SemanticLocation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SeekBar;
import android.widget.TextView;

public class SemanticLocationAdapter extends BaseAdapter {

	private static final String LOGTAG = "SemanticLocationAdapter";

	private List<SemanticLocation> semanticLocations;
	private LayoutInflater myLayoutInflater;
	private Context context;

	public SemanticLocationAdapter(Context context, List<SemanticLocation> semanticLocations) {
		this.semanticLocations = semanticLocations;
		myLayoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.context = context;
	}

	@Override
	public int getCount() {
		return semanticLocations.size();
	}

	@Override
	public Object getItem(int i) {
		return semanticLocations.get(i);
	}

	@Override
	public long getItemId(int i) {
		return 0;
	}

	@Override
	public View getView(final int position, final View convertView, ViewGroup parent) {

		final View view = myLayoutInflater.inflate(R.layout.listitem_privacyprofile, null);
		TextView textView = (TextView) view.findViewById(R.id.textView1);
		textView.setText(semanticLocations.get(position).name);

		// Seekbar
		SeekBar privacyBar = (SeekBar) view.findViewById(R.id.seekbar);
		privacyBar.setProgressDrawable(context.getResources()
				.getDrawable(R.drawable.seekbarbgimage));
		privacyBar.setProgress(semanticLocations.get(position).userSentivity);
		privacyBar.setOnSeekBarChangeListener(new mySeekBarChangeListener(context, semanticLocations
				.get(position)));

		return view;
	}
}
