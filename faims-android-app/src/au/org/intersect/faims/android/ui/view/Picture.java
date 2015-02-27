package au.org.intersect.faims.android.ui.view;

import java.io.Serializable;

import au.org.intersect.faims.android.util.Compare;

public class Picture implements Serializable{

	private static final long serialVersionUID = -7547795077196975425L;

	private String id;
	private String name;
	private String url;

	public Picture(String id, String name, String url){
		this.id = id;
		this.name = name;
		this.url = url;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Picture){
			Picture other = (Picture) o;
			return Compare.equal(getId(), other.getId());
		}
		return super.equals(o);
	}
}
