package au.org.intersect.faims.android.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
	private String moduleName;
	
	public void init(String path, String moduleName, String propertiesFile) {
		this.properties = new HashMap<String, String>();
		this.path = path;
		this.moduleName = moduleName;
		generatePropertiesMap(propertiesFile);
	}

	private void generatePropertiesMap(String propertiesFile) {
		try {
			FileInputStream fileInputStream = new FileInputStream(path+"/"+propertiesFile);
			PropertyResourceBundle propertyResourceBundle = new PropertyResourceBundle(fileInputStream);
			for(String s : propertyResourceBundle.keySet()){
				properties.put(s, new String(propertyResourceBundle.getString(s).getBytes("ISO-8859-1"),"UTF-8"));
			}
		} catch (FileNotFoundException e) {
			FLog.d("Required faims.properties is not found in the module");
		} catch (IOException e) {
		}
		
		try{
			FileInputStream fileInputStream = new FileInputStream(path+"/faims_"+moduleName.replaceAll("\\s", "_")+".properties");
			PropertyResourceBundle propertyResourceBundle = new PropertyResourceBundle(fileInputStream);
			for(String s : propertyResourceBundle.keySet()){
				properties.put(s, new String(propertyResourceBundle.getString(s).getBytes("ISO-8859-1"),"UTF-8"));
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
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
