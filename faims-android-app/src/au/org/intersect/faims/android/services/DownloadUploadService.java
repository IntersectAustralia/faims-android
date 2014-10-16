package au.org.intersect.faims.android.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.entity.mime.content.ContentBody;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FileInfo;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.database.FileRecord;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.JsonUtil;

import com.google.inject.Inject;

public abstract class DownloadUploadService extends IntentService {

	@Inject
	FAIMSClient faimsClient;
	
	@Inject
	DatabaseManager databaseManager;

	protected boolean serviceInterrupted;
	protected Module serviceModule;
	protected boolean overwrite;
	protected Result serviceResult;

	public DownloadUploadService(String name) {
		super(name);

		FAIMSApplication.getInstance().injectMembers(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		faimsClient.interrupt();
		serviceInterrupted = true;

		FLog.d("stopping download upload service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		FLog.d("starting download upload service");

		try {
			initService(intent);
			performService();
		} catch (Exception e) {
			FLog.e("error in download upload service", e);
			serviceResult = Result.FAILURE;
		} finally {
			try {
				Bundle extras = intent.getExtras();
				Messenger messenger = (Messenger) extras.get("MESSENGER");
				Message msg = Message.obtain();
				msg.obj = serviceResult;
				messenger.send(msg);
			} catch (Exception me) {
				FLog.e("error sending message", me);
			}
		}
	}

	protected void initService(Intent intent) {
		serviceModule = (Module) intent.getExtras().get("module");
		overwrite = intent.getExtras().getBoolean("overwrite");
	}

	protected abstract void performService() throws Exception;

	// Download Helpers
	protected boolean downloadFiles(String name, String infoUri,
			String downloadUri, File downloadDirectory)
			throws Exception {
		FLog.d("downloading files for " + serviceModule.name);

		Result infoResult = faimsClient.fetchRequestObject(infoUri);
		if (infoResult.resultCode != FAIMSClientResultCode.SUCCESS) {
			serviceResult = infoResult;
			return false;
		}

		JSONObject jsonInfo = (JSONObject) infoResult.data;
		JSONArray jsonFiles = jsonInfo.getJSONArray("files");
		for (int i = 0; i < jsonFiles.length(); i++) {
			JSONObject fileInfo = jsonFiles.getJSONObject(i);

			File file = new File(downloadDirectory.getPath() + '/'
					+ fileInfo.getString("file"));

			if (!downloadFile(name, downloadUri, file, FileInfo.fromJson(fileInfo), overwrite)) {
				return false;
			}
		}

		serviceResult = Result.SUCCESS;
		serviceResult.data = infoResult.data;
		return true;
	}
	
	protected boolean downloadFile(String name, String downloadUri, File downloadFile, FileInfo fileInfo, boolean overwrite) throws Exception {
		// check if file exists and is overwritable
		if (overwrite || !downloadFile.exists()) {
			// check if there is enough space to download file
			long size = fileInfo.size;
			if (FileUtil.getExternalStorageSpace() < size) {
				FLog.d("download file failed because there is not enough space");
				serviceResult = new Result(FAIMSClientResultCode.FAILURE,
						FAIMSClientErrorCode.STORAGE_LIMIT_ERROR);
				return false;
			}
	
			// download file
			File parentDirectory = downloadFile.getParentFile();
			if (parentDirectory != null) {
				FileUtil.makeDirs(parentDirectory);
			}
			
			Result downloadResult = faimsClient.downloadFile(
					downloadUri
							+ "&request_file="
							+ URLEncoder.encode(fileInfo.filename,
									"UTF-8"), downloadFile);
			if (downloadResult.resultCode != FAIMSClientResultCode.SUCCESS) {
				FileUtil.delete(downloadFile);
				serviceResult = downloadResult;
				return false;
			}
	
			// check if md5 hash matches
			String md5checksum = fileInfo.md5;
			if (!FileUtil.generateMD5Hash(downloadFile).equals(md5checksum)) {
				FileUtil.delete(downloadFile);
				
				if (serviceInterrupted) {
					FLog.d("download interrupted");
					serviceResult = new Result(FAIMSClientResultCode.INTERRUPTED);
					return false;
				}
				
				FLog.d("downloaded file failed because file is corrupted");
				serviceResult = new Result(FAIMSClientResultCode.FAILURE,
						FAIMSClientErrorCode.DOWNLOAD_CORRUPTED_ERROR);
				return false;
			}

			FLog.d("downloaded file: " + downloadFile);
		}
		
		serviceResult = Result.SUCCESS;
		return true;
	}

	protected boolean uploadFile(String name, String uploadUri,
			File uploadFile, File baseDirectory, HashMap<String, ContentBody> extraParts)
			throws Exception {
		FLog.d("uploading file for " + serviceModule.name);

		// get path relative to home directory
		Result uploadResult = faimsClient.uploadFile(uploadUri, uploadFile,
				pathFromBaseDirectory(baseDirectory, uploadFile), extraParts);
		if (uploadResult.resultCode != FAIMSClientResultCode.SUCCESS) {
			serviceResult = uploadResult;
			return false;
		}

		FLog.d("uploaded file: " + uploadFile);

		serviceResult = Result.SUCCESS;
		return true;
	}
	
	protected boolean uploadSyncFiles(String name, String uploadUri, File baseDirectory) throws Exception {
		ArrayList<FileInfo> files = databaseManager.fileRecord().getFilesToUpload(name);
		for (FileInfo info : files) {
			if (uploadFile(name, uploadUri, serviceModule.getDirectoryPath(info.filename), baseDirectory, null)) {
				databaseManager.fileRecord().updateFile(info.filename, FileRecord.UPLOADED);
			} else {
				return false;
			}
		}
		serviceResult = Result.SUCCESS;
		return true;
	}
	
	protected boolean downloadSyncFiles(String name, String downloadUri, File baseDirectory) throws Exception {
		ArrayList<FileInfo> files = databaseManager.fileRecord().getFilesToDownload(name);
		for (FileInfo info : files) {
			File file;
			String originalFilename = info.filename;
			if (info.thumbnailFilename == null) {
				file = serviceModule.getDirectoryPath(info.filename);
				info.filename = pathFromBaseDirectory(baseDirectory, file).getPath(); // filename needs to be relative to base directory when requesting file from webserver
			} else {
				file = serviceModule.getDirectoryPath(info.thumbnailFilename);
				info.filename = pathFromBaseDirectory(baseDirectory, file).getPath(); // filename needs to be relative to base directory when requesting file from webserver
				info.size = info.thumbnailSize;
				info.md5 = info.thumbnailMD5;
			}
			if (downloadFile(name, downloadUri, file, info, false)) {
				databaseManager.fileRecord().updateFile(originalFilename, FileRecord.DOWNLOADED);
			} else {
				return false;
			}
		}
		serviceResult = Result.SUCCESS;
		return true;
	}
	
	protected void addHostToModuleSettings() {
		File file = serviceModule.getDirectoryPath("module.settings");
		if (file.exists()) {
			InputStream is = null;
			FileWriter writer = null;
			try {
				is = new FileInputStream(file);
				String config = FileUtil.convertStreamToString(is);
				JSONObject moduleSettings = JsonUtil.deserializeJsonString(config);
				moduleSettings.put("host", faimsClient.getPlainHost());
				writer = new FileWriter(file);
				writer.write(moduleSettings.toString());
				writer.close();
			} catch (Exception e) {
				FLog.w("cannot add host to modules settings ", e);
				
				try {
					if (is != null)
						is.close();
					if (writer != null)
						writer.close();
				} catch (IOException ioe) {
					FLog.e("error closing file streams", ioe);
				}
			}
		} else {
			FLog.w("Settings file doesn't exist to add host to");
		}
	}
	
	private File pathFromBaseDirectory(File baseDirectory, File file) {
		String fullpath = file.getAbsolutePath();
		String basepath = baseDirectory.getAbsolutePath() + '/';
		return new File(fullpath.replace(basepath, ""));
	}

}
