package au.org.intersect.faims.android.data;

import com.google.gson.JsonObject;

public class FileInfo {

	public String filename;
	public long size;
	public String md5;
	
	public static FileInfo fromJson(JsonObject object) {
		FileInfo p = new FileInfo();
		p.filename = object.get("file").getAsString();
		p.size = object.get("size").getAsLong();
		p.md5 = object.get("md5").getAsString();
		return p;
	}
}
