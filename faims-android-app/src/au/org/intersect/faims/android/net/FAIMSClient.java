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
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.data.FileInfo;
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

	private static final String BASE_DIR = "/faims/projects/";

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
	
	public FAIMSClientResultCode uploadDatabase(Project project, File file) {
		return uploadFile(file, "/android/project/" + project.key + "/upload_db");
	}
	
	public FAIMSClientResultCode uploadFile(File file, String path) {
		try {
			initClient();
			
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("file", new FileBody(file, "binary/octet-stream"));
			entity.addPart("md5", new StringBody(FileUtil.generateMD5Hash(file.getPath())));
			
			HttpPost post = new HttpPost(new URI(getUri(path)));
			post.setEntity(entity);
			
			HttpResponse response = httpClient.execute(post);
			
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				Log.d("FAIMS", "upload failed");
				
				return FAIMSClientResultCode.SERVER_FAILURE;
			}
			
			Log.d("FAIMS", "uploaded file!");
			
			return FAIMSClientResultCode.SUCCESS;
			
		} catch (Exception e) {
			Log.e("FAIMS", "cannot upload file", e);
			
			return FAIMSClientResultCode.SERVER_FAILURE;
			
		} finally {
			cleanupClient();
		}
	}
	
	public FAIMSClientResultCode fetchProjectList(LinkedList<Project> projects) {
		FAIMSLog.log();

		InputStream stream = null;
		try {			
			initClient();
			
			HttpEntity entity = getRequest(getUri("/android/projects"));
			
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
	
	public FAIMSClientResultCode downloadProject(Project project) {
		return downloadFile("/android/project/" + project.key + "/archive", "/android/project/" + project.key + "/download", BASE_DIR);
	}
	
	public FAIMSClientResultCode downloadDatabase(Project project) {
		return downloadFile("/android/project/" + project.key + "/archive_db", "/android/project/" + project.key + "/download_db", BASE_DIR + project.dir);
	}
	
	public FAIMSClientResultCode downloadFile(String infoPath, String downloadPath, String dir) {
		FAIMSLog.log();
		
		InputStream stream = null;
		File file = null;
		
		try {
			initClient();
			
			FileInfo info = getFileInfo(infoPath);
			
	        long freeSpace = FileUtil.getExternalStorageSpace();
	        
	        FAIMSLog.log("freespace: " + String.valueOf(freeSpace));
	        FAIMSLog.log("filesize: " + String.valueOf(info.size));
	        
	        if (info.size > freeSpace) {
	        	return FAIMSClientResultCode.STORAGE_LIMIT_ERROR;
	        } 
	        
	        file = downloadArchive(downloadPath, info);
			
			if (file == null) {
				return FAIMSClientResultCode.DOWNLOAD_CORRUPTED;
			}
			
			FileUtil.untarFile(Environment.getExternalStorageDirectory() + dir, file.getAbsolutePath());
			
			file.delete();
			
			FAIMSLog.log("downloaded file!");
			
			return FAIMSClientResultCode.SUCCESS;
	        
		} catch (Exception e) {
			FAIMSLog.log(e);
			
			// remove downloaded file
			if (file != null) {
				file.delete();
			}
				
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
	
	private FileInfo getFileInfo(String path) throws IOException {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			HttpEntity entity = getRequest(getUri(path));
			
			stream = entity.getContent();
			
			JsonObject object = JsonUtil.deserializeJsonObject(stream);
			
			return FileInfo.fromJson(object);
			
		} finally {
			if (stream != null) stream.close();
		}
		
	}
	
	private File downloadArchive(String path, FileInfo archive) throws Exception {
		FAIMSLog.log();
		
		InputStream stream = null;
		
		try {
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, DATA_TIMEOUT);
			
			HttpEntity entity = getRequest(getUri(path), params);
			
			stream = entity.getContent();
			
	    	FileUtil.makeDirs(Environment.getExternalStorageDirectory() + BASE_DIR); // make sure directory exists
			
	    	File tempFile = File.createTempFile("temp_", ".tar.gz", new File(Environment.getExternalStorageDirectory() + BASE_DIR));
	    	
			FileUtil.saveFile(stream, tempFile.getAbsolutePath());
			
			String md5 = FileUtil.generateMD5Hash(tempFile.getAbsolutePath());
			
			FAIMSLog.log("filename.md5Hash: " + md5);
			FAIMSLog.log("archive.md5Hash:  " + archive.md5);
			
			if (!archive.md5.equals(md5)) {
				
				tempFile.delete();
				
				return null;
			}
			
			return tempFile;
		} finally {
			// TODO check if file needs to be deleted
			if (stream != null) stream.close();
		}
	}
	
	private String getUri(String path) {
		FAIMSLog.log(serverDiscovery.getServerHost() + path);
		
		return serverDiscovery.getServerHost() + path;
	}
	
	private HttpEntity getRequest(String uri) throws IOException {
		return getRequest(uri, null);
	}
	
	private HttpEntity getRequest(String uri, HttpParams params) throws IOException {
		FAIMSLog.log(uri);
		
		HttpGet get = new HttpGet(uri);
		
		if (params != null) {
			get.setParams(params);
		}

		HttpResponse response = httpClient.execute(get);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			Log.d("FAIMS", "failed request: " + uri);
			return null;
		}
		
		HttpEntity entity = response.getEntity();
		
		return entity;
	}
	
	public void invalidate() {
		serverDiscovery.invalidateServerHost();
	}

	public void interrupt() {
		if (httpClient != null) {
			httpClient.getConnectionManager().shutdown();
		}
	}
	
}
