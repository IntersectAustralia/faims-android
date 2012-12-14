package au.org.intersect.faims.android.net;

import java.util.LinkedList;

import au.org.intersect.faims.android.data.Project;

public interface IFAIMSClient {

	public FAIMSClientResultCode fetchProjectList(LinkedList<Project> projects);
	
	public FAIMSClientResultCode downloadProjectArchive(Project project);
	
}
