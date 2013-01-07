package au.org.intersect.faims.android.util;

import java.util.HashMap;
import java.util.LinkedList;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.view.View;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.form.TabGroup;
import au.org.intersect.faims.android.ui.form.Tab;

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
    
    private HashMap<String, View> viewMap; 
    
    public UIRenderer(FormEntryController fem, Context context) {
        this.fem = fem;
        this.context = context;
        this.tabGroupMap = new HashMap<String, TabGroup>();
        this.tabGroupList = new LinkedList<TabGroup>(); 
        this.viewMap = new HashMap<String, View>();
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
    	
    	for (int i = 0; i < element.getChildren().size(); i++) {
    		
    		element = this.fem.getModel().getForm().getChild(groupIndex);
    		
	    	if (element instanceof GroupDef) {
	    		
	    		GroupDef tabGroupElement = (GroupDef) element;
	    		FormEntryCaption tabGroupCaption = this.fem.getModel().getCaptionPrompt(groupIndex);
	    		TabGroup tabGroup = new TabGroup();
	    		tabGroup.setContext(context);
	    		tabGroup.setLabel(tabGroupCaption.getQuestionText());
	    		
	    		tabGroupMap.put(tabGroupCaption.getQuestionText(), tabGroup);
	    		tabGroupList.add(tabGroup);
	    		
	            // descend into group
	            FormIndex tabIndex = this.fem.getModel().incrementIndex(groupIndex, true);
	
	            for (int j = 0; j < tabGroupElement.getChildren().size(); j++) {  
	            	
	                element = this.fem.getModel().getForm().getChild(tabIndex);
	                
	                if (element instanceof GroupDef) {
	                	
	                	GroupDef tabElement = (GroupDef) element;
	                	FormEntryCaption tabCaption = this.fem.getModel().getCaptionPrompt(tabIndex);
	                    Tab tab = tabGroup.createTab(tabCaption.getQuestionText());
	                    
	                    System.out.println(tabCaption.getQuestionText());
	                	
	                    FormIndex inputIndex = this.fem.getModel().incrementIndex(tabIndex, true);
	                    
	                    for (int k = 0; k < tabElement.getChildren().size(); k++) {	
	                        FormEntryPrompt input = this.fem.getModel().getQuestionPrompt(inputIndex);
	                        View view = tab.addInput(input);
	                        
	                        // use paths as ids
	                        System.out.println(input.getIndex().getReference().toString().replaceAll("\\[[^\\]*].", ""));
	                        viewMap.put(input.getIndex().getReference().toString().replaceAll("\\[[^\\]*].", ""), view);
	
	                        inputIndex = this.fem.getModel().incrementIndex(inputIndex, false);
	                    }
	                    
	                }
	                
	                tabIndex = this.fem.getModel().incrementIndex(tabIndex, false);
	            }
	    	}
	    	
	    	groupIndex = this.fem.getModel().incrementIndex(groupIndex, false);
    	}
    	
    }
    
    public void showTabGroup(Activity activity, int index) {
    	FragmentManager fm = activity.getFragmentManager();
	    
	    FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragment_content, tabGroupList.get(index));
        ft.commit();
    }
    
    public void showTabGroup(Activity activity, String label) {
    	FragmentManager fm = activity.getFragmentManager();
    	FragmentTransaction ft = fm.beginTransaction();
	    ft.replace(R.id.fragment_content, tabGroupMap.get(label));
        ft.addToBackStack(null);
        ft.commit();
    }
    
    public View getViewByRef(String ref) {
    	return viewMap.get(ref);
    }
    
    public TabGroup getTabGroupByLabel(String label){
    	return this.tabGroupMap.get(label);
    }
}