package au.org.intersect.faims.android.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.LinkedList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.projects.ProjectInfo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class FAIMSClient implements IFAIMSClient {

	private AndroidHttpClient httpClient;
	
	public FAIMSClient() {
		httpClient = AndroidHttpClient.newInstance(
				"Mozilla/5.0(Linux; U; Android 2.2; en-gb; LG-P500 Build/FRF91) AppleWebKit/533.0 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
	}
	
	@Override
	public void fetchProjectList(IFAIMSClient.FAIMClientListener<LinkedList<ProjectInfo>> handler) {
		Log.d("debug", "FAIMSClient.fetchProjectList");
		try {
			DiscoveryServer ds = DiscoveryServer.getInstance();
	
			Log.d("debug", ds.getServerHost() + "/android/projects");
			HttpGet get = new HttpGet(ds.getServerHost() + "/android/projects");
			HttpResponse response = httpClient.execute(get);
			HttpEntity entity = response.getEntity();
			
			InputStreamReader input = new InputStreamReader(entity.getContent());
	        StringBuilder str = new StringBuilder();
	        int value;
	        while((value = input.read()) > 0)
	            str.append((char) value);
	        
	        JsonReader reader = new JsonReader(new StringReader(str.toString()));
	        JsonParser parser = new JsonParser();
	        JsonArray data = parser.parse(reader).getAsJsonArray();
	        

	        Log.d("debug", "JsonArray: " + data.toString());
	        
	        
	        LinkedList<ProjectInfo> projects = new LinkedList<ProjectInfo>();
	        for (int i = 0; i < data.size(); i++) {
	        	ProjectInfo p = ProjectInfo.fromJson(data.get(i).getAsJsonObject());
	        	projects.push(p);
	        	
	        	Log.d("debug", "ProjectInfo: " + p.name.toString());
	        }
	        
	        handler.handleResponse(true, projects);
	        
		} catch(IOException e) {
			Log.d("debug", e.toString());
			handler.handleResponse(false, null);
		}
	}
	
	@Override
	public void downloadProjectArchive(IFAIMSClient.FAIMClientListener<ProjectInfo> handler, ProjectInfo project) {
		Log.d("debug", "FAIMSClient: downloadProjectArchive");
		
		try {
			DiscoveryServer ds = DiscoveryServer.getInstance();
			
			Log.d("debug", ds.getServerHost() + "/android/" + project.id + "archive");
			HttpGet get = new HttpGet(ds.getServerHost() + "/android/" + project.id + "archive");
			HttpResponse response = httpClient.execute(get);
			HttpEntity entity = response.getEntity();
			
			InputStreamReader input = new InputStreamReader(entity.getContent());
	        StringBuilder str = new StringBuilder();
	        int value;
	        while((value = input.read()) > 0)
	            str.append((char) value);
	        
	        JsonReader reader = new JsonReader(new StringReader(str.toString()));
	        JsonParser parser = new JsonParser();
	        JsonObject data = parser.parse(reader).getAsJsonObject();
	        
	        Log.d("debug", "JsonArray: " + data.toString());
	        
	        get = new HttpGet(ds.getServerHost() + "/android/" + project.id + "/download");
			response = httpClient.execute(get);
			entity = response.getEntity();
			
			File file = new File(Environment.getExternalStorageDirectory(), "project.tar.gz");

	        FileOutputStream fileOutput = new FileOutputStream(file);
		        
			byte[] buffer = new byte[1024];
	        int bufferLength = 0; //used to store a temporary size of the buffer

	        while ( (bufferLength = entity.getContent().read(buffer)) > 0 ) {
	                fileOutput.write(buffer, 0, bufferLength);

	        }
	        fileOutput.close();
	        
	        Log.d("debug", "Saved file");
			
		} catch (IOException e) {
			Log.d("debug", e.toString());
		}
	}
	
}
