package au.org.intersect.faims.android.ui.view;

import au.org.intersect.faims.android.data.FormAttribute;

public class ViewGenerator {

	protected String ref;
	protected String name;
	private FormAttribute attribute;
	protected String style;
	
	public ViewGenerator() {
		
	}

	public ViewGenerator(String ref, String name, FormAttribute attribute, String style) {
		this.ref = ref;
		this.name = name;
		this.attribute = attribute;
		this.style = style;
	}

	public String getRef() {
		return this.ref;
	}
	
	public String getName() {
		return this.name;
	}
	
	public FormAttribute attribute() {
		return this.attribute;
	}

	public String getStyle() {
		return this.style;
	}

}
