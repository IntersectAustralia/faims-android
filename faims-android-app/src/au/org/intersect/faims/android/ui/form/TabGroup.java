package au.org.intersect.faims.android.ui.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;
import au.org.intersect.faims.android.util.BeanShellLinker;

public class TabGroup extends Fragment {
	
	private Context context;
	private TabHost tabHost;
	private HashMap<String, Tab> tabMap;
	private LinkedList<Tab> tabs;
	private List<String> onLoadCommands;
	private List<String> onShowCommands;
	private String label = "";
	
	
	public TabGroup() {
		tabMap = new HashMap<String, Tab>();
		tabs = new LinkedList<Tab>();
		onLoadCommands = new ArrayList<String>();
		onShowCommands = new ArrayList<String>();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, 
    		                  ViewGroup container,
                              Bundle savedInstanceState) {	
		
		Log.d("FAIMS","TabGroup: " + this.label + " onCreateView()");
		if (tabHost == null){
			tabHost = (TabHost) inflater.inflate(R.layout.tab_group, container, false);
			tabHost.setup();
			
			/*
			tabHost.addTab(createTabSpec(tabHost, "Test 1"));
			tabHost.addTab(createTabSpec(tabHost, "Test 2"));
			tabHost.addTab(createTabSpec(tabHost, "Test 3"));
			*/
			
			for (Tab tab : tabs) {
				tabHost.addTab(tab.createTabSpec(tabHost));
			}
			
			if(this.onLoadCommands.size() > 0){
				executeCommands(this.onLoadCommands);
			}
		}
		
		if(this.onShowCommands.size() > 0){
			executeCommands(this.onShowCommands);
		}
		
		return tabHost;
    }
	
	/*
	private TabSpec createTabSpec(TabHost tabHost, final String name) {
		TabSpec spec = tabHost.newTabSpec(name);
		spec.setIndicator(name);
		
		final ScrollView scrollView = new ScrollView(this.context);
		
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
		
        spec.setContent(new TabContentFactory() {

            @Override
            public View createTabContent(String tag) {
            	TextView text =  new TextView(context);
            	text.setText(name);
                return text;
            }
        });
        return spec;
	}
	*/
	
	public Tab createTab(String name, String label) {
		Tab tab = new Tab(context, name, label);
		tabMap.put(name, tab);
		tabs.add(tab);
        return tab;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getLabel(){
		return this.label;
	}
	
	public void addOnLoadCommand(String command){
		this.onLoadCommands.add(command);
	}
	
	public void addOnShowCommand(String command){
		this.onShowCommands.add(command);
	}
	
	private void executeCommands(List<String> commands){

		BeanShellLinker linker = ((ShowProjectActivity) getActivity()).getBeanShellLinker();
		
		for(String command : commands){
			linker.execute(command);	
		}
	}

}
