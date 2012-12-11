package au.org.intersect.faims.android.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.projects.ProjectInfo;

public class FAIMSClient implements IFAIMSClient {

	private AndroidHttpClient httpClient;
	
	public FAIMSClient() {
		httpClient = AndroidHttpClient.newInstance(
				"Mozilla/5.0(Linux; U; Android 2.2; en-gb; LG-P500 Build/FRF91) AppleWebKit/533.0 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
	}
	
	@Override
	public void fetchProjectList(FAIMSResponseHandler<List<ProjectInfo>> handler) {
		Log.d("debug", "FAIMSClient: fetchProjectList");
		
		DiscoverServer.getInstance().findServer(new DiscoverServer.ServerFoundHandler() {
			
			@Override 
			public void handleServerFound(boolean success) {
				
				Log.d("debug", "FAIMSClient: handleServerFound");
				
				try {
					
					HttpGet get = new HttpGet("http://172.16.30.43:3000/android/projects");
					get.setHeader(HTTP.CONTENT_TYPE, "application/json" );
					HttpResponse response = httpClient.execute(get);
					HttpEntity entity = response.getEntity();
			        Log.d("debug", EntityUtils.toString(entity));
			        
				} catch(IOException e) {
					Log.d("debug", e.toString());
				}
			}
			
		});
	}
	
	@Override
	public void downloadProjectArchive(FAIMSResponseHandler<String> handler) {
		Log.d("debug", "FAIMSClient: downloadProjectArchive");
		
		DiscoverServer.getInstance().findServer(new DiscoverServer.ServerFoundHandler() {
			
			@Override 
			public void handleServerFound(boolean success) {
				
				Log.d("debug", "FAIMSClient: handleServerFound");
				
				try {
					HttpGet get = new HttpGet("http://172.16.30.43:3000/android/project/46/download");
					//get.setHeader(HTTP.CONTENT_TYPE, "application/json" );
					HttpResponse response = httpClient.execute(get);
					HttpEntity entity = response.getEntity();
					InputStream inputStream = entity.getContent();
					
					File file = new File(Environment.getExternalStorageDirectory(), "db.sqlite3");

			        FileOutputStream fileOutput = new FileOutputStream(file);
				        
					byte[] buffer = new byte[1024];
			        int bufferLength = 0; //used to store a temporary size of the buffer

			        while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
			                fileOutput.write(buffer, 0, bufferLength);

			        }
			        fileOutput.close();
			        
			        Log.d("debug", "Saved file");
					
				} catch (IOException e) {
					Log.d("debug", e.toString());
				}
				
			}
		});
	}
	
}
