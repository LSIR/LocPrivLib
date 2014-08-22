package org.epfl.locationprivacy.privacyprofile.activities;

import java.util.ArrayList;
import java.util.HashMap;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.util.Utils;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;

public class PrivacyProfileMapFragment extends Fragment {

	GoogleMap googleMap;
	MapView mapView;
	Bundle mBundle;
	ArrayList<Polyline> polylines = new ArrayList<Polyline>();
	Polygon polygon = null;
	Polygon currDrawableGridCell = null;
	MyPolygon currSelectedGridCell = null;
	SeekBar privacyBar;
	HashMap<String, Polygon> idToDrawablePolygon = new HashMap<String, Polygon>();
	GridDBDataSource gridDBDataSource;

	public PrivacyProfileMapFragment() {
		super();
		gridDBDataSource = GridDBDataSource.getInstance(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_privacyprofile_map, container, false);

		// seekbar & saveButton
		privacyBar = (SeekBar) rootView.findViewById(R.id.seekbar);
		privacyBar.setProgressDrawable(getActivity().getResources().getDrawable(
				R.drawable.seekbarbgimage));
		privacyBar.setEnabled(false);
		final Button saveButton = (Button) rootView.findViewById(R.id.savesensitivitybutton);
		saveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// update gridcell
				int seekBarValue = privacyBar.getProgress();
				Double sensitivity = (seekBarValue * 1.0) / 100.0;
				int gridId = Integer.parseInt(currSelectedGridCell.getName());
				gridDBDataSource.updateGridCellSensititivity(gridId, sensitivity);

				// draw or remove
				if (sensitivity > 0) {
					Polygon newPolygon = Utils.drawPolygon(currSelectedGridCell, googleMap,
							0x33FF0000);
					Polygon prev = idToDrawablePolygon.remove(currSelectedGridCell.getName());
					if (prev != null)
						prev.remove();
					idToDrawablePolygon.put(currSelectedGridCell.getName(), newPolygon);

				} else
					idToDrawablePolygon.remove(currSelectedGridCell.getName()).remove();

				Toast.makeText(getActivity(), "Successfully saved", Toast.LENGTH_SHORT).show();
			}
		});

		// Make sure that google play services are OK
		if (Utils.googlePlayServicesOK(getActivity())) {

			MapsInitializer.initialize(getActivity());
			mapView = (MapView) rootView.findViewById(R.id.privacyprofilemap);
			mapView.onCreate(mBundle);

			if (initMap()) {

				// Go to laussane map
				LatLng latLng = new LatLng(46.526092, 6.584415);
				CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 11);
				googleMap.moveCamera(cameraUpdate);

				// Draw Grid
				MyPolygon topLeftGridCell = gridDBDataSource.findGridCell(0);
				LatLng topLeftPoint = topLeftGridCell.getPoints().get(0);
				refreshMapGrid(Utils.LAUSSANE_GRID_HEIGHT_CELLS, Utils.LAUSSANE_GRID_WIDTH_CELLS,
						topLeftPoint);

				// Query Previously saved grid cells which have customized sensitivity
				ArrayList<MyPolygon> sensitiveGridCells = gridDBDataSource.findSensitiveGridCells();
				for (MyPolygon savedGridCell : sensitiveGridCells) {
					Polygon p = Utils.drawPolygon(savedGridCell, googleMap, 0x33FF0000);
					idToDrawablePolygon.put(savedGridCell.getName(), p);
				}

				// Detecting Touch Events
				googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
					@Override
					public void onMapClick(LatLng point) {
						Log.d("test", point.toString());
						
						currSelectedGridCell = gridDBDataSource.findGridCell(point.latitude,
								point.longitude);
						Log.d("test", currSelectedGridCell.getName());
						if (currSelectedGridCell == null) {
							Toast.makeText(getActivity(), "Chose a loction inside the Grid",
									Toast.LENGTH_SHORT).show();

							// deactivate scroll & activate save button
							saveButton.setEnabled(false);
							privacyBar.setEnabled(false);
						} else {
							//--> remove select grid
							if (currDrawableGridCell != null)
								currDrawableGridCell.remove();

							//--> add new one
							currDrawableGridCell = Utils.drawPolygon(currSelectedGridCell,
									googleMap, 0x3300FF00);

							// activate scroll & activate save button
							saveButton.setEnabled(true);
							privacyBar.setEnabled(true);

							// update sensitivity bar
							int currSensitivity = (int) (currSelectedGridCell.getSensitivity() * 100);
							privacyBar.setProgress(currSensitivity);

							// test sensititivy
							Toast.makeText(getActivity(),"CellID: "+currSelectedGridCell.getName()+
									"Sensitivity : " + currSelectedGridCell.getSensitivity(),
									Toast.LENGTH_SHORT).show();

						}
					}
				});

			} else {
				Toast.makeText(getActivity(), "Map not available", Toast.LENGTH_SHORT).show();
			}

		} else {
			Toast.makeText(getActivity(), "Google Play Service Not Available", Toast.LENGTH_SHORT)
					.show();
		}

		return rootView;
	}

	private boolean initMap() {
		googleMap = mapView.getMap();
		return googleMap != null;
	}

	//=================================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBundle = savedInstanceState;
	}

	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mapView.onPause();
	}

	@Override
	public void onDestroy() {
		mapView.onDestroy();
		super.onDestroy();
	}

	//=================================================================================

	private void refreshMapGrid(int heightCells, int widthCells, LatLng topLeftPoint) {

		// generate Map Grid
		int arrRows = heightCells + 1;
		int arrCols = widthCells + 1;
		LatLng[][] mapGrid = Utils.generateMapGrid(arrRows, arrCols, topLeftPoint);

		//Remove old grid from map
		Utils.removeOldMapGrid(polylines, polygon);

		//Draw new grid on map
		polylines = Utils.drawMapGrid(mapGrid, googleMap);
		polygon = Utils.drawObfuscationArea(mapGrid, googleMap);
	}

}
