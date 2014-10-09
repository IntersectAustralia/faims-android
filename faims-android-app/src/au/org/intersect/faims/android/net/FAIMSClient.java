package au.org.intersect.faims.android.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map.Entry;

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
import org.json.JSONArray;
import org.json.JSONObject;

import android.net.http.AndroidHttpClient;
import android.util.Base64;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.JsonUtil;

import com.google.inject.Inject;
import com.google.inject.Singleton;

// TODO create unit tests by injecting AndroidHttpClient with mock client
@Singleton
public class FAIMSClient {

	private static final int CONNECTION_TIMEOUT = 300 * 1000; // 5 minutes
	private static final int DATA_TIMEOUT = 3600 * 1000; // 10 hours
	
	// Note: this is temporary 	
	private static final String USERNAME = "faimsandroidapp";	
	private static final String TOKEN = "YiQIeV39sdhb2ltRmOyGN";

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
	
	public void invalidate() {
		serverDiscovery.invalidateServerHost();
	}

	public void interrupt() {
		// note: this method is non synchronized because we want to terminate currently running operations
		if (httpClient != null) {
			isInterrupted = true;
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	private String getCredentials() {
		return "Basic " + Base64.encodeToString((USERNAME+":"+TOKEN).getBytes(), Base64.NO_WRAP);
	}
	
	public Result fetchRequestArray(String requestUri) {
		return fetchRequestObject(requestUri, true);
	}
	
	public Result fetchRequestObject(String requestUri) {
		return fetchRequestObject(requestUri, false);
	}
	
	public Result fetchRequestObject(String requestUri, boolean asArray) {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			try {			
				initClient();
				
				HttpResponse response = getRequest(getUri(requestUri));
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.SC_ACCEPTED) {
					FLog.d("fetch request busy");
					return new Result(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.BUSY_ERROR);
				} else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					FLog.d("fetch request failed");
					return Result.FAILURE;
				}
				
				HttpEntity entity = response.getEntity();			
				if (isInterrupted) {
					FLog.d("fetch request interrupted");				
					return Result.INTERRUPTED;
				}
				
				stream = entity.getContent();	
				if (asArray) {
					JSONArray json = JsonUtil.deserializeJsonArray(stream);				
					return new Result(FAIMSClientResultCode.SUCCESS, null, json);
				} else {
					JSONObject json = JsonUtil.deserializeJsonObject(stream);				
					return new Result(FAIMSClientResultCode.SUCCESS, null, json);
				}
			} catch(Exception e) {
				FLog.e("error fetching request", e);				
				return Result.FAILURE;				
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
	
	public Result downloadFile(String requestUri, File location) {
		synchronized(FAIMSClient.class) {
			InputStream stream = null;
			try {
				initClient();
				
				HttpResponse response = getRequest(getUri(requestUri));
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.SC_ACCEPTED) {
					FLog.d("download file busy");
					return new Result(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.BUSY_ERROR);
				} else if (statusCode != HttpStatus.SC_OK) {
					FLog.d("download file failed");
					return Result.FAILURE;
				}
				
				HttpEntity entity = response.getEntity();
				if (isInterrupted) {
					FLog.d("download file interrupted");				
					return Result.INTERRUPTED;
				}
				
				stream = entity.getContent();		
				FileUtil.saveFile(stream, location);
				
				return Result.SUCCESS;
			} catch (Exception e) {
				if (isInterrupted) {
					FLog.d("download file interrupted");				
					return Result.INTERRUPTED;
				}
				FLog.e("error downloading file", e);			
				return Result.FAILURE;				
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
	
	public Result uploadFile(String requestUri, File uploadFile, File uploadPath, HashMap<String, ContentBody> extraParts) {
		synchronized(FAIMSClient.class) {
			try {
				initClient();
				
				MultipartEntity entity = new MultipartEntity();
				entity.addPart("file", new FileBody(uploadFile, "binary/octet-stream"));
				entity.addPart("request_file", new StringBody(uploadPath.getPath()));
				entity.addPart("md5", new StringBody(FileUtil.generateMD5Hash(uploadFile)));			
				if (extraParts != null) {
					for (Entry<String, ContentBody> entry : extraParts.entrySet()) {
						entity.addPart(entry.getKey(), entry.getValue());
					}
				}
				
				if (isInterrupted) {
					FLog.d("upload file interrupted");			
					return Result.INTERRUPTED;
				}
				
				HttpPost post = createPostRequest(getUri(requestUri));
				post.setEntity(entity);				
				HttpResponse response = httpClient.execute(post);			
				if (isInterrupted) {
					FLog.d("upload file interrupted");				
					return Result.INTERRUPTED;
				}
				
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK) {
					FLog.d("upload file failed");				
					return Result.FAILURE;
				}
				
				return Result.SUCCESS;	
			} catch (Exception e) {
				if (isInterrupted) {
					FLog.d("upload file interrupted");				
					return Result.INTERRUPTED;
				}
				FLog.e("error uploading file", e);				
				return Result.FAILURE;				
			} finally {
				cleanupClient();
			}
		}		
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
	
	public String getPlainHost() {
		return serverDiscovery.getPlainServerHost();
	}
	
	private String getUri(String path) throws Exception {
		return serverDiscovery.getServerHost() + path;
	}
	
	private HttpResponse getRequest(String uri) throws IOException {
		FLog.d("request: " + uri);
		
		HttpGet get = createGetRequest(uri);
		
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, DATA_TIMEOUT);
		get.setParams(params);

		HttpResponse response = httpClient.execute(get);
		return response;
	}
	
}
