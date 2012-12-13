package au.org.intersect.faims.android.net;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import au.org.intersect.faims.android.projects.ProjectInfo;
import au.org.intersect.faims.util.FileUtil;
import au.org.intersect.faims.util.JsonUtil;

import com.google.gson.JsonObject;

public class FAIMSClient implements IFAIMSClient {
	
	public static final int SUCCESS = 0;
	public static final int FAILURE = 1;
	public static final int DOWNLOAD_TOO_BIG = 2;
	public static final int DOWNLOAD_CORRUPTED = 3;

	private AndroidHttpClient httpClient;
	
	private IFAIMSClient.FAIMClientListener<List<ProjectInfo>> projectListListener;
	private IFAIMSClient.FAIMClientListener<ProjectInfo> downloadArchiveListener;
	private ProjectInfo downloadArchiveProject;

	private void createClient() {
		httpClient = AndroidHttpClient.newInstance(
				"Mozilla/5.0(Linux; U; Android 2.2; en-gb; LG-P500 Build/FRF91) AppleWebKit/533.0 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
	
	}
	
	private void destroyClient() {
		httpClient.close();
	}
	
	@Override
	public void fetchProjectList(IFAIMSClient.FAIMClientListener<List<ProjectInfo>> listener) {
		Log.d("debug", "FAIMSClient.fetchProjectList");
		
		projectListListener = listener;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				createClient();
				try {
					
					HttpEntity entity = getRequest("/android/projects");
					List<ProjectInfo> projects = JsonUtil.deserializeProjects(entity.getContent());
					
					Log.d("debug", "FAIMSClient.receivedProjectList");
					projectListListener.handleResponse(SUCCESS, projects);
			        
				} catch(IOException e) {
					Log.d("debug", e.toString());
					projectListListener.handleResponse(FAILURE, null);
				} finally {
					destroyClient();
				}
			}
			
		}).start();
		
	}
	
	@Override
	public void downloadProjectArchive(ProjectInfo project, IFAIMSClient.FAIMClientListener<ProjectInfo> listener) {
		Log.d("debug", "FAIMSClient: downloadProjectArchive");
		
		downloadArchiveListener = listener;
		downloadArchiveProject = project;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				createClient();
				try {
					
					HttpEntity entity = getRequest("/android/project/" + downloadArchiveProject.id + "/archive");
					JsonObject data = JsonUtil.deserializeJsonObject(entity.getContent());
			        long freeSpace = FileUtil.getExternalStorageSpace();
			        
			        Log.d("debug", "freespace: " + String.valueOf(freeSpace));
			        Log.d("debug", "filesize: " + String.valueOf(data.get("size").getAsLong()));
			        
			        if (data.get("size").getAsLong() > freeSpace) {
			        	
			        	downloadArchiveListener.handleResponse(DOWNLOAD_TOO_BIG, null);
			        } else {
			        	
			        	entity = getRequest("/android/project/" + downloadArchiveProject.id + "/download");
			        	Log.d("debug", "FAIMSClient.contentLength: " + entity.getContentLength());
			        	
						FileUtil.makeDirs("/faims/projects");
						
						String filename = "/faims/projects/" + data.get("file").getAsString();
						
						FileUtil.saveFile(entity.getContent(), filename);
						
						Log.d("debug", "FAIMSClient.savedFile: " + filename);
						
						String md5Hash = FileUtil.generateMD5Hash(filename);
						
						
						
						Log.d("debug", "File.md5Hash: " + md5Hash);
						Log.d("debug", "Data.md5Hash: " + data.get("md5").getAsString());
						
						if (!data.get("md5").getAsString().equals(md5Hash)) {
							Log.d("debug", "FAIMSClient.deleteFile: " + filename);
							FileUtil.deleteFile(filename);
							
							downloadArchiveListener.handleResponse(DOWNLOAD_CORRUPTED, null);
						} else {
						
							FileUtil.untarFromStream("/faims/projects", filename);
							
							Log.d("debug", "FAIMSClient.deleteFile: " + filename);
							FileUtil.deleteFile(filename);
							
							Log.d("debug", "FAIMSClient.savedProject");
							downloadArchiveListener.handleResponse(SUCCESS, downloadArchiveProject);
						}
			        }
					
				} catch (IOException e) {
					Log.d("debug", e.toString());
					downloadArchiveListener.handleResponse(FAILURE, null);
				} catch (Exception e) {
					Log.d("debug", e.toString());
					downloadArchiveListener.handleResponse(FAILURE, null);
				} finally {
					destroyClient();
				}
			}
		}).start();
		
	}
	
	private HttpEntity getRequest(String path) throws IOException {
		ServerDiscovery ds = ServerDiscovery.getInstance();
		Log.d("debug", ds.getServerHost() + path);
		
		HttpGet get = new HttpGet(ds.getServerHost() + path);
		HttpResponse response = httpClient.execute(get);
		HttpEntity entity = response.getEntity();
		
		return entity;
	}
	
}
