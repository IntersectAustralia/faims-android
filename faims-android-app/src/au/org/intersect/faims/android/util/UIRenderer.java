package au.org.intersect.faims.android.util;

import java.util.HashMap;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.form.TabGroup;
import au.org.intersect.faims.android.ui.form.TabView;

/**
 * Class that reads the ui defintion file and render the UI
 * 
 * @author danielt
 * 
 */
public class UIRenderer {

    private FormEntryController fem;
    
    private Context context;
    
    private HashMap<String, TabGroup> tabGroups;

    public UIRenderer(FormEntryController fem, Context context) {
        this.fem = fem;
        this.context = context;
        this.tabGroups = new HashMap<String, TabGroup>();
    }

    /**
     * Render the tabs and questions inside the tabs
     * 
     */
    public void render(Activity activity) {
    	
    	FormIndex currentIndex = this.fem.getModel().getFormIndex();
    	
    	if (currentIndex.isBeginningOfFormIndex()) {
            currentIndex = this.fem.getModel().incrementIndex(currentIndex, true);
        }
    	
    	IFormElement element = this.fem.getModel().getForm().getChild(currentIndex);
    	
    	TabGroup first = null;
    	
    	if (element instanceof GroupDef) {
    		
    		FormEntryCaption tabGroupCaption = this.fem.getModel().getCaptionPrompt(currentIndex);
    		
    		System.out.println(tabGroupCaption.getQuestionText());
    		
    		TabGroup tabGroup = new TabGroup();
    		tabGroup.setContext(context);
    		
    		if (first == null)
    			first = tabGroup;
    		
    		tabGroups.put(tabGroupCaption.getQuestionText(), tabGroup);
    		
    		GroupDef tabGroupElement = (GroupDef) element;
    		
            // descend into group
            FormIndex idxChild = this.fem.getModel().incrementIndex(currentIndex, true);

            for (int i = 0; i < tabGroupElement.getChildren().size(); i++) {
                
                element = this.fem.getModel().getForm().getChild(idxChild);
                
                if (element instanceof GroupDef) {
                	
                    FormEntryCaption tabCaption = this.fem.getModel().getCaptionPrompt(idxChild);
                    
                    System.out.println(tabCaption.getQuestionText());
                	
                	TabView tab = tabGroup.createTab(tabCaption.getQuestionText());
                	
                	GroupDef tabElement = (GroupDef) element;
                	
                    idxChild = this.fem.getModel().incrementIndex(idxChild, true);
                    
                    for (int j = 0; j < tabElement.getChildren().size(); j++) {
                    	
                        FormEntryPrompt input = this.fem.getModel().getQuestionPrompt(idxChild);

                        tab.addInput(input);

                        idxChild = this.fem.getModel().incrementIndex(idxChild, false);
                    }
                    
                }
            }
    	}
    	
    	FragmentManager fm = activity.getFragmentManager();
	    //Fragment fragment = fm.findFragmentById(R.id.fragment_content);
	    
	    FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragment_content, first);
        ft.commit();
    }
    
    //public View getViewByRef(String ref) {
    //	return viewMap.get(ref);
    //}
}