package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.Arch16n;

import com.google.inject.Inject;

@SuppressLint("ValidFragment")
public class TabGroup extends Fragment {
	
	public interface TabTask {
		public void onShow();
	}
	
	@Inject
	BeanShellLinker beanShellLinker;
	
	@Inject
	Arch16n arch16n;
	
	private TabHost tabHost;
	private HashMap<String, Tab> tabMap;
	private LinkedList<Tab> tabs;
	private List<String> onLoadCommands;
	private List<String> onShowCommands;
	private TabTask showTask;
	private Tab lastTab;
	
	private String ref;
	private String name;
	private String label;
	private String archEntType;
	private String relType;

	private Bundle tempSavedInstanceState;

	private boolean isVisible;
	
	public TabGroup() {
	}
	
	public TabGroup(String ref, String name, String label, String archEntType, String relType) {
		FAIMSApplication.getInstance().injectMembers(this);
		
		if(archEntType != null && relType != null){
			FLog.w("tabgroup can only contain either archEntId or relId not both");
		}
		
		this.ref = ref;
		this.name = name;
		this.label = arch16n.substituteValue(label);
		this.archEntType = archEntType;
		this.relType = relType;
		this.tabMap = new HashMap<String, Tab>();
		this.tabs = new LinkedList<Tab>();
		this.onLoadCommands = new ArrayList<String>();
		this.onShowCommands = new ArrayList<String>();
	}

	@Override
    public View onCreateView(LayoutInflater inflater, 
    		                  ViewGroup container,
                              Bundle savedInstanceState) {
		if (ref != null) {
		
			if (tabHost == null) {
				tabHost = (TabHost) inflater.inflate(R.layout.tab_group, container, false);
				tabHost.setup();
				
				for (Tab tab : tabs) {
					tabHost.addTab(tab.createTabSpec(tabHost));
				}
				
				TabWidget widget = tabHost.getTabWidget();
				if (widget != null) {
					boolean first = true;
					for (int i = 0; i < widget.getChildCount(); i++) {
						Tab tab = tabs.get(i);
						if (tab.getHidden()) {
							widget.getChildAt(i).setVisibility(View.GONE);
						} else if (first) {
							tabHost.setCurrentTab(i);
							first = false;
						}
					}
					if (first == true) {
						// all tabs are hidden
					}
				}
				
				if(this.onLoadCommands.size() > 0){
					executeCommands(this.onLoadCommands);
				}
			}
			
			if(this.onShowCommands.size() > 0){
				executeCommands(this.onShowCommands);
			}
			
			// TODO does this listener need to be removed?
			tabHost.setOnTabChangedListener(new OnTabChangeListener() {
	
				@Override
				public void onTabChanged(String arg0) {
					if (lastTab != null) {
						lastTab.onHideTab();
						lastTab = null;
					}
					
					Tab tab = getCurrentTab();
					if (tab != null) {
						tab.onShowTab();
						lastTab = tab;
					}
				}
				
			});
			
			// restore after tabgroup is shown
			restoreFromTempBundle();
			
			// execute a task after tabgroup is shown
			if (showTask != null) {
				showTask.onShow();
				showTask = null;
			}
			
			// Solves a prob the back button gives us with the TabHost already having a parent
			if (tabHost.getParent() != null){
				((ViewGroup) tabHost.getParent()).removeView(tabHost);
			}
			
			onShowTabGroup();
			
			return tabHost;
		}
		return null;
    }
	
	@Override
	public void onDestroyView() {
		if (ref != null) {
			onHideTabGroup();
		}
		super.onDestroyView();
	}
	
	public Tab addTab(Tab tab) {
		tabMap.put(name, tab);
		tabs.add(tab);
        return tab;
	}
	
	public Tab showTab(String name) {
		if (tabHost != null) {
			for (int i = 0; i < tabs.size(); i++) {
				Tab tab = tabs.get(i);
				if (tab.getName().equals(name)) {
					TabWidget widget = tabHost.getTabWidget();
					widget.getChildAt(i).setVisibility(View.VISIBLE);
					tab.setHidden(false);
					tabHost.setCurrentTab(i);
					if(tab.getScrollViewForTab() != null){
						tab.getScrollViewForTab().scrollTo(0, 0);
					}
					return tab;
				}
			}
		}
		return null;
	}
	
	public void hideTab(String name){
		if (tabHost != null) {
			for (int i = 0; i < tabs.size(); i++) {
				Tab tab = tabs.get(i);
				if (tab.getName().equals(name)) {
					TabWidget widget = tabHost.getTabWidget();
					widget.getChildAt(i).setVisibility(View.GONE);
				}
			}
		}
	}
	
	public void clearTabs() {
		for (Tab tab : tabs) {
			tab.clearViews();
		}
	}
	
	public Tab getCurrentTab(){
		if (tabHost != null) {
			return tabs.get(tabHost.getCurrentTab());
		}
		return null;
	}
	
	public void setCurrentTab(Tab tab) {
		showTab(tab.getName());
	}

	public Tab getTab(String name){
		for (int i = 0; i < tabs.size(); i++) {
			Tab tab = tabs.get(i);
			if (tab.getName().equals(name)) {
				return tab;
			}
		}
		return null;
	}

	public LinkedList<Tab> getTabs() {
		return tabs;
	}
	
	public String getName() {
		return this.name;
	}

	public String getRef() {
		return this.ref;
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
		for(String command : commands){
			beanShellLinker.execute(command);	
		}
	}

	public String getArchEntType() {
		return archEntType;
	}

	public String getRelType() {
		return relType;
	}

	public boolean isArchEnt(){
		return this.archEntType != null && this.relType == null;
	}

	public boolean isRelationship() {
		return this.relType != null && this.archEntType == null;
	}
	
	public void onShowTabGroup() {	
		Tab tab = getCurrentTab();
		if (tab != null) {
			tab.onShowTab();
			lastTab = getCurrentTab();
		}
		resetTabGroupOnShow();
		isVisible = true;
	}
	
	public void onHideTabGroup() {
		Tab tab = getCurrentTab();
		if (tab != null) {
			tab.onHideTab();
			lastTab = null;
		}
		isVisible = false;
	}
	
	protected void resetTabGroupOnShow() {
		if (tabHost != null) {
			tabHost.setCurrentTab(0);
			if(!getTabs().isEmpty()){
				for (Tab tab : getTabs()) {
					if(tab.getScrollViewForTab() != null){
						tab.getScrollViewForTab().scrollTo(0, 0);
					}
				}
			}
		}
	}
	
	public void setOnShowTask(TabTask task) {
		if (isVisible) {
			task.onShow();
		} else {
			this.showTask = task;
		}
	}
	
	public void saveTo(Bundle savedInstanceState){
		for (Tab tab : tabs) {
			tab.saveTo(savedInstanceState);
		}
		
		Tab tab = getCurrentTab();
		if (tab != null) {
			String tabLabel = tab.getName();
			savedInstanceState.putString("currentTabLabel", tabLabel);
		}
	}

	public void restoreFrom(Bundle savedInstanceState){
		tempSavedInstanceState = savedInstanceState;
	}
	
	public void restoreFromTempBundle() {
		if (tempSavedInstanceState != null) {
			for(Tab tab : tabs){
				tab.restoreFrom(tempSavedInstanceState);
			}
			
			String tabLabel = tempSavedInstanceState.getString("currentTabLabel");
			if (tabLabel != null) {
				setCurrentTab(getTab(tabLabel));
			}
			tempSavedInstanceState = null;
		}
	}

}
