package au.org.intersect.faims.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.org.intersect.faims.android.log.FLog;

import com.google.inject.Singleton;

@Singleton
public class Arch16n {

	private Map<String,String> properties;
	private String path;
	
	public void init(String path, String propertiesFile) {
		this.properties = new HashMap<String, String>();
		this.path = path;
		generatePropertiesMap(propertiesFile);
	}
	
	public void destroy() {
		this.properties = null;
		this.path = null;
	}

	private void generatePropertiesMap(String propertiesFile) {
		try {
			if (!new File(path+"/"+propertiesFile).exists()) return;
			FileInputStream fileInputStream = new FileInputStream(path+"/"+propertiesFile);
			PropertyResourceBundle propertyResourceBundle = new PropertyResourceBundle(fileInputStream);
			for(String s : propertyResourceBundle.keySet()){
				properties.put(s, new String(propertyResourceBundle.getString(s).getBytes("ISO-8859-1"),"UTF-8"));
			}
		} catch (Exception e) {
			FLog.e("error trying to read properties file", e);
		}
	}

	public String getProperties(String property){
		return this.properties.get(property);
	}

	public String substituteValue(String value){
		if (value == null) return value;
		
		Pattern pattern = Pattern.compile("\\{(.+?)\\}");
	    Matcher matcher = pattern.matcher(value);
	    StringBuffer buffer = new StringBuffer();

	    while (matcher.find()) {
	    	String replacement = getProperties(matcher.group(1));
	    	if (replacement != null) {
	    		matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
	    	}
	    }
	    matcher.appendTail(buffer);
	    return buffer.toString();
	}
}
