package au.org.intersect.faims.android.ui.view;

import java.lang.ref.WeakReference;
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
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

@SuppressLint("ValidFragment")
public class TabGroup extends Fragment {
	
	public static int tabGroupId = 1;
	
	private WeakReference<ShowModuleActivity> activityRef;
	private TabHost tabHost;
	private HashMap<String, Tab> tabMap;
	private LinkedList<Tab> tabs;
	private List<String> onLoadCommands;
	private List<String> onShowCommands;
	private String label = "";
	private String archEntType;
	private String relType;
	private IRestoreActionListener actionListener;
	private Tab lastTab;
	private String id;
	
	public TabGroup(){
		
	}
	
	public TabGroup(WeakReference<ShowModuleActivity> activityRef, String archEntType, String relType, IRestoreActionListener actionListener) {
		if(archEntType != null && relType != null){
			FLog.w("tabgroup can only contain either archEntId or relId not both");
		}
		this.activityRef = activityRef;
		this.archEntType = archEntType;
		this.relType = relType;
		this.actionListener = actionListener;
		this.tabMap = new HashMap<String, Tab>();
		this.tabs = new LinkedList<Tab>();
		this.onLoadCommands = new ArrayList<String>();
		this.onShowCommands = new ArrayList<String>();
		this.id = "TabGroup" + tabGroupId++;
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, 
    		                  ViewGroup container,
                              Bundle savedInstanceState) {	
		
		if (tabHost == null){
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
					// TODO: maybe hide the frame layout
				}
			}
			
			if(this.onLoadCommands.size() > 0){
				executeCommands(this.onLoadCommands);
			}
		}
		
		if(this.onShowCommands.size() > 0){
			executeCommands(this.onShowCommands);
		}
		
		if(savedInstanceState != null && !savedInstanceState.isEmpty()){
			this.actionListener.restoreViewValuesForTabGroup(this);
			this.actionListener.restoreTabsForTabGroup(this);
		}
		
		// Solves a prob the back button gives us with the TabHost already having a parent
		if (tabHost.getParent() != null){
			((ViewGroup) tabHost.getParent()).removeView(tabHost);
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
		
		onShowTabGroup();
		
		return tabHost;
    }
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		onHideTabGroup();
	}
	
	public Tab createTab(String name, String label, boolean hidden, boolean scrollable, String reference) {
		Tab tab = new Tab(activityRef.get(), name, label, hidden, scrollable, reference);
		tabMap.put(name, tab);
		tabs.add(tab);
        return tab;
	}
	
	public Tab showTab(String name) {
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
		return null;
	}

	public Tab getCurrentTab(){
		return tabs.get(tabHost.getCurrentTab());
	}
	
	public void hideTab(String name){
		for (int i = 0; i < tabs.size(); i++) {
			Tab tab = tabs.get(i);
			if (tab.getName().equals(name)) {
				TabWidget widget = tabHost.getTabWidget();
				widget.getChildAt(i).setVisibility(View.GONE);
			}
		}
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

		BeanShellLinker linker = ((ShowModuleActivity) getActivity()).getBeanShellLinker();
		
		for(String command : commands){
			linker.execute(command);	
		}
	}

	public LinkedList<Tab> getTabs() {
		return tabs;
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

	public void clearTabs() {
		for (Tab tab : tabs) {
			tab.clearViews();
		}
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
	}
	
	public void onHideTabGroup() {
		Tab tab = getCurrentTab();
		if (tab != null) {
			tab.onHideTab();
			lastTab = null;
		}
	}

	public String getGroupTag() {
		return id;
	}
	
	protected void resetTabGroupOnShow(){
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
