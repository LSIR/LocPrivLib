package org.epfl.locationprivacy.virtualtransitiongenerator.activities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.epfl.locationprivacy.map.databases.GridDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.util.Utils;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GoogleDirectionAPI extends AsyncTask<String, String, String> {

	private static final String LOGTAG = "GoogleDirectionAPI";
	GoogleMap googleMap;
	GridDBDataSource gridDBDataSource;
	Pair<MyPolygon, LatLng> originInfo;
	Pair<MyPolygon, LatLng> destinationInfo;
	String transportationMean;
	ArrayList<Polygon> polygons;
	ArrayList<Marker> markers;
	ArrayList<Polyline> polylines;
	Context context;
	NumberFormat formatter = new DecimalFormat("#0.00");

	public GoogleDirectionAPI(GoogleMap map, GridDBDataSource gridDB,
	                          Pair<MyPolygon, LatLng> origin, Pair<MyPolygon, LatLng> destination,
	                          String transportationMean, Context context) {
		googleMap = map;
		gridDBDataSource = gridDB;
		this.originInfo = origin;
		this.destinationInfo = destination;
		this.transportationMean = transportationMean;
		polygons = new ArrayList<Polygon>();
		markers = new ArrayList<Marker>();
		polylines = new ArrayList<Polyline>();
		this.context = context;
	}

	@Override
	protected String doInBackground(String... uri) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response;
		String responseString = null;
		try {
			response = httpclient.execute(new HttpGet(uri[0]));
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();
				responseString = out.toString();
			} else {
				//Closes the connection.
				response.getEntity().getContent().close();
				throw new IOException(statusLine.getReasonPhrase());
			}
		} catch (ClientProtocolException e) {
			//TODO Handle problems..
		} catch (IOException e) {
			//TODO Handle problems..
		}
		Log.d("MainActivity", "back00000:" + responseString);
		return responseString;
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);

		if (result == null) {
			Toast.makeText(context,
					              "Google API doesn't respond, You might have lost internet connection",
					              Toast.LENGTH_SHORT).show();
			return;
		}

		JsonElement jelement = new JsonParser().parse(result.toString());
		JsonObject jobject = jelement.getAsJsonObject();
		String status = jobject.get("status").getAsString();
		Log.d(LOGTAG, status);
		JsonObject firstRoute = jobject.getAsJsonArray("routes").get(0).getAsJsonObject();
		JsonObject firstLeg = firstRoute.getAsJsonArray("legs").get(0).getAsJsonObject();
		JsonArray steps = firstLeg.getAsJsonArray("steps");

		//--> draw origin
		markers.add(googleMap.addMarker(new MarkerOptions().position(originInfo.second).title(
				                                                                                     originInfo.first.getSemantic())));
		polygons.add(Utils.drawPolygon(originInfo.first, googleMap, 0x33FF0000));

		LatLng previousPoint = originInfo.second;
		long accDuration = 0;
		for (int i = 0; i < steps.size(); i++) {
			JsonObject step = steps.get(i).getAsJsonObject();

			double lat = step.get("end_location").getAsJsonObject().get("lat").getAsDouble();
			double lng = step.get("end_location").getAsJsonObject().get("lng").getAsDouble();
			LatLng currPoint = new LatLng(lat, lng);
			LatLng currentCell = Utils.findCellTopLeftPoint(currPoint);
			accDuration += step.get("duration").getAsJsonObject().get("value").getAsLong();
			String accDurationString = formatter.format(accDuration * 1.0 / 60.0) + " min "
					                           + transportationMean;

			//--> draw grid cell
			MyPolygon gridCell = gridDBDataSource.findGridCell(Utils.computeCellIDFromPosition(currentCell));
			if (gridCell != null)
				polygons.add(Utils.drawPolygon(gridCell, googleMap, 0x3300FF00));

			//--> draw marker
			MarkerOptions markerOptions2 = new MarkerOptions().position(currPoint)
					                               .title("Step: " + (i + 1)).snippet(accDurationString);
			markers.add(googleMap.addMarker(markerOptions2));

			//--> draw arrow
			if (previousPoint != null) {
				markers.add(Utils.DrawArrowHead(googleMap, previousPoint, currPoint));
				polylines.add(googleMap.addPolyline(new PolylineOptions()
						                                    .add(previousPoint, currPoint).width(5).color(Color.BLUE)));
			}

			previousPoint = currPoint;
		}

		// time to destination
		Long tripTimeInSec = firstLeg.get("duration").getAsJsonObject().get("value").getAsLong();
		String tripTimeString = formatter.format(tripTimeInSec * 1.0 / 60.0) + " min "
				                        + transportationMean;

		//--> draw destination
		markers.add(Utils.DrawArrowHead(googleMap, previousPoint, destinationInfo.second));
		polylines.add(googleMap.addPolyline(new PolylineOptions()
				                                    .add(previousPoint, destinationInfo.second).width(5).color(Color.BLUE)));
		markers.add(googleMap.addMarker(new MarkerOptions().position(destinationInfo.second)
				                                .snippet(tripTimeString).title(destinationInfo.first.getSemantic())));
		polygons.add(Utils.drawPolygon(destinationInfo.first, googleMap, 0x33FF0000));

		// move camera to origin centroid
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(originInfo.second, 17);
		googleMap.moveCamera(cameraUpdate);

		Log.d(LOGTAG, "Total Time: " + firstLeg.get("duration").getAsJsonObject().get("text"));
		Log.d(LOGTAG, " -> " + firstLeg.get("duration").getAsJsonObject().get("value") + " sec");

	}

	public void clearMap() {
		for (Polyline p : polylines) {
			p.remove();
		}
		polylines.clear();

		for (Polygon p : polygons) {
			p.remove();
		}
		polygons.clear();

		for (Marker m : markers) {
			m.remove();
		}
		markers.clear();

	}
}
