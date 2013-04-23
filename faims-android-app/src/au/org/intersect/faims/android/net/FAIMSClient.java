package au.org.intersect.faims.android.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

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
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.FileInfo;
import au.org.intersect.faims.android.data.Project;
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

	private static final int CONNECTION_TIMEOUT = 60 * 1000;
	
	private static final int DATA_TIMEOUT = 60 * 1000;

	@Inject
	ServerDiscovery serverDiscovery;
	
	AndroidHttpClient httpClient;
	
	private boolean isInterrupted;
	
	private void initClient() throws UnknownHostException {
		if (httpClient == null) {
			isInterrupted = false;
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
	
	public Result uploadDatabase(Project project, File file, String userId) {
		try {
			HashMap<String, ContentBody> extraParts = new HashMap<String, ContentBody>();
			extraParts.put("user", new StringBody(userId));
			return uploadFile(file, "/android/project/" + project.key + "/upload_db", extraParts);
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
				
				HttpPost post = new HttpPost(new URI(getUri(path)));
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
	
	public FetchResult fetchProjectList() {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			try {			
				initClient();
				
				HttpEntity entity = getRequest(getUri("/android/projects"));
				
				if (isInterrupted) {
					FLog.d("fetch projects list interrupted");
					
					return FetchResult.INTERRUPTED;
				}
				
				stream = entity.getContent();
				
				List<Project> ps = JsonUtil.deserializeProjects(stream);
				
				ArrayList<Project> projects = new ArrayList<Project>();
				for (Project p : ps) {
					projects.add(p);
				}
				
				FLog.d("fetched projects!");
		        
				return new FetchResult(FAIMSClientResultCode.SUCCESS, null, projects);
			} catch(Exception e) {
				FLog.e("error fetching projects list", e);
				
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
	
	public FetchResult fetchDatabaseVersion(Project project) {
		synchronized(FAIMSClient.class) {
	
			InputStream stream = null;
			try {			
				initClient();
				
				FileInfo info = getFileInfo("/android/project/" + project.key + "/archive_db");
				
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
					if (stream != null) stream.close();
				} catch (IOException e) {
					FLog.e("error closing stream", e);
				}
				
				cleanupClient();
			}
		}
	}
	
	public DownloadResult downloadProject(Project project) {
		return downloadFile("/android/project/" + project.key + "/archive", 
				"/android/project/" + project.key + "/download", 
				Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir);
	}
	
	public DownloadResult downloadDatabase(Project project) {
		return downloadFile("/android/project/" + project.key + "/archive_db", 
				"/android/project/" + project.key + "/download_db", 
				Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key);
	}
	
	public DownloadResult downloadDatabase(Project project, String version, String dir) {
		return downloadFile("/android/project/" + project.key + "/archive_db?version=" + version, 
				"/android/project/" + project.key + "/download_db?version=" + version, 
				dir);
	}
	
	public DownloadResult downloadFile(String infoPath, String downloadPath, String dir) {
		return downloadFile(infoPath, downloadPath, dir, false);
	}
	
	public DownloadResult downloadFile(String infoPath, String downloadPath, String dir, boolean chooseFileFromInfo) {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			File file = null;
			try {
				initClient();
				
				FileInfo info = getFileInfo(infoPath);
				
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
		        	file = downloadArchive(downloadPath + "?file=" + URLEncoder.encode(info.filename, "UTF-8"), info);
		        } else {
		        	file = downloadArchive(downloadPath, info);
		        }
		        
		        if (isInterrupted) {
					FLog.d("download file interrupted");
					
					return DownloadResult.INTERRUPTED;
				}
				
				if (file == null) {
					FLog.d("download corrupted");
					return new DownloadResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.DOWNLOAD_CORRUPTED_ERROR, info);
				}
				
				FileUtil.untarFile(dir, file.getAbsolutePath());
				
				FLog.d("downloaded file!");
				
				return new DownloadResult(FAIMSClientResultCode.SUCCESS, null, info);
		        
			} catch (Exception e) {
				FLog.e("error downloading file", e);
				
				return DownloadResult.FAILURE;
				
			} finally {
				
				if (file != null) {
					file.delete();
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
	
	private FileInfo getFileInfo(String path) throws Exception {
		InputStream stream = null;
		
		try {
			FileInfo info = new FileInfo();
			
			HttpEntity entity = getRequest(getUri(path));
			
			stream = entity.getContent();
			
			JsonObject object = JsonUtil.deserializeJsonObject(stream);
			
			info.parseJson(object);
			
			return info;
			
		} finally {
			if (stream != null) stream.close();
		}
		
	}
	
	private File downloadArchive(String path, FileInfo archive) throws Exception {
		InputStream stream = null;
		
		try {
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, DATA_TIMEOUT);
			
			HttpEntity entity = getRequest(getUri(path), params);
			
			if (isInterrupted) {
				FLog.d("download archive interrupted");
				
				return null;
			}
			
			stream = entity.getContent();
			
	    	FileUtil.makeDirs(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir); // make sure directory exists
			
	    	File tempFile = File.createTempFile("temp_", ".tar.gz", new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir));
	    	
			FileUtil.saveFile(stream, tempFile.getAbsolutePath());
			
			if (isInterrupted) {
				FLog.d("download archive interrupted");
				
				return null;
			}
			
			String md5 = FileUtil.generateMD5Hash(tempFile.getAbsolutePath());
			
			if (isInterrupted) {
				FLog.d("download archive interrupted");
				
				return null;
			}
			
			if (!archive.md5.equals(md5)) {
				
				tempFile.delete();
				
				return null;
			}
			
			return tempFile;
		} finally {
			if (stream != null) stream.close();
		}
	}
	
	private String getUri(String path) throws Exception {
		return serverDiscovery.getServerHost() + path;
	}
	
	private HttpEntity getRequest(String uri) throws IOException {
		return getRequest(uri, null);
	}
	
	private HttpEntity getRequest(String uri, HttpParams params) throws IOException {
		FLog.d(uri);
		
		HttpGet get = new HttpGet(uri);
		
		if (params != null) {
			get.setParams(params);
		}

		HttpResponse response = httpClient.execute(get);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			FLog.d("request failed " + uri);
			return null;
		}
		
		HttpEntity entity = response.getEntity();
		
		return entity;
	}

	public Result uploadDirectory(String projectDir, String uploadDir, String requestExcludePath, String uploadPath) {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			File file = null;
			try {
				initClient();
				
				String uploadDirPath = projectDir + "/" + uploadDir;
				
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
				
				HttpEntity entity = getRequest(getUri(requestExcludePath));
				
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
				boolean canUpload = !isListSubsetOfList(localFiles, serverFiles);
				
				if (!canUpload) {
					FLog.d("no files to upload");
					return Result.SUCCESS;
				}
				
				file = new File(projectDir + "/" + UUID.randomUUID());
				FileUtil.tarFile(uploadDirPath, "", file.getAbsolutePath(), serverFiles);
				
				if (isInterrupted) {
					FLog.d("upload directory interrupted");
					
					return Result.INTERRUPTED;
				}
				
				return uploadFile(file, uploadPath);
			} catch (Exception e) {
				FLog.e("error uploading directory", e);
				return Result.FAILURE;
			} finally {
				
				if (file != null) {
					file.delete();
				}
				
				try {
					if (stream != null) stream.close();
				} catch (IOException ioe) {
					FLog.e("error closing stream", ioe);
				}
				
				cleanupClient();
			}
		}
	}

	public DownloadResult downloadDirectory(String projectDir, String downloadDir, String requestExcludePath, String infoPath, String downloadPath) {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			try {
				initClient();
				
				HttpEntity entity = getRequest(getUri(requestExcludePath));
				
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
				
				String downloadDirPath = projectDir + "/" + downloadDir;
				
				// make sure dir exists
				FileUtil.makeDirs(downloadDirPath);
				
				List<String> localFiles = FileUtil.listDir(downloadDirPath);
				
				FLog.d("local Files: " + serverFiles.toString());
				
				// check if new files to download
				boolean canDownload = !isListSubsetOfList(serverFiles, localFiles);
				
				if (!canDownload) {
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
				
				return downloadFile(sb.toString(), downloadPath, downloadDirPath, true);
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
	
	private boolean isListSubsetOfList(List<String> subFiles, List<String> files) {
		for (String sf : subFiles) {
			boolean fileInList = false;
			for (String f : files) {
				if (sf.equals(f)) {
					fileInList = true;
					break;
				}
			}
			if (!fileInList) {
				return false;
			}
		}
		return true;
	}
	

	public void invalidate() {
		serverDiscovery.invalidateServerHost();
	}

	public void interrupt() {
		synchronized(FAIMSClient.class) {
			if (httpClient != null) {
				isInterrupted = true;
				httpClient.getConnectionManager().shutdown();
			}
		}
	}
	
}
