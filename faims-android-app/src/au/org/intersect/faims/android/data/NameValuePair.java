package au.org.intersect.faims.android.data;

import java.io.Serializable;

public class NameValuePair implements Serializable{
	
	private static final long serialVersionUID = -3844093092418319974L;

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
	
	public String toString() {
		return name;
	}
	
	public String toJSON(){
		return "{\"name\":\"" + this.name + "\",\"value\":\"" + this.value + "\"}";
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof NameValuePair){
			NameValuePair other = (NameValuePair) o;
			return this.getName().equals(other.getName()) && this.getValue().equals(other.getValue());
		}
		return false;
	}
}

