package au.org.intersect.faims.android.data;

import java.util.Vector;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.form.api.FormEntryPrompt;

import au.org.intersect.faims.android.ui.view.Tab;

public class FormAttribute {
	
	public int controlType;
	public int dataType;
	public String questionText;
	public String questionType;
	public String questionAppearance;
	public String name;
	public String type;
	public boolean sync;
	public boolean certainty;
	public boolean annotation;
	public boolean readOnly;
	public boolean info;
	public boolean map;
	public boolean table;
	public boolean web;
	public Vector<SelectChoice> selectChoices;

	public static FormAttribute parseFromInput(FormEntryPrompt input) {
		FormAttribute attribute = new FormAttribute();
		attribute.controlType = input.getControlType();
		attribute.dataType = input.getDataType();
		attribute.questionText = input.getQuestionText();
		attribute.questionType = input.getQuestion().getAdditionalAttribute(null, "type");
		attribute.questionAppearance = input.getQuestion().getAppearanceAttr();
		attribute.name = input.getQuestion().getAdditionalAttribute(null, "faims_attribute_name");
		attribute.type = input.getQuestion().getAdditionalAttribute(null, "faims_attribute_type");
		if (attribute.type == null) attribute.type = Tab.FREETEXT;
		attribute.certainty = !"false".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_certainty"));
		attribute.annotation = !"false".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_annotation"));
		attribute.info = !"false".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_info"));
		attribute.readOnly = "true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_read_only"));
		attribute.map = "true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_map"));
		attribute.table = "true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_table"));
		attribute.web = "true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_web"));
		attribute.sync = "true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_sync"));
		attribute.selectChoices = input.getSelectChoices();
		return attribute;
	}

}
