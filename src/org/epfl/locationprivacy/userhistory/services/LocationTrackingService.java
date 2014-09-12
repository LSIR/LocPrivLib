package org.epfl.locationprivacy.userhistory.services;

import java.util.Random;

import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.userhistory.databases.LocationTableDataSource;
import org.epfl.locationprivacy.userhistory.databases.TransitionTableDataSource;
import org.epfl.locationprivacy.userhistory.models.Transition;
import org.epfl.locationprivacy.util.Utils;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationTrackingService extends Service implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	private static final String LOGTAG = "LocationTrackingService";
	private static final int SAMPLING_INTERVAL_IN_MILLISECONDS = 5000;
	private LocationClient locationClient;
	Random random;
	int previousLocID = -1;
	int previousTimeID = -1;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		Toast.makeText(this, "Congrats! MyService Created", Toast.LENGTH_SHORT).show();
		Log.d(LOGTAG, "onCreate");

		//Random Number
		random = new Random();

	}

	@Override
	public void onStart(Intent intent, int startId) {
		Toast.makeText(this, "My Service Started", Toast.LENGTH_SHORT).show();
		Log.d(LOGTAG, "onStart");

		locationClient = new LocationClient(this, this, this);
		locationClient.connect();
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "MyService Stopped", Toast.LENGTH_SHORT).show();
		Log.d(LOGTAG, "onDestroy");
		locationClient.removeLocationUpdates(this);
	}

	//===================================================
	@Override
	public void onConnected(Bundle arg0) {
		Log.d(LOGTAG, "onConnected");
		LocationRequest locationRequest = LocationRequest.create();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locationRequest.setInterval(SAMPLING_INTERVAL_IN_MILLISECONDS);
		locationRequest.setFastestInterval(SAMPLING_INTERVAL_IN_MILLISECONDS);
		locationClient.requestLocationUpdates(locationRequest, this);
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
	}

	@Override
	public void onDisconnected() {
	}

	//===================================================
	@Override
	public void onLocationChanged(Location location) {
		Log.d(LOGTAG, "onLocationChanged");
		// open DBs
		GridDBDataSource gridDBDataSource = GridDBDataSource.getInstance(this);
		TransitionTableDataSource transitionTableDataSource = TransitionTableDataSource
				.getInstance(this);
		LocationTableDataSource locationTableDataSource = LocationTableDataSource.getInstance(this);

		// random number
		double minLat = 46.508463;
		double maxLat = 46.529253;
		double minLng = 6.606302;
		double maxLng = 6.655912;

		double randomLatitude = minLat + (maxLat - minLat) * random.nextDouble();
		double randomLongitude = minLng + (maxLng - minLng) * random.nextDouble();
		location.setLatitude(randomLatitude);
		location.setLongitude(randomLongitude);

		//Output current Location
		String msg = "Location : " + location.getLatitude() + "," + location.getLongitude();
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

		// Adding location sample to Location table
		long currTime = System.currentTimeMillis();
		org.epfl.locationprivacy.userhistory.models.Location newLocation = new org.epfl.locationprivacy.userhistory.models.Location(
				location.getLatitude(), location.getLongitude(), currTime);
		locationTableDataSource.create(newLocation);

		//==== Testing
		Log.d(LOGTAG, "No of locations: " + locationTableDataSource.countRows() + " Rows");
		//		for (org.epfl.locationprivacy.userhistory.models.Location l : locationTableDataSource
		//				.findAll())
		//			Log.d(LOGTAG, l.toString());

		// Adding transition to transition table
		MyPolygon currLocPolygon = gridDBDataSource.findGridCell(randomLatitude, randomLongitude);
		int currLocID = Integer.parseInt(currLocPolygon.getName());
		int currTimeID = Utils.findDayPortionID(currTime);
		if (previousLocID != -1) {
			Transition newTransition = new Transition(previousLocID, currLocID, previousTimeID,
					currTimeID, 1);
			transitionTableDataSource.updateOrInsert(newTransition);

			//==== Testing
			Log.d(LOGTAG, "No of transitios: " + transitionTableDataSource.countRows() + " Rows");
			//			for (Transition t : transitionTableDataSource.findAll())
			//				Log.d(LOGTAG, t.toString());
		}
		previousLocID = currLocID;
		previousTimeID = currTimeID;
	}
}
