package com.spatialite.test.sqlite;

import com.spatialite.test.utilities.DatabaseTestCase;

import jsqlite.Stmt;

public class DatabaseGeneralTest extends DatabaseTestCase {
	private static final String TAG = "DatabaseGeneralTest";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		openNewDatabase("test.sqlite");
	}

	@Override
	protected void tearDown() throws Exception {
		closeDatabase();
		deleteDatabase();
		super.tearDown();
	}

	public void testSimpleStringBinding01() throws Exception {
		db.exec("CREATE TABLE test (num TEXT, value TEXT);", null);

		Stmt stmt01 = db.prepare("INSERT INTO test (num, value) VALUES (?,?);");
		stmt01.bind(1, "foo");
		stmt01.bind(2, "bar");
		stmt01.step();
		stmt01.close();

		Stmt stmt02 = db.prepare("SELECT * FROM test WHERE num = ?;");
		stmt02.bind(1, "foo");
		if (stmt02.step()) {
			assertEquals(2, stmt02.column_count());
			assertEquals(jsqlite.Constants.SQLITE3_TEXT, stmt02.column_type(0));
			assertEquals(jsqlite.Constants.SQLITE3_TEXT, stmt02.column_type(1));
			assertEquals("foo", stmt02.column_string(0));
			assertEquals("bar", stmt02.column_string(1));
		} else {
			fail("Query failed");
		}
		stmt02.close();

		db.exec("DROP TABLE test;", null);
	}

	public void testSimpleStringBinding02() throws Exception {
		db.exec("CREATE TABLE test (num INTEGER, value REAL);", null);

		Stmt stmt01 = db
				.prepare("INSERT INTO test (num, value) VALUES (@ONE,?);");
		assertEquals(2, stmt01.bind_parameter_count());
		assertEquals("@ONE", stmt01.bind_parameter_name(1));
		assertEquals(1, stmt01.bind_parameter_index("@ONE"));
		stmt01.bind(1, 1);
		stmt01.bind(2, 2.145);
		stmt01.step();
		stmt01.reset();
		stmt01.bind(1, 21);
		stmt01.bind(2, 22.145);
		stmt01.step();
		stmt01.close();

		Stmt stmt02 = db.prepare("SELECT * FROM test WHERE num = ?;");
		stmt02.bind(1, 1);
		if (stmt02.step()) {
			assertEquals(2, stmt02.column_count());
			assertEquals(1, stmt02.bind_parameter_count());
			assertEquals(jsqlite.Constants.SQLITE_INTEGER,
					stmt02.column_type(0));
			assertEquals(jsqlite.Constants.SQLITE_FLOAT, stmt02.column_type(1));
			assertEquals("1", stmt02.column_string(0));
			assertEquals("2.145", stmt02.column_string(1));
			assertEquals(1, stmt02.column_long(0));
			assertEquals(2.145, stmt02.column_double(1));
		} else {
			fail("Query failed");
		}
		stmt02.close();

		Stmt stmt03 = db.prepare("SELECT * FROM test WHERE num = ?;");
		stmt03.bind(1, 21);
		if (stmt03.step()) {
			assertEquals(2, stmt03.column_count());
			assertEquals(1, stmt03.bind_parameter_count());
			assertEquals(jsqlite.Constants.SQLITE_INTEGER,
					stmt03.column_type(0));
			assertEquals(jsqlite.Constants.SQLITE_FLOAT, stmt03.column_type(1));
			assertEquals("21", stmt03.column_string(0));
			assertEquals("22.145", stmt03.column_string(1));
			assertEquals(21, stmt03.column_long(0));
			assertEquals(22.145, stmt03.column_double(1));
		} else {
			fail("Query failed");
		}
		stmt03.close();

		db.exec("DROP TABLE test;", null);
	}

	public void testSimpleStringBinding03() throws Exception {
		db.exec("CREATE TABLE test (num TEXT, value TEXT);", null);

		String args[] = new String[1];
		args[0] = "foo";
		db.exec("INSERT INTO test (num, value) VALUES (%Q,'ss');", null, args);

		Stmt stmt01 = db.prepare("SELECT * from test;");
		if (stmt01.step()) {
			assertEquals("foo", stmt01.column_string(0));
			assertEquals("ss", stmt01.column_string(1));
		} else {
			fail("Query failed");
		}
		stmt01.close();

		db.exec("DROP TABLE test;", null);
	}

	public void testErrorConditions() throws Exception {
		db.exec("CREATE TABLE test (num INTEGER, value REAL);", null);

		Stmt stmt01 = db
				.prepare("INSERT INTO test (num, value) VALUES (@ONE,?);");
		assertEquals(2, stmt01.bind_parameter_count());
		assertEquals("@ONE", stmt01.bind_parameter_name(1));
		assertEquals(1, stmt01.bind_parameter_index("@ONE"));
		stmt01.bind(1, 1);
		stmt01.bind(2, 2.145);

		// Bind to invalid position
		try {
			stmt01.bind(12, 22.145);
			fail("expected exception not thrown");
		} catch (jsqlite.Exception e) {
			// expected
		}

		stmt01.step();

		// Re-bind before reset
		try {
			stmt01.bind(2, 22.145);
			fail("expected exception not thrown");
		} catch (jsqlite.Exception e) {
			// expected
		}

		stmt01.reset();
		stmt01.bind(1, 21);
		stmt01.bind(2, 22.145);
		stmt01.step();
		stmt01.close();

		// Bind to closed statement
		try {
			stmt01.bind(2, 22.145);
			fail("expected exception not thrown");
		} catch (jsqlite.Exception e) {
			// expected
		}

		// Close already closed statement
		try {
			stmt01.close();
			fail("expected exception not thrown");
		} catch (jsqlite.Exception e) {
			// expected
		}

		stmt01 = db.prepare("INSERT INTO test (num, value) VALUES (@ONE,?);");
		stmt01.bind(1, 21);
		stmt01.bind(2, 22.145);
		stmt01.step();
		stmt01.close();

		db.exec("DROP TABLE test;", null);
	}

	public void testErrorConditionsInvalidSql() throws Exception {
		db.exec("CREATE TABLE test (num TEXT, value TEXT);", null);

		// Invalid SQL
		try {
			db.prepare("INSERT INTO test (num, value) VLUES (@ONE,?);");
			fail("expected exception not thrown");
		} catch (jsqlite.Exception e) {
			// expected
		}

		// Invalid SQL - Database.exec(..)
		try {
			db.exec("INSERT INTO test (num, value) VLUES (3,5);", null);
			fail("expected exception not thrown");
		} catch (jsqlite.Exception e) {
			// expected
		}

		db.exec("DROP TABLE test;", null);
	}

	public void testStatementConstraint() throws Exception {
		db.exec("CREATE TABLE test (num INTEGER NOT NULL);", null);
		Stmt st = db.prepare("INSERT INTO test (num) VALUES (?)");

		// Try to insert NULL, which violates the constraint
		try {
			// st.bind(1,"NULL");
			while (st.step()) {
				// do nothing
			}

			fail("expected exception not thrown");
		} catch (jsqlite.Exception e) {
			// expected
		}

		st.reset();
		st.bind(1, 1);
		while (st.step()) {
			// do nothing
		}
		st.close();

		db.exec("DROP TABLE test;", null);
	}
}
