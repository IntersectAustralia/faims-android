package au.org.intersect.faims.android.ui.view;

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

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.drawer.NavigationDrawer;
import au.org.intersect.faims.android.util.Arch16n;
import au.org.intersect.faims.android.util.FileUtil;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UIRenderer {
	
	private static final String TAG = "uirenderer:";
	
	@Inject 
	BeanShellLinker beanShellLinker;
	
	@Inject
	Arch16n arch16n;
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	@Inject
	NavigationDrawer navigationDrawer;
	
    private FormEntryController fem;
    
    private WeakReference<ShowModuleActivity> activityRef;
    
    private ArrayList<TabGroupGenerator> tabGroupGeneratorList;
    
    private ArrayList<TabGroup> tabGroupList;
    private HashMap<String, TabGroup> tabGroupMap;
    
    private ArrayList<Tab> tabList;
    private HashMap<String, Tab> tabMap;
    
    private LinkedList<View> viewList;
    private HashMap<String, View> viewMap;
    private HashMap<String, Tab> viewTabMap; 
    
    protected List<Integer> indexes;

	private Map<String,Map<String,String>> styles;

	private Bundle tempSavedInstanceState;
	
	public void init(ShowModuleActivity activity) {
		FAIMSApplication.getInstance().injectMembers(this);
		this.fem = null;
        this.activityRef = new WeakReference<ShowModuleActivity>(activity);
        this.tabGroupGeneratorList = new ArrayList<TabGroupGenerator>();  
        this.tabGroupList = new ArrayList<TabGroup>();
        this.tabGroupMap = new HashMap<String, TabGroup>();
        this.tabList = new ArrayList<Tab>(); 
        this.tabMap = new HashMap<String, Tab>();
        this.viewList = new LinkedList<View>();
        this.viewMap = new HashMap<String, View>();
        this.viewTabMap = new HashMap<String, Tab>();
        this.indexes = null;
        this.styles = new HashMap<String, Map<String,String>>();
    }
	
	public void navigateToTabGroup(int index) {
		try {
			FragmentManager fm = activityRef.get().getSupportFragmentManager();
			
	    	int size = navigationDrawer.getTabGroupCount();
	    	int count = size - 1;
	    	while (count > index) {
	    		if (count > index && count < size - 1) {
	    			TabGroup tabGroup = (TabGroup) fm.findFragmentByTag(navigationDrawer.peekTabGroup().getRef());
	    			tabGroup.addPopCounter();
	    		}
	    		navigationDrawer.popTabGroupNoUpdate();
	    		activityRef.get().updateActionBarTitle();
	    		fm.popBackStack();
	    		count--;
	    	}
	    	
	    	navigationDrawer.update();
		} catch (Exception e) {
			FLog.e("Error tyring to navigate to tab group", e);
		}
	}

    public TabGroup showTabGroup(int index) {
    	FragmentManager fm = activityRef.get().getSupportFragmentManager();
    	
    	TabGroup tabGroup = this.tabGroupList.get(index);
    	if (tabGroup == null) return null;
    	
    	if (tabGroup == navigationDrawer.peekTabGroup()) {
    		tabGroup.onShowTabGroup();
    		return tabGroup;
    	}
	    
	    FragmentTransaction ft = fm.beginTransaction();
        if(navigationDrawer.peekTabGroup() == null){
        	ft.replace(R.id.fragment_content, tabGroup, tabGroup.getRef());
        }else{
        	ft.replace(R.id.fragment_content, tabGroup, tabGroup.getRef());   	
        	ft.addToBackStack(navigationDrawer.peekTabGroup().getRef());
        }
        ft.commit();
        
        navigationDrawer.pushTabGroup(tabGroup);
        
        return tabGroup;
    }
    
    public TabGroup showTabGroup(String name) {
    	FragmentManager fm = activityRef.get().getSupportFragmentManager();
    	
    	TabGroup tabGroup = this.tabGroupMap.get(name);
    	if (tabGroup == null) return null;

    	if (tabGroup == navigationDrawer.peekTabGroup()){
    		tabGroup.onShowTabGroup();
    		return tabGroup;
    	}
    	
    	FragmentTransaction ft = fm.beginTransaction();
	    if(navigationDrawer.peekTabGroup() == null){
        	ft.replace(R.id.fragment_content, tabGroup, tabGroup.getRef());
        }else{
        	ft.replace(R.id.fragment_content, tabGroup, tabGroup.getRef());
        	ft.addToBackStack(navigationDrawer.peekTabGroup().getRef());
        }
        ft.commit();
        
        navigationDrawer.pushTabGroup(tabGroup);
        
        return tabGroup;
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
    
    public ArrayList<Tab> getTabList() {
		return tabList;
	}
    
    public Tab getTabForView(String ref){
    	return viewTabMap.get(ref);
    }
    
    public View getViewByRef(String ref) {
    	return viewMap.get(ref);
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
	
	private void saveTabGroupStack(Bundle savedInstanceState){
		FragmentManager fragmentManager = activityRef.get().getSupportFragmentManager();
		indexes = new ArrayList<Integer>();
		for(int i = 0; i < fragmentManager.getBackStackEntryCount(); i++){
			TabGroup tabGroup = (TabGroup) fragmentManager.findFragmentByTag(fragmentManager.getBackStackEntryAt(i).getName());
			indexes.add(tabGroupList.indexOf(tabGroup));
		}
		indexes.add(tabGroupList.indexOf(navigationDrawer.peekTabGroup()));
		savedInstanceState.putIntegerArrayList(TAG + "indexes", (ArrayList<Integer>) indexes);
	}

	private void restoreTabGroupStack(Bundle savedInstanceState){
		indexes = savedInstanceState.getIntegerArrayList(TAG + "indexes");
		if(indexes != null){
			for(int i = 0; i < indexes.size(); i++) {
				showTabGroup(indexes.get(i));
			}
		}
	}
	
	private void saveTabGroups(Bundle savedInstanceState){
		autoSaveManager.flush(false);
		
		for (TabGroup tabGroup : tabGroupList) {
			tabGroup.saveTo(savedInstanceState);
		}
	}
	
	private void restoreTabGroups(Bundle savedInstanceState){
		for (TabGroup tabGroup : tabGroupList) {
			tabGroup.restoreFrom(savedInstanceState);
		}
	}

	public void saveTo(Bundle savedInstanceState) {
		saveTabGroupStack(savedInstanceState);
		saveTabGroups(savedInstanceState);
	}

	public void restoreFrom(Bundle savedInstanceState) {
		tempSavedInstanceState = savedInstanceState;
	}
	
	private void restoreFromTempBundle() {
		if (tempSavedInstanceState != null) {
			restoreTabGroupStack(tempSavedInstanceState);
			restoreTabGroups(tempSavedInstanceState);
			tempSavedInstanceState = null;
		}
	}
	
	public void destroy() {
		if (tempSavedInstanceState != null) {
			tempSavedInstanceState = null;
			for (TabGroup tabGroup : tabGroupList) {
				tabGroup.clearTempBundle();
			}
		}
	}
	
	private void clearBackStack() {
		FragmentManager fm = activityRef.get().getSupportFragmentManager();
		while(fm.popBackStackImmediate());
	}
	
	public void createUI() throws Exception {
		boolean showFirsTabGroup = tempSavedInstanceState == null;
		clearBackStack();
		createTabGroups();
		
		if (showFirsTabGroup) {
			showTabGroup(0);
		}
	}
	
	public void restoreUI() {
		try {
			autoSaveManager.pause();	
			restoreFromTempBundle();
		} finally {
			autoSaveManager.resume();
		}
	}
	
	private void createTabGroups() throws Exception {
		for (TabGroupGenerator tabGroupGen : tabGroupGeneratorList) {
			TabGroup tabGroup = tabGroupGen.generate();
			tabGroupList.add(tabGroup);
			tabGroupMap.put(tabGroup.getRef(), tabGroup);
			
			createTabs(tabGroupGen, tabGroup);
		}
	}
	
	private void createTabs(TabGroupGenerator tabGroupGen, TabGroup tabGroup) throws Exception {
		for (TabGenerator tabGen : tabGroupGen.tabGeneratorList()) {
			Tab tab = tabGen.generate();
			tabList.add(tab);
			tabMap.put(tab.getRef(), tab);
			tabGroup.addTab(tab);
			
			createViews(tabGen, tab, tabGroup);
		}
	}
	
	private void createViews(TabGenerator tabGen, Tab tab, TabGroup tabGroup) throws Exception {
		for (ViewGenerator viewGen : tabGen.viewGeneratorList()) {
			if (viewGen instanceof ContainerGenerator) {
				ContainerGenerator containerGen = (ContainerGenerator) viewGen;
				createContainers(containerGen, null, tab, tabGroup);
			} else {
				createInput(viewGen, null, tab, tabGroup);
			}
		}
	}
	
	private void createContainers(ContainerGenerator containerGen, CustomLinearLayout parentLayout, Tab tab, TabGroup tabGroup) throws Exception {
		CustomLinearLayout layout = containerGen.generate(tab, activityRef.get(), getStyleMappings(containerGen.getStyle()));
		tab.addCustomContainer(containerGen.getRef(), layout, parentLayout);
		for (ViewGenerator viewGen : containerGen.viewGeneratorList()) {
			if (viewGen instanceof ContainerGenerator) {
				ContainerGenerator childContainerGen = (ContainerGenerator) viewGen;
				createContainers(childContainerGen, layout, tab, tabGroup);
			} else {
				createInput(viewGen, layout, tab, tabGroup);
			}
		}
	}
	
	private void createInput(ViewGenerator viewGen, LinearLayout layout, Tab tab, TabGroup tabGroup) throws Exception {
		tab.addCustomView(viewGen.getRef(), viewGen.attribute(), tabGroup.isArchEnt(), tabGroup.isRelationship(), layout);
	}
	
	public void addViewToTab(String ref, View view, Tab tab) {
		viewMap.put(ref, view);	
		viewList.add(view);
		viewTabMap.put(ref, tab);
	}
	
	public void removeViewFromTab(String ref) {
		View view = viewMap.get(ref);
		if (view != null) {
			viewMap.remove(ref);
			viewList.remove(viewList.indexOf(view));
			viewTabMap.remove(ref);
		} else {
			FLog.w("cannot remove view " + ref);
		}
	}
	
	public void parseSchema(String path) {
		fem = FileUtil.readXmlContent(path);
		
    	FormIndex currentIndex = fem.getModel().getFormIndex();
    	
    	IFormElement element = fem.getModel().getForm().getChild(currentIndex);
    	FormIndex groupIndex = fem.getModel().incrementIndex(currentIndex, true);
    	
    	int groups = element.getChildren().size();
    	for (int i = 0; i < groups; i++) {
    		
    		element = fem.getModel().getForm().getChild(groupIndex);
	    	if (element instanceof GroupDef) {
	    		
	    		GroupDef tabGroupElement = (GroupDef) element;
	    		FormEntryCaption tabGroupCaption = fem.getModel().getCaptionPrompt(groupIndex);
	    		
	    		String tabGroupName = tabGroupCaption.getIndex().getReference().getNameLast();

	    		if("style".equals(tabGroupName)){
	    			parseStyle(tabGroupElement, groupIndex);
	    		}else{
	    			parseTabGroups(tabGroupElement, groupIndex, tabGroupCaption, tabGroupName);
	    		}
	    	
	    		groupIndex = fem.getModel().incrementIndex(groupIndex, false);
	    	}
    	}
    	
    }

	private void parseTabGroups(GroupDef tabGroupElement, FormIndex groupIndex, FormEntryCaption tabGroupCaption, String tabGroupName) {
		IFormElement element;
		String archEntType = tabGroupCaption.getFormElement().getAdditionalAttribute(null, "faims_archent_type");
		String relType = tabGroupCaption.getFormElement().getAdditionalAttribute(null, "faims_rel_type");

		String tabGroupLabel = tabGroupCaption.getQuestionText();
		String tabGroupRef = tabGroupName;
		TabGroupGenerator tabGroupGen = new TabGroupGenerator(tabGroupRef, tabGroupLabel, archEntType, relType);
		tabGroupGeneratorList.add(tabGroupGen);
		
		// descend into group
		FormIndex tabIndex = this.fem.getModel().incrementIndex(groupIndex,true);

		int tabs = tabGroupElement.getChildren().size();
		for (int i = 0; i < tabs; i++) {
			element = this.fem.getModel().getForm().getChild(tabIndex);

			if (element instanceof GroupDef) {
				parseTab(element, tabIndex, tabGroupRef, tabGroupGen);
			}

			tabIndex = this.fem.getModel().incrementIndex(tabIndex, false);
		}
	}

	private void parseTab(IFormElement element, FormIndex tabIndex, String tabGroupRef, TabGroupGenerator tabGroupGen) {
		GroupDef tabElement = (GroupDef) element;
		FormEntryCaption tabCaption = this.fem.getModel().getCaptionPrompt(tabIndex);

		String tabName = tabCaption.getIndex().getReference().getNameLast();
		boolean faims_hidden = "true".equals(tabElement.getAdditionalAttribute(null, "faims_hidden"));
		boolean faims_scrollable = !"false".equals(tabElement.getAdditionalAttribute(null, "faims_scrollable"));
		String tabRef = tabGroupRef + "/" + tabName; 
		
		TabGenerator tabGen = new TabGenerator(tabRef, tabName, tabCaption.getQuestionText(), faims_hidden, faims_scrollable, activityRef);
		tabGroupGen.addTabGenerator(tabGen);

		// descend into group
		FormIndex containerIndex = this.fem.getModel().incrementIndex(tabIndex, true);

		for (int i = 0; i < tabElement.getChildren().size(); i++) {
			element = this.fem.getModel().getForm().getChild(containerIndex);

			if (element instanceof GroupDef) {
				parseContainer(element, containerIndex, tabRef, tabGen, null);
			} else {
				parseInput(element, containerIndex, tabRef, tabGen, null);
			}
			containerIndex = this.fem.getModel().incrementIndex(containerIndex, false);
		}
	}
	
	private void parseInput(IFormElement element, FormIndex childIndex, String tabRef, TabGenerator tabGen, ContainerGenerator parentContainerGen) {
		QuestionDef questionElement = (QuestionDef) element;
		String style = questionElement.getAdditionalAttribute(null,"faims_style");
		FormEntryPrompt input = this.fem.getModel().getQuestionPrompt(childIndex);
		String viewName = input.getIndex().getReference().getNameLast();
		String viewRef = tabRef + "/" + viewName;
		
		ViewGenerator viewGen = new ViewGenerator(viewRef, FormInputDef.parseFromInput(input), style);
		if (parentContainerGen != null) {
			parentContainerGen.addViewGenerator(viewGen);
		} else {
			tabGen.addViewGenerator(viewGen);
		}
	}

	private void parseContainer(IFormElement element, FormIndex childIndex, String tabRef, TabGenerator tabGen, ContainerGenerator parentContainerGen) {
		GroupDef childContainerElement = (GroupDef) element;
		String style = childContainerElement.getAdditionalAttribute(null,"faims_style");
		FormEntryCaption viewCaption = this.fem.getModel().getCaptionPrompt(childIndex);
		String viewName = viewCaption.getIndex().getReference().getNameLast();
		String viewRef = parentContainerGen != null ? parentContainerGen.getRef() + "/" + viewName : tabRef + "/" + viewName;
		FormIndex inputIndex = this.fem.getModel().incrementIndex(childIndex,true);
				
		ContainerGenerator containerGen = new ContainerGenerator(viewRef, style);
		if (parentContainerGen != null) {
			parentContainerGen.addViewContainer(containerGen);
		} else {
			tabGen.addViewContainer(containerGen);
		}
		
		for (int i = 0; i < childContainerElement.getChildren().size(); i++) {
			element = this.fem.getModel().getForm().getChild(inputIndex);
			
			if (element instanceof GroupDef) {
				parseContainer(element, inputIndex, tabRef, tabGen, containerGen);
			} else {
				parseInput(element, inputIndex, tabRef, tabGen, containerGen);
			}
			inputIndex = this.fem.getModel().incrementIndex(inputIndex, false);
		}
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
					inputIndex = this.fem.getModel().incrementIndex(inputIndex, false);
				}
				styles.put(styleName, attributes);
			}

			tabIndex = this.fem.getModel().incrementIndex(tabIndex, false);
		}
	}
	
	public List<Map<String, String>> getStyleMappings(String style) {
		List<Map<String, String>> styleMappings = new ArrayList<Map<String, String>>();
		if (style != null) {
			String[] styles = style.split(" ");
			for (String s : styles) {
				styleMappings.add(this.styles.get(s));
			}
		}
		return styleMappings;
	}

}