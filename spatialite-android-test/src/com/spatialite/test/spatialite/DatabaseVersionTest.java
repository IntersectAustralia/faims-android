package com.spatialite.test.spatialite;

import jsqlite.Stmt;
import android.test.AndroidTestCase;

public class DatabaseVersionTest extends AndroidTestCase {
	jsqlite.Database db = null;

	// Place version information here so that it can be changed easily when the
	// libraries change.
	private static final String SPATIALITE_VERSION = "3.0.1";
	private static final String PROJ4_VERSION = "Rel. 4.7.1, 23 September 2009";
	private static final String GEOS_VERSION = "3.2.2-CAPI-1.6.2";

	public void testVersions() throws Exception {
		Stmt stmt01 = db.prepare("SELECT spatialite_version();");
		if (stmt01.step()) {
			assertEquals(SPATIALITE_VERSION, stmt01.column_string(0));
		}

		stmt01 = db.prepare("SELECT proj4_version();");
		if (stmt01.step()) {
			assertEquals(PROJ4_VERSION, stmt01.column_string(0));
		}

		stmt01 = db.prepare("SELECT geos_version();");
		if (stmt01.step()) {
			assertEquals(GEOS_VERSION, stmt01.column_string(0));
		}
		stmt01.close();
	}
}
