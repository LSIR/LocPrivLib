package org.epfl.locationprivacy.map.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.epfl.locationprivacy.R;

import org.epfl.locationprivacy.map.OSMWrapperAPI;
import org.epfl.locationprivacy.map.databases.VenuesCondensedDBDataSource;
import org.epfl.locationprivacy.map.databases.VenuesCondensedDBOpenHelper;
import org.epfl.locationprivacy.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SemanticMapFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	private static final String LOGTAG = "PrivacyProfileMapFragment";
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;
	GoogleMap googleMap;
	MapView mapView;
	Bundle mBundle;
	Location currentLocation = null;
	private LatLng firstCorner = null;
	private LatLng secondCorner = null;
	private Button clearButton;
	private Button getSemanticButton;
	private Button deleteDatabase;
	private ProgressDialog progressDialog;
	private VenuesCondensedDBOpenHelper dbOpenHelper;
	private ArrayList<Pair<LatLng, LatLng>> pairs;

	public SemanticMapFragment() {
		super();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_semantic_map, container, false);

		MapsInitializer.initialize(getActivity());
		mapView = (MapView) rootView.findViewById(R.id.semantic_map);
		mapView.onCreate(mBundle);

		dbOpenHelper = VenuesCondensedDBOpenHelper.getInstance(getActivity());

		if (initMap()) {
			// First we need to check availability of play services
			if (Utils.checkPlayServices(this.getActivity(), this.getActivity().getApplicationContext())) {

				// Building the GoogleApi client
				buildGoogleApiClient();
			}

			deleteDatabase = (Button) rootView.findViewById(R.id.delete_database_button);
			deleteDatabase.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new DialogFragment() {
						@Override
						public Dialog onCreateDialog(Bundle savedInstanceState) {
							// Use the Builder class for convenient dialog construction
							AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
							builder.setMessage("Are you sure you want to delete semantic informations ?")
								.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dbOpenHelper.onUpgrade(dbOpenHelper.getWritableDatabase(), 0, 0);
										Toast.makeText(getActivity(), "The semantic locations database is now empty", Toast.LENGTH_LONG).show();
										firstCorner = null;
										secondCorner = null;
										googleMap.clear();

										//Adding Marker
										String timeStamp = dateFormat.format(new Date());
										String markerTitle = timeStamp + " " + currentLocation.toString();
										MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
										googleMap.addMarker(markerOptions);
										drawDownloadedArea();
									}
								})
								.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										// User cancelled the dialog
									}
								});
							// Create the AlertDialog object and return it
							return builder.create();
						}
					}.show(getFragmentManager(), LOGTAG);
				}
			});
			clearButton = (Button) rootView.findViewById(R.id.clear_points_button);
			clearButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					firstCorner = null;
					secondCorner = null;
					googleMap.clear();

					//Adding Marker
					String timeStamp = dateFormat.format(new Date());
					String markerTitle = timeStamp + " " + currentLocation.toString();
					MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
					googleMap.addMarker(markerOptions);
					drawDownloadedArea();
				}
			});

			getSemanticButton = (Button) rootView.findViewById(R.id.get_semantic_button);
			getSemanticButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (firstCorner != null && secondCorner != null) {
						final Pair<LatLng, LatLng> corners = getCorners(firstCorner, secondCorner);
						if (!checkAreaInclusion(corners)) {
							ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
							NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

							if (!mWifi.isConnected()) {
								Toast.makeText(getActivity(), "You must be connected to Wifi to perform this operation !", Toast.LENGTH_LONG).show();
							} else {
								new AsyncTask<Void, Integer, Void>() {
									@Override
									protected void onPreExecute() {
										//Create a new progress dialog
										progressDialog = new ProgressDialog(getActivity());
										//Set the progress dialog to display a horizontal progress bar
										progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
										//Set the dialog title to 'Loading...'
										progressDialog.setTitle("Loading...");
										//Set the dialog message to 'Loading application View, please wait...'
										progressDialog.setMessage("Loading Semantic informations from OSM...");
										//This dialog can't be canceled by pressing the back key
										progressDialog.setCancelable(false);
										//This dialog isn't indeterminate
										progressDialog.setIndeterminate(true);
										//Display the progress dialog
										progressDialog.show();
									}

									//The code to be executed in a background thread.
									@Override
									protected Void doInBackground(Void... params) {
										long start = System.currentTimeMillis();
										//Get the current thread's token
										synchronized (this) {
											OSMWrapperAPI.updateSemanticLocations(getActivity(), corners.first, corners.second);
										}
										long end = System.currentTimeMillis();
										if ((boolean) Utils.getBuildConfigValue(getActivity(), "LOGGING")) {
											Log.d(LOGTAG, "Time to save semantic area : " + (end - start) + " ms.");
										}
										return null;
									}

									//after executing the code in the thread
									@Override
									protected void onPostExecute(Void result) {
										// Save area into db
										VenuesCondensedDBDataSource dbDataSource = VenuesCondensedDBDataSource.getInstance(getActivity());
										dbDataSource.insertSemanticArea(corners.first, corners.second);
										// Clear the map and show areas
										firstCorner = null;
										secondCorner = null;
										googleMap.clear();

										//Adding Marker
										String timeStamp = dateFormat.format(new Date());
										String markerTitle = timeStamp + " " + currentLocation.toString();
										MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
										googleMap.addMarker(markerOptions);
										drawDownloadedArea();

										//close the progress dialog
										progressDialog.dismiss();
									}
								}.execute();
							}
						} else {
							Toast.makeText(getActivity(), "This area is already loaded", Toast.LENGTH_SHORT).show();
							// Clear the map and show areas
							firstCorner = null;
							secondCorner = null;
							googleMap.clear();

							//Adding Marker
							String timeStamp = dateFormat.format(new Date());
							String markerTitle = timeStamp + " " + currentLocation.toString();
							MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
							googleMap.addMarker(markerOptions);
							drawDownloadedArea();
						}
					} else {
						Toast.makeText(getActivity(), "Please select two locations on the map to create an area", Toast.LENGTH_LONG).show();
					}
				}
			});

			googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
				@Override
				public void onMapClick(LatLng point) {
					if (firstCorner == null) {
						firstCorner = point;
						//Adding Marker
						String timeStamp = dateFormat.format(new Date());
						String markerTitle = timeStamp + " " + point.toString();
						MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(point);
						googleMap.addMarker(markerOptions);
					} else if (secondCorner == null) {
						secondCorner = point;
						//Adding Marker
						String timeStamp = dateFormat.format(new Date());
						String markerTitle = timeStamp + " " + point.toString();
						MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(point);
						googleMap.addMarker(markerOptions);

						drawArea(firstCorner, secondCorner, googleMap);
					}
				}
			});

			drawDownloadedArea();
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
		Log.i(LOGTAG, "Connection failed: ConnectionResult.getErrorCode() = "
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

			//Adding Marker
			String timeStamp = dateFormat.format(new Date());
			String markerTitle = timeStamp + " " + latLng.toString();
			MarkerOptions markerOptions = new MarkerOptions().title(markerTitle).position(latLng);
			googleMap.addMarker(markerOptions);

		} else {
			Toast.makeText(this.getActivity(), "Current Location is not available, Can't Access GPS data", Toast.LENGTH_LONG).show();
		}
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

	public Pair<LatLng, LatLng> getCorners(LatLng corner1, LatLng corner2) {
		LatLng topRight, bottomLeft;
		if (corner1.latitude > corner2.latitude && corner1.longitude > corner2.longitude) {
			topRight = corner1;
			bottomLeft = corner2;
		} else if (corner1.latitude > corner2.latitude && corner1.longitude < corner2.longitude) {
			topRight = new LatLng(corner1.latitude, corner2.longitude);
			bottomLeft = new LatLng(corner2.latitude, corner1.longitude);
		} else if (corner1.latitude < corner2.latitude && corner1.longitude < corner2.longitude) {
			topRight = corner2;
			bottomLeft = corner1;
		} else {
			topRight = new LatLng(corner2.latitude, corner1.longitude);
			bottomLeft = new LatLng(corner1.latitude, corner2.longitude);
		}

		return new Pair<>(topRight, bottomLeft);
	}

	/**
	 * Draw area
	 *
	 * @param corner1
	 * @param corner2
	 * @param googleMap
	 * @return
	 */
	private Polygon drawArea(LatLng corner1, LatLng corner2, GoogleMap googleMap) {

		Pair<LatLng, LatLng> corners = getCorners(corner1, corner2);
		LatLng topRight = corners.first;
		LatLng bottomLeft = corners.second;

		PolygonOptions polygonOptions = new PolygonOptions().fillColor(0x330000FF)
			.strokeColor(Color.BLUE).strokeWidth(1);

		// Left side
		polygonOptions.add(new LatLng(topRight.latitude, bottomLeft.longitude)).add(bottomLeft);


		// Bottom
		polygonOptions.add(bottomLeft).add(new LatLng(bottomLeft.latitude, topRight.longitude));

		// Right side
		polygonOptions.add(new LatLng(bottomLeft.latitude, topRight.longitude)).add(topRight);

		// Top is done automatically

		return googleMap.addPolygon(polygonOptions);
	}

	/**
	 * Get area from db and draw them
	 */
	private void drawDownloadedArea() {
		VenuesCondensedDBDataSource dbDataSource = VenuesCondensedDBDataSource.getInstance(getActivity());
		pairs = dbDataSource.findAllStoredSemanticArea();
		for (Pair<LatLng, LatLng> pair : pairs) {
			drawArea(pair.first, pair.second, googleMap);
		}
	}

	/**
	 * Check if pair is included in one of existing zone.
	 * If an existing is included into pair, this area is removed from the db.
	 *
	 * @param pair
	 * @return
	 */
	private boolean checkAreaInclusion(Pair<LatLng, LatLng> pair) {
		double latFirst = pair.first.latitude;
		double longFirst = pair.first.longitude;
		double latSecond = pair.second.latitude;
		double longSecond = pair.second.longitude;

		for (Pair<LatLng, LatLng> p : pairs) {
			double pLatFirst = p.first.latitude;
			double pLongFirst = p.first.longitude;
			double pLatSecond = p.second.latitude;
			double pLongSecond = p.second.longitude;
			if (pLatFirst >= latFirst && pLongFirst >= longFirst && pLatSecond <= latSecond && pLongSecond <= longSecond) {
				return true;
			}
			if (pLatFirst <= latFirst && pLongFirst <= longFirst && pLatSecond >= latSecond && pLongSecond >= longSecond) {
				VenuesCondensedDBDataSource.getInstance(getActivity()).deleteSemanticArea(p);
			}
		}
		return false;
	}
}
