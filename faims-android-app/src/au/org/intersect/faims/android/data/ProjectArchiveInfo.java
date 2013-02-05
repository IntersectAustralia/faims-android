package au.org.intersect.faims.android.data;

import com.google.gson.JsonObject;

public class ProjectArchiveInfo {

	public String filename;
	public long size;
	public String md5;
	
	public static ProjectArchiveInfo fromJson(JsonObject object) {
		ProjectArchiveInfo p = new ProjectArchiveInfo();
		p.filename = object.get("file").getAsString();
		p.size = object.get("size").getAsLong();
		p.md5 = object.get("md5").getAsString();
		return p;
	}
}
