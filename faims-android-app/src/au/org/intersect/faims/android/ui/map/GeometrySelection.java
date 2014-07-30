package au.org.intersect.faims.android.ui.map;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;

public class GeometrySelection {
	
	private ArrayList<String> dataList;
	private String name;
	private boolean active;
	private GeometryStyle pointStyle;
	private GeometryStyle lineStyle;
	private GeometryStyle polygonStyle;
	
	public GeometrySelection(String name) {
		this.name = name;
		this.active = true;
		dataList = new ArrayList<String>();
		
		setPointStyle(GeometryStyle.defaultPointStyle());
		setLineStyle(GeometryStyle.defaultLineStyle());
		setPolygonStyle(GeometryStyle.defaultPolygonStyle());
		
		pointStyle.pointColor = Color.CYAN;
		lineStyle.pointColor = Color.CYAN;
		lineStyle.lineColor = Color.CYAN;
		polygonStyle.pointColor = Color.CYAN;
		polygonStyle.lineColor = Color.CYAN;
		polygonStyle.polygonColor = 0x4400FFFF;
	}
	
	public GeometrySelection(String name, boolean active, GeometryStyle pointStyle,
			GeometryStyle lineStyle, GeometryStyle polygonStyle, JSONArray data) throws JSONException {
		this.name = name;
		this.active = active;
		dataList = new ArrayList<String>();
		for (int i=0; i<data.length(); i++) {
			dataList.add(data.getString(i));
		}
		
		this.pointStyle = pointStyle;
		this.lineStyle = lineStyle;
		this.polygonStyle = polygonStyle;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<String> getDataList() {
		return dataList;
	}
	
	public boolean hasData(String data) {
		return dataList.contains(data);
	}
	
	public void addData(String data) {
		if (hasData(data)) {
			FLog.w("already contains geometry");
			return;
		}
		dataList.add(data);
	}
	
	public void removeData(String data) {
		if (!hasData(data)) {
			FLog.w("does not contain geometry");
			return;
		}
		dataList.remove(data);
	}

	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean value) {
		this.active = value;
	}

	public GeometryStyle getPointStyle() {
		return pointStyle;
	}

	public void setPointStyle(GeometryStyle pointStyle) {
		this.pointStyle = pointStyle;
	}

	public GeometryStyle getLineStyle() {
		return lineStyle;
	}

	public void setLineStyle(GeometryStyle lineStyle) {
		this.lineStyle = lineStyle;
	}

	public GeometryStyle getPolygonStyle() {
		return polygonStyle;
	}

	public void setPolygonStyle(GeometryStyle polygonStyle) {
		this.polygonStyle = polygonStyle;
	}

	public void saveToJSON(JSONObject json) {
		try {
			json.put("name", name);
			json.put("active", active);
			JSONObject point = new JSONObject();
			pointStyle.saveToJSON(point);
			json.put("pointStyle", point);
			JSONObject line = new JSONObject();
			lineStyle.saveToJSON(line);
			json.put("lineStyle", line);
			JSONObject polygon = new JSONObject();
			polygonStyle.saveToJSON(polygon);
			json.put("polygonStyle", polygon);
			JSONArray data = new JSONArray();
			for (String s : dataList) {
				data.put(s);
			}
			json.put("data", data);
		} catch (JSONException e) {
			FLog.e("Couldn't serialize GeometrySelection", e);
		}
	}
}
