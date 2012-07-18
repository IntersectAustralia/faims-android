package com.spatialite.test.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import com.spatialite.utilities.ActivityHelper;

import android.test.AndroidTestCase;

public class JDBCTest extends AndroidTestCase {
	private static final String TAG = "JDBCTest";
	
	Connection conn = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		Class.forName("jsqlite.JDBCDriver").newInstance();
		String db = "jdbc:jsqlite:" + getDatabaseName("test.sqlite").toString();
		conn = DriverManager.getConnection(db);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		conn.close();
		getDatabaseName("test.sqlite").delete();
	}
	
	public void testSimple01() throws Exception {
		Statement stat = conn.createStatement();
        stat.executeUpdate("create table primes (number int);");
        stat.executeUpdate("insert into primes values (2);");
        stat.executeUpdate("insert into primes values (3);");
        stat.executeUpdate("insert into primes values (5);");
        stat.executeUpdate("insert into primes values (7);");

        ResultSet rs = stat.executeQuery("select * from primes order by number asc");
        if(rs.first()){
        	assertEquals(2,rs.getInt(1));
        }
        if(rs.next()){
        	assertEquals(3,rs.getInt(1));
        }
        if(rs.next()){
        	assertEquals(5,rs.getInt(1));
        }
        if(rs.next()){
        	assertEquals(7,rs.getInt(1));
        }
        
        stat.execute("drop table primes;");
	}
	
	private File getDatabaseName(String dbName) {
		return new File(ActivityHelper.getPath(getContext(),
				true), dbName);
	}
}
