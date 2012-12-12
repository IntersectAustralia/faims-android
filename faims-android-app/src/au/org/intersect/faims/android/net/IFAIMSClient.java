package au.org.intersect.faims.android.net;

import java.util.List;

import au.org.intersect.faims.android.projects.ProjectInfo;

public interface IFAIMSClient {
	
	public interface FAIMClientListener<T> {
		void handleResponse(int resultCode, T content);
	}

	void fetchProjectList(FAIMClientListener<List<ProjectInfo>> handler);
	
	void downloadProjectArchive(ProjectInfo project, FAIMClientListener<ProjectInfo> handler);
}
