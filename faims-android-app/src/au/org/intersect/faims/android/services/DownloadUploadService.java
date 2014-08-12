package au.org.intersect.faims.android.services;

import java.io.File;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.apache.http.entity.mime.content.ContentBody;

import roboguice.RoboGuice;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.util.FileUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

public abstract class DownloadUploadService extends IntentService {

	@Inject
	FAIMSClient faimsClient;

	protected boolean serviceInterrupted;
	protected Module serviceModule;
	protected Result serviceResult;

	public DownloadUploadService(String name) {
		super(name);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		RoboGuice.getBaseApplicationInjector(this.getApplication()).injectMembers(this);
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
	}

	protected abstract void performService() throws Exception;

	// Download Helpers

	protected boolean downloadFiles(String name, String infoUri,
			String downloadUri, File downloadDirectory) throws Exception {
		return downloadFiles(name, infoUri, downloadUri, downloadDirectory, true);
	}

	protected boolean downloadFiles(String name, String infoUri,
			String downloadUri, File downloadDirectory, boolean overwrite)
			throws Exception {
		FLog.d("downloading files for " + serviceModule.name);

		Result infoResult = faimsClient.fetchRequestObject(infoUri);
		if (infoResult.resultCode != FAIMSClientResultCode.SUCCESS) {
			serviceResult = infoResult;
			return false;
		}

		JsonObject jsonInfo = (JsonObject) infoResult.data;
		JsonArray jsonFiles = jsonInfo.getAsJsonArray("files");
		for (int i = 0; i < jsonFiles.size(); i++) {
			JsonObject fileInfo = jsonFiles.get(i).getAsJsonObject();

			File downloadFile = new File(downloadDirectory.getPath() + '/'
					+ fileInfo.get("file").getAsString());

			// check if file exists and is overwritable
			if (!overwrite && downloadFile.exists()) {
				continue;
			}

			// check if there is enough space to download file
			long size = fileInfo.get("size").getAsLong();
			if (FileUtil.getExternalStorageSpace() < size) {
				FLog.d("download file failed because there is not enough space");
				serviceResult = new Result(FAIMSClientResultCode.FAILURE,
						FAIMSClientErrorCode.STORAGE_LIMIT_ERROR);
				return false;
			}

			// download file
			File parentDirectory = downloadFile.getParentFile();
			if (parentDirectory != null) {
				FileUtil.makeDirs(parentDirectory.getPath());
			}
			Result downloadResult = faimsClient.downloadFile(
					downloadUri
							+ "&request_file="
							+ URLEncoder.encode(fileInfo.get("file").getAsString(),
									"UTF-8"), downloadFile);
			if (downloadResult.resultCode != FAIMSClientResultCode.SUCCESS) {
				serviceResult = downloadResult;
				return false;
			}

			// check if md5 hash matches
			String md5checksum = fileInfo.get("md5").getAsString();
			if (!FileUtil.generateMD5Hash(downloadFile.getPath()).equals(md5checksum)) {
				FLog.d("downloaded file failed because file is corrupted");
				serviceResult = new Result(FAIMSClientResultCode.FAILURE,
						FAIMSClientErrorCode.DOWNLOAD_CORRUPTED_ERROR);
				return false;
			}

			FLog.d("downloaded file: " + downloadFile);
		}

		serviceResult = Result.SUCCESS;
		serviceResult.data = infoResult.data;
		return true;
	}

	protected boolean uploadFiles(String name, String uploadUri,
			List<File> uploadFiles, File baseDirectory) throws Exception {
		return uploadFiles(name, uploadUri, uploadFiles, baseDirectory, null, null);
	}

	protected boolean uploadFiles(String name, String uploadUri,
			List<File> uploadFiles, File baseDirectory, HashMap<String, ContentBody> extraParts)
			throws Exception {
		return uploadFiles(name, uploadUri, uploadFiles, baseDirectory, extraParts, null);
	}

	protected boolean uploadFiles(String name, String uploadUri,
			List<File> uploadFiles, File baseDirectory, HashMap<String, ContentBody> extraParts,
			String excludeFileFromRequestUri) throws Exception {
		FLog.d("uploading files for " + serviceModule.name);

		// get files to exclude
		JsonArray jsonFiles = null;
		if (excludeFileFromRequestUri != null) {
			Result infoResult = faimsClient
					.fetchRequestObject(excludeFileFromRequestUri);
			if (infoResult.resultCode != FAIMSClientResultCode.SUCCESS) {
				serviceResult = infoResult;
				return false;
			}

			JsonObject jsonInfo = (JsonObject) infoResult.data;
			jsonFiles = jsonInfo.getAsJsonArray("files");
		}

		for (int i = 0; i < uploadFiles.size(); i++) {
			File uploadFile = uploadFiles.get(i);

			// check if file is excluded
			if (excludeFile(baseDirectory, uploadFile, jsonFiles)) {
				continue;
			}

			// get path relative to home directory
			Result uploadResult = faimsClient.uploadFile(uploadUri, uploadFile,
					pathFromBaseDirectory(baseDirectory, uploadFile), extraParts);
			if (uploadResult.resultCode != FAIMSClientResultCode.SUCCESS) {
				serviceResult = uploadResult;
				return false;
			}

			FLog.d("uploaded file: " + uploadFile);
		}

		serviceResult = Result.SUCCESS;
		return true;
	}

	private boolean excludeFile(File baseDirectory, File uploadFile, JsonArray jsonFiles)
			throws Exception {
		if (jsonFiles != null) {
			for (int i = 0; i < jsonFiles.size(); i++) {
				String uploadPath = pathFromBaseDirectory(baseDirectory, uploadFile).getPath();
				String downloadPath = jsonFiles.get(i).getAsJsonObject().get("file").getAsString();
				if (uploadPath.equals(downloadPath)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private File pathFromBaseDirectory(File baseDirectory, File file) {
		String fullpath = file.getAbsolutePath();
		String basepath = baseDirectory.getAbsolutePath() + '/';
		return new File(fullpath.replace(basepath, ""));
	}

}
