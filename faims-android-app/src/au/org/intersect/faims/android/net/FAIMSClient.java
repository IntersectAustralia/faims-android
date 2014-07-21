package au.org.intersect.faims.android.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.util.Base64;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.FileInfo;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.JsonUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// TODO create unit tests by injecting AndroidHttpClient with mock client
@Singleton
public class FAIMSClient {

	private static final int CONNECTION_TIMEOUT = 3600 * 1000;
	
	private static final int DATA_TIMEOUT = 3600 * 1000;
	
	private static final String USERNAME = "faimsandroidapp";
	
	private static final String TOKEN = "YiQIeV39sdhb2ltRmOyGN";

	private static final int PARAMS_STRING_LIMIT = 1800;

	private static final int UPLOAD_FILE_LIMIT = 5;

	@Inject
	ServerDiscovery serverDiscovery;
	
	AndroidHttpClient httpClient;
	
	private boolean isInterrupted;
	
	private File uploadFileRef;
	
	private TarArchiveOutputStream uploadFileOS;
	private TarArchiveInputStream downloadFileIS;
	
	private File tempFile;
	private File tempDir;
	private File tempArchive;
	
	private void initClient() throws UnknownHostException {
		if (httpClient == null) {
			isInterrupted = false;
			uploadFileRef = null;
			uploadFileOS = null;
			downloadFileIS = null;
			tempFile = null;
			tempDir = null;
			tempArchive = null;
			String userAgent = InetAddress.getLocalHost().toString();
			httpClient = AndroidHttpClient.newInstance(userAgent);
		}
	}
	
	private void cleanupClient() {
		if (httpClient != null) {
			httpClient.close();
			httpClient = null;
		}
	}
	
	public Result uploadDatabase(Module module, File file, String userId) {
		try {
			HashMap<String, ContentBody> extraParts = new HashMap<String, ContentBody>();
			extraParts.put("user", new StringBody(userId));
			return uploadFile(file, "/android/module/" + module.key + "/db_upload", extraParts);
		} catch (Exception e) {
			FLog.e("error uploading database", e);
			return new Result(FAIMSClientResultCode.FAILURE);
		} 
	}
	
	public Result uploadFile(File file, String path) {
		return uploadFile(file, path, null);
	}
	
	// note: this upload can be killed
	public Result uploadFile(File file, String path, HashMap<String, ContentBody> extraParts) {
		synchronized(FAIMSClient.class) {
			FLog.d("uploading file " + file.getName());
			try {
				
				initClient();
				
				MultipartEntity entity = new MultipartEntity();
				entity.addPart("file", new FileBody(file, "binary/octet-stream"));
				entity.addPart("md5", new StringBody(FileUtil.generateMD5Hash(file.getPath())));
				
				if (extraParts != null) {
					for (Entry<String, ContentBody> entry : extraParts.entrySet()) {
						entity.addPart(entry.getKey(), entry.getValue());
					}
				}
				
				HttpPost post = createPostRequest(getUri(path));
				post.setEntity(entity);
				
				HttpResponse response = httpClient.execute(post);
				
				if (isInterrupted) {
					FLog.d("upload interrupted");
					
					return Result.INTERRUPTED;
				}
				
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					FLog.d("upload failed");
					
					return Result.FAILURE;
				}
				
				FLog.d("uploaded file!");
				
				return Result.SUCCESS;
				
			} catch (Exception e) {
				FLog.e("error uploading file", e);
				
				return Result.FAILURE;
				
			} finally {
				cleanupClient();
			}
		}
	}
	
	public FetchResult fetchModuleList() {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			try {			
				initClient();
				
				HttpResponse response = getRequest(getUri("/android/modules"));
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					FLog.d("request failed");
					return FetchResult.FAILURE;
				}
				
				HttpEntity entity = response.getEntity();
				
				if (isInterrupted) {
					FLog.d("fetch modules list interrupted");
					
					return FetchResult.INTERRUPTED;
				}
				
				stream = entity.getContent();
				
				List<Module> ps = JsonUtil.deserializeModules(stream);
				
				ArrayList<Module> modules = new ArrayList<Module>();
				for (Module p : ps) {
					modules.add(p);
				}
				
				FLog.d("fetched modules!");
		        
				return new FetchResult(FAIMSClientResultCode.SUCCESS, null, modules);
			} catch(Exception e) {
				FLog.e("error fetching modules list", e);
				
				return FetchResult.FAILURE;
				
			} finally {
				
				try {
					if (stream != null) stream.close();
				} catch (IOException e) {
					FLog.e("error closing stream", e);
				}
				
				cleanupClient();
			}
		}
	}
	
	public FetchResult fetchDatabaseVersion(Module module) {
		synchronized(FAIMSClient.class) {
	
			InputStream infoStream = null;
			InputStream stream = null;
			try {			
				initClient();
				
				FileInfo info = new FileInfo();
				
				HttpResponse response = getRequest(getUri("/android/module/" + module.key + "/db_archive"));
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
					FLog.d("request busy");
					return new FetchResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.BUSY_ERROR);
				} else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					FLog.d("request failed");
					return null;
				}
				
				HttpEntity entity = response.getEntity();
				
				infoStream = entity.getContent();
				
				JsonObject object = JsonUtil.deserializeJsonObject(infoStream);
				
				info.parseJson(object);
				
				if (isInterrupted) {
					FLog.d("fetch database version interrupted");
					
					return FetchResult.INTERRUPTED;
				}
				
				FLog.d("fetched database version!");
		        
				return new FetchResult(FAIMSClientResultCode.SUCCESS, null, info);
			} catch(Exception e) {
				FLog.e("error fetching database version", e);
				
				return FetchResult.FAILURE;
				
			} finally {
				
				try {
					if (infoStream != null) infoStream.close();
				} catch (IOException e) {
					FLog.e("error closing stream", e);
				}
				
				try {
					if (stream != null) stream.close();
				} catch (IOException e) {
					FLog.e("error closing stream", e);
				}
				
				cleanupClient();
			}
		}
	}
	
	public DownloadResult downloadSettings(Module module) {
		return downloadFile("/android/module/" + module.key + "/settings_archive", 
				"/android/module/" + module.key + "/settings_download", 
				Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + module.key);
	}
	
	public DownloadResult downloadDatabase(Module module) {
		return downloadFile("/android/module/" + module.key + "/db_archive", 
				"/android/module/" + module.key + "/db_download", 
				Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + module.key);
	}
	
	public DownloadResult downloadDatabase(Module module, String version, String dir) {
		return downloadFile("/android/module/" + module.key + "/db_archive?version=" + version, 
				"/android/module/" + module.key + "/db_download?version=" + version, 
				dir);
	}
	
	public DownloadResult downloadFile(String infoPath, String downloadPath, String dir) {
		return downloadFile(infoPath, downloadPath, dir, false);
	}
	
	public DownloadResult downloadFile(String infoPath, String downloadPath, String dir, boolean chooseFileFromInfo) {
		synchronized(FAIMSClient.class) {
			FLog.d("downloading file");
			InputStream infoStream = null;
			InputStream stream = null;
			tempFile = null;
			tempDir = null;
			try {
				initClient();
				
				FileInfo info = new FileInfo();
				
				HttpResponse response = getRequest(getUri(infoPath));
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
					FLog.d("request busy");
					return new DownloadResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.BUSY_ERROR);
				} else if (statusCode != HttpStatus.SC_OK) {
					FLog.d("request failed");
					return DownloadResult.FAILURE;
				}
				
				HttpEntity entity = response.getEntity();
				
				infoStream = entity.getContent();
				
				JsonObject object = JsonUtil.deserializeJsonObject(infoStream);
				
				info.parseJson(object);

				if (isInterrupted) {
					FLog.d("download file interrupted");
					
					return DownloadResult.INTERRUPTED;
				}
				
		        long freeSpace = FileUtil.getExternalStorageSpace();
		        
		        if (info.size > freeSpace) {
		        	FLog.d("storage limit error");
		        	return new DownloadResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.STORAGE_LIMIT_ERROR, info);
		        } 
		        
		        if (chooseFileFromInfo) {
		        	tempFile = downloadArchive(downloadPath + "?file=" + URLEncoder.encode(info.filename, "UTF-8"), info);
		        } else {
		        	tempFile = downloadArchive(downloadPath, info);
		        }
		        
		        if (isInterrupted) {
					FLog.d("download file interrupted");
					
					return DownloadResult.INTERRUPTED;
				}
				
				if (tempFile == null) {
					FLog.d("download corrupted");
					return new DownloadResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.DOWNLOAD_CORRUPTED_ERROR, info);
				}
				
				downloadFileIS = FileUtil.createTarInputStream(tempFile.getAbsolutePath());
				
				// unpack into temp directory
				
				tempDir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + "temp_" + UUID.randomUUID());
				tempDir.mkdirs();
				
				FileUtil.untarFile(tempDir.getAbsolutePath(), downloadFileIS);
				
				if (isInterrupted) {
					FLog.d("download file interrupted");
					
					return DownloadResult.INTERRUPTED;
				}
				
				// move files
				
				FileUtil.moveDir(tempDir.getAbsolutePath(), dir);
				
				FLog.d("downloaded file!");
				
				return new DownloadResult(FAIMSClientResultCode.SUCCESS, null, info);
		        
			} catch (Exception e) {
				FLog.e("error downloading file", e);
				
				return DownloadResult.FAILURE;
				
			} finally {
				
				if (tempFile != null) {
					FileUtil.delete(tempFile);
				}
				
				if (tempDir != null) {
					FileUtil.delete(tempDir);
				}
				
				try {
					if (infoStream != null) infoStream.close();
				} catch (IOException e) {
					FLog.e("error closing stream", e);
				}
				
				try {
					if (stream != null) stream.close();
				} catch (IOException e) {
					FLog.e("error closing stream", e);
				}
				
				try {
					if (downloadFileIS != null) downloadFileIS.close();
				} catch (IOException e) {
					FLog.e("error closing stream", e);
				}
				
				cleanupClient();
			}
		}
	}
	
	private File downloadArchive(String path, FileInfo archive) throws Exception {
		InputStream stream = null;
		tempArchive = null;
		try {
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, DATA_TIMEOUT);
			
			HttpResponse response = getRequest(getUri(path), params);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				FLog.d("request failed");
				return null;
			}
			HttpEntity entity = response.getEntity();
			
			if (isInterrupted) {
				FLog.d("download archive interrupted");
				
				return null;
			}
			
			stream = entity.getContent();
			
	    	FileUtil.makeDirs(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir); // make sure directory exists
			
	    	tempArchive = File.createTempFile("temp_", ".tar.gz", new File(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir));
	    	
			FileUtil.saveFile(stream, tempArchive.getAbsolutePath());
			
			if (isInterrupted) {
				FLog.d("download archive interrupted");
				
				return null;
			}
			
			String md5 = FileUtil.generateMD5Hash(tempArchive.getAbsolutePath());
			
			if (isInterrupted) {
				FLog.d("download archive interrupted");
				
				return null;
			}
			
			if (!archive.md5.equals(md5)) {
				
				FileUtil.delete(tempArchive);
				
				return null;
			}
			
			return tempArchive;
		} finally {
			if (stream != null) stream.close();
			
			if (isInterrupted && tempArchive != null) {
				FileUtil.delete(tempArchive);
			}
		}
	}
	
	private String getUri(String path) throws Exception {
		return serverDiscovery.getServerHost() + path;
	}
	
	private HttpResponse getRequest(String uri) throws IOException {
		return getRequest(uri, null);
	}
	
	private HttpResponse getRequest(String uri, HttpParams params) throws IOException {
		FLog.d(uri);
		
		HttpGet get = createGetRequest(uri);
		
		if (params != null) {
			get.setParams(params);
		}

		HttpResponse response = httpClient.execute(get);
		return response;
	}

	public Result uploadDirectory(String moduleDir, String uploadDir, String requestExcludePath, String uploadPath) {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			try {
				initClient();
				
				String uploadDirPath = moduleDir + "/" + uploadDir;
				if (!new File(uploadDirPath).isDirectory()) {
					FLog.d("no files to upload");
					return Result.SUCCESS;
				}
				
				List<String> localFiles = FileUtil.listDir(uploadDirPath);
				FLog.d("local files:" + localFiles.toString());
				
				if (localFiles.size() == 0) {
					FLog.d("no files to upload");
					return Result.SUCCESS;
				}
			
				HttpResponse response = getRequest(getUri(requestExcludePath));
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
					FLog.d("request busy");
					return new Result(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.BUSY_ERROR);
				} else if (statusCode != HttpStatus.SC_OK) {
					FLog.d("request failed");
					return Result.FAILURE;
				}
				
				HttpEntity entity = response.getEntity();
				if (isInterrupted) {
					FLog.d("upload directory interrupted");
					return Result.INTERRUPTED;
				}
				
				stream = entity.getContent();
				JsonObject object = JsonUtil.deserializeJsonObject(stream);
				
				ArrayList<String> serverFiles = new ArrayList<String>();
				JsonArray filesArray = object.getAsJsonArray("files");
				for (int i = 0; i < filesArray.size(); i++) {
					serverFiles.add(filesArray.get(i).getAsString());
				}
				FLog.d("server Files: " + serverFiles.toString());
				
				// check if new files to upload
				int numOfFilesToUpload = localFiles.size() - countFilesInList(serverFiles, localFiles);
				if (numOfFilesToUpload <= 0) {
					FLog.d("no files to upload");
					return Result.SUCCESS;
				}
				
				uploadFileRef = File.createTempFile("temp_", ".tar.gz", new File(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir));
				uploadFileOS = FileUtil.createTarOutputStream(uploadFileRef.getAbsolutePath());
				FileUtil.tarFile(uploadDirPath, "", uploadFileOS, serverFiles, UPLOAD_FILE_LIMIT);
				
				if (isInterrupted) {
					FLog.d("upload directory interrupted");
					return Result.INTERRUPTED;
				}
					
				Result result = uploadFile(uploadFileRef, uploadPath);
				// check if you have more files to upload
				if (result.resultCode == FAIMSClientResultCode.SUCCESS && numOfFilesToUpload > UPLOAD_FILE_LIMIT) {
					return new Result(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.BUSY_ERROR);
				}
				
				return result;
			} catch (Exception e) {
				FLog.e("error uploading directory", e);
				return Result.FAILURE;
			} finally {
				if (uploadFileRef != null) {
					FileUtil.delete(uploadFileRef);
				}
				
				try {
					if (stream != null) stream.close();
				} catch (IOException ioe) {
					FLog.e("error closing stream", ioe);
				}
				
				try {
					if (uploadFileOS != null) uploadFileOS.close();
				} catch (IOException ioe) {
					FLog.e("error closing stream", ioe);
				}
				
				cleanupClient();
			}
		}
	}

	public DownloadResult downloadDirectory(String moduleDir, String downloadDir, String requestExcludePath, String infoPath, String downloadPath) {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			try {
				initClient();
				
				HttpResponse response = getRequest(getUri(requestExcludePath));
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
					FLog.d("request busy");
					return new DownloadResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.BUSY_ERROR);
				} else if (statusCode != HttpStatus.SC_OK) {
					FLog.d("request failed");
					return DownloadResult.FAILURE;
				}
				
				HttpEntity entity = response.getEntity();
				if (isInterrupted) {
					FLog.d("download directory interrupted");
					return DownloadResult.INTERRUPTED;
				}
				
				stream = entity.getContent();
				JsonObject object = JsonUtil.deserializeJsonObject(stream);
				
				ArrayList<String> serverFiles = new ArrayList<String>();
				JsonArray filesArray = object.getAsJsonArray("files");
				for (int i = 0; i < filesArray.size(); i++) {
					serverFiles.add(filesArray.get(i).getAsString());
				}
				FLog.d("server Files: " + serverFiles.toString());
				
				if (serverFiles.size() == 0) {
					FLog.d("no files to download");
					return DownloadResult.SUCCESS;
				}
				
				String downloadDirPath = moduleDir + "/" + downloadDir;
				
				// make sure dir exists
				FileUtil.makeDirs(downloadDirPath);
				List<String> localFiles = FileUtil.listDir(downloadDirPath);
				FLog.d("local Files: " + localFiles.toString());
				
				// check if new files to download
				int numOfFilesToDownload = serverFiles.size() - countFilesInList(localFiles, serverFiles);
				if (numOfFilesToDownload <= 0) {
					FLog.d("no files to download");
					return DownloadResult.SUCCESS;
				}
				
				// construct info path with exclude files
				StringBuilder sb = new StringBuilder(infoPath + "?");
				for (String lf : localFiles) {
					sb.append("files[]=");
					sb.append(lf);
					sb.append("&");
				}
				
				String excludeFileParams = sb.toString();
				if (excludeFileParams.length() > PARAMS_STRING_LIMIT) {
					FLog.d("url string to big to download");
					return DownloadResult.SUCCESS;
				}
				
				return downloadFile(excludeFileParams, downloadPath, downloadDirPath, true);
			} catch (Exception e) {
				FLog.e("error downloading directory", e);

				return DownloadResult.FAILURE;
			} finally {
				try {
					if (stream != null) stream.close();
				} catch (IOException ioe) {
					FLog.e("error closing stream", ioe);
				}
				
				cleanupClient();
			}
		}
	}
	
	private int countFilesInList(List<String> subFiles, List<String> files) {
		int count = 0;
		for (String sf : subFiles) {
			for (String f : files) {
				if (sf.equals(f)) {
					count++;
					break;
				}
			}
		}
		return count;
	}
	

	public void invalidate() {
		serverDiscovery.invalidateServerHost();
	}

	public void interrupt() {
		// note: this method is non synchronized because we want to terminate currently running operations
		if (httpClient != null) {
			isInterrupted = true;
			if (tempArchive != null) {
				FileUtil.delete(tempArchive);
			}
			if (tempFile != null) {
				FileUtil.delete(tempFile);
			}
			if (tempDir != null) {
				FileUtil.delete(tempDir);
			}
			if (uploadFileRef != null) {
				FileUtil.delete(uploadFileRef);
			}
			try {
				if (uploadFileOS != null) uploadFileOS.close();
			} catch (Exception e) {
				FLog.e("error closing stream", e);
			}
			try {
				if (downloadFileIS != null) downloadFileIS.close();
			} catch (Exception e) {
				FLog.e("error closing stream", e);
			}
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	private String getCredentials() {
		return "Basic " + Base64.encodeToString((USERNAME+":"+TOKEN).getBytes(), Base64.NO_WRAP);
	}
	
	private HttpPost createPostRequest(String uri) {
		HttpPost post = new HttpPost(uri);
		post.setHeader("Authorization", getCredentials());
		return post;
	}
	
	private HttpGet createGetRequest(String uri) {
		HttpGet get = new HttpGet(uri);
		get.setHeader("Authorization", getCredentials());
		return get;
	}
	
}
