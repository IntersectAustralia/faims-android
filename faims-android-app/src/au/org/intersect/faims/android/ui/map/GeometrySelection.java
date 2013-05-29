package au.org.intersect.faims.android.ui.map;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import android.graphics.Color;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;

public class GeometrySelection {
	
	private ArrayList<String[]> dataList;
	private String name;
	private boolean active;
	private GeometryStyle pointStyle;
	private GeometryStyle lineStyle;
	private GeometryStyle polygonStyle;
	
	public GeometrySelection(String name) {
		this.name = name;
		this.active = true;
		dataList = new ArrayList<String[]>();
		
		setPointStyle(GeometryStyle.defaultPointStyle());
		setLineStyle(GeometryStyle.defaultLineStyle());
		setPolygonStyle(GeometryStyle.defaultPolygonStyle());
		
		getPointStyle().pointColor = Color.CYAN;
		getLineStyle().pointColor = Color.CYAN;
		getLineStyle().lineColor = Color.CYAN;
		getPolygonStyle().pointColor = Color.CYAN;
		getPolygonStyle().lineColor = Color.CYAN;
		getPolygonStyle().polygonColor = Color.CYAN;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<String[]> getDataList() {
		return dataList;
	}
	
	private boolean compare(String[] ud1, String[] ud2) {
		if (ud1 == null || ud2 == null || ud1.length != ud2.length) return false;
		for (int i = 0; i < ud1.length; i++) {
			if (!ud1[i].equals(ud2[i])) return false;
		}
		return true;
	}
	
	public boolean hasData(String[] userData) {
		if (userData == null) return false;
		
		for (String[] ud : dataList) {
			if (compare(ud, userData)) return true;
		}
		
		return false;
	}
	
	public void addData(String[] userData) {
		if (hasData(userData)) {
			FLog.w("already contains geometry");
			return;
		}
		dataList.add(userData);
	}
	
	public void removeData(String[] userData) {
		if (!hasData(userData)) {
			FLog.w("does not contain geometry");
			return;
		}
		ListIterator<String[]> iterator = dataList.listIterator();
		while(iterator.hasNext()) {
			if (compare(iterator.next(), userData)) {
				iterator.remove();
			}
		}
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
