package au.org.intersect.faims.android.ui.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

public class TabGenerator {

	private String tabRef;
	private String tabName;
	private String tabLabel;
	private boolean faims_hidden;
	private boolean faims_scrollable;
	private ArrayList<ViewGenerator> viewGeneratorList;
	private WeakReference<ShowModuleActivity> activityRef;

	public TabGenerator(String tabRef, String tabName, String tabLabel,
			boolean faims_hidden, boolean faims_scrollable, WeakReference<ShowModuleActivity> activityRef) {
		this.tabRef = tabRef;
		this.tabName = tabName;
		this.tabLabel = tabLabel;
		this.faims_hidden = faims_hidden;
		this.faims_scrollable = faims_scrollable;
		this.activityRef = activityRef;
		this.viewGeneratorList = new ArrayList<ViewGenerator>();
	}

	public void addViewContainer(ContainerGenerator containerGen) {
		viewGeneratorList.add(containerGen);
	}

	public void addViewGenerator(ViewGenerator viewGen) {
		viewGeneratorList.add(viewGen);
	}

	public Tab generate() {
		return new Tab(tabRef, tabName, tabLabel, faims_hidden, faims_scrollable, activityRef);
	}

	public ArrayList<ViewGenerator> viewGeneratorList() {
		return viewGeneratorList;
	}
	
}
