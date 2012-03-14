package com.spatialite.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jsqlite.Stmt;

import com.spatialite.R;
import com.spatialite.utilities.ActivityHelper;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TableListActivity extends ListActivity {
	private static final String TAG = "TableListActivity";
	TableListAdapter listAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tablelist);

		listAdapter = new TableListAdapter(this, R.layout.tablelist_row,
				new ArrayList<TableInfo>());
		setListAdapter(listAdapter);
		fillList();
	}

	private void fillList() {
		try {
			String dbFile = ActivityHelper.getDataBase(this,
					getString(R.string.test_db));
			if (dbFile == null) {
				ActivityHelper.showAlert(this,
						getString(R.string.error_locate_failed));
				throw new IOException(getString(R.string.error_locate_failed));
			}

			jsqlite.Database db = new jsqlite.Database();
			db.open(dbFile.toString(), jsqlite.Constants.SQLITE_OPEN_READONLY);

			Stmt stmt = db
					.prepare("SELECT f_table_name, type, srid FROM geometry_columns;");
			while (stmt.step()) {
				String tableName = stmt.column_string(0);
				String type = stmt.column_string(1);
				String srid = stmt.column_string(2);
				listAdapter.add(new TableInfo(tableName, type, srid));
			}
			db.close();
		} catch (jsqlite.Exception e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private class TableInfo {
		private String tableName;
		private String type;
		private String srid;

		TableInfo(String tableName, String type, String srid) {
			this.tableName = tableName;
			this.type = type;
			this.srid = srid;
		}

		public String getTableName() {
			return tableName;
		}

		public String getType() {
			return type;
		}

		public String getSrid() {
			return srid;
		}
	}

	private class TableListAdapter extends ArrayAdapter<TableInfo> {
		List<TableInfo> objects;

		public TableListAdapter(Context context, int layoutResourceId,
				List<TableInfo> objects) {
			super(context, layoutResourceId, objects);
			this.objects = objects;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = null;

			if (null == convertView) {
				v = getLayoutInflater().inflate(R.layout.tablelist_row, null);
			} else {
				v = convertView;
			}

			TextView name = (TextView) v.findViewById(R.id.table_name);
			name.setText(objects.get(position).getTableName());
			
			TextView type = (TextView) v.findViewById(R.id.type);
			type.setText(objects.get(position).getType());

			return v;
		}
	}
}
