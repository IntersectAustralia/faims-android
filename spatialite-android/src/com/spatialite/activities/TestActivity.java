package com.spatialite.activities;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import com.spatialite.R;
import com.spatialite.R.id;
import com.spatialite.R.layout;
import com.spatialite.utilities.ActivityHelper;
import com.spatialite.utilities.AssetHelper;

import jsqlite.Callback;
import jsqlite.Exception;
import jsqlite.Stmt;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class TestActivity extends Activity implements OnClickListener {

	private static final String TAG = "TestActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.button1) {
			try {

				TextView view = (TextView)findViewById(R.id.txt_result);
				view.setText("Result: Failed");
				
				String dbFile = ActivityHelper.getDataBase(this,
						getString(R.string.test_db));
				if (dbFile == null) {
					ActivityHelper.showAlert(this, getString(R.string.error_locate_failed));
					throw new IOException(getString(R.string.error_locate_failed));
				}

				jsqlite.Database db = new jsqlite.Database();
				db.open(dbFile.toString(),
						jsqlite.Constants.SQLITE_OPEN_READONLY);

				Callback cb = new Callback() {
					@Override
					public void columns(String[] coldata) {
						Log.v(TAG, "Columns: " + Arrays.toString(coldata));
					}

					@Override
					public void types(String[] types) {
						Log.v(TAG, "Types: " + Arrays.toString(types));
					}

					@Override
					public boolean newrow(String[] rowdata) {
						Log.v(TAG, "Row: " + Arrays.toString(rowdata));

						return false;
					}
				};
				
				String query = "SELECT name, peoples, AsText(Geometry) from Towns where peoples > 350000";
				Stmt st = db.prepare(query);
				st.step();
				st.close();

				db.exec("select Distance(PointFromText('point(-77.35368 39.04106)', 4326), PointFromText('point(-77.35581 39.01725)', 4326));",
						cb);
				db.exec("SELECT name, peoples, AsText(Geometry), GeometryType(Geometry), NumPoints(Geometry), SRID(Geometry), IsValid(Geometry) from Towns where peoples > 350000;",
						cb);
				db.exec("SELECT Distance( Transform(MakePoint(4.430174797, 51.01047063, 4326), 32631), Transform(MakePoint(4.43001276, 51.01041585, 4326),32631));",
						cb);

				db.close();
				view.setText("Result: Passed");
			} catch (jsqlite.Exception e) {
				Log.e(TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}
}