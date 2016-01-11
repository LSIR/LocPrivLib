package org.epfl.locationprivacy.map;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.epfl.locationprivacy.map.databases.VenuesCondensedDBDataSource;
import org.epfl.locationprivacy.map.models.MyPolygon;
import org.epfl.locationprivacy.map.models.OSMNode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.epfl.locationprivacy.map.models.OSMRelation;
import org.epfl.locationprivacy.map.models.OSMSemantic;
import org.epfl.locationprivacy.map.models.OSMWay;
import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;
import org.epfl.locationprivacy.privacyprofile.models.SemanticLocation;
import org.epfl.locationprivacy.util.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The OpenStreetMap Wrapper to get semantic locations
 * Code inspired by http://wiki.openstreetmap.org/wiki/Java_Access_Example
 */
public final class OSMWrapperAPI {

	private static final String LOGTAG = "OSMWrapperAPI";
	private static final String OVERPASS_API = "http://www.overpass-api.de/api/interpreter";


	/**
	 * @param context     the context
	 * @param xmlDocument an XML Document given by the call to Overpass
	 * @param amenities   the list of amenities to find
	 */
	@SuppressWarnings("nls")
	private static void loadSemanticLocations(Context context, Document xmlDocument, List<String> amenities) {
		HashMap<String, OSMSemantic> osmElements = new HashMap<>();

		ArrayList<String> amenitiesIds = new ArrayList<>();

		// Read XML file
		Node osmRoot = xmlDocument.getFirstChild();
		NodeList osmXMLNodes = osmRoot.getChildNodes();

		// First we search all positions that have a wanted tag (from amenities list given in argument)
		for (int i = 1; i < osmXMLNodes.getLength(); i++) {
			Node item = osmXMLNodes.item(i);
			// NODE
			if (item.getNodeName().equals("node")) {
				NamedNodeMap attributes = item.getAttributes();
				NodeList tagXMLNodes = item.getChildNodes();
				Map<String, String> tags = new HashMap<>();
				for (int j = 1; j < tagXMLNodes.getLength(); j++) {
					Node tagItem = tagXMLNodes.item(j);
					NamedNodeMap tagAttributes = tagItem.getAttributes();
					if (tagAttributes != null) {
						tags.put(tagAttributes.getNamedItem("k").getNodeValue(), tagAttributes.getNamedItem("v")
							.getNodeValue());
						if (amenities.contains(tagAttributes.getNamedItem("v").getNodeValue())) {
							amenitiesIds.add(attributes.getNamedItem("id").getNodeValue());
						}
					}
				}
				Node namedItemID = attributes.getNamedItem("id");
				Node namedItemLat = attributes.getNamedItem("lat");
				Node namedItemLon = attributes.getNamedItem("lon");
				Node namedItemVersion = attributes.getNamedItem("version");

				String id = namedItemID.getNodeValue();
				String latitude = namedItemLat.getNodeValue();
				String longitude = namedItemLon.getNodeValue();
				String version = "0";
				if (namedItemVersion != null) {
					version = namedItemVersion.getNodeValue();
				}
				String name = "";
				if (tags.containsKey("name")) {
					name = tags.get("name");
				}
				osmElements.put(id, new OSMNode(id, name, tags.get("amenity"), latitude, longitude, tags, version));
				// WAY
			} else if (item.getNodeName().equals("way")) {
				NamedNodeMap attributes = item.getAttributes();
				NodeList tagXMLNodes = item.getChildNodes();
				Map<String, String> tags = new HashMap<>();
				List<String> nd = new ArrayList<>();
				for (int j = 1; j < tagXMLNodes.getLength(); j++) {
					Node tagItem = tagXMLNodes.item(j);
					NamedNodeMap tagAttributes = tagItem.getAttributes();
					if (tagAttributes != null && tagItem.getNodeName().equals("tag")) {
						tags.put(tagAttributes.getNamedItem("k").getNodeValue(), tagAttributes.getNamedItem("v")
							.getNodeValue());
						if (amenities.contains(tagAttributes.getNamedItem("v").getNodeValue())) {
							amenitiesIds.add(attributes.getNamedItem("id").getNodeValue());
						}
					} else if (tagAttributes != null && tagItem.getNodeName().equals("nd")) {
						nd.add(tagAttributes.getNamedItem("ref").getNodeValue());
					}
				}
				Node namedItemID = attributes.getNamedItem("id");
				Node namedItemVersion = attributes.getNamedItem("version");

				String id = namedItemID.getNodeValue();
				String version = "0";
				if (namedItemVersion != null) {
					version = namedItemVersion.getNodeValue();
				}
				String name = "";
				if (tags.containsKey("name")) {
					name = tags.get("name");
				}
				osmElements.put(id, new OSMWay(id, name, tags.get("amenity"), nd, tags, version));
				// RELATION
			} else if (item.getNodeName().equals("relation")) {
				NamedNodeMap attributes = item.getAttributes();
				NodeList tagXMLNodes = item.getChildNodes();
				Map<String, String> tags = new HashMap<>();
				List<String> relations = new ArrayList<>();
				for (int j = 1; j < tagXMLNodes.getLength(); j++) {
					Node tagItem = tagXMLNodes.item(j);
					NamedNodeMap tagAttributes = tagItem.getAttributes();
					if (tagAttributes != null && tagItem.getNodeName().equals("tag")) {
						tags.put(tagAttributes.getNamedItem("k").getNodeValue(), tagAttributes.getNamedItem("v")
							.getNodeValue());
						if (amenities.contains(tagAttributes.getNamedItem("v").getNodeValue())) {
							amenitiesIds.add(attributes.getNamedItem("id").getNodeValue());
						}
					} else if (tagAttributes != null && tagItem.getNodeName().equals("member")) {
						if (tagAttributes.getNamedItem("type").getNodeValue().equals("way") || tagAttributes.getNamedItem("type").getNodeValue().equals("node")) {
							relations.add(tagAttributes.getNamedItem("ref").getNodeValue());
						}
					}
				}
				Node namedItemID = attributes.getNamedItem("id");
				Node namedItemVersion = attributes.getNamedItem("version");

				String id = namedItemID.getNodeValue();
				String version = "0";
				if (namedItemVersion != null) {
					version = namedItemVersion.getNodeValue();
				}
				String name = "";
				if (tags.containsKey("name")) {
					name = tags.get("name");
				}
				osmElements.put(id, new OSMRelation(id, name, tags.get("amenity"), relations, tags, version));
			}

		}
		VenuesCondensedDBDataSource dataSource = VenuesCondensedDBDataSource.getInstance(context);
		// We get the already existing tags from the db
		HashMap<String, OSMSemantic> elementsInDB = dataSource.findAllSemanticLocationsInDB();

		// Then we add each element to the db if it has a subtype and is not already in the db
		// If it is already in the db, we simply compare the 'version' field.
		for (int i = 0; i < amenitiesIds.size(); i++) {
			OSMSemantic element = osmElements.get(amenitiesIds.get(i));
			if (element == null || element.getSubtype() == null) {
				Log.i(LOGTAG, "An element is null or without subtype for id " + amenitiesIds.get(i));
				continue;
			}
			if (elementsInDB.containsKey(element.getId())) {
				if (Double.valueOf(element.getVersion()) > Double.valueOf(elementsInDB.get(element.getId()).getVersion())) {
					dataSource.delete(elementsInDB.get(element.getId()));
				} else {
					continue;
				}
			}
			// NODE
			if (element.getClass().equals(OSMNode.class)) {
				OSMNode node = (OSMNode) element;
				ArrayList<LatLng> points = new ArrayList<>();
				points.add(new LatLng(Double.valueOf(node.getLat()), Double.valueOf(node.getLong())));
				MyPolygon point = new MyPolygon(node.getId(), node.getSubtype(), points);
				dataSource.insertPolygonIntoDB(node, point);

				// WAY
			} else if (element.getClass().equals(OSMWay.class)) {
				OSMWay way = (OSMWay) osmElements.get(element.getId());
				if (!(way.getNodes().get(0).equals(way.getNodes().get(way.getNodes().size() - 1)))) {
					Log.i(LOGTAG, "There are ways that are not considered :" + way.toString());
					continue;
				}
				ArrayList<LatLng> points = new ArrayList<>();
				for (String nodeID : ((OSMWay) element).getNodes()) {
					OSMNode node = (OSMNode) osmElements.get(nodeID);
					points.add(new LatLng(Double.valueOf(node.getLat()), Double.valueOf(node.getLong())));
				}
				MyPolygon point = new MyPolygon(way.getId(), way.getSubtype(), points);
				dataSource.insertPolygonIntoDB(way, point);

				// RELATION
			} else if (element.getClass().equals(OSMRelation.class)) {
				OSMRelation rel = (OSMRelation) osmElements.get(element.getId());
				if (rel.getTags().containsKey("type") && !rel.getTags().get("type").equals("multipolygon")) {
					Log.i(LOGTAG, "There are Relations that are not considered :" + rel.toString());
					continue;
				}
				ArrayList<LatLng> points = new ArrayList<>();
				for (String wayID : ((OSMRelation) element).getWays()) {
					OSMWay way = (OSMWay) osmElements.get(wayID);
					for (String nodeID : way.getNodes()) {
						OSMNode node = (OSMNode) osmElements.get(nodeID);
						points.add(new LatLng(Double.valueOf(node.getLat()), Double.valueOf(node.getLong())));
					}
				}
				MyPolygon point = new MyPolygon(rel.getId(), rel.getSubtype(), points);
				// Insertion into the db
				dataSource.insertPolygonIntoDB(rel, point);
			}
		}
	}

	/**
	 * @return the nodes in the formulated query
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	private static Document getNodesViaOverpass(LatLng topRight, LatLng bottomLeft, List<String> amenities) throws IOException, ParserConfigurationException, SAXException {
		String hostname = OVERPASS_API;
		String topRightLat = Double.toString(topRight.latitude);
		String topRightLong = Double.toString(topRight.longitude);
		String bottomLeftLat = Double.toString(bottomLeft.latitude);
		String bottomLeftLong = Double.toString(bottomLeft.longitude);

		String queryString = "(";
		String regexAmenities = "";
		for (int i = 0; i < amenities.size(); i++) {
			regexAmenities += amenities.get(i);
			if (i < amenities.size() - 1) {
				regexAmenities += "|";
			}
		}

		queryString += "relation[\"amenity\"~\"" + regexAmenities + "\"](" + bottomLeftLat + "," + bottomLeftLong + "," + topRightLat + "," + topRightLong + ");";
		queryString += "way[\"amenity\"~\"" + regexAmenities + "\"](" + bottomLeftLat + "," + bottomLeftLong + "," + topRightLat + "," + topRightLong + ");";
		queryString += "node[\"amenity\"~\"" + regexAmenities + "\"](" + bottomLeftLat + "," + bottomLeftLong + "," + topRightLat + "," + topRightLong + ");";
		queryString += ");(._;>;);out body qt;";

		URL osm = new URL(hostname);
		HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
		printout.writeBytes("data=" + URLEncoder.encode(queryString, "utf-8"));
		printout.flush();
		printout.close();

		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		return docBuilder.parse(connection.getInputStream());
	}

	/**
	 * Read semantic tags
	 *
	 * @param context
	 * @return
	 */
	private static List<String> loadSemanticTags(Context context) {
		SemanticLocationsDataSource semanticLocationsDataSource = SemanticLocationsDataSource.getInstance(context);

		List<SemanticLocation> semanticLocations = semanticLocationsDataSource.findAll();
		ArrayList<String> semanticLocationsNames = new ArrayList<>();
		for (SemanticLocation semanticLocation : semanticLocations) {
			semanticLocationsNames.add(semanticLocation.name);
		}
		return semanticLocationsNames;
	}

	private static void log(Context context, String s) {
		if ((boolean) Utils.getBuildConfigValue(context, "LOGGING")) {
			Log.d(LOGTAG, s);
			Utils.createNewLoggingFolder(context, LOGTAG);
			Utils.appendLog(LOGTAG + ".txt", s, context);
		}
	}

	/**
	 * Update the semantic location given two points
	 *
	 * @param context
	 * @param topRight   the top right location of the area to load
	 * @param bottomLeft the bottom left location of the area to load
	 */
	public static boolean updateSemanticLocations(Context context, LatLng topRight, LatLng bottomLeft) {
		List<String> amenities = loadSemanticTags(context);
		try {
			log(context, "========= START DOWNLOADING SEMANTIC DATA FROM OVERPASS ====");
			log(context, "Loading data for area : " + topRight.toString() + " and " + bottomLeft.toString());
			long start = System.currentTimeMillis();
			Document doc = getNodesViaOverpass(topRight, bottomLeft, amenities);
			long end = System.currentTimeMillis();
			log(context, "========= END DOWNLOADING SEMANTIC DATA FROM OVERPASS ====");
			log(context, "Loading data from OverpassAPI took " + (end - start) + " ms.");
			start = System.currentTimeMillis();
			loadSemanticLocations(context, doc, amenities);
			end = System.currentTimeMillis();
			log(context, "Storing semantic data for zone : " + topRight.toString() + " and " + bottomLeft.toString() + " took " + (end-start) + " ms.");
		} catch (Exception e) {
			log(context, "Error getting semantic Locations : " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
}