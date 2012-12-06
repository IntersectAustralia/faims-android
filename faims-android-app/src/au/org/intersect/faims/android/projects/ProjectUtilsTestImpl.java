package au.org.intersect.faims.android.projects;

import java.util.ArrayList;
import java.util.List;

public class ProjectUtilsTestImpl implements IProjectUtils {

	@Override
	public List<String> getProjectList() {
		
		List<String> projectList = new ArrayList<String>();
		
		projectList.add("Test Project One");
		projectList.add("Test Project Two");
		projectList.add("Test Project Three");
		
		return projectList;
	}

}
