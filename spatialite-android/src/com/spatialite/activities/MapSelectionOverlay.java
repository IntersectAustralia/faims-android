package com.spatialite.activities;


import jsqlite.Exception;
import jsqlite.Stmt;

import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MapSelectionOverlay extends Overlay {
	private jsqlite.Database db = null;
	private TextView text = null;
	
	public MapSelectionOverlay(String dbName, TextView text) throws Exception {
		this.text = text;
		db = new jsqlite.Database();
		db.open(dbName, jsqlite.Constants.SQLITE_OPEN_READONLY);
	}
	
	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		try {
			Stmt stmt = db.prepare("SELECT name FROM Regions WHERE ST_Within(ST_Transform(MakePoint(?,?,4326),32632),Geometry);");
			stmt.bind(1, p.getLongitudeE6() / 1E6);
			stmt.bind(2, p.getLatitudeE6() / 1E6);
			
			if (stmt.step()) {
				String region = stmt.column_string(0);
				text.setText(region);
			}
			stmt.close();
		} catch (Exception e) {
		}
		
		return true;
	}

}
