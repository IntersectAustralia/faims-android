package au.org.intersect.faims.android.services;

import java.io.File;
import java.util.UUID;

import roboguice.RoboGuice;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.DownloadResult;
import au.org.intersect.faims.android.data.FetchResult;
import au.org.intersect.faims.android.data.FileInfo;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.managers.DatabaseManager;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ProjectUtil;

import com.google.inject.Inject;

public class SyncDownloadDatabaseService extends DownloadDatabaseService {

	@Inject
	DatabaseManager databaseManager;
	
	public SyncDownloadDatabaseService() {
		super("SyncDownloadDatabaseService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		RoboGuice.getBaseApplicationInjector(this.getApplication()).injectMembers(this);
	}
	
	@Override
	protected FAIMSClientResultCode doDownload(Intent intent) {
		File tempDir = null;
		try {
			Project project = (Project) intent.getExtras().get("project");
			Log.d("FAIMS", "downloading database for " + project.name);
			
			FileInfo info;
			int serverVersion;
			int projectVersion;
			
			// check if there is a new version to download
			FetchResult fetchResult = faimsClient.fetchDatabaseVersion(project);
			if (fetchResult.code == FAIMSClientResultCode.SUCCESS) {
				info = (FileInfo) fetchResult.data;
				serverVersion = Integer.parseInt(info.version == null ? "0" : info.version);
				projectVersion = Integer.parseInt(project.version == null ? "0" : project.version);
				if (serverVersion == projectVersion) {
					Log.d("FAIMS", "database already latest");
					return fetchResult.code;
				}
			} else {
				Log.d("FAIMS", "failed to get database version");
				return fetchResult.code;
			}
			
			int syncVersion = projectVersion + 1;
			
			String dir = "/faims/projects/" + project.key + "/" + UUID.randomUUID();
			
			// download database from version
			tempDir = new File(Environment.getExternalStorageDirectory() + dir);
			tempDir.mkdirs();
			
			DownloadResult downloadResult = faimsClient.downloadDatabase(project, String.valueOf(syncVersion), dir);
			
			if (downloadResult.code == FAIMSClientResultCode.SUCCESS) {
				Log.d("FAIMS", "merging database to version " + serverVersion);
				
				// merge database 
				databaseManager.mergeDatabaseFrom(new File(tempDir.getAbsoluteFile() + "/db.sqlite3"));
				
				// update settings
				project = ProjectUtil.getProject(project.key); // get the latest settings
				project.version = downloadResult.info.version;
				ProjectUtil.saveProject(project);
			}
			
			return downloadResult.code;
		} catch (Exception e) {
			Log.e("FAIMS", "could not download database", e);
		} finally {
			if (tempDir != null) {
				FileUtil.deleteDirectory(tempDir);
			}
		}
		return null;
	}
}
