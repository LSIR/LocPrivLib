package org.epfl.locationprivacy.adapters;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.models.SemanticLocation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SemanticLocationAdapter extends BaseAdapter {

	private static final String LOGTAG = "SemanticLocationAdapter";

	private List<SemanticLocation> semanticLocations;
	private LayoutInflater myLayoutInflater;

	public SemanticLocationAdapter(Context context, List<SemanticLocation> semanticLocations) {
		this.semanticLocations = semanticLocations;
		myLayoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
	public View getView(int position, final View convertView, ViewGroup parent) {

		final View view = myLayoutInflater.inflate(R.layout.listitem_privacyprofile, null);
		TextView textView = (TextView) view.findViewById(R.id.textView1);
		textView.setText(semanticLocations.get(position).name);
		
		final NumberFormat formatter = new DecimalFormat("#0.0");     
		

		// Seekbar
		SeekBar privacyBar = (SeekBar) view.findViewById(R.id.seekbar);
		privacyBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				TextView textView2 = (TextView) view.findViewById(R.id.textView2);
				textView2.setText(formatter.format(progress/10.0) + " KM");
			}
		});

		return view;
	}
}
