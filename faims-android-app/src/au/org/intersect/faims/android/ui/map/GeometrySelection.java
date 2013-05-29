package au.org.intersect.faims.android.ui.map;

import java.util.ArrayList;
import java.util.List;

import com.nutiteq.geometry.Geometry;

import au.org.intersect.faims.android.log.FLog;

public class GeometrySelection {
	
	private ArrayList<String[]> dataList;
	private String name;
	private boolean active;
	
	public GeometrySelection(String name) {
		this.name = name;
		this.active = true;
		dataList = new ArrayList<String[]>();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<String[]> getGeometryList() {
		return dataList;
	}
	
	public boolean hasGeometry(Geometry geom) {
		if (geom.userData == null) return false;
		
		for (String[] userData : dataList) {
			if (userData == geom.userData) return true;
		}
		
		return false;
	}
	
	public void addGeometry(Geometry geom) {
		if (hasGeometry(geom)) {
			FLog.w("already contains geometry");
			return;
		}
		dataList.add((String[]) geom.userData);
	}
	
	public void removeGeometry(Geometry geom) {
		if (hasGeometry(geom)) {
			FLog.w("does not contain geometry");
			return;
		}
		dataList.remove((String[]) geom.userData);
	}

	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean value) {
		this.active = value;
		FLog.d("active: " + value);
	}

}
