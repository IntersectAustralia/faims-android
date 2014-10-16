package au.org.intersect.faims.android.database;

import java.io.File;
import java.util.ArrayList;

import jsqlite.Stmt;
import au.org.intersect.faims.android.data.FileInfo;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;

public class FileRecord extends Database {
	
	public static final String ATTACHED = "attached";
	public static final String UPLOADED = "uploaded";
	public static final String DOWNLOADED = "downloaded";
	
	public static final String APP = "app";
	public static final String SERVER = "server";

	public FileRecord(File dbFile) {
		super(dbFile);
	}
	
	public ArrayList<FileInfo> getFilesToUpload(String type) throws Exception {
		ArrayList<FileInfo> files = new ArrayList<FileInfo>();
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction(db);
			st = db.prepare(DatabaseQueries.GET_UPLOAD_FILES);
			st.bind(1, type);
			while (st.step()) {
				FileInfo info = new FileInfo();
				info.filename = st.column_string(0);
				info.md5 = st.column_string(1);
				info.size = st.column_int(2);
				info.type = st.column_string(3);
				info.state = st.column_string(4);
				info.timestamp = st.column_string(5);
				info.deleted = "1".equals(st.column_string(6));
				info.thumbnailFilename = st.column_string(7);
				info.thumbnailMD5 = st.column_string(8);
				info.thumbnailSize = st.column_int(9);
				files.add(info);
			}
			st.close();
			st = null;
			commitTransaction(db);
		} finally {
			closeStmt(st);
			closeDB(db);
		}
		return files;
	}
	
	public ArrayList<FileInfo> getFilesToDownload(String type) throws Exception {
		ArrayList<FileInfo> files = new ArrayList<FileInfo>();
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction(db);
			st = db.prepare(DatabaseQueries.GET_DOWNLOAD_FILES);
			st.bind(1, type);
			while (st.step()) {
				FileInfo info = new FileInfo();
				info.filename = st.column_string(0);
				info.md5 = st.column_string(1);
				info.size = st.column_int(2);
				info.type = st.column_string(3);
				info.state = st.column_string(4);
				info.timestamp = st.column_string(5);
				info.deleted = "1".equals(st.column_string(6));
				info.thumbnailFilename = st.column_string(7);
				info.thumbnailMD5 = st.column_string(8);
				info.thumbnailSize = st.column_int(9);
				files.add(info);
			}
			st.close();
			st = null;
			commitTransaction(db);
		} finally {
			closeStmt(st);
			closeDB(db);
		}
		return files;
	}
	
	public void updateFile(String filename, String state) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction(db);
			st = db.prepare(DatabaseQueries.UPDATE_FILE);
			st.bind(1, state);
			st.bind(2, filename);
			st.step();
			st.close();
			st = null;
			commitTransaction(db);
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}

	public void insertFile(String filename, boolean sync, File file) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction(db);
			st = db.prepare(DatabaseQueries.INSERT_FILE);
			st.bind(1, filename);
			st.bind(2, FileUtil.generateMD5Hash(file));
			st.bind(3, 0);
			st.bind(4, sync ? APP : SERVER);
			st.bind(5, ATTACHED);
			st.bind(6, DateUtil.getCurrentTimestampGMT());
			st.bind(7);
			st.bind(8);
			st.bind(9);
			st.bind(10);
			st.step();
			st.close();
			st = null;
			commitTransaction(db);
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}
	
	public boolean hasFileChanges() throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction(db);
			st = db.prepare(DatabaseQueries.HAS_FILE_CHANGES);
			boolean result = false;
			if (st.step()) {
				result = st.column_int(0) > 0;
			}
			st.close();
			st = null;
			commitTransaction(db);
			return result;
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}
	
	public ArrayList<FileInfo> getSyncedFiles(boolean withThumbnail) throws Exception {
		ArrayList<FileInfo> files = new ArrayList<FileInfo>();
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction(db);
			st = db.prepare(withThumbnail ? DatabaseQueries.GET_ALL_SYNCED_FILES_WITH_THUMBNAILS : DatabaseQueries.GET_ALL_SYNCED_FILES);
			while (st.step()) {
				FileInfo info = new FileInfo();
				info.filename = st.column_string(0);
				info.md5 = st.column_string(1);
				info.size = st.column_int(2);
				info.type = st.column_string(3);
				info.state = st.column_string(4);
				info.timestamp = st.column_string(5);
				info.deleted = "1".equals(st.column_string(6));
				info.thumbnailFilename = st.column_string(7);
				info.thumbnailMD5 = st.column_string(8);
				info.thumbnailSize = st.column_int(9);
				files.add(info);
			}
			st.close();
			st = null;
			commitTransaction(db);
		} finally {
			closeStmt(st);
			closeDB(db);
		}
		return files;
	}
	
}