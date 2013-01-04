package au.org.intersect.faims.android.ui.form;

public class NameValuePair{
	
	private String name = "";
	private String value = "";
	
	public NameValuePair(String name, String value){
		this.name = name;
		this.value = value;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getValue(){
		return this.value;
	}
	
	public String toJSON(){
		return "{\"name\":\"" + this.name + "\",\"value\":\"" + this.value + "\"}";
	}
}
