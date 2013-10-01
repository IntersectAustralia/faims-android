package au.org.intersect.faims.android.net;

import java.util.ArrayList;
import java.util.UUID;

import au.org.intersect.faims.android.data.FileInfo;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.util.TestModuleUtil;

import com.google.inject.Provider;

public class TestFAIMSClient extends FAIMSClient {
	
	private int modulesCount = 0;
	private FAIMSClientResultCode modulesCode;
	private FAIMSClientResultCode downloadResultCode;
	private FAIMSClientErrorCode downlaodErrorCode;

	@Override
	public FetchResult fetchModuleList() {
		ArrayList<Module> modules = new ArrayList<Module>();
		for (int i = 0; i < modulesCount; i++) {
			modules.add(new Module("Module " + i, UUID.randomUUID().toString()));
		}
		return new FetchResult(modulesCode, null, modules);
	}

	@Override
	public DownloadResult downloadSettings(Module module) {
		TestModuleUtil.createModuleFrom(module.name, module.key, "Common");
		FileInfo info = new FileInfo();
		info.version = "0";
		return new DownloadResult(downloadResultCode, downlaodErrorCode, info);
	}
	
	@Override
	public DownloadResult downloadDatabase(Module module) {
		return new DownloadResult(downloadResultCode, downlaodErrorCode);
	}
	
	@Override
	public DownloadResult downloadDirectory(String moduleDir, String downloadDir, String requestExcludePath, String infoPath, String downloadPath) {
		return new DownloadResult(downloadResultCode, downlaodErrorCode);
	}
	
	public void setModulesCount(int value) {
		modulesCount = value;
	}
	
	public void setModulesResultCode(FAIMSClientResultCode value) {
		modulesCode = value;
	}
	
	public void setDownloadResultCode(FAIMSClientResultCode resultCode, FAIMSClientErrorCode errorCode) {
		downloadResultCode = resultCode;
		downlaodErrorCode = errorCode;
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
