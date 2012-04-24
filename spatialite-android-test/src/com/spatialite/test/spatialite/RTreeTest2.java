package com.spatialite.test.spatialite;

import com.spatialite.test.utilities.DatabaseTestCase;

import jsqlite.Callback;
import jsqlite.Stmt;
import android.util.Log;

public class RTreeTest2 extends DatabaseTestCase {
	private static final String TAG = "RTreeTest2";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		openExistingDatabase("test-2.3.sqlite");
	}

	@Override
	protected void tearDown() throws Exception {
		closeDatabase();
		deleteDatabase();
		super.tearDown();
	}

	public void testIndex() throws Exception {
		db.exec("select DisableSpatialIndex('HighWays','Geometry')",
				new Callback() {
					public void types(String[] types) {
					}

					public boolean newrow(String[] rowdata) {
						return false;
					}

					public void columns(String[] coldata) {
					}
				});

		// Create index
		Stmt stmt1 = db
				.prepare("select createspatialindex('HighWays','Geometry')");
		if (stmt1.step()) {
			assertEquals(1, stmt1.column_int(0));
		} else {
			fail("couldn't create spatial index");
		}
		stmt1.close();

		// Check index
		Stmt stmt2 = db.prepare("select CheckSpatialIndex()");
		if (stmt2.step()) {
			assertEquals(1, stmt2.column_int(0));
		} else {
			fail("index check failed");
		}
		stmt2.close();

		// MATCH - prepare query
		Stmt stmt3 = db
				.prepare("SELECT PK_UID FROM HighWays WHERE ROWID IN ( select pkid from idx_HighWays_Geometry where pkid match RTreeDistWithin(675182.242021, 4679818.170099,.000001))");
		if (stmt3.step()) {
			assertEquals(661, stmt3.column_int(0));
		} else {
			fail("query failed");
		}
		stmt3.close();

		// MATCH - exec
		db.exec("SELECT PK_UID FROM HighWays WHERE ROWID IN ( select pkid from idx_HighWays_Geometry where pkid match RTreeDistWithin(675182.242021, 4679818.170099,.000001))",
				new Callback() {
					public void types(String[] types) {
					}

					public boolean newrow(String[] rowdata) {
						assertEquals(661, Integer.parseInt(rowdata[0]));
						return false;
					}

					public void columns(String[] coldata) {
					}
				});

		// using raw index database - prepare
		Stmt stmt4 = db
				.prepare("SELECT PK_UID FROM HighWays WHERE ROWID IN (SELECT pkid FROM idx_HighWays_Geometry WHERE xmin<675182.242022 AND xmax>675182.242020 AND ymin<4679818.170100 AND ymax>4679818.170098)");
		if (stmt4.step()) {
			assertEquals(661, stmt4.column_int(0));
		} else {
			fail("query failed");
		}
		stmt4.close();

		// using raw index database - exec
		String sql = "SELECT pkid FROM idx_HighWays_Geometry WHERE xmin<675182.242022 AND xmax>675182.242020 AND ymin<4679818.170100 AND ymax>4679818.170098";
		Log.i(TAG, sql);
		db.exec(sql, new Callback() {
			public void types(String[] types) {
			}

			public boolean newrow(String[] rowdata) {
				assertEquals(661, Integer.parseInt(rowdata[0]));
				return false;
			}

			public void columns(String[] coldata) {
			}
		});

		// disable index
		Stmt stmt9 = db
				.prepare("select DisableSpatialIndex('HighWays','Geometry')");
		if (stmt9.step()) {
			assertEquals(1, stmt9.column_int(0));
		} else {
			fail("couldn't disable spatial index");
		}
		stmt9.close();

	}
}
