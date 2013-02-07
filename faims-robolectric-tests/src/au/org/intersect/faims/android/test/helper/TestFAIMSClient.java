package au.org.intersect.faims.android.test.helper;

import java.util.LinkedList;

import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

import com.google.inject.Provider;

public class TestFAIMSClient extends FAIMSClient {
	
	private int projectsCount = 0;
	private FAIMSClientResultCode projectsCode;
	private FAIMSClientResultCode downloadCode;

	@Override
	public FAIMSClientResultCode fetchProjectList(LinkedList<Project> projects) {
		for (int i = 0; i < projectsCount; i++) {
			projects.add(new Project(String.valueOf(i).replace("\\s+", "_"), "Project " + i, String.valueOf(i)));
		}
		return projectsCode;
	}

	@Override
	public FAIMSClientResultCode downloadProject(String projectId) {
		
		return downloadCode;
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
