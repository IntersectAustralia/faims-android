package au.org.intersect.faims.android.data;

import java.io.Serializable;

import com.google.gson.JsonObject;

public class Project implements Serializable {

	private static final long serialVersionUID = -6351485413843633422L;
	
	public String name;
	public String id;
	public String dir;
	
	public Project() {
		
	}
	
	public Project(String dir) {
		this.dir = dir;
	}
	
	public Project(String dir, String name, String id) {
		this.name = name;
		this.id = id;
	}
	
	public static Project fromJson(String dir, JsonObject object) {
		Project p = new Project(dir);
		if (object.has("name"))	p.name = object.get("name").getAsString();
		if (object.has("id")) p.id = object.get("id").getAsString();
		return p;
	}
	
	public static Project fromJson(JsonObject object) {
		Project p = new Project();
		if (object.has("name"))	p.name = object.get("name").getAsString();
		if (object.has("id")) p.id = object.get("id").getAsString();
		if (p.name != null) p.dir = p.name.replace("\\s+", "_");
		return p;
	}
	
}
