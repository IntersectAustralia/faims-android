package com.spatialite.activities;

import jsqlite.Exception;
import jsqlite.Stmt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Map overlay to highlight various regions when the uses clicks on the MapView.
 */
public class MapSelectionOverlay extends Overlay {
	private static final String TAG = MapSelectionOverlay.class.getName();

	private final jsqlite.Database mDatabase;
	private final TextView mTextVew;

	// Allocate once and reuse
	private final Paint mPaint = new Paint();
	private final Path mPath = new Path();

	// Results
	private String mRegion;
	private Geometry mGeometry;

	/**
	 * @param  databaseName  Name of database containing the Regions table. Cannot be null.
	 * @param  textVew TextView used to display the region name. Cannot be null.
	 */
	public MapSelectionOverlay(String databaseName, TextView textVew) throws Exception {
		// TODO make sure databaseName and textView are not null
		
		mTextVew = textVew;
		
		// Open readonly database
		mDatabase = new jsqlite.Database();
		mDatabase.open(databaseName, jsqlite.Constants.SQLITE_OPEN_READONLY);

		// Edit paint style
		mPaint.setDither(true);
		mPaint.setColor(Color.rgb(128, 136, 231));
		mPaint.setAlpha(100);
		mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(6);
	}

	/**
	 * Handle a "tap" event.
	 * 
	 * @see com.google.android.maps.Overlay#onTap(GeoPoint, MapView)
	 */
	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		try {
			// Create query
			// TODO reuse stmt
			Stmt stmt = mDatabase
					.prepare("SELECT name, AsBinary(ST_Transform(geometry,4326)) FROM Regions WHERE ST_Within(ST_Transform(MakePoint(?,?,4326),32632),Geometry);");
			stmt.bind(1, p.getLongitudeE6() / 1E6);
			stmt.bind(2, p.getLatitudeE6() / 1E6);

			if (stmt.step()) {
				// Set region name
				mRegion = stmt.column_string(0);
				mTextVew.setText(mRegion);
				
				// Create JTS geometry from binary representation
				// returned from database
				try {
					mGeometry = new WKBReader().read(stmt.column_bytes(1));
				} catch (ParseException e) {
					mGeometry = null;
					Log.e(TAG, e.getMessage());
				}
			}
			stmt.close();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}

		// Indicate tap was handled
		return true;
	}

	/**
	 * Draw the overlay over the map.
	 * 
	 * @see com.google.android.maps.Overlay#draw(Canvas, MapView, boolean)
	 */
	@Override
	public void draw(Canvas canvas, MapView mapv, boolean shadow) {
		super.draw(canvas, mapv, shadow);

		if (mGeometry != null) {
			// TODO There could be more than one geometries  
			Geometry g = mGeometry.getGeometryN(0);
			final Point p = new Point();
			boolean first = true;

			mPath.reset();
			for (Coordinate c : g.getCoordinates()) {
				// Convert lat/lon to pixels on screen
				// GeoPoint is immutable so allocation is unavoidable
				Projection projection = mapv.getProjection();
				projection.toPixels(new GeoPoint((int) (c.y * 1E6), (int) (c.x * 1E6)), p);

				// Set path starting point to first coordinate
				// otherwise default start is (0,0)
				if (first) {
					mPath.moveTo(p.x, p.y);
					first = false;
				}
				
				// Add new point to path
				mPath.lineTo(p.x, p.y);
			}
		}

		// Draw the path with give paint
		canvas.drawPath(mPath, mPaint);
	}
}
