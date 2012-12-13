package au.org.intersect.faims.android.data;

import com.google.gson.JsonObject;

public class ProjectArchive {

	public String filename;
	public long size;
	public String md5;
	
	public static ProjectArchive fromJson(JsonObject object) {
		ProjectArchive p = new ProjectArchive();
		p.filename = object.get("file").getAsString();
		p.size = object.get("size").getAsLong();
		p.md5 = object.get("md5").getAsString();
		return p;
	}
}
