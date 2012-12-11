package au.org.intersect.faims.android.net;

import java.util.List;

import au.org.intersect.faims.android.projects.ProjectInfo;

public interface IFAIMSClient {

	void fetchProjectList(FAIMSResponseHandler<List<ProjectInfo>> handler);
	void downloadProjectArchive(FAIMSResponseHandler<String> handler);
}
