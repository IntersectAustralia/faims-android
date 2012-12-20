package au.org.intersect.faims.android.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.net.http.AndroidHttpClient;
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
	
	private AndroidHttpClient httpClient;
	
	private void createClient() throws UnknownHostException {
		FAIMSLog.log();
		
		String userAgent = InetAddress.getLocalHost().toString();
		httpClient = AndroidHttpClient.newInstance(userAgent);
		
		FAIMSLog.log("userAgent is " + userAgent);
	}
	
	private void destroyClient() {
		FAIMSLog.log();
		
		httpClient.close();
		httpClient = null;
	}
	
	public FAIMSClientResultCode fetchProjectList(LinkedList<Project> projects) {
		FAIMSLog.log();

		InputStream stream = null;
		try {			
			createClient();
			
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
			
			destroyClient();
		}
	}
	
	public FAIMSClientResultCode downloadProjectArchive(Project project) {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			createClient();
			
			ProjectArchive archive = getProjectArchive(project);
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
	        
	        String filename = getProjectDownload(project, archive);
			
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
			
			destroyClient();
		}
	}
	
	private ProjectArchive getProjectArchive(Project project) throws IOException {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			HttpEntity entity = getRequest("/android/project/" + project.id + "/archive");
			
			stream = entity.getContent();
			
			JsonObject object = JsonUtil.deserializeJsonObject(stream);
			
			return ProjectArchive.fromJson(object);
			
		} finally {
			if (stream != null) stream.close();
		}
		
	}
	
	private String getProjectDownload(Project project, ProjectArchive archive) throws Exception {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			HttpEntity entity = getRequest("/android/project/" + project.id + "/download");
			
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
	
	private HttpEntity getRequest(String path) throws IOException {
		FAIMSLog.log(path);
		
		HttpGet get = new HttpGet(serverDiscovery.getServerHost() + path);
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
	
}
