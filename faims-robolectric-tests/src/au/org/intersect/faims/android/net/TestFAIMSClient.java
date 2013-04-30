package au.org.intersect.faims.android.net;

import java.util.ArrayList;
import java.util.UUID;

import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.util.TestProjectUtil;

import com.google.inject.Provider;

public class TestFAIMSClient extends FAIMSClient {
	
	private int projectsCount = 0;
	private FAIMSClientResultCode projectsCode;
	private FAIMSClientResultCode downloadResultCode;
	private FAIMSClientErrorCode downlaodErrorCode;

	@Override
	public FetchResult fetchProjectList() {
		ArrayList<Project> projects = new ArrayList<Project>();
		for (int i = 0; i < projectsCount; i++) {
			projects.add(new Project("Project " + i, UUID.randomUUID().toString()));
		}
		return new FetchResult(projectsCode, null, projects);
	}

	@Override
	public DownloadResult downloadSettings(Project project) {
		TestProjectUtil.createProjectFrom(project.name, project.key, "Common");
		return new DownloadResult(downloadResultCode, downlaodErrorCode);
	}
	
	public void setProjectsCount(int value) {
		projectsCount = value;
	}
	
	public void setProjectsResultCode(FAIMSClientResultCode value) {
		projectsCode = value;
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
				client.setProjectsCount(count);
				client.setProjectsResultCode(fetchCode);
				client.setDownloadResultCode(resultCode, errorCode);
				return client;
			}
			
		};
	}
	
}
