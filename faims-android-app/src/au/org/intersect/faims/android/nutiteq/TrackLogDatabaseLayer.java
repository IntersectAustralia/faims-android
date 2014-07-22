package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;
import au.org.intersect.faims.android.data.User;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.projections.Projection;

public class TrackLogDatabaseLayer extends DatabaseLayer {

	private Map<User, Boolean> users;

	public TrackLogDatabaseLayer(int layerId, String name, Projection projection, CustomMapView mapView, Type type,
			String queryName, String querySql, int maxObjects, Map<User, Boolean> users,
			GeometryStyle pointStyle,
			GeometryStyle lineStyle,
			GeometryStyle polygonStyle) {
		super(layerId, name, projection, mapView, type, queryName, querySql,
				maxObjects, pointStyle, lineStyle, polygonStyle);
		this.users = users;
	}
	
	public void toggleUser(User user, boolean isChecked){
		users.put(user, isChecked);
	}
	
	@Override
	public void loadData(Envelope envelope, int zoom) {
		try {
			ArrayList<MapPos> pts = mapView.getMapBoundaryPts();
			if (type == Type.GPS_TRACK) {
				Vector<Geometry> objects = new Vector<Geometry>();
				for(Entry<User, Boolean> user : users.entrySet()){
					if(user.getValue()){
						Vector<Geometry> objectTemp = null;
						String md5Hex = new String(Hex.encodeHex(DigestUtils.md5(user.getKey().getFirstName() + " " + user.getKey().getLastName())));
						int hue =  (int) Long.parseLong(md5Hex.substring(0, 10),16) % 360;
						float[] hsv = new float[3];
						hsv[0] = hue < 0 ? hue + 360 : hue;
						hsv[1] = 1;
						hsv[2] = 1;
						GeometryStyle pointStyle = GeometryStyle.defaultPointStyle();
						pointStyle.pointColor = Color.HSVToColor(hsv);
						this.pointStyle = pointStyle;

						objectTemp = databaseManager.fetchRecord().fetchVisibleGPSTrackingForUser(pts, maxObjects, querySql, user.getKey().getUserId());
						createElementsInLayer(zoom, objectTemp, objects, GeometryData.Type.ENTITY);
					}
				}
				
				setVisibleElementsList(objects);
				
			}else {
				super.loadData(envelope, zoom);
			}
		} catch (Exception e) {
			FLog.e("error rendering track log layer", e);
		}
		
		if (textLayer != null) {
			textLayer.renderOnce();
			textLayer.calculateVisibleElements(envelope, zoom);
		}
	}
	
	public void saveToJSON(JSONObject json) {
		try {
			json.put("name", getName());
			json.put("type", "TrackLogDatabaseLayer");
			json.put("queryName", getQueryName());
			json.put("querySQL", getQuerySQL());
			JSONObject point = new JSONObject();
			pointStyle.saveToJSON(point);
			json.put("pointStyle", point);
			JSONObject line = new JSONObject();
			pointStyle.saveToJSON(point);
			json.put("lineStyle", line);
			JSONObject polygon = new JSONObject();
			pointStyle.saveToJSON(point);
			json.put("polygonStyle", polygon);
			json.put("visible", isVisible());
			if (getTextLayer() != null) {
				JSONObject textLayer = new JSONObject();
				getTextLayer().saveToJSON(textLayer);
				json.put("textLayer", textLayer);
			}
			JSONArray userList = new JSONArray();
			for (Entry<User, Boolean> entry : users.entrySet()) {
				JSONObject user = new JSONObject();
				entry.getKey().saveToJSON(user);
				user.put("checked", entry.getValue());
			}
			json.put("users", userList);
		} catch (JSONException e) {
			FLog.e("Couldn't serialize TrackLogDatabaseLayer", e);
		}
	}
}
