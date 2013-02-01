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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.data.ProjectArchive;
import au.org.intersect.faims.android.util.FAIMSLog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.JsonUtil;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// TODO create unit tests by injecting AndroidHttpClient with mock client
@Singleton
public class FAIMSClient {

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
	
	public FAIMSClientResultCode uploadDatabase(File file) {
		return uploadFile(file, "/android/upload_db");
	}
	
	public FAIMSClientResultCode uploadFile(File file, String uri) {
		try {
			initClient();
			
			FileEntity entity = new FileEntity(file, "binary/octet-stream");
			entity.setChunked(true);
			
			HttpPost post = new HttpPost(new URI(uri));
			post.setEntity(entity);
			
			HttpResponse response = httpClient.execute(post);
			if (response.getEntity() == null) {
				return FAIMSClientResultCode.SERVER_FAILURE;
			}
			
			return FAIMSClientResultCode.SUCCESS;
			
		} catch (Exception e) {
			Log.e("FAIMS", "cannot upload file", e);
		} finally {
			cleanupClient();
		}
		
		return FAIMSClientResultCode.SERVER_FAILURE;
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
			
			return FAIMSClientResultCode.SERVER_FAILURE;
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
		
		try {
			initClient();
			
			ProjectArchive archive = getProjectArchive(projectId);
	        long freeSpace = FileUtil.getExternalStorageSpace();
	        
	        FAIMSLog.log("freespace: " + String.valueOf(freeSpace));
	        FAIMSLog.log("filesize: " + String.valueOf(archive.size));
	        
	        if (archive.size > freeSpace) {
	        	return FAIMSClientResultCode.STORAGE_LIMIT_ERROR;
	        } 
        	
	        // quit if thread is interrupted
	        if (Thread.interrupted()) {
	        	return FAIMSClientResultCode.SERVER_FAILURE;
	        }
	        
	        String filename = getProjectDownload(projectId, archive);
			
			if (filename == null) {
				return FAIMSClientResultCode.DOWNLOAD_CORRUPTED;
			}
			
			// quit if thread is interrupted
	        if (Thread.interrupted()) {
	        	FileUtil.deleteFile(filename);
	        	
	        	return FAIMSClientResultCode.SERVER_FAILURE;
	        }
			
			FileUtil.untarFromStream("/faims/projects", filename);
			
			FileUtil.deleteFile(filename);
			
			FAIMSLog.log("downloaded project!");
			
			return FAIMSClientResultCode.SUCCESS;
	        
		} catch (Exception e) {
			FAIMSLog.log(e);
			
			return FAIMSClientResultCode.SERVER_FAILURE;
		} finally {
			
			try {
				if (stream != null) stream.close();
			} catch (IOException e) {
				FAIMSLog.log(e);
			}
			
			cleanupClient();
		}
	}
	
	private ProjectArchive getProjectArchive(String projectId) throws IOException {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			HttpEntity entity = getRequest("/android/project/" + projectId + "/archive");
			
			stream = entity.getContent();
			
			JsonObject object = JsonUtil.deserializeJsonObject(stream);
			
			return ProjectArchive.fromJson(object);
			
		} finally {
			if (stream != null) stream.close();
		}
		
	}
	
	private String getProjectDownload(String projectId, ProjectArchive archive) throws Exception {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			HttpEntity entity = getRequest("/android/project/" + projectId + "/download");
			
			stream = entity.getContent();
			
	    	FileUtil.makeDirs("/faims/projects");
			
			String filename = "/faims/projects/" + archive.filename;
			
			FileUtil.saveFile(stream, filename);
			
			String md5 = FileUtil.generateMD5Hash(filename);
			
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
		return serverDiscovery.getServerHost() + path;
	}
	
	private HttpEntity getRequest(String path) throws IOException {
		FAIMSLog.log(path);
		
		HttpGet get = new HttpGet(getURI(path));
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
