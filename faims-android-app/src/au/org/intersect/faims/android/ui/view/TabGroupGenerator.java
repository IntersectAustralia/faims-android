package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;

public class TabGroupGenerator {

	private String tabGroupRef;
	private String tabGroupLabel;
	private String archEntType;
	private String relType;
	private ArrayList<TabGenerator> tabGeneratorList;

	public TabGroupGenerator(String tabGroupRef, String tabGroupLabel,
			String archEntType, String relType) {
		this.tabGroupRef = tabGroupRef;
		this.tabGroupLabel = tabGroupLabel;
		this.archEntType = archEntType;
		this.relType = relType;
		this.tabGeneratorList = new ArrayList<TabGenerator>();
	}

	public void addTabGenerator(TabGenerator tabGen) {
		tabGeneratorList.add(tabGen);
	}

	public TabGroup generate() {
		return new TabGroup(tabGroupRef, tabGroupRef, tabGroupLabel, archEntType, relType);
	}

	public ArrayList<TabGenerator> tabGeneratorList() {
		return tabGeneratorList;
	}
	
}
