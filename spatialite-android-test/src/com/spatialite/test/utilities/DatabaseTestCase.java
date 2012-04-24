package com.spatialite.test.utilities;

import java.io.File;
import java.io.IOException;

import com.spatialite.utilities.ActivityHelper;
import com.spatialite.utilities.AssetHelper;

import jsqlite.Exception;
import android.test.AndroidTestCase;

public abstract class DatabaseTestCase extends AndroidTestCase {

	// Specifies whether to place the temporary database in the internal (phone)
	// or external (SD card) storage.
	private static final boolean USE_EXTERNAL_STORAGE = true;

	protected jsqlite.Database db = null;
	protected File dbPath = null;

	public void openExistingDatabase(String dbName) throws Exception,
			IOException {
		dbPath = getDatabaseName(dbName);
		if (!dbPath.exists()) {
			AssetHelper.CopyAsset(getContext(), dbPath.getParentFile(), dbName);
		}

		db = new jsqlite.Database();
		db.open(dbPath.toString(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
	}

	public void openNewDatabase(String dbName) throws Exception, IOException {
		dbPath = getDatabaseName(dbName);

		db = new jsqlite.Database();
		db.open(dbPath.toString(), jsqlite.Constants.SQLITE_OPEN_READWRITE
				| jsqlite.Constants.SQLITE_OPEN_CREATE);
	}

	private File getDatabaseName(String dbName) {
		return new File(ActivityHelper.getPath(getContext(),
				USE_EXTERNAL_STORAGE), dbName);
	}

	public void deleteDatabase() {
		if (dbPath != null) {
			dbPath.delete();
			dbPath = null;
		}
	}

	public void closeDatabase() throws Exception {
		if (db == null) {
			return;
		}

		db.close();
		db = null;
	}
}
