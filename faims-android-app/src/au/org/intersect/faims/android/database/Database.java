package au.org.intersect.faims.android.database;

import java.io.File;
import java.util.Arrays;

import jsqlite.Callback;
import jsqlite.Stmt;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.log.FLog;

import com.google.inject.Inject;

public class Database {
	
	@Inject
	DatabaseManager databaseManager;
	
	protected File dbFile;
	protected jsqlite.Database db;
	
	public Database(File dbFile) {
		FAIMSApplication.getInstance().injectMembers(this);
		this.dbFile = dbFile;
	}
	
	protected void notifyListeners() {
		databaseManager.notifyListeners();
	}
	
	public void debugVersion() {
		jsqlite.Database db = null;
		try {
			db = openDB();
			
			db.exec("select spatialite_version(), sqlite_version(), proj4_version(), geos_version(), lwgeom_version();", new Callback() {
				@Override
				public void columns(String[] coldata) {
					FLog.d("Columns: " + Arrays.toString(coldata));
				}

				@Override
				public void types(String[] types) {
					FLog.d("Types: " + Arrays.toString(types));
				}

				@Override
				public boolean newrow(String[] rowdata) {
					FLog.d("Row: " + Arrays.toString(rowdata));

					return false;
				}
			});
			
		} catch (Exception e) {
			FLog.e("error dumping database", e);
		} finally {
			closeDB();
		}
	}

	public static void debugDump(File file) {
		jsqlite.Database db = null;
		try {
			db = new jsqlite.Database();
			db.open(file.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
			db.exec("PRAGMA temp_store = 2", null);
			
			db.exec("select * from archentity;", new Callback() {
				@Override
				public void columns(String[] coldata) {
					FLog.d("Columns: " + Arrays.toString(coldata));
				}

				@Override
				public void types(String[] types) {
					FLog.d("Types: " + Arrays.toString(types));
				}

				@Override
				public boolean newrow(String[] rowdata) {
					FLog.d("Row: " + Arrays.toString(rowdata));

					return false;
				}
			});
			
		} catch (Exception e) {
			FLog.e("error dumping database", e);
		} finally {
			try {
				if (db != null) {
					db.close();
					db = null;
				}
			} catch (Exception e) {
				FLog.e("error closing database", e);
			}
		}
	}
	
	protected jsqlite.Database openDB() throws jsqlite.Exception {
		return openDB(jsqlite.Constants.SQLITE_OPEN_READONLY);
	}
	
	protected jsqlite.Database openDB(int type) throws jsqlite.Exception {
		db = new jsqlite.Database();
		db.open(dbFile.getPath(), type);
		db.exec("PRAGMA temp_store = 2", null);
		return db;
	}
	
	protected void closeDB() {
		try {
			if (db != null) {
				db.close();
				db = null;
			}
		} catch (Exception e) {
			FLog.e("error closing database", e);
		}
	}
	
	protected void closeStmt(Stmt stmt) {
		try {
			if (stmt != null) stmt.close();
		} catch(Exception e) {
			FLog.e("error closing statement", e);
		}
	}
	
	protected void beginTransaction() throws jsqlite.Exception {
		if (db == null) {
			FLog.e("Cannot begin transaction");
			return;
		}
		db.exec("BEGIN", createCallback());
	}
	
	protected void commitTransaction() throws jsqlite.Exception {
		if (db == null) {
			FLog.e("Cannot commit transaction");
			return;
		}
		db.exec("COMMIT", createCallback());
	}
	
	public void interrupt() {
		closeDB();
	}
	
	protected Callback createCallback() {
		return new Callback() {
			@Override
			public void columns(String[] coldata) {
				FLog.d("Columns: " + Arrays.toString(coldata));
			}

			@Override
			public void types(String[] types) {
				FLog.d("Types: " + Arrays.toString(types));
			}

			@Override
			public boolean newrow(String[] rowdata) {
				FLog.d("Row: " + Arrays.toString(rowdata));

				return false;
			}
		};
	}

	protected String clean(String s) {
		if (s != null && "".equals(s.trim())) return null;
		return s;
	}

}
