package au.org.intersect.faims.android.ui.map;

import java.util.ArrayList;
import java.util.List;

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
		
		getPointStyle().pointColor = Color.CYAN;
		getLineStyle().pointColor = Color.CYAN;
		getLineStyle().lineColor = Color.CYAN;
		getPolygonStyle().pointColor = Color.CYAN;
		getPolygonStyle().lineColor = Color.CYAN;
		getPolygonStyle().polygonColor = Color.CYAN & 0x44FFFFFF;
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

}
