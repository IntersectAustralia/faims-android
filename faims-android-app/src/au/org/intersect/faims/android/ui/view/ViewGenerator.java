package au.org.intersect.faims.android.ui.view;

import au.org.intersect.faims.android.data.FormInputDef;

public class ViewGenerator {

	protected String ref;
	private FormInputDef attribute;
	protected String style;
	
	public ViewGenerator() {
	}

	public ViewGenerator(String ref, FormInputDef attribute, String style) {
		this.ref = ref;
		this.attribute = attribute;
		this.style = style;
	}

	public String getRef() {
		return this.ref;
	}
	
	public FormInputDef attribute() {
		return this.attribute;
	}

	public String getStyle() {
		return this.style;
	}

}
