package au.org.intersect.faims.android.ui.form;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FAIMSLog;

/**
 * Class that reads the ui defintion file and render the UI
 * 
 * @author danielt
 * 
 */
public class UIRenderer implements IRestoreActionListener{

    private FormEntryController fem;
    
    private Context context;
    
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

    public UIRenderer(FormEntryController fem, Arch16n arch16n, Context context) {
        this.fem = fem;
        this.arch16n = arch16n;
        this.context = context;
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
	    		String archEntType = tabGroupCaption.getFormElement().getAdditionalAttribute(null, "faims_archent_type");
	    		String relType = tabGroupCaption.getFormElement().getAdditionalAttribute(null, "faims_rel_type");
	    		TabGroup tabGroup = new TabGroup(archEntType,relType, this);
	    		tabGroup.setContext(context);
	    		String tabGroupText = tabGroupCaption.getQuestionText();
	    		tabGroupText = arch16n.substituteValue(tabGroupText);
	    		tabGroup.setLabel(tabGroupText);
	    		
	    		String tabGroupName = tabGroupCaption.getIndex().getReference().getNameLast();
	    		FAIMSLog.log(tabGroupName);
	    		tabGroupMap.put(tabGroupName, tabGroup);
	    		tabGroupList.add(tabGroup);
	    		
	            // descend into group
	            FormIndex tabIndex = this.fem.getModel().incrementIndex(groupIndex, true);
	
	            int tabs = tabGroupElement.getChildren().size();
	            for (int j = 0; j < tabs; j++) {  
	            	
	                element = this.fem.getModel().getForm().getChild(tabIndex);
	                
	                if (element instanceof GroupDef) {
	                	
	                	GroupDef tabElement = (GroupDef) element;
	                	FormEntryCaption tabCaption = this.fem.getModel().getCaptionPrompt(tabIndex);
	                    
	                	String tabName = tabCaption.getIndex().getReference().getNameLast();
	                	Tab tab = tabGroup.createTab(tabName, tabCaption.getQuestionText(), "true".equals(tabElement
                                .getAdditionalAttribute(null, "faims_hidden")), !"false".equals(tabElement
                                        .getAdditionalAttribute(null, "faims_scrollable")), arch16n, tabGroupName + "/" + tabName);	                 
	                	
	                	FAIMSLog.log(tabGroupName + "/" + tabName);
	                    tabMap.put(tabGroupName + "/" + tabName, tab);
	                    tabList.add(tab);
	                    
	                    FormIndex inputIndex = this.fem.getModel().incrementIndex(tabIndex, true);
	                    
	                    for (int k = 0; k < tabElement.getChildren().size(); k++) {	
	                        FormEntryPrompt input = this.fem.getModel().getQuestionPrompt(inputIndex);
	                        String viewName = input.getIndex().getReference().getNameLast();
	                        View view = tab.addInput(input,tabGroupName + "/" + tabName + "/" + viewName,viewName, directory, tabGroup.isArchEnt(), tabGroup.isRelationship());
	                        
	                        FAIMSLog.log(tabGroupName + "/" + tabName + "/" + viewName);
	                        viewMap.put(tabGroupName + "/" + tabName + "/" + viewName, view);
	                        viewTabMap.put(tabGroupName + "/" + tabName + "/" + viewName, tab);
	                        viewList.add(view);
	                        
	                        inputIndex = this.fem.getModel().incrementIndex(inputIndex, false);
	                    }
	                    
	                }
	                
	                tabIndex = this.fem.getModel().incrementIndex(tabIndex, false);
	            }
	    	}
	    	
	    	groupIndex = this.fem.getModel().incrementIndex(groupIndex, false);
    	}
    	
    }
    
    public TabGroup showTabGroup(FragmentActivity activity, int index) {
    	FragmentManager fm = activity.getSupportFragmentManager();
    	
    	TabGroup tabGroup = this.tabGroupList.get(index);
    	String tag = "TabGroup " + String.valueOf(this.tabGroupList.indexOf(tabGroup));
	    TabGroup currentTabGroup = (TabGroup) fm.findFragmentByTag(tag);
	    if (currentTabGroup != null && currentTabGroup.isVisible()) return currentTabGroup;
	    
	    String currentTag = "TabGroup " + String.valueOf(this.tabGroupList.indexOf(this.currentTabGroup));
		
	    FragmentTransaction ft = fm.beginTransaction();
        if(this.currentTabGroup == null){
        	ft.add(R.id.fragment_content, tabGroup, tag);
        }else{
        	ft.replace(R.id.fragment_content, tabGroup, tag);
        	ft.addToBackStack(currentTag);
        }
        this.currentTabGroup = tabGroup;
        ft.commit();
        
        return tabGroup;
    }
    
    public TabGroup showTabGroup(FragmentActivity activity, String name) {
    	FragmentManager fm = activity.getSupportFragmentManager();
    	
    	TabGroup tabGroup = this.tabGroupMap.get(name);
    	if (tabGroup == null) return null;
    	
    	String tag = "TabGroup " + String.valueOf(this.tabGroupList.indexOf(tabGroup));
	    TabGroup currentTabGroup = (TabGroup) fm.findFragmentByTag(tag);
	    if (currentTabGroup != null && currentTabGroup.isVisible()) return currentTabGroup;
    	
	    String currentTag = "TabGroup " + String.valueOf(this.tabGroupList.indexOf(this.currentTabGroup));
	    
    	FragmentTransaction ft = fm.beginTransaction();
	    if(this.currentTabGroup == null){
        	ft.add(R.id.fragment_content, tabGroup, tag);
        }else{
        	ft.replace(R.id.fragment_content, tabGroup, tag);
        	ft.addToBackStack(currentTag);
        }
	    this.currentTabGroup = tabGroup;
        ft.commit();
        
        return tabGroup;
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

	private Object getFieldValue(String ref) {
		try{
			Object obj = getViewByRef(ref);
			
			if (obj instanceof TextView){
				TextView tv = (TextView) obj;
				return tv.getText().toString();
			}
			else if (obj instanceof Spinner){
				Spinner spinner = (Spinner) obj;
				NameValuePair pair = (NameValuePair) spinner.getSelectedItem();
				if (pair == null) return "";
				return pair.getValue();
			}
			else if (obj instanceof LinearLayout){
				LinearLayout ll = (LinearLayout) obj;
				
				View child0 = ll.getChildAt(0);
				
				if( child0 instanceof CheckBox){
					List<NameValuePair> valueList = new ArrayList<NameValuePair>();
					
					for(int i = 0; i < ll.getChildCount(); ++i){
						View view = ll.getChildAt(i);
						
						if (view instanceof CustomCheckBox){
							CustomCheckBox cb = (CustomCheckBox) view;
							if (cb.isChecked()) {
								valueList.add(new NameValuePair(cb.getValue(), "true"));
							}
						}
					}
					return valueList;
				}
				else if (child0 instanceof RadioGroup){
					RadioGroup rg = (RadioGroup) child0;
					String value = "";
					for(int i = 0; i < rg.getChildCount(); ++i){
						View view = rg.getChildAt(i);
						
						if (view instanceof CustomRadioButton){
							CustomRadioButton rb = (CustomRadioButton) view;
							if (rb.isChecked()){
								value = rb.getValue();
								break;
							}
						}
					}
					return value;
				}
				else{
					return null;
				}
			}
			else if (obj instanceof DatePicker) {
				DatePicker date = (DatePicker) obj;
				return DateUtil.getDate(date);
			} 
			else if (obj instanceof TimePicker) {
				TimePicker time = (TimePicker) obj;
				return DateUtil.getTime(time);
			}
			else if (obj instanceof CustomHorizontalScrollView){
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				if(horizontalScrollView.getSelectedImageView() != null){
					return horizontalScrollView.getSelectedImageView().getPicture().getId();
				}else{
					return "";
				}
			}
			else {
				return null;
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception getting field value",e);
			return null;
		}
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
				return null;
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception getting field certainty",e);
			return "";
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
				return null;
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception getting field annotation",e);
			return null;
		}
	}

	private void setFieldValue(String ref, Object valueObj) {
		try{
			Object obj = getViewByRef(ref);
			
			if (valueObj instanceof String){
				
				String value = (String) valueObj;
				value = arch16n.substituteValue(value);
				
				if (obj instanceof TextView){
					TextView tv = (TextView) obj;
					tv.setText(value);
				}
				else if (obj instanceof Spinner){
					Spinner spinner = (Spinner) obj;
					
					for( int i = 0; i < spinner.getAdapter().getCount(); ++i ){
						NameValuePair pair = (NameValuePair) spinner.getItemAtPosition(i);
						if (value.equalsIgnoreCase(pair.getValue())){
							spinner.setSelection(i);
							break;
						}
					}
				}
				else if (obj instanceof LinearLayout){
					LinearLayout ll = (LinearLayout) obj;
					
					View child0 = ll.getChildAt(0);
					
					if (child0 instanceof RadioGroup){
						RadioGroup rg = (RadioGroup) child0;
						List<CustomRadioButton> buttons = new ArrayList<CustomRadioButton>();
						for(int i = 0; i < rg.getChildCount(); ++i){
							View view = rg.getChildAt(i);
							if (view instanceof CustomRadioButton){
								buttons.add((CustomRadioButton) view);
							}
						}
						rg.removeAllViews();
						for (CustomRadioButton rb : buttons) {
							CustomRadioButton radioButton = new CustomRadioButton(rg.getContext());
                            radioButton.setText(rb.getText());
                            radioButton.setValue(rb.getValue());
                            if (rb.getValue().toString().equalsIgnoreCase(value)){
                            	radioButton.setChecked(true);
							}
                            rg.addView(radioButton);
                        	
                        }
						
					}else if (child0 instanceof CheckBox){
						for(int i = 0; i < ll.getChildCount(); ++i){
							View view = ll.getChildAt(i);
							if (view instanceof CustomCheckBox){
								CustomCheckBox cb = (CustomCheckBox) view;
								if (cb.getValue().toString().equalsIgnoreCase(value)){
									cb.setChecked(true);
									break;
								}
							}
						}
					}
				}
				else if (obj instanceof DatePicker) {
					DatePicker date = (DatePicker) obj;
					DateUtil.setDatePicker(date, value);
				} 
				else if (obj instanceof TimePicker) {
					TimePicker time = (TimePicker) obj;
					DateUtil.setTimePicker(time, value);
				}else if (obj instanceof CustomHorizontalScrollView){
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					for (CustomImageView customImageView : horizontalScrollView.getImageViews()) {
						if(customImageView.getPicture().getId().equals(value)){
							customImageView.setBackgroundColor(Color.GREEN);
							horizontalScrollView.setSelectedImageView(customImageView);
							break;
						}
					};
				}
			}
			
			else if (valueObj instanceof List<?>){
				
				@SuppressWarnings("unchecked")
				List<NameValuePair> valueList = (List<NameValuePair>) valueObj;
				
				if (obj instanceof LinearLayout){
					LinearLayout ll = (LinearLayout) obj;
					
					for(NameValuePair pair : valueList){
						for(int i = 0; i < ll.getChildCount(); ++i){
							View view = ll.getChildAt(i);
							if (view instanceof CustomCheckBox){
								CustomCheckBox cb = (CustomCheckBox) view;
								if (cb.getValue().toString().equalsIgnoreCase(arch16n.substituteValue(pair.getName()))){
									cb.setChecked("true".equals(pair.getValue()));
									break;
								}
							}
						}
					}
				}
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception setting field value",e);
		}
	}

	private void setFieldCertainty(String ref, Object valueObj) {
		try{
			Object obj = getViewByRef(ref);
			
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
				}
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception setting field certainty",e);
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
				}
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception setting field annotation",e);
		}
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
		}
		savedInstanceState.putSerializable("viewValues", (Serializable) viewValues);
		savedInstanceState.putSerializable("viewCertainties", (Serializable) viewCertainties);
		savedInstanceState.putSerializable("viewAnnotations", (Serializable) viewAnnotations);
	}

	@SuppressWarnings("unchecked")
	public void restoreViewValues(Bundle savedInstanceState){
		viewValues = (Map<String, Object>) savedInstanceState.getSerializable("viewValues");
		viewCertainties = (Map<String, Object>) savedInstanceState.getSerializable("viewCertainties");
		viewAnnotations = (Map<String, Object>) savedInstanceState.getSerializable("viewAnnotations");
	}
	
	@Override
	public void restoreViewValuesForTabGroup(TabGroup tabGroup){
		for (Tab tab : tabGroup.getTabs()) {
			for(String reference : tab.getViewReference().values()){
				setFieldValue(reference, viewValues.get(reference));
				setFieldCertainty(reference, viewCertainties.get(reference));
				setFieldAnnotation(reference, viewAnnotations.get(reference));
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