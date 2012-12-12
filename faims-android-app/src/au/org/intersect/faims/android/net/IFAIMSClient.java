package au.org.intersect.faims.android.net;

import java.util.LinkedList;

import au.org.intersect.faims.android.projects.ProjectInfo;

public interface IFAIMSClient {
	
	public interface FAIMClientListener<T> {
		void handleResponse(boolean success, T content);
	}

	void fetchProjectList(FAIMClientListener<LinkedList<ProjectInfo>> handler);
	void downloadProjectArchive(FAIMClientListener<ProjectInfo> handler, ProjectInfo project);
}
