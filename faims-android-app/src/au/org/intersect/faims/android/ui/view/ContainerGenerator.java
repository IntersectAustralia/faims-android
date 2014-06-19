package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.widget.LinearLayout;

public class ContainerGenerator extends ViewGenerator {

	private ArrayList<ViewGenerator> viewGeneratorList;

	public ContainerGenerator(String style) {
		this.style = style;
		this.viewGeneratorList = new ArrayList<ViewGenerator>();
	}

	public void addViewContainer(ContainerGenerator containerGen) {
		viewGeneratorList.add(containerGen);
	}

	public void addViewGenerator(ViewGenerator viewGen) {
		viewGeneratorList.add(viewGen);
	}

	public ArrayList<ViewGenerator> viewGeneratorList() {
		return viewGeneratorList;
	}

	public LinearLayout generate(Tab tab, Context context, List<Map<String, String>> styleMappings) {
		return new CustomLinearLayout(context, styleMappings);
	}

}
