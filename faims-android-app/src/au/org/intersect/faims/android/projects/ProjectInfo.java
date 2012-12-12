package au.org.intersect.faims.android.projects;

import com.google.gson.JsonObject;

public class ProjectInfo {

	public String name;
	public String id;
	
	public static ProjectInfo fromJson(JsonObject object) {
		ProjectInfo p = new ProjectInfo();
		p.name = object.get("name").getAsString();
		p.name = object.get("id").getAsString();
		return p;
	}
	
}
