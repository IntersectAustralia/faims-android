package au.org.intersect.faims.android.data;

import java.io.Serializable;

import com.google.gson.JsonObject;

public class Project implements Serializable {

	private static final long serialVersionUID = -6351485413843633422L;
	
	public String name;
	public String key;
	public String dir;
	public int version;
	public String timestamp;
	
	public Project() {
		
	}
	
	public Project(String dir) {
		this.dir = dir;
	}
	
	public Project(String dir, String name, String key) {
		this.dir = dir;
		this.name = name;
		this.key = key;
	}
	
	public static Project fromJson(String dir, JsonObject object) {
		Project p = new Project(dir);
		if (object.has("name"))	p.name = object.get("name").getAsString();
		if (object.has("key")) p.key = object.get("key").getAsString();
		if (object.has("version")) p.version = object.get("version").getAsInt();
		if (object.has("timestamp")) p.timestamp = object.get("timestamp").getAsString();
		return p;
	}
	
	public static Project fromJson(JsonObject object) {
		Project p = new Project();
		if (object.has("name"))	p.name = object.get("name").getAsString();
		if (object.has("key")) p.key = object.get("key").getAsString();
		if (p.name != null) p.dir = p.name.replace("\\s+", "_");
		if (object.has("version")) p.version = object.get("version").getAsInt();
		if (object.has("timestamp")) p.timestamp = object.get("timestamp").getAsString();
		return p;
	}

	public Object toJson() {
		JsonObject object = new JsonObject();
		object.addProperty("name", name);
		object.addProperty("key",  key);
		object.addProperty("dir", dir);
		object.addProperty("version", version);
		object.addProperty("timestamp", timestamp);
		return object;
	}
	
}
