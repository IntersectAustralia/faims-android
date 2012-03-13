package com.spatialite.activities;

import java.io.IOException;

import com.spatialite.R;
import com.spatialite.utilities.ActivityHelper;
import com.spatialite.utilities.AssetHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = MainActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btn_install_to_application) {
			try {
				AssetHelper.CopyAsset(this,
						ActivityHelper.getPath(this, false),
						getString(R.string.test_db));
			} catch (IOException e) {
				ActivityHelper.showAlert(this,
						getString(R.string.error_copy_failed));
			}
		} else if (v.getId() == R.id.btn_install_to_external) {
			try {
				AssetHelper.CopyAsset(this, ActivityHelper.getPath(this, true),
						getString(R.string.test_db));
			} catch (IOException e) {
				ActivityHelper.showAlert(this,
						getString(R.string.error_copy_failed));
			}
		} else if (v.getId() == R.id.btn_run_tests) {
			Intent myIntent = new Intent(this, TestActivity.class);
			startActivity(myIntent);
		}
	}
}
