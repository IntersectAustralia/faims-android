package au.org.intersect.faims.android.database;

import java.io.File;
import java.util.Arrays;

import jsqlite.Callback;
import jsqlite.Function;
import jsqlite.FunctionContext;
import jsqlite.Stmt;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.formatter.StringFormatter;
import au.org.intersect.faims.android.log.FLog;

import com.google.inject.Inject;

public class Database {
	
	@Inject
	DatabaseManager databaseManager;
	
	protected File dbFile;
	
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
			closeDB(db);
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
		return openDB(dbFile, type);
	}
	
	protected jsqlite.Database openDB(File file, int type) throws jsqlite.Exception {
		jsqlite.Database db = new jsqlite.Database();
		db.open(file.getPath(), type);
		db.exec("PRAGMA temp_store = 2", null);
		loadFunctions(db);
		return db;
	}
	
	protected void closeDB(jsqlite.Database db) {
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
	
	protected void beginTransaction(jsqlite.Database db) throws jsqlite.Exception {
		if (db == null) {
			FLog.e("Cannot begin transaction");
			return;
		}
		db.exec("BEGIN", createCallback());
	}
	
	protected void commitTransaction(jsqlite.Database db) throws jsqlite.Exception {
		if (db == null) {
			FLog.e("Cannot commit transaction");
			return;
		}
		db.exec("COMMIT", createCallback());
	}
	
	public void interrupt(jsqlite.Database db) {
		closeDB(db);
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
	
	public void loadFunctions(jsqlite.Database db) throws jsqlite.Exception {
		db.create_function("format",  -1, new Function() {

			@Override
			public void function(FunctionContext fc, String[] args) {
				try {
					if (args == null || args.length < 1) {
						fc.set_result((String) null);
					} else {
						if (args[0] == null) {
							String value = "";
							for (int i = 1 ; i < args.length; i++) {
								if (!"".equals(value)) {
									value += ", ";
								}
								value += args[i];
							}
							fc.set_result(value);
						} else {
							String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
							fc.set_result(new StringFormatter(args[0]).preCompute().evaluate(newArgs));
						}
					}
				} catch (Exception e) {
					FLog.e("Error trying to run formatter with arguments " + (args == null ? "" : args.toString()), e);
					fc.set_error("Error trying to run formatter with arguments " + (args == null ? "" : args.toString()));
				}
			}

			@Override
			public void step(FunctionContext fc, String[] args) {
			}

			@Override
			public void last_step(FunctionContext fc) {	
			}
			
		});
	}

}
