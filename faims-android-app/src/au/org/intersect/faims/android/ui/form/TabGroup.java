package au.org.intersect.faims.android.ui.form;

import java.util.LinkedList;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import au.org.intersect.faims.android.R;

public class TabGroup extends Fragment {
	
	private Context context;
	private TabHost tabHost;
	private LinkedList<Tab> tabs;
	
	public TabGroup() {
		tabs = new LinkedList<Tab>();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {	
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
	
	public Tab createTab(String name) {
		Tab tab = new Tab(context, name);
		tabs.push(tab);
        return tab;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public void setLabel(String tabGroupLabel) {
		// TODO Auto-generated method stub
		
	}
	
}
