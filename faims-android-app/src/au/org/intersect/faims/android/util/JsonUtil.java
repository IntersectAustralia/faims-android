package au.org.intersect.faims.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {

	public static String serializeServerPacket(String ip, String port) throws JSONException {
		JSONObject object = new JSONObject();
    	object.put("android_ip", ip);
    	object.put("android_port", port);
    	return object.toString();
	}
	
	public static JSONObject deserializeServerPacket(String json) throws IOException, JSONException {
		return deserializeJsonString(json);
	}
	
	public static JSONObject deserializeJsonString(String json) throws IOException, JSONException {
		return new JSONObject(json); 
	}
	
	public static JSONObject deserializeJsonObject(InputStream stream) throws IOException, JSONException {
        return new JSONObject(streamToString(stream));
	}
	
	public static JSONArray deserializeJsonArray(InputStream stream) throws IOException, JSONException {
		return new JSONArray(streamToString(stream));
	}
	
	private static String streamToString(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream);
        StringBuilder sb = new StringBuilder();
        int value;
        while((value = reader.read()) > 0)
            sb.append((char) value);
        return sb.toString();
	}

}
