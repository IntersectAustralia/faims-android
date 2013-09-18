package au.org.intersect.faims.android.ui.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.R;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import au.org.intersect.faims.android.data.VocabularyTerm;
import au.org.intersect.faims.android.log.FLog;

public class HierarchicalSpinner extends CustomSpinner {

	private List<VocabularyTerm> terms;
	
	private Stack<VocabularyTerm> parentTerms;
	
	private List<VocabularyTerm> currentTerms;

	protected boolean loadedTerms;

	private List<VocabularyTerm> currentItems;

	public HierarchicalSpinner(Context context) {
		super(context);
	}

	public HierarchicalSpinner(Context context,
			String name, String type, String ref) {
		super(context, name, type, ref);
		
		setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int index, long arg3) {
				if (index < 0 || index >= currentTerms.size()) {
					FLog.w("selecting item that does not exist");
					return;
				}
				
				TextView text = (TextView) view;
				StringBuilder sb = new StringBuilder();
				
				for (VocabularyTerm term : parentTerms) {
					if (sb.length() != 0) sb.append(" > ");
					sb.append(term.getName());
				}
				
				if (index >= parentTerms.size()) {
					if (sb.length() != 0) sb.append(" > ");
					sb.append(currentTerms.get(index).getName());
				}
				
				text.setText(sb);
				
				if (currentTerms.get(index).terms != null) {
					performClick();
				}
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
	}

	public void setTerms(List<VocabularyTerm> terms) {
		this.terms = terms;
		this.parentTerms = new Stack<VocabularyTerm>();
		loadTerms();
	}
	
	private void loadTerms() {
		loadedTerms = true;
		
		if (parentTerms.size() == 0) { 
			currentTerms = terms;
			currentItems = terms;
			
			setAdapter(new ArrayAdapter<VocabularyTerm>(this.getContext(), R.layout.simple_dropdown_item_1line, currentItems));
		} else {
			VocabularyTerm selectedTerm = parentTerms.peek();
			currentTerms = new ArrayList<VocabularyTerm>();
			currentItems = new ArrayList<VocabularyTerm>();
			for (VocabularyTerm term : parentTerms) {
				currentTerms.add(term);
				currentItems.add(new VocabularyTerm("", "Back to: " + term.getName(), null, null));
			}
			currentTerms.addAll(selectedTerm.terms);
			currentItems.addAll(selectedTerm.terms);
			
			setAdapter(new ArrayAdapter<VocabularyTerm>(this.getContext(), R.layout.simple_dropdown_item_1line, currentItems));
		}
	}
	
	@Override
	public Object getSelectedItem() {
		// TODO Auto-generated method stub
		return super.getSelectedItem();
	}
	
	@Override
	public void setSelection(int position) {
		if (currentTerms == null) return;
		
		try {
			VocabularyTerm selectedTerm = currentTerms.get(position);
			
			if (position >= parentTerms.size()) {
				if (selectedTerm.terms != null) {
					parentTerms.push(selectedTerm);
					loadTerms();
					super.setSelection(parentTerms.size() - 1);
				} else {
					super.setSelection(position);
				}
			} else {
				VocabularyTerm parentTerm = null;
				while(parentTerms.size() > position) {
					parentTerm = parentTerms.pop();
				}
				loadTerms();
				if (parentTerms.size() == 0) {
					super.setSelection(terms.indexOf(parentTerm));
				} else {
					super.setSelection(parentTerms.peek().terms.indexOf(parentTerm) + parentTerms.size());
				}
			}
			
		} catch (Exception e) {
			FLog.e("error selecting item on hierarchical spinner", e);
		}
	}
	
}
