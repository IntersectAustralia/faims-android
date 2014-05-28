package au.org.intersect.faims.android.ui.view;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.util.Arch16n;
import au.org.intersect.faims.android.util.FileUtil;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UIRenderer implements IRestoreActionListener {

	@Inject 
	BeanShellLinker beanShellLinker;
	
	@Inject
	Arch16n arch16n;
	
    private FormEntryController fem;
    
    private WeakReference<ShowModuleActivity> activityRef;
    
    private HashMap<String, TabGroup> tabGroupMap;
    private LinkedList<TabGroup> tabGroupList;
    
    private HashMap<String, Tab> tabMap;
    private LinkedList<Tab> tabList;
    
    private HashMap<String, View> viewMap;
    private HashMap<String, Tab> viewTabMap; 
    private LinkedList<View> viewList;
    
    protected List<Integer> indexes;
    
    private TabGroup currentTabGroup;
    
    private Map<String, Tab> tabReferenceMap;
    
    private String currentTabLabel;
    
	private Map<String, Object> viewValues;
	private Map<String, Object> viewCertainties;
	private Map<String, Object> viewAnnotations;
	private Map<String, String> viewDirtyReasons;

	private Map<String,Map<String,String>> styles;
	
	public void init(ShowModuleActivity activity) {
		FAIMSApplication.getInstance().injectMembers(this);
		this.fem = null;
        this.activityRef = new WeakReference<ShowModuleActivity>(activity);
        this.tabGroupMap = new HashMap<String, TabGroup>();
        this.tabGroupList = new LinkedList<TabGroup>(); 
        this.tabMap = new HashMap<String, Tab>();
        this.tabList = new LinkedList<Tab>(); 
        this.viewMap = new HashMap<String, View>();
        this.viewTabMap = new HashMap<String, Tab>();
        this.viewList = new LinkedList<View>();
        this.indexes = null;
        this.currentTabGroup = null;
        this.tabReferenceMap = null;
        this.currentTabLabel = null;
        this.viewValues = new HashMap<String, Object>();
        this.viewCertainties = new HashMap<String, Object>();
        this.viewAnnotations = new HashMap<String, Object>();
        this.viewDirtyReasons = new HashMap<String, String>();
        this.styles = new HashMap<String, Map<String,String>>();
    }
	
    public void createUI() {
    	
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
	    			parseTabGroups(groupIndex, tabGroupElement, tabGroupCaption, tabGroupName);
	    		}
	    	
	    	groupIndex = this.fem.getModel().incrementIndex(groupIndex, false);
	    	}
    	}
    	
    }

	private void parseTabGroups(FormIndex groupIndex, GroupDef tabGroupElement, FormEntryCaption tabGroupCaption, String tabGroupName) {
		IFormElement element;
		String archEntType = tabGroupCaption.getFormElement().getAdditionalAttribute(null, "faims_archent_type");
		String relType = tabGroupCaption.getFormElement().getAdditionalAttribute(null, "faims_rel_type");

		TabGroup tabGroup = new TabGroup(activityRef, archEntType, relType, this);
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
				parseTab(tabGroupName, element, tabGroup, tabIndex);
			}

			tabIndex = this.fem.getModel().incrementIndex(tabIndex, false);
		}
	}

	private void parseTab(String tabGroupName, IFormElement element, TabGroup tabGroup, FormIndex tabIndex) {
		GroupDef tabElement = (GroupDef) element;
		FormEntryCaption tabCaption = this.fem.getModel().getCaptionPrompt(tabIndex);

		String tabName = tabCaption.getIndex().getReference().getNameLast();
		Tab tab = tabGroup.createTab(tabName, tabCaption.getQuestionText(),"true".equals(tabElement.getAdditionalAttribute(null,
						"faims_hidden")), !"false".equals(tabElement.getAdditionalAttribute(null, "faims_scrollable")), tabGroupName + "/" + tabName);

		tabMap.put(tabGroupName + "/" + tabName, tab);
		tabList.add(tab);

		FormIndex containerIndex = this.fem.getModel().incrementIndex(tabIndex, true);

		for (int i = 0; i < tabElement.getChildren().size(); i++) {
			element = this.fem.getModel().getForm().getChild(containerIndex);

			if (element instanceof GroupDef) {
				parseContainer(null, tabGroupName, tabName, element,tabGroup, containerIndex, tab, 1);
			} else {
				parseInput(tabGroupName, tabName, element, tabGroup,tab, containerIndex, null);
			}
			containerIndex = this.fem.getModel().incrementIndex(containerIndex, false);
		}
	}

	private void parseContainer(LinearLayout containerLayout, String tabGroupName, String tabName, IFormElement element,
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
				parseContainer(childContainerLayout, tabGroupName,tabName, element, tabGroup, inputIndex, tab, ++depth);
			} else {
				parseInput(tabGroupName, tabName, element, tabGroup,tab, inputIndex, childContainerLayout);
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

	private void parseInput(String tabGroupName, String tabName, IFormElement element, TabGroup tabGroup, Tab tab,
			FormIndex childIndex, LinearLayout containerLayout) {
		QuestionDef questionElement = (QuestionDef) element;
		String style = questionElement.getAdditionalAttribute(null,"faims_style");
		FormEntryPrompt input = this.fem.getModel().getQuestionPrompt(childIndex);
		String viewName = input.getIndex().getReference().getNameLast();
		View view = tab.addCustomView(containerLayout, FormAttribute.parseFromInput(input), tabGroupName + "/"+ tabName + "/" + viewName, viewName,
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
    	if (tabGroup == this.currentTabGroup) {
    		tabGroup.resetTabGroupOnShow();
    		return currentTabGroup;
    	}
	    
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
    	if (tabGroup == this.currentTabGroup){
    		tabGroup.resetTabGroupOnShow();
    		return currentTabGroup;
    	}
    	
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

	public LinkedList<Tab> getTabList() {
		return tabList;
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
			Object obj = getViewByRef(reference);
			if (obj instanceof ICustomView) {
				viewValues.put(reference, beanShellLinker.getFieldValue(reference));
				viewCertainties.put(reference, beanShellLinker.getFieldCertainty(reference));
				viewAnnotations.put(reference, beanShellLinker.getFieldAnnotation(reference));
				viewDirtyReasons.put(reference, beanShellLinker.getFieldDirty(reference));
			}
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
				Object obj = getViewByRef(reference);
				if (obj instanceof ICustomView) {
					beanShellLinker.setFieldValue(reference, viewValues.get(reference));
					beanShellLinker.setFieldCertainty(reference, viewCertainties.get(reference));
					beanShellLinker.setFieldAnnotation(reference, viewAnnotations.get(reference));
					beanShellLinker.setFieldDirty(reference, viewDirtyReasons.get(reference) != null, viewDirtyReasons.get(reference));
				}
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

	public void loadSchema(String filepath) {
		this.fem = FileUtil.readXmlContent(filepath);
	}
}