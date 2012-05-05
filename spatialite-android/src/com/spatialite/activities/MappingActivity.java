package com.spatialite.activities;

import java.util.List;

import jsqlite.Exception;

import android.os.Bundle;
import android.widget.TextView;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.spatialite.R;
import com.spatialite.utilities.ActivityHelper;

public class MappingActivity extends MapActivity {
	List<Overlay> overlays = null;
	MapView mapView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		overlays = mapView.getOverlays();

		String dbFile = ActivityHelper.getDataBase(this,
				getString(R.string.test_db));

		TextView textRegion = (TextView)findViewById(R.id.text_region);
		
		if (dbFile != null) {
			try {
				overlays.add(new MapSelectionOverlay(dbFile, textRegion));
			} catch (Exception e) {

			}
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}
