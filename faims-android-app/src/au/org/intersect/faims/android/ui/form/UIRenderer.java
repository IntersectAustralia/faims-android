package au.org.intersect.faims.android.ui.form;

import java.util.HashMap;
import java.util.LinkedList;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.util.FAIMSLog;

/**
 * Class that reads the ui defintion file and render the UI
 * 
 * @author danielt
 * 
 */
public class UIRenderer {

    private FormEntryController fem;
    
    private Context context;
    
    private HashMap<String, TabGroup> tabGroupMap;
    private LinkedList<TabGroup> tabGroupList;
    
    private HashMap<String, Tab> tabMap;
    private LinkedList<Tab> tabList;
    
    private HashMap<String, View> viewMap; 
    private LinkedList<View> viewList;
    
    public UIRenderer(FormEntryController fem, Context context) {
        this.fem = fem;
        this.context = context;
        this.tabGroupMap = new HashMap<String, TabGroup>();
        this.tabGroupList = new LinkedList<TabGroup>(); 
        this.tabMap = new HashMap<String, Tab>();
        this.tabList = new LinkedList<Tab>(); 
        this.viewMap = new HashMap<String, View>();
        this.viewList = new LinkedList<View>(); 
    }

    /**
     * Render the tabs and questions inside the tabs
     * 
     */
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
	    		String archEntType = tabGroupCaption.getFormElement().getAdditionalAttribute(null, "faims_archent_type");
	    		String relType = tabGroupCaption.getFormElement().getAdditionalAttribute(null, "faims_rel_type");
	    		TabGroup tabGroup = new TabGroup(archEntType,relType);
	    		tabGroup.setContext(context);
	    		tabGroup.setLabel(tabGroupCaption.getQuestionText());
	    		
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
                                        .getAdditionalAttribute(null, "faims_scrollable")));	                 
	                	
	                	FAIMSLog.log(tabGroupName + "/" + tabName);
	                    tabMap.put(tabGroupName + "/" + tabName, tab);
	                    tabList.add(tab);
	                    
	                    FormIndex inputIndex = this.fem.getModel().incrementIndex(tabIndex, true);
	                    
	                    for (int k = 0; k < tabElement.getChildren().size(); k++) {	
	                        FormEntryPrompt input = this.fem.getModel().getQuestionPrompt(inputIndex);
	                        String viewName = input.getIndex().getReference().getNameLast();
	                        View view = tab.addInput(input,tabGroupName + "/" + tabName + "/" + viewName,viewName);
	                        
	                        FAIMSLog.log(tabGroupName + "/" + tabName + "/" + viewName);
	                        viewMap.put(tabGroupName + "/" + tabName + "/" + viewName, view);
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
	    
	    FragmentTransaction ft = fm.beginTransaction();
	    TabGroup tabGroup = this.tabGroupList.get(index);
        ft.add(R.id.fragment_content, tabGroup);
        ft.commit();
        return tabGroup;
    }
    
    public TabGroup showTabGroup(FragmentActivity activity, String name) {
    	FragmentManager fm = activity.getSupportFragmentManager();

    	FragmentTransaction ft = fm.beginTransaction();
    	TabGroup tabGroup = this.tabGroupMap.get(name);
    	if (tabGroup == null) return null;
	    ft.replace(R.id.fragment_content, tabGroup);
        ft.addToBackStack(null);
        ft.commit();
        return tabGroup;
    }
    
    public View getViewByRef(String ref) {
    	return viewMap.get(ref);
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
		if (tabGroup == null) return null;
		return tabGroup.showTab(tab);
	}
}