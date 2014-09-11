package au.org.intersect.faims.android.net;

import java.io.File;
import java.util.HashMap;

import org.apache.http.entity.mime.content.ContentBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Provider;

public class TestFAIMSClient extends FAIMSClient {
	
	private int modulesCount;
	private FAIMSClientResultCode fetchCode;
	private FAIMSClientResultCode downloadResultCode;
	private FAIMSClientErrorCode downlaodErrorCode;
	
	public void setDownloadResultCode(FAIMSClientResultCode resultCode, FAIMSClientErrorCode errorCode) {
		downloadResultCode = resultCode;
		downlaodErrorCode = errorCode;
	}
	
	public void setModulesCount(int count) {
		this.modulesCount = count;
	}
	
	public void setModulesResultCode(FAIMSClientResultCode fetchCode) {
		this.fetchCode = fetchCode;
	}
	
	@Override
	public Result fetchRequestArray(String requestUri) {
		if (fetchCode == FAIMSClientResultCode.SUCCESS) {
			Result result = Result.SUCCESS;
			try {
				JSONArray jsonArray = new JSONArray();
				for (int i = 0; i < modulesCount; i++) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("name", "Module " + i);
					jsonObject.put("key", "key_" + i);
					jsonObject.put("version", "0");
					jsonArray.put(jsonObject);
				}
				result.data = jsonArray;
			} catch (JSONException e) {
				System.out.println(e);
			}
			return result;
		}
		return new Result(fetchCode);
	}

	@Override
	public Result fetchRequestObject(String requestUri) {
		return new Result(fetchCode);
	}

	@Override
	public Result downloadFile(String requestUri, File location) {
		return new Result(downloadResultCode, downlaodErrorCode);
	}

	@Override
	public Result uploadFile(String requestUri, File uploadFile,
			File uploadPath, HashMap<String, ContentBody> extraParts) {
		// TODO Auto-generated method stub
		return super.uploadFile(requestUri, uploadFile, uploadPath, extraParts);
	}

	public static Provider<TestFAIMSClient> createProvider(final int count, final FAIMSClientResultCode fetchCode, final FAIMSClientResultCode resultCode, final FAIMSClientErrorCode errorCode)
	{
		return new Provider<TestFAIMSClient>() {

			@Override
			public TestFAIMSClient get() {
				TestFAIMSClient client = new TestFAIMSClient();
				client.setModulesCount(count);
				client.setModulesResultCode(fetchCode);
				client.setDownloadResultCode(resultCode, errorCode);
				return client;
			}
			
		};
	}
	
}
