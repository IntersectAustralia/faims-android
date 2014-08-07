package au.org.intersect.faims.android.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.form.api.FormEntryPrompt;

public class FormInputDef implements Serializable {
	
	private static final long serialVersionUID = 100808462162745802L;
	
	public static String INTEGER_FIELD = "integer";
	public static String DECIMAL_FIELD = "decimal";
	public static String LONG_FIELD = "long";
	public static String TEXT_AREA_FIELD = "textarea";
	
	public static String IMAGE_TYPE = "image";
	public static String RADIO_TYPE = "full";
	public static String LIST_TYPE = "compact";
	public static String CAMERA_TYPE = "camera";
	public static String VIDEO_TYPE = "video";
	public static String FILE_TYPE = "file";
	
	public int controlType;
	public int dataType;
	public String questionText;
	public String questionType;
	public String questionAppearance;
	public String name;
	public String type;
	public String styleClass;
	public boolean sync;
	public boolean certainty;
	public boolean annotation;
	public boolean readOnly;
	public boolean info;
	public boolean map;
	public boolean table;
	public boolean web;
	public ArrayList<NameValuePair> selectChoices;
	
	public FormInputDef() {
		type = Attribute.FREETEXT;
	}
	
	public FormInputDef createTextField() {
		this.controlType = Constants.CONTROL_INPUT;
		this.dataType = Constants.NO_SELECTION;
		return this;
	}
	
	public FormInputDef createTextField(String type) {
		this.controlType = Constants.CONTROL_INPUT;
		if (INTEGER_FIELD.equals(type)) {
			this.dataType = Constants.DATATYPE_INTEGER;
		} else if (DECIMAL_FIELD.equals(type)) {
			this.dataType = Constants.DATATYPE_DECIMAL;
		} else if (LONG_FIELD.equals(type)) {
			this.dataType = Constants.DATATYPE_LONG;
		} else if (TEXT_AREA_FIELD.equals(type)) {
			this.dataType = Constants.DATATYPE_TEXT;
		} else {
			this.dataType = Constants.NO_SELECTION;
		}
		return this;
	}
	
	public FormInputDef createDatePicker() {
		this.controlType = Constants.CONTROL_INPUT;
		this.dataType = Constants.DATATYPE_DATE;
		return this;
	}
	
	public FormInputDef createTimePicker() {
		this.controlType = Constants.CONTROL_INPUT;
		this.dataType = Constants.DATATYPE_TIME;
		return this;
	}
	
	public FormInputDef createMap() {
		this.controlType = Constants.CONTROL_INPUT;
		this.dataType = Constants.NO_SELECTION;
		this.map = true;
		return this;
	}
	
	public FormInputDef createTable() {
		this.controlType = Constants.CONTROL_INPUT;
		this.dataType = Constants.NO_SELECTION;
		this.table = true;
		return this;
	}
	
	public FormInputDef createWebView() {
		this.controlType = Constants.CONTROL_INPUT;
		this.dataType = Constants.NO_SELECTION;
		this.web = true;
		return this;
	}
	
	public FormInputDef createPictureGallery() {
		this.controlType = Constants.CONTROL_SELECT_ONE;
		this.dataType = Constants.DATATYPE_CHOICE;
		this.questionType = IMAGE_TYPE;
		return this;
	}
	
	public FormInputDef createRadioGroup() {
		this.controlType = Constants.CONTROL_SELECT_ONE;
		this.dataType = Constants.DATATYPE_CHOICE;
		this.questionAppearance = RADIO_TYPE;
		return this;
	}
	
	public FormInputDef createList() {
		this.controlType = Constants.CONTROL_SELECT_ONE;
		this.dataType = Constants.DATATYPE_CHOICE;
		this.questionAppearance = LIST_TYPE;
		return this;
	}
	
	public FormInputDef createDropDown() {
		this.controlType = Constants.CONTROL_SELECT_ONE;
		this.dataType = Constants.DATATYPE_CHOICE;
		return this;
	}
	
	public FormInputDef createCameraGallery(boolean sync) {
		this.controlType = Constants.CONTROL_SELECT_MULTI;
		this.dataType = Constants.DATATYPE_CHOICE_LIST;
		this.questionType = CAMERA_TYPE;
		this.sync = sync;
		return this;
	}
	
	public FormInputDef createVideoGallery(boolean sync) {
		this.controlType = Constants.CONTROL_SELECT_MULTI;
		this.dataType = Constants.DATATYPE_CHOICE_LIST;
		this.questionType = VIDEO_TYPE;
		this.sync = sync;
		return this;
	}
	
	public FormInputDef createFileGroup(boolean sync) {
		this.controlType = Constants.CONTROL_SELECT_MULTI;
		this.dataType = Constants.DATATYPE_CHOICE_LIST;
		this.questionType = FILE_TYPE;
		this.sync = sync;
		return this;
	}
	
	public FormInputDef createCheckboxGroup() {
		this.controlType = Constants.CONTROL_SELECT_MULTI;
		this.dataType = Constants.DATATYPE_CHOICE_LIST;
		return this;
	}
	
	public FormInputDef createButton() {
		this.controlType = Constants.CONTROL_TRIGGER;
		return this;
	}
	
	public FormInputDef setLabel(String label) {
		this.questionText = label;
		return this;
	}
	
	public FormInputDef setAttributeName(String name) {
		this.name = name;
		return this;
	}
	
	public FormInputDef setAttributeType(String type) {
		this.type = type;
		return this;
	}
	
	public FormInputDef setCertaintyEnabled(boolean enabled) {
		this.certainty = enabled;
		return this;
	}
	
	public FormInputDef setAnnotationEnabled(boolean enabled) {
		this.annotation = enabled;
		return this;
	}
	
	public FormInputDef setInfoEnabled(boolean enabled) {
		this.info = enabled;
		return this;
	}
	
	public FormInputDef setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}
	
	public FormInputDef setStyleCss(String style) {
		this.styleClass = style;
		return this;
	}
	
	public FormInputDef addChoice(String name, String value) {
		if (this.selectChoices == null) {
			this.selectChoices = new ArrayList<NameValuePair>();
		}
		this.selectChoices.add(new NameValuePair(name, value));
		return this;
	}

	public static FormInputDef parseFromInput(FormEntryPrompt input) {
		FormInputDef inputDef = new FormInputDef();
		
		inputDef.controlType = input.getControlType();
		inputDef.dataType = input.getDataType();
		inputDef.questionText = input.getQuestionText();
		inputDef.questionType = input.getQuestion().getAdditionalAttribute(null, "type");
		inputDef.questionAppearance = input.getQuestion().getAppearanceAttr();
		inputDef.map = "true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_map"));
		inputDef.table = "true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_table"));
		inputDef.web = "true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_web"));
		inputDef.sync = "true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_sync"));
		
		inputDef.name = input.getQuestion().getAdditionalAttribute(null, "faims_attribute_name");
		inputDef.type = input.getQuestion().getAdditionalAttribute(null, "faims_attribute_type");
		inputDef.styleClass = input.getQuestion().getAdditionalAttribute(null, "faims_style_class");
		if (inputDef.type == null) inputDef.type = Attribute.FREETEXT;
		
		inputDef.certainty = !"false".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_certainty"));
		inputDef.annotation = !"false".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_annotation"));
		
		inputDef.info = !"false".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_info"));
		inputDef.readOnly = "true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_read_only"));
		
		Vector<SelectChoice> choices = input.getSelectChoices();
		if (choices != null) {
			ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
			for (SelectChoice choice : choices) {
				pairs.add(new NameValuePair(choice.getLabelInnerText(), choice.getValue()));
			}
			inputDef.selectChoices = pairs;
		}
		return inputDef;
	}

}
