package au.org.intersect.faims.android.projects;

import java.util.ArrayList;
import java.util.List;

public class ProjectUtilsImpl implements IProjectUtils {

	@Override
	public List<String> getProjectList() {
		
		List<String> projectList = new ArrayList<String>();
		
		projectList.add("Project One");
		projectList.add("Project Two");
		projectList.add("Project Three");
		
		return projectList;
	}

}
