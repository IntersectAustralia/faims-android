package au.org.intersect.faims.android.net;

import java.util.LinkedList;
import java.util.UUID;

import au.org.intersect.faims.android.data.DownloadResult;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.util.TestProjectUtil;

import com.google.inject.Provider;

public class TestFAIMSClient extends FAIMSClient {
	
	private int projectsCount = 0;
	private FAIMSClientResultCode projectsCode;
	private FAIMSClientResultCode downloadCode;

	@Override
	public FAIMSClientResultCode fetchProjectList(LinkedList<Project> projects) {
		for (int i = 0; i < projectsCount; i++) {
			projects.add(new Project("Project " + i, UUID.randomUUID().toString()));
		}
		return projectsCode;
	}

	@Override
	public DownloadResult downloadProject(Project project) {
		TestProjectUtil.createProjectFrom(project.name, project.key, "Common");
		DownloadResult result = new DownloadResult();
		result.code = downloadCode;
		return result;
	}
	
	public void setProjectsCount(int value) {
		projectsCount = value;
	}
	
	public void setProjectsResultCode(FAIMSClientResultCode value) {
		projectsCode = value;
	}
	
	public void setDownloadResultCode(FAIMSClientResultCode value) {
		downloadCode = value;
	}
	
	public static Provider<TestFAIMSClient> createProvider(final int count, final FAIMSClientResultCode fetchCode, final FAIMSClientResultCode downloadCode)
	{
		return new Provider<TestFAIMSClient>() {

			@Override
			public TestFAIMSClient get() {
				TestFAIMSClient client = new TestFAIMSClient();
				client.setProjectsCount(count);
				client.setProjectsResultCode(fetchCode);
				client.setDownloadResultCode(downloadCode);
				return client;
			}
			
		};
	}
	
}
