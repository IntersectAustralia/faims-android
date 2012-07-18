package com.spatialite.activities;

import java.io.FileNotFoundException;
import java.util.List;

import jsqlite.Exception;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.spatialite.R;
import com.spatialite.utilities.ActivityHelper;

public class MappingActivity extends MapActivity {
	private static final String TAG = MappingActivity.class.getName();
	List<Overlay> mOverlays;
	MapView mMapView;
	MapController mMapController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);
		mMapController = mMapView.getController();
		mOverlays = mMapView.getOverlays();

		try {
			String dbFile = ActivityHelper.getDataBase(this,
					getString(R.string.test_db));

			TextView textRegion = (TextView) findViewById(R.id.text_region);

			// Add overlay to MapView
			mOverlays.add(new MapSelectionOverlay(dbFile, textRegion));

		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		
		// Center map on Italy
		mMapController.setCenter(new GeoPoint((int) (42.78 * 1E6), (int) (11.82 * 1E6)));
		mMapController.setZoom(7);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}
