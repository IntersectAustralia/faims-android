package au.org.intersect.faims.android.ui.form;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;
import au.org.intersect.faims.android.util.DateUtil;

/**
 * Class that reads the ui defintion file and render the UI
 * 
 * @author danielt
 * 
 */
public class UIRenderer implements IRestoreActionListener{

    private FormEntryController fem;
    
    private WeakReference<ShowProjectActivity> activityRef;
    
    private HashMap<String, TabGroup> tabGroupMap;
    private LinkedList<TabGroup> tabGroupList;
    
    private HashMap<String, Tab> tabMap;
    private LinkedList<Tab> tabList;
    
    private HashMap<String, View> viewMap;
    private HashMap<String, Tab> viewTabMap; 
    private LinkedList<View> viewList;
    
    private Arch16n arch16n;
    
    protected List<Integer> indexes;
    
    private TabGroup currentTabGroup;
    
    private Map<String, Tab> tabReferenceMap;
    
    private String currentTabLabel;
    
	private Map<String, Object> viewValues;
	private Map<String, Object> viewCertainties;
	private Map<String, Object> viewAnnotations;
	private Map<String, String> viewDirtyReasons;

	private Map<String,Map<String,String>> styles;

	public UIRenderer(FormEntryController fem, Arch16n arch16n, ShowProjectActivity activity) {
        this.fem = fem;
        this.arch16n = arch16n;
        this.activityRef = new WeakReference<ShowProjectActivity>(activity);
        this.tabGroupMap = new HashMap<String, TabGroup>();
        this.tabGroupList = new LinkedList<TabGroup>(); 
        this.viewTabMap = new HashMap<String, Tab>();
        this.tabMap = new HashMap<String, Tab>();
        this.tabList = new LinkedList<Tab>(); 
        this.viewMap = new HashMap<String, View>();
        this.viewList = new LinkedList<View>();
        this.viewValues = new HashMap<String, Object>();
        this.viewCertainties = new HashMap<String, Object>();
        this.viewAnnotations = new HashMap<String, Object>();
        this.viewDirtyReasons = new HashMap<String, String>();
        this.styles = new HashMap<String, Map<String,String>>();
    }

    /**
     * Render the tabs and questions inside the tabs
     * 
     */
    public void createUI(String directory) {
    	
    	FormIndex currentIndex = this.fem.getModel().getFormIndex();
    	
    	/*
    	if (currentIndex.isBeginningOfFormIndex()) {
            currentIndex = this.fem.getModel().incrementIndex(currentIndex, true);
        }
    	*/
    	
    	IFormElement element = this.fem.getModel().getForm().getChild(currentIndex);
    	FormIndex groupIndex = this.fem.getModel().incrementIndex(currentIndex, true);
    	
    	int groups = element.getChildren().size();
    	for (int i = 0; i < groups; i++) {
    		
    		element = this.fem.getModel().getForm().getChild(groupIndex);
	    	if (element instanceof GroupDef) {
	    		
	    		GroupDef tabGroupElement = (GroupDef) element;
	    		FormEntryCaption tabGroupCaption = this.fem.getModel().getCaptionPrompt(groupIndex);
	    		
	    		String tabGroupName = tabGroupCaption.getIndex().getReference().getNameLast();

	    		if("style".equals(tabGroupName)){
	    			parseStyle(tabGroupElement,groupIndex);
	    		}else{
	    			parseTabGroups(directory, groupIndex, tabGroupElement, tabGroupCaption, tabGroupName);
	    		}
	    	
	    	groupIndex = this.fem.getModel().incrementIndex(groupIndex, false);
	    	}
    	}
    	
    }

	private void parseTabGroups(String directory, FormIndex groupIndex, GroupDef tabGroupElement, FormEntryCaption tabGroupCaption, String tabGroupName) {
		IFormElement element;
		String archEntType = tabGroupCaption.getFormElement().getAdditionalAttribute(null, "faims_archent_type");
		String relType = tabGroupCaption.getFormElement().getAdditionalAttribute(null, "faims_rel_type");

		TabGroup tabGroup = new TabGroup(archEntType, relType, this);
		tabGroup.setActivity(activityRef);
		String tabGroupText = tabGroupCaption.getQuestionText();
		tabGroupText = arch16n.substituteValue(tabGroupText);
		tabGroup.setLabel(tabGroupText);

		tabGroupMap.put(tabGroupName, tabGroup);
		tabGroupList.add(tabGroup);

		// descend into group
		FormIndex tabIndex = this.fem.getModel().incrementIndex(groupIndex,true);

		int tabs = tabGroupElement.getChildren().size();
		for (int i = 0; i < tabs; i++) {
			element = this.fem.getModel().getForm().getChild(tabIndex);

			if (element instanceof GroupDef) {
				parseTab(directory, tabGroupName, element, tabGroup, tabIndex);
			}

			tabIndex = this.fem.getModel().incrementIndex(tabIndex, false);
		}
	}

	private void parseTab(String directory, String tabGroupName, IFormElement element, TabGroup tabGroup, FormIndex tabIndex) {
		GroupDef tabElement = (GroupDef) element;
		FormEntryCaption tabCaption = this.fem.getModel().getCaptionPrompt(tabIndex);

		String tabName = tabCaption.getIndex().getReference().getNameLast();
		Tab tab = tabGroup.createTab(tabName, tabCaption.getQuestionText(),"true".equals(tabElement.getAdditionalAttribute(null,
						"faims_hidden")), !"false".equals(tabElement.getAdditionalAttribute(null, "faims_scrollable")), arch16n, tabGroupName + "/" + tabName);

		tabMap.put(tabGroupName + "/" + tabName, tab);
		tabList.add(tab);

		FormIndex containerIndex = this.fem.getModel().incrementIndex(tabIndex, true);

		for (int i = 0; i < tabElement.getChildren().size(); i++) {
			element = this.fem.getModel().getForm().getChild(containerIndex);

			if (element instanceof GroupDef) {
				parseContainer(null, directory, tabGroupName, tabName, element,tabGroup, containerIndex, tab, 1);
			} else {
				parseInput(directory, tabGroupName, tabName, element, tabGroup,tab, containerIndex, null);
			}
			containerIndex = this.fem.getModel().incrementIndex(containerIndex, false);
		}
	}

	private void parseContainer(LinearLayout containerLayout, String directory, String tabGroupName, String tabName, IFormElement element,
			TabGroup tabGroup, FormIndex childIndex, Tab tab, int depth) {
		if (depth > 5) {

			AlertDialog.Builder builder = new AlertDialog.Builder(this.activityRef.get());
			builder.setTitle("Parsing Error");
			builder.setMessage("Child depth can not be more than 5");
			builder.setNeutralButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// User clicked OK button
						}
					});
			builder.create().show();

			return;
		}
		GroupDef childContainerElement = (GroupDef) element;
		FormIndex inputIndex = this.fem.getModel().incrementIndex(childIndex,true);
		String style = childContainerElement.getAdditionalAttribute(null,"faims_style");
		LinearLayout childContainerLayout = tab.addChildContainer(containerLayout, getStyleMappings(style));

		for (int i = 0; i < childContainerElement.getChildren().size(); i++) {
			element = this.fem.getModel().getForm().getChild(inputIndex);
			if (element instanceof GroupDef) {
				parseContainer(childContainerLayout, directory, tabGroupName,tabName, element, tabGroup, inputIndex, tab, ++depth);
			} else {
				parseInput(directory, tabGroupName, tabName, element, tabGroup,tab, inputIndex, childContainerLayout);
			}
			inputIndex = this.fem.getModel().incrementIndex(inputIndex, false);
		}
	}

	private List<Map<String, String>> getStyleMappings(String style) {
		List<Map<String, String>> styleMappings = new ArrayList<Map<String, String>>();
		if (style != null) {
			String[] styles = style.split(" ");
			for (String s : styles) {
				styleMappings.add(this.styles.get(s));
			}
		}
		return styleMappings;
	}

	private void parseInput(String directory, String tabGroupName, String tabName, IFormElement element, TabGroup tabGroup, Tab tab,
			FormIndex childIndex, LinearLayout containerLayout) {
		QuestionDef questionElement = (QuestionDef) element;
		String style = questionElement.getAdditionalAttribute(null,"faims_style");
		FormEntryPrompt input = this.fem.getModel().getQuestionPrompt(childIndex);
		String viewName = input.getIndex().getReference().getNameLast();
		View view = tab.addInput(containerLayout, FormAttribute.parseFromInput(input), tabGroupName + "/"+ tabName + "/" + viewName, viewName, directory,
				tabGroup.isArchEnt(), tabGroup.isRelationship(),getStyleMappings(style));

		viewMap.put(tabGroupName + "/" + tabName + "/" + viewName, view);
		viewTabMap.put(tabGroupName + "/" + tabName + "/" + viewName, tab);
		viewList.add(view);
	}

	private void parseStyle(GroupDef tabGroupElement, FormIndex groupIndex) {
		FormIndex tabIndex = this.fem.getModel().incrementIndex(groupIndex,true);

		int tabs = tabGroupElement.getChildren().size();
		for (int i = 0; i < tabs; i++) {
			IFormElement element = this.fem.getModel().getForm().getChild(tabIndex);
			if (element instanceof GroupDef) {
				GroupDef tabElement = (GroupDef) element;
				FormEntryCaption tabCaption = this.fem.getModel().getCaptionPrompt(tabIndex);
				String styleName = tabCaption.getIndex().getReference().getNameLast();
				FormIndex inputIndex = this.fem.getModel().incrementIndex(tabIndex, true);
				Map<String, String> attributes = new HashMap<String, String>();
				for (int j = 0; j < tabElement.getChildren().size(); j++) {
					FormEntryPrompt input = this.fem.getModel().getQuestionPrompt(inputIndex);
					String attributeName = input.getIndex().getReference().getNameLast();
					String attributeValue = input.getQuestionText();
					attributes.put(attributeName, attributeValue);
					inputIndex = this.fem.getModel().incrementIndex(inputIndex,false);
				}
				styles.put(styleName, attributes);
			}

			tabIndex = this.fem.getModel().incrementIndex(tabIndex, false);
		}
	}

    public TabGroup showTabGroup(FragmentActivity activity, int index) {
    	FragmentManager fm = activity.getSupportFragmentManager();
    	
    	TabGroup tabGroup = this.tabGroupList.get(index);
    	if (tabGroup == null) return null;
    	invalidateListViews(tabGroup);
    	if (tabGroup == this.currentTabGroup) return currentTabGroup;
	    
	    FragmentTransaction ft = fm.beginTransaction();
        if(this.currentTabGroup == null){
        	ft.add(R.id.fragment_content, tabGroup, tabGroup.getGroupTag());
        }else{
        	ft.replace(R.id.fragment_content, tabGroup, tabGroup.getGroupTag());
        	ft.addToBackStack(currentTabGroup.getGroupTag());
        }
        this.currentTabGroup = tabGroup;
        ft.commit();
        
        return tabGroup;
    }
    
    public TabGroup showTabGroup(FragmentActivity activity, String name) {
    	FragmentManager fm = activity.getSupportFragmentManager();
    	
    	TabGroup tabGroup = this.tabGroupMap.get(name);
    	if (tabGroup == null) return null;
    	invalidateListViews(tabGroup);
    	if (tabGroup == this.currentTabGroup) return currentTabGroup;
    	
    	FragmentTransaction ft = fm.beginTransaction();
	    if(this.currentTabGroup == null){
        	ft.add(R.id.fragment_content, tabGroup, tabGroup.getGroupTag());
        }else{
        	ft.replace(R.id.fragment_content, tabGroup, tabGroup.getGroupTag());
        	ft.addToBackStack(currentTabGroup.getGroupTag());
        }
	    this.currentTabGroup = tabGroup;
        ft.commit();
        
        return tabGroup;
    }

    public void invalidateListViews(TabGroup tabGroup){
    	for(Tab tab : tabGroup.getTabs()){
			for(View view : tab.getAllChildrenViews()){
				if(view instanceof CustomListView){
					CustomListView listView = (CustomListView) view;
					listView.invalidateViews();
				}
			}
		}
    }

    public View getViewByRef(String ref) {
    	return viewMap.get(ref);
    }
    
    public Tab getTabForView(String ref){
    	return viewTabMap.get(ref);
    }
    
    public TabGroup getTabGroupByLabel(String label){
    	return this.tabGroupMap.get(label);
    }
    
    public Tab getTabByLabel(String label) {
    	if (label == null) return null;
		String[] labels = label.split("/");
		if (labels.length < 2) return null;
		String group = labels[0];
		String tab = labels[1];
		TabGroup tabGroup = tabGroupMap.get(group);
		Tab currentTab = (tabGroup == null) ? null : tabGroup.getTab(tab);
		return currentTab;
    }

	public Tab showTab(String label) {
		if (label == null) return null;
		String[] labels = label.split("/");
		if (labels.length < 2) return null;
		String group = labels[0];
		String tab = labels[1];
		TabGroup tabGroup = tabGroupMap.get(group);
		Tab currentTab = (tabGroup == null) ? null : tabGroup.showTab(tab);
		return currentTab;
	}
	
	public List<Integer> getIndexes() {
		return indexes;
	}

	public void addIndexes(int index) {
		if(indexes == null){
			indexes = new ArrayList<Integer>();
		}
		this.indexes.add(index);
	}
	
	public TabGroup getCurrentTabGroup() {
		return currentTabGroup;
	}

	public void setCurrentTabGroup(TabGroup currentTabGroup) {
		this.currentTabGroup = currentTabGroup;
	}

	public Object getFieldValue(String ref) {

		try {
			Object obj = getViewByRef(ref);

			if (obj instanceof TextView) {
				TextView tv = (TextView) obj;
				return tv.getText().toString();
			} else if (obj instanceof CustomSpinner) {
				CustomSpinner spinner = (CustomSpinner) obj;
				return spinner.getValue();
			} else if (obj instanceof LinearLayout) {
				LinearLayout ll = (LinearLayout) obj;

				View child0 = ll.getChildAt(0);

				if (child0 instanceof CheckBox) {
					List<NameValuePair> valueList = new ArrayList<NameValuePair>();

					for (int i = 0; i < ll.getChildCount(); ++i) {
						View view = ll.getChildAt(i);

						if (view instanceof CustomCheckBox) {
							CustomCheckBox cb = (CustomCheckBox) view;
							if (cb.isChecked()) {
								valueList.add(new NameValuePair(cb.getValue(),
										"true"));
							}
						}
					}
					return valueList;
				} else if (child0 instanceof HorizontalScrollView) {
					
					HorizontalScrollView horizontalScrollView = (HorizontalScrollView) child0;
					View child1 = horizontalScrollView.getChildAt(0);
					if(child1 instanceof RadioGroup){
						RadioGroup rg = (RadioGroup) child1;
						String value = "";
						for (int i = 0; i < rg.getChildCount(); ++i) {
							View view = rg.getChildAt(i);
	
							if (view instanceof CustomRadioButton) {
								CustomRadioButton rb = (CustomRadioButton) view;
								if (rb.isChecked()) {
									value = rb.getValue();
									break;
								}
							}
						}
						return value;
					}
				} else {
					return null;
				}
			} else if (obj instanceof DatePicker) {
				DatePicker date = (DatePicker) obj;
				return DateUtil.getDate(date);
			} else if (obj instanceof TimePicker) {
				TimePicker time = (TimePicker) obj;
				return DateUtil.getTime(time);
			} else if (obj instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				if (!horizontalScrollView.isMulti()) {
					if(horizontalScrollView.getSelectedImageViews() != null && !horizontalScrollView.getSelectedImageViews().isEmpty()){
						return horizontalScrollView.getSelectedImageViews().get(0)
								.getPicture().getId();
					}else{
						return "";
					}
				} else {
					if (horizontalScrollView.getSelectedImageViews() != null && !horizontalScrollView.getSelectedImageViews().isEmpty()) {
						List<String> selectedPictures = new ArrayList<String>();
						for (CustomImageView imageView : horizontalScrollView
								.getSelectedImageViews()) {
							selectedPictures.add(imageView.getPicture()
									.getUrl());
						}
						return selectedPictures;
					}
					return "";
				}
			} else if (obj instanceof CustomListView) {
				CustomListView listView = (CustomListView) obj;
				if (listView.getSelectedItems() != null) {
					List<String> audios = new ArrayList<String>();
					for (Object item : listView.getSelectedItems()) {
						NameValuePair pair = (NameValuePair) item;
						audios.add(pair.getValue());
					}
					return audios;
				} else {
					return "";
				}
			} else {
				FLog.w("cannot find view " + ref);
				return null;
			}
		} catch (Exception e) {
			FLog.e("error getting field value " + ref, e);
		}
		return null;
	}

	private Object getFieldCertainty(String ref){
		
		try{
			Object obj = getViewByRef(ref);
			
			if (obj instanceof CustomEditText){
				CustomEditText tv = (CustomEditText) obj;
				return String.valueOf(tv.getCurrentCertainty());
			}
			else if (obj instanceof CustomSpinner){
				CustomSpinner spinner = (CustomSpinner) obj;
				return String.valueOf(spinner.getCurrentCertainty());
			}
			else if (obj instanceof CustomLinearLayout){
				CustomLinearLayout layout = (CustomLinearLayout) obj;
				return String.valueOf(layout.getCurrentCertainty());
			}
			else if (obj instanceof CustomDatePicker) {
				CustomDatePicker date = (CustomDatePicker) obj;
				return String.valueOf(date.getCurrentCertainty());
			} 
			else if (obj instanceof CustomTimePicker) {
				CustomTimePicker time = (CustomTimePicker) obj;
				return String.valueOf(time.getCurrentCertainty());
			}
			else if (obj instanceof CustomHorizontalScrollView){
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				return String.valueOf(horizontalScrollView.getCurrentCertainty());
			}
			else {
				// TODO show warning
				return null;
			}
		}
		catch(Exception e){
			FLog.e("error getting field certainty",e);
			// TODO show warning
			return null;
		}
	}

	private Object getFieldAnnotation(String ref){
		
		try{
			Object obj = getViewByRef(ref);
			
			if (obj instanceof CustomEditText){
				CustomEditText tv = (CustomEditText) obj;
				return tv.getCurrentAnnotation();
			}
			else if (obj instanceof CustomSpinner){
				CustomSpinner spinner = (CustomSpinner) obj;
				return spinner.getCurrentAnnotation();
			}
			else if (obj instanceof CustomLinearLayout){
				CustomLinearLayout layout = (CustomLinearLayout) obj;
				return layout.getCurrentAnnotation();
			}
			else if (obj instanceof CustomHorizontalScrollView){
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				return horizontalScrollView.getCurrentAnnotation();
			}
			else {
				// TODO show warning
				return null;
			}
		}
		catch(Exception e){
			FLog.e("error getting field annotation",e);
			// TODO show warning
			return null;
		}
	}
	
	private String getFieldDirty(String ref){
		
		try{
			Object obj = getViewByRef(ref);
			
			if (obj instanceof CustomEditText){
				CustomEditText tv = (CustomEditText) obj;
				return tv.getDirtyReason();
			}
			else if (obj instanceof CustomSpinner){
				CustomSpinner spinner = (CustomSpinner) obj;
				return spinner.getDirtyReason();
			}
			else if (obj instanceof CustomLinearLayout){
				CustomLinearLayout layout = (CustomLinearLayout) obj;
				return layout.getDirtyReason();
			}
			else if (obj instanceof CustomDatePicker) {
				CustomDatePicker date = (CustomDatePicker) obj;
				return date.getDirtyReason();
			} 
			else if (obj instanceof CustomTimePicker) {
				CustomTimePicker time = (CustomTimePicker) obj;
				return time.getDirtyReason();
			}
			else if (obj instanceof CustomHorizontalScrollView){
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				return horizontalScrollView.getDirtyReason();
			}
			else {
				// TODO show warning
				return null;
			}
		}
		catch(Exception e){
			FLog.e("error getting field annotation",e);
			// TODO show warning
			return null;
		}
	}

	public LinkedList<Tab> getTabList() {
		return tabList;
	}

	private ArrayList<NameValuePair> convertToNameValuePairs(Collection<?> valuesObj) throws Exception {
		ArrayList<NameValuePair> pairs = null;
		try {
			@SuppressWarnings("unchecked")
			List<NameValuePair> values = (List<NameValuePair>) valuesObj;
			pairs = new ArrayList<NameValuePair>();
			for (NameValuePair p : values) {
				pairs.add(new NameValuePair(arch16n
						.substituteValue(p.getName()), p.getValue()));
			}
		} catch (Exception e) {
			try {
				@SuppressWarnings("unchecked")
				List<List<String>> values = (List<List<String>>) valuesObj;
				pairs = new ArrayList<NameValuePair>();
				for (List<String> list : values) {
					pairs.add(new NameValuePair(arch16n
							.substituteValue(list.get(1)), list.get(0)));
				}
			} catch (Exception ee) {
				@SuppressWarnings("unchecked")
				List<String> values = (List<String>) valuesObj;
				pairs = new ArrayList<NameValuePair>();
				for (String value : values) {
					pairs.add(new NameValuePair(arch16n
							.substituteValue(value), arch16n
							.substituteValue(value)));
				}
			}
		}
		return pairs;
	}
	
	public void setFieldValue(String ref, Object valueObj) {
		try {
			Object obj = getViewByRef(ref);

			if (valueObj instanceof Number) {
				valueObj = valueObj.toString();
			}

			if (valueObj instanceof String) {

				String value = (String) valueObj;
				value = arch16n.substituteValue(value);

				if (obj instanceof TextView) {
					TextView tv = (TextView) obj;
					tv.setText(value);
				} else if (obj instanceof CustomSpinner) {
					CustomSpinner spinner = (CustomSpinner) obj;
					spinner.setValue(value);
				} else if (obj instanceof LinearLayout) {
					LinearLayout ll = (LinearLayout) obj;

					View child0 = ll.getChildAt(0);

					if (child0 instanceof HorizontalScrollView) {
						HorizontalScrollView horizontalScrollView = (HorizontalScrollView) child0;
						View child1 = horizontalScrollView.getChildAt(0);
						if(child1 instanceof RadioGroup){
							RadioGroup rg = (RadioGroup) child1;
							List<CustomRadioButton> buttons = new ArrayList<CustomRadioButton>();
							for (int i = 0; i < rg.getChildCount(); ++i) {
								View view = rg.getChildAt(i);
								if (view instanceof CustomRadioButton) {
									buttons.add((CustomRadioButton) view);
								}
							}
							rg.removeAllViews();
							for (CustomRadioButton rb : buttons) {
								CustomRadioButton radioButton = new CustomRadioButton(
										rg.getContext());
								radioButton.setText(rb.getText());
								radioButton.setValue(rb.getValue());
								if (rb.getValue().toString()
										.equalsIgnoreCase(value)) {
									radioButton.setChecked(true);
								}
								rg.addView(radioButton);
	
							}
						}

					} else if (child0 instanceof CheckBox) {
						for (int i = 0; i < ll.getChildCount(); ++i) {
							View view = ll.getChildAt(i);
							if (view instanceof CustomCheckBox) {
								CustomCheckBox cb = (CustomCheckBox) view;
								if (cb.getValue().toString()
										.equalsIgnoreCase(value)) {
									cb.setChecked(true);
									break;
								}
							}
						}
					}
				} else if (obj instanceof DatePicker) {
					DatePicker date = (DatePicker) obj;
					DateUtil.setDatePicker(date, value);
				} else if (obj instanceof TimePicker) {
					TimePicker time = (TimePicker) obj;
					DateUtil.setTimePicker(time, value);
				} else if (obj instanceof CustomHorizontalScrollView) {
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					for (CustomImageView customImageView : horizontalScrollView
							.getImageViews()) {
						if (!horizontalScrollView.isMulti()) {
							if (customImageView.getPicture().getId().equals(value)) {
								customImageView.setBackgroundColor(Color.BLUE);
								horizontalScrollView
									.addSelectedImageView(customImageView);
								break;
							}
						}else{
							if (customImageView.getPicture().getUrl().equals(value)) {
								customImageView.setBackgroundColor(Color.BLUE);
								horizontalScrollView
									.addSelectedImageView(customImageView);
								break;
							}
						}
					}
				} else {
					FLog.w("cannot find view " + ref);
				}
			}

			else if (valueObj instanceof List<?>) {
				
				if (obj instanceof LinearLayout) {
					LinearLayout ll = (LinearLayout) obj;
					
					List<NameValuePair> valueList = convertToNameValuePairs((Collection<?>) valueObj);

					for (NameValuePair pair : valueList) {
						for (int i = 0; i < ll.getChildCount(); ++i) {
							View view = ll.getChildAt(i);
							if (view instanceof CustomCheckBox) {
								CustomCheckBox cb = (CustomCheckBox) view;
								if (cb.getValue()
										.toString()
										.equalsIgnoreCase(
												arch16n
														.substituteValue(
																pair.getName()))) {
									cb.setChecked("true".equals(pair.getValue()));
									break;
								}
							}
						}
					}
				} else if (obj instanceof CustomHorizontalScrollView) {
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					
					@SuppressWarnings("unchecked")
					List<String> valueList = (List<String>) valueObj;
					
					for (String value : valueList) {
						for (CustomImageView customImageView : horizontalScrollView
								.getImageViews()) {
							if (!horizontalScrollView.isMulti()) {
								if (customImageView.getPicture().getId().equals(value)) {
									customImageView.setBackgroundColor(Color.BLUE);
									horizontalScrollView
											.addSelectedImageView(customImageView);
								}
							}else{
								if (customImageView.getPicture().getUrl().equals(value)) {
									customImageView.setBackgroundColor(Color.BLUE);
									horizontalScrollView
											.addSelectedImageView(customImageView);
								}
							}
						}
					}
				} else {
					FLog.w("cannot find view " + ref);
				}
			}
		} catch (Exception e) {
			FLog.e("error setting field value " + ref, e);
		}
	}

	private void setFieldCertainty(String ref, Object valueObj) {
		try{
			Object obj = getViewByRef(ref);
			
			if (valueObj instanceof Number) {
				valueObj = valueObj.toString();
			}
			
			if (valueObj instanceof String){
				
				float value = Float.valueOf((String) valueObj);
				
				if (obj instanceof CustomEditText){
					CustomEditText tv = (CustomEditText) obj;
					tv.setCertainty(value);
					tv.setCurrentCertainty(value);
				}
				else if (obj instanceof CustomSpinner){
					CustomSpinner spinner = (CustomSpinner) obj;
					spinner.setCertainty(value);
					spinner.setCurrentCertainty(value);
				}
				else if (obj instanceof CustomLinearLayout){
					CustomLinearLayout layout = (CustomLinearLayout) obj;
					layout.setCertainty(value);
					layout.setCurrentCertainty(value);
				}
				else if (obj instanceof CustomDatePicker) {
					CustomDatePicker date = (CustomDatePicker) obj;
					date.setCertainty(value);
					date.setCurrentCertainty(value);
				} 
				else if (obj instanceof CustomTimePicker) {
					CustomTimePicker time = (CustomTimePicker) obj;
					time.setCertainty(value);
					time.setCurrentCertainty(value);
				}else if (obj instanceof CustomHorizontalScrollView){
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					horizontalScrollView.setCertainty(value);
					horizontalScrollView.setCurrentCertainty(value);
				}else {
					// TODO show warning
				}
			}
		}
		catch(Exception e){
			FLog.e("error setting field certainty",e);
			// TODO show warning
		}
	}

	private void setFieldAnnotation(String ref, Object valueObj) {
		try{
			Object obj = getViewByRef(ref);
			
			if (valueObj instanceof String){
				
				String value = (String) valueObj;
				
				if (obj instanceof CustomEditText){
					CustomEditText tv = (CustomEditText) obj;
					tv.setAnnotation(value);
					tv.setCurrentAnnotation(value);
				}
				else if (obj instanceof CustomSpinner){
					CustomSpinner spinner = (CustomSpinner) obj;
					spinner.setAnnotation(value);
					spinner.setCurrentAnnotation(value);
				}
				else if (obj instanceof CustomLinearLayout){
					CustomLinearLayout layout = (CustomLinearLayout) obj;
					layout.setAnnotation(value);
					layout.setCurrentAnnotation(value);
				}else if (obj instanceof CustomHorizontalScrollView){
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					horizontalScrollView.setAnnotation(value);
					horizontalScrollView.setCurrentAnnotation(value);
				}else {
					// TODO show warning
				}
			}
		}
		catch(Exception e){
			FLog.e("error setting field annotation",e);
			// TODO show warning
		}
	}
	
	public void setFieldDirty(String ref, boolean isDirty, String isDirtyReason) {
		try {
			Object obj = getViewByRef(ref);
			
			if (obj != null) {
				Button dirtyButton = getTabForView(ref).getDirtyButton(ref);
				if (dirtyButton != null) {
					dirtyButton.setVisibility(isDirty ? View.VISIBLE : View.GONE);
				}
			}

			if (obj instanceof CustomEditText) {
				CustomEditText tv = (CustomEditText) obj;
				tv.setDirty(isDirty);
				tv.setDirtyReason(isDirtyReason);
			} else if (obj instanceof CustomSpinner) {
				CustomSpinner spinner = (CustomSpinner) obj;
				spinner.setDirty(isDirty);
				spinner.setDirtyReason(isDirtyReason);
			} else if (obj instanceof CustomLinearLayout) {
				CustomLinearLayout layout = (CustomLinearLayout) obj;
				layout.setDirty(isDirty);
				layout.setDirtyReason(isDirtyReason);
			} else if (obj instanceof CustomDatePicker) {
				CustomDatePicker date = (CustomDatePicker) obj;
				date.setDirty(isDirty);
				date.setDirtyReason(isDirtyReason);
			} else if (obj instanceof CustomTimePicker) {
				CustomTimePicker time = (CustomTimePicker) obj;
				time.setDirty(isDirty);
				time.setDirtyReason(isDirtyReason);
			} else if (obj instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				horizontalScrollView.setDirty(isDirty);
				horizontalScrollView.setDirtyReason(isDirtyReason);
			} else {
				FLog.w("cannot set field isDirty " + ref + " = "
						+ isDirty);
			}
		} catch (Exception e) {
			FLog.e("error setting field isDirty " + ref, e);
		}
	}

	@SuppressWarnings("rawtypes")
	public List<View> getViewByType(Class type){
		List<View> result = new ArrayList<View>();
		for(View view : viewList){
			if(view.getClass().equals(type)){
				result.add(view);
			}
		}
		return result;
	}
	
	public void storeBackStack(Bundle savedInstanceState, FragmentManager fragmentManager){
		indexes = null;
		for(int i = 0; i < fragmentManager.getBackStackEntryCount(); i++){
			TabGroup tabGroup = (TabGroup) fragmentManager.findFragmentByTag(fragmentManager.getBackStackEntryAt(i).getName());
			addIndexes(this.tabGroupList.indexOf(tabGroup));
		}
	    addIndexes(this.tabGroupList.indexOf(currentTabGroup));
		savedInstanceState.putIntegerArrayList("indexes", (ArrayList<Integer>) getIndexes());
	}

	public void restoreBackStack(Bundle savedInstanceState, FragmentActivity activity){
		currentTabGroup = null;
		indexes = savedInstanceState.getIntegerArrayList("indexes");
		if(indexes != null){
			for(int index : indexes){
				showTabGroup(activity, index);
			}
		}
	}

	public void storeTabs(Bundle savedInstanceState){
		savedInstanceState.putParcelableArray("tabList", tabList.toArray(new Tab[tabList.size()]));
		this.currentTabLabel = currentTabGroup.getCurrentTab().getReference();
		savedInstanceState.putString("currentTabLabel", this.currentTabLabel);
	}

	public void restoreTabs(Bundle savedInstanceState){
		Parcelable[] restoredTabs = savedInstanceState.getParcelableArray("tabList");
		tabReferenceMap = new HashMap<String, Tab>();
		for(Parcelable parcelable : restoredTabs){
			Tab tab = (Tab) parcelable;
			tabReferenceMap.put(tab.getReference(),tab);
		}
		this.currentTabLabel = savedInstanceState.getString("currentTabLabel");
	}

	public void storeViewValues(Bundle savedInstanceState){
		for(String reference : viewMap.keySet()){
			viewValues.put(reference, getFieldValue(reference));
			viewCertainties.put(reference, getFieldCertainty(reference));
			viewAnnotations.put(reference, getFieldAnnotation(reference));
			viewDirtyReasons.put(reference, getFieldDirty(reference));
		}
		savedInstanceState.putSerializable("viewValues", (Serializable) viewValues);
		savedInstanceState.putSerializable("viewCertainties", (Serializable) viewCertainties);
		savedInstanceState.putSerializable("viewAnnotations", (Serializable) viewAnnotations);
		savedInstanceState.putSerializable("viewDirtyReasons", (Serializable) viewDirtyReasons);
	}

	@SuppressWarnings("unchecked")
	public void restoreViewValues(Bundle savedInstanceState){
		viewValues = (Map<String, Object>) savedInstanceState.getSerializable("viewValues");
		viewCertainties = (Map<String, Object>) savedInstanceState.getSerializable("viewCertainties");
		viewAnnotations = (Map<String, Object>) savedInstanceState.getSerializable("viewAnnotations");
		viewDirtyReasons = (Map<String, String>) savedInstanceState.getSerializable("viewDirtyReasons");
	}
	
	@Override
	public void restoreViewValuesForTabGroup(TabGroup tabGroup){
		for (Tab tab : tabGroup.getTabs()) {
			for(String reference : tab.getViewReference().values()){
				setFieldValue(reference, viewValues.get(reference));
				setFieldCertainty(reference, viewCertainties.get(reference));
				setFieldAnnotation(reference, viewAnnotations.get(reference));
				setFieldDirty(reference, viewDirtyReasons.get(reference) != null, viewDirtyReasons.get(reference));
			}
		}
	}

	@Override
	public void restoreTabsForTabGroup(TabGroup tabGroup) {
		if(tabReferenceMap != null){
			for(Tab tab : tabGroup.getTabs()){
				Tab retrievedTab = this.tabReferenceMap.get(tab.getReference());
				if(!retrievedTab.getHidden()){
					showTab(tab.getReference());
				}
			}
			for(Tab tab : tabGroup.getTabs()){
				if(tab.getReference().equals(currentTabLabel)){
					showTab(currentTabLabel);
					break;
				}
			}
		}
	}
}