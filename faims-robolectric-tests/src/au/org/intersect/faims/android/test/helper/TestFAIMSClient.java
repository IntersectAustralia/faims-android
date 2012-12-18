package au.org.intersect.faims.android.test.helper;

import java.util.LinkedList;

import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.IFAIMSClient;

public class TestFAIMSClient implements IFAIMSClient {

	@Override
	public FAIMSClientResultCode fetchProjectList(LinkedList<Project> projects) {
		for (int i = 0; i < 10; i++) {
			projects.add(new Project("Project " + i, String.valueOf(i)));
		}
		return FAIMSClientResultCode.SUCCESS;
	}

	@Override
	public FAIMSClientResultCode downloadProjectArchive(Project project) {
		
		return FAIMSClientResultCode.SUCCESS;
	}

}
