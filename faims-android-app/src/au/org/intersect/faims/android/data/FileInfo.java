package au.org.intersect.faims.android.data;

import com.google.gson.JsonObject;

public class FileInfo {

	public String filename;
	public long size;
	public String md5;
	public String version;
	public String timestamp;
	
	public void parseJson(JsonObject object) {
		filename = object.get("file").getAsString();
		size = object.get("size").getAsLong();
		md5 = object.get("md5").getAsString();
		if (object.has("version")) version = object.get("version").getAsString();
		if (object.has("timestamp")) timestamp = object.get("timestamp").getAsString();
	}
	
	public static FileInfo fromJson(JsonObject object) {
		FileInfo p = new FileInfo();
		p.parseJson(object);
		return p;
	}
}
