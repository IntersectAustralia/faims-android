package au.org.intersect.faims.android.data;

import org.json.JSONException;
import org.json.JSONObject;

public class FileInfo {

	public String filename;
	public String md5;
	public long size;
	public String type;
	public String state;
	public String timestamp;
	public boolean deleted;
	public String thumbnailFilename;
	public String thumbnailMD5;
	public long thumbnailSize;
	
	public void parseJson(JSONObject object) throws JSONException {
		filename = object.getString("file");
		md5 = object.getString("md5");
		size = object.getLong("size");
	}
	
	public static FileInfo fromJson(JSONObject object) throws JSONException {
		FileInfo p = new FileInfo();
		p.parseJson(object);
		return p;
	}
}
