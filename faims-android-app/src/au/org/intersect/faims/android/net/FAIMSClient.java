package au.org.intersect.faims.android.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.data.ProjectArchiveInfo;
import au.org.intersect.faims.android.util.FAIMSLog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.JsonUtil;

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
	
	private void initClient() throws UnknownHostException {
		FAIMSLog.log();
		
		String userAgent = InetAddress.getLocalHost().toString();
		httpClient = AndroidHttpClient.newInstance(userAgent);
		
		FAIMSLog.log("userAgent is " + userAgent);
	}
	
	private void cleanupClient() {
		FAIMSLog.log();
		
		httpClient.close();
		httpClient = null;
	}
	
	public FAIMSClientResultCode uploadDatabase(String projectId, File file) {
		return uploadFile(file, "/android/project/" + projectId + "/upload_db");
	}
	
	public FAIMSClientResultCode uploadFile(File file, String uri) {
		try {
			initClient();
			
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("file", new FileBody(file, "binary/octet-stream"));
			entity.addPart("md5", new StringBody(FileUtil.generateMD5Hash(file.getPath())));
			
			HttpPost post = new HttpPost(new URI(getURI(uri)));
			post.setEntity(entity);
			
			HttpResponse response = httpClient.execute(post);
			
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				Log.d("FAIMS", "upload failed");
				
				return FAIMSClientResultCode.SERVER_FAILURE;
			}
			
			return FAIMSClientResultCode.SUCCESS;
			
		} catch (Exception e) {
			Log.e("FAIMS", "cannot upload file", e);
			
			if (e instanceof InterruptedException) {
				return FAIMSClientResultCode.CANCELLED;
			} else {
				return FAIMSClientResultCode.SERVER_FAILURE;
			}
			
		} finally {
			cleanupClient();
		}
	}
	
	public FAIMSClientResultCode fetchProjectList(LinkedList<Project> projects) {
		FAIMSLog.log();

		InputStream stream = null;
		try {			
			initClient();
			
			HttpEntity entity = getRequest("/android/projects");
			
			stream = entity.getContent();
			
			List<Project> ps = JsonUtil.deserializeProjects(stream);
			
			for (Project p : ps) {
				projects.push(p);
			}
			
			FAIMSLog.log("fetched projects!");
	        
			return FAIMSClientResultCode.SUCCESS;
		} catch(Exception e) {
			FAIMSLog.log(e);
			
			if (e instanceof InterruptedException) {
				return FAIMSClientResultCode.CANCELLED;
			} else {
				return FAIMSClientResultCode.SERVER_FAILURE;
			}
			
		} finally {
			
			try {
				if (stream != null) stream.close();
			} catch (IOException e) {
				FAIMSLog.log(e);
			}
			
			cleanupClient();
		}
	}
	
	public FAIMSClientResultCode downloadProjectArchive(String projectId) {
		FAIMSLog.log();
		
		InputStream stream = null;
		String filename = null;
		try {
			initClient();
			
			ProjectArchiveInfo archive = getProjectArchive(projectId);
	        long freeSpace = FileUtil.getExternalStorageSpace();
	        
	        FAIMSLog.log("freespace: " + String.valueOf(freeSpace));
	        FAIMSLog.log("filesize: " + String.valueOf(archive.size));
	        
	        if (archive.size > freeSpace) {
	        	return FAIMSClientResultCode.STORAGE_LIMIT_ERROR;
	        } 
	        
	        filename = getProjectDownload(projectId, archive);
			
			if (filename == null) {
				return FAIMSClientResultCode.DOWNLOAD_CORRUPTED;
			}
			
			FileUtil.untarFromStream("/faims/projects", filename);
			
			FileUtil.deleteFile(filename);
			
			FAIMSLog.log("downloaded project!");
			
			return FAIMSClientResultCode.SUCCESS;
	        
		} catch (Exception e) {
			FAIMSLog.log(e);
			
			// remove file if downloaded
			if (filename != null) {
				try {
				
					FileUtil.deleteFile(filename);
					
				} catch (IOException ioe) {
					FAIMSLog.log(ioe);
				}
			}
			
			if (e instanceof InterruptedException) {
				return FAIMSClientResultCode.CANCELLED;
			} else {
				return FAIMSClientResultCode.SERVER_FAILURE;
			}
			
		} finally {
			
			try {
				if (stream != null) stream.close();
			} catch (IOException e) {
				FAIMSLog.log(e);
			}
			
			cleanupClient();
		}
	}
	
	private ProjectArchiveInfo getProjectArchive(String projectId) throws IOException {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			HttpEntity entity = getRequest("/android/project/" + projectId + "/archive");
			
			stream = entity.getContent();
			
			JsonObject object = JsonUtil.deserializeJsonObject(stream);
			
			return ProjectArchiveInfo.fromJson(object);
			
		} finally {
			if (stream != null) stream.close();
		}
		
	}
	
	private String getProjectDownload(String projectId, ProjectArchiveInfo archive) throws Exception {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, DATA_TIMEOUT);
			
			HttpEntity entity = getRequest("/android/project/" + projectId + "/download", params);
			
			stream = entity.getContent();
			
	    	FileUtil.makeDirs("/faims/projects");
			
			String filename = "/faims/projects/" + archive.filename;
			
			FileUtil.saveFile(stream, filename);
			
			String md5 = FileUtil.generateMD5Hash(FileUtil.toPath(filename));
			
			FAIMSLog.log("filename.md5Hash: " + md5);
			FAIMSLog.log("archive.md5Hash:  " + archive.md5);
			
			if (!archive.md5.equals(md5)) {
				
				FileUtil.deleteFile(filename);
				
				return null;
			}
			
			return filename;
		} finally {
			// TODO check if file needs to be deleted
			if (stream != null) stream.close();
		}
	}
	
	private String getURI(String path) {
		FAIMSLog.log(serverDiscovery.getServerHost() + path);
		
		return serverDiscovery.getServerHost() + path;
	}
	
	private HttpEntity getRequest(String path) throws IOException {
		return getRequest(path, null);
	}
	
	private HttpEntity getRequest(String path, HttpParams params) throws IOException {
		FAIMSLog.log(path);
		
		HttpGet get = new HttpGet(getURI(path));
		
		if (params != null) {
			get.setParams(params);
		}

		HttpResponse response = httpClient.execute(get);
		HttpEntity entity = response.getEntity();
		
		return entity;
	}

	public void cancelRequest() {
		FAIMSLog.log();
		if (httpClient != null) {
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	public void invalidate() {
		serverDiscovery.invalidateServerHost();
	}
	
}
