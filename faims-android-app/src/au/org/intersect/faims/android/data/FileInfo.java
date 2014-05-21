package au.org.intersect.faims.android.data;

import org.json.JSONException;
import org.json.JSONObject;

public class FileInfo {

	public String filename;
	public long size;
	public String md5;
	
	public void parseJson(JSONObject object) throws JSONException {
		filename = object.getString("file");
		size = object.getLong("size");
		md5 = object.getString("md5");
	}
	
	public static FileInfo fromJson(JSONObject object) throws JSONException {
		FileInfo p = new FileInfo();
		p.parseJson(object);
		return p;
	}
}
