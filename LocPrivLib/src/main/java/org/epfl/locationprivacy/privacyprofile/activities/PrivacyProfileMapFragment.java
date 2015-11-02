package org.epfl.locationprivacy.privacyprofile.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.util.Utils;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;

public class PrivacyProfileMapFragment extends Fragment implements OnSeekBarChangeListener,
		                                                                   GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	private static final String TAG = "PrivacyProfileMapFragment";
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;
	GoogleMap googleMap;
	MapView mapView;
	Bundle mBundle;
	ArrayList<Polyline> polylines = new ArrayList<Polyline>();
	Polygon polygon = null;
	Polygon currDrawableGridCell = null;
	MyPolygon currSelectedGridCell = null;
	SeekBar privacyBar;
	CheckBox checkBox;
	HashMap<String, Polygon> idToDrawablePolygon = new HashMap<String, Polygon>();
	GridDBDataSource gridDBDataSource;
	Location currentLocation = null;
	LatLng[][] mapGrid = null;

	public PrivacyProfileMapFragment() {
		super();
		gridDBDataSource = GridDBDataSource.getInstance(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_privacyprofile_map, container, false);

		//checkbox
		checkBox = (CheckBox) rootView.findViewById(R.id.checkbox);
		checkBox.setChecked(false);
		checkBox.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Integer newSensitivity;
				if (checkBox.isChecked()) {
					//enable privacy bar
					privacyBar.setEnabled(true);
					privacyBar.setProgress(0);
					newSensitivity = 0;

					// draw new map red layer (sensitive grid cell)
					Polygon newPolygon = Utils.drawPolygon(currSelectedGridCell, googleMap,
							                                      0x33FF0000);
					idToDrawablePolygon.put(currSelectedGridCell.getName(), newPolygon);
				} else {
					//disable privacy bar
					privacyBar.setEnabled(false);
					newSensitivity = null;

					// remove red layer
					Polygon prev = idToDrawablePolygon.remove(currSelectedGridCell.getName());
					prev.remove();
				}

				//save sensitivity to db

				int gridId = Integer.parseInt(currSelectedGridCell.getName());
				if (gridDBDataSource.findGridCell(gridId) == null) {
					gridDBDataSource.insertPolygonIntoDB(currSelectedGridCell);
				} else {
					gridDBDataSource.updateGridCellSensitivity(gridId, newSensitivity);
				}
				Toast.makeText(getActivity(), "Successfully saved value: " + newSensitivity,
						              Toast.LENGTH_SHORT).show();

			}
		});

		// seekbar
		privacyBar = (SeekBar) rootView.findViewById(R.id.seekbar);
		privacyBar.setProgressDrawable(getActivity().getResources().getDrawable(
				                                                                       R.drawable.seekbarbgimage));
		privacyBar.setEnabled(false);
		privacyBar.setOnSeekBarChangeListener(this);

		MapsInitializer.initialize(getActivity());
		mapView = (MapView) rootView.findViewById(R.id.privacyprofilemap);
		mapView.onCreate(mBundle);

		if (initMap()) {
			// First we need to check availability of play services
			if (Utils.checkPlayServices(this.getActivity(), this.getActivity().getApplicationContext())) {

				// Building the GoogleApi client
				buildGoogleApiClient();
			}

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
					currSelectedGridCell = gridDBDataSource.findGridCell(point.latitude, point.longitude);
					if (currSelectedGridCell == null) {
						LatLng cellPosition = Utils.findCellTopLeftPoint(point);
						int cellID = Utils.computeCellIDFromPosition(cellPosition);
						ArrayList<LatLng> corners = Utils.computeCellCornerPoints(cellPosition);
						currSelectedGridCell = new MyPolygon(cellID + "", "", corners);
					}


					//--> remove select grid
					if (currDrawableGridCell != null)
						currDrawableGridCell.remove();

					//--> add new one
					currDrawableGridCell = Utils.drawPolygon(currSelectedGridCell,
							                                        googleMap, 0x3300FF00);

					// activate scroll & checkbox
					if (currSelectedGridCell.getSensitivityAsInteger() != null) {
						checkBox.setChecked(true);
						privacyBar.setEnabled(true);

						// update sensitivity bar
						int currSensitivity = currSelectedGridCell
								                      .getSensitivityAsInteger();
						privacyBar.setProgress(currSensitivity);
					} else {
						// deactivate scroll and check box
						privacyBar.setEnabled(false);
						checkBox.setChecked(false);
					}

					// test sensitivity
					Toast.makeText(
							              getActivity(),
							              "CellID: " + currSelectedGridCell.getName() + "Sensitivity : "
									              + currSelectedGridCell.getSensitivityAsDouble(),
							              Toast.LENGTH_SHORT).show();

				}
			});

			// Refresh the grid according to the current position on the map and the zoom
			googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
				@Override
				public void onCameraChange(CameraPosition cameraPosition) {
					LatLng position = cameraPosition.target;
					int height;
					int width;
					LatLng topLeftPoint;
					if (cameraPosition.zoom > 11.5 && cameraPosition.zoom <= 12.5) {
						height = Utils.GRID_HEIGHT_CELLS * 3 / 2;
						width = Utils.GRID_WIDTH_CELLS * 3 / 2;
					} else if (cameraPosition.zoom > 12.5 && cameraPosition.zoom <= 14.5) {
						height = Utils.GRID_HEIGHT_CELLS;
						width = Utils.GRID_WIDTH_CELLS;
					} else if (cameraPosition.zoom > 14.5 && cameraPosition.zoom <= 16.5) {
						height = Utils.GRID_HEIGHT_CELLS / 2;
						width = Utils.GRID_WIDTH_CELLS / 2;
					} else if (cameraPosition.zoom > 16.5 && cameraPosition.zoom <= 20.5) {
						height = Utils.GRID_HEIGHT_CELLS / 4;
						width = Utils.GRID_WIDTH_CELLS / 4;
					} else if (cameraPosition.zoom > 20.5) {
						height = Utils.GRID_HEIGHT_CELLS / 8;
						width = Utils.GRID_WIDTH_CELLS / 8;
					} else {
						height = Utils.GRID_HEIGHT_CELLS;
						width = Utils.GRID_WIDTH_CELLS;
					}
					topLeftPoint = Utils.findGridTopLeftPoint(position, height, width);
					refreshMapGrid(height, width, topLeftPoint);
				}
			});

		} else {
			Toast.makeText(getActivity(), "Map not available", Toast.LENGTH_SHORT).show();
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
		Utils.checkPlayServices(this.getActivity(), this.getActivity().getApplicationContext());
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

	@Override
	public void onStart() {
		super.onStart();
		if (mGoogleApiClient != null) {
			mGoogleApiClient.connect();
		}
	}

	/**
	 * Google api callback methods
	 */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
				           + result.getErrorCode());
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		mGoogleApiClient.connect();
	}

	@Override
	public void onConnected(Bundle arg0) {
		Toast.makeText(this.getActivity(), "Connected to location service", Toast.LENGTH_SHORT).show();

		MapsInitializer.initialize(this.getActivity());

		currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
		if (currentLocation != null) {

			//Animate
			LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14);
			googleMap.moveCamera(cameraUpdate);

			// FIXME : Keep the marker ?
			//Adding Marker
			String timeStamp = dateFormat.format(new Date());
			String markerTitle = timeStamp + " " + latLng.toString();
			MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(latLng);
			googleMap.addMarker(markerOptions);

			// top Left corner
			LatLng centerPoint = new LatLng(latLng.latitude, latLng.longitude);
			LatLng topLeftPoint = Utils.findGridTopLeftPoint(centerPoint, Utils.GRID_HEIGHT_CELLS, Utils.GRID_WIDTH_CELLS);

			// generate Map Grid
			int arrRows = Utils.GRID_HEIGHT_CELLS + 1;
			int arrCols = Utils.GRID_WIDTH_CELLS + 1;
			mapGrid = Utils.generateMapGrid(arrRows, arrCols, topLeftPoint);

			// refresh map
			refreshMapGrid(Utils.GRID_HEIGHT_CELLS, Utils.GRID_WIDTH_CELLS, topLeftPoint);

		} else {
			Toast.makeText(this.getActivity(), "Current Location is not available, Can't Access GPS data", Toast.LENGTH_LONG).show();
		}
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

	//===================================================================================
	//seek bar methods
	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekbar) {
		// update gridcell
		int sensitivity = seekbar.getProgress();
		int gridId = Integer.parseInt(currSelectedGridCell.getName());
		gridDBDataSource.updateGridCellSensitivity(gridId, sensitivity);

		Toast.makeText(getActivity(), "Successfully saved value: " + sensitivity,
				              Toast.LENGTH_SHORT).show();
	}

	/**
	 * Creating google api client object
	 */
	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this.getActivity().getApplicationContext())
				                   .addConnectionCallbacks(this)
				                   .addOnConnectionFailedListener(this)
				                   .addApi(LocationServices.API).build();
	}
}
