package au.org.intersect.faims.android.ui.form;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.ListView;

public class CustomListView extends ListView {

	private String name;
	private String type;
	private String value;
	private List<Object> selectedItems;
	
	public CustomListView(Context context) {
		super(context);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String value) {
		name = value;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String value) {
		type = value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

	public List<Object> getSelectedItems(){
		return selectedItems;
	}

	public void addSelectedItem(Object item){
		if(selectedItems == null){
			selectedItems = new ArrayList<Object>();
		}
		selectedItems.add(item);
	}
	
	public void removeSelectedItem(Object item){
		if(selectedItems != null){
			selectedItems.remove(item);
		}
	}

	public void removeSelectedItems(){
		if(selectedItems != null){
			selectedItems.clear();
		}
	}
}
