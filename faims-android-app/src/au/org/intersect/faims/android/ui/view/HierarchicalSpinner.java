package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.data.VocabularyTerm;
import au.org.intersect.faims.android.log.FLog;

public class HierarchicalSpinner extends CustomSpinner {
	
	class HierarchicalOnItemSelectListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
		{
			if (ignoresSelectEvents() == false) {
				if (listener != null) {
					listener.onItemSelected(parent, view, position, id);
				}
				
				if(internalListener != null) {
					internalListener.onItemSelected(parent, view, position, id);
				}
				
				notifySave();
			} else {
				setIgnoreSelectEvents(false);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent)
		{
			if (ignoresSelectEvents() == false) {
				if (listener != null) {
					listener.onNothingSelected(parent);
				}
				
				if(internalListener != null) {
					internalListener.onNothingSelected(parent);
				}
				
				notifySave();
			} else {
				setIgnoreSelectEvents(false);
			}
		}
		
	}

	private List<VocabularyTerm> terms;
	
	private Stack<VocabularyTerm> parentTerms;
	
	private List<VocabularyTerm> currentTerms;

	private List<VocabularyTerm> currentItems;

	private boolean lastSelected;
	
	private VocabularyTerm lastSelectedItem;

	private HashMap<String, VocabularyTerm> vocabIdToParentTerm;
	private HashMap<String, List<VocabularyTerm>> vocabIdToParentTerms;

	private OnItemSelectedListener listener;
	private OnItemSelectedListener internalListener;
	private HierarchicalOnItemSelectListener customListener;

	public HierarchicalSpinner(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}

	public HierarchicalSpinner(Context context, FormAttribute attribute, String ref) {
		super(context, attribute, ref);
		FAIMSApplication.getInstance().injectMembers(this);
		customListener = new HierarchicalOnItemSelectListener();
		super.setOnItemSelectedListener(customListener);
	}
	
	private void mapVocabToParent() {
		this.vocabIdToParentTerm = new HashMap<String, VocabularyTerm>();
		this.vocabIdToParentTerms = new HashMap<String, List<VocabularyTerm>>();
		
		for (VocabularyTerm term : terms) {
			vocabIdToParentTerm.put(term.id, null);
			vocabIdToParentTerms.put(term.id, null);
			if (term.terms != null) {
				mapVocabToParent(term);
			}
		}
	}
	
	private void mapVocabToParent(VocabularyTerm parentTerm) {
		for (VocabularyTerm term : parentTerm.terms) {
			vocabIdToParentTerm.put(term.id, parentTerm);
			vocabIdToParentTerms.put(term.id, parentTerm.terms);
			if (term.terms != null) {
				mapVocabToParent(term);
			}
		}
	}
	
	@Override
	public void setOnItemSelectedListener(OnItemSelectedListener listener) {
		this.listener = listener;
	}
	
	/* 
	 * Override the setAdapter method to set ignore boolean
	 * Without, this was causing unwanted click events when changing/loading tabs
	 */
	@Override
	public void setAdapter(SpinnerAdapter adapter)
	{
		this.ignoreSelectOnce = true;
		super.setAdapter(adapter);	
	}
	
	public void setTerms(List<VocabularyTerm> terms) {
		this.terms = terms;
		
		mapVocabToParent();
		
		this.parentTerms = new Stack<VocabularyTerm>();
		
		this.internalListener = new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int index, long arg3) {
				try {
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
					
					if (lastSelected && currentTerms.get(index).terms != null) {
						performClick();
					}
					
					lastSelected = false;
				} catch (Exception e) {
					FLog.e("error on item select", e);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
			
		};
		
		loadTerms();
	}
	
	private void loadTerms() {
		if (parentTerms.size() == 0) { 
			currentTerms = terms;
			currentItems = terms;
			
			setAdapter(new ArrayAdapter<VocabularyTerm>(this.getContext(), R.layout.multiline_spinner_dropdown_item, currentItems));
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
			
			setAdapter(new ArrayAdapter<VocabularyTerm>(this.getContext(), R.layout.multiline_spinner_dropdown_item, currentItems));
		}
	}
	
	@Override
	public void setSelection(int position) {
		setSelectionItem(position, true);
	}
	
	private void setSelectionItem(int position, boolean selected) {
		if (terms == null) {
			super.setSelection(position);
			return;
		}
		lastSelected = selected;
		
		try {
			VocabularyTerm selectedTerm = currentTerms.get(position);
			
			if (position >= parentTerms.size()) {
				if (selectedTerm.terms != null) {
					parentTerms.push(selectedTerm);
					loadTerms();
					super.setSelection(parentTerms.size() - 1);
					if(selected && !selectedTerm.equals(lastSelectedItem)) {
						listener.onItemSelected(this, getChildAt(position), position, 0);
					}
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
				if(selected && !selectedTerm.equals(lastSelectedItem)) {
					listener.onItemSelected(this, getChildAt(position), position, 0);
				}
			}
			lastSelectedItem = selectedTerm;
		} catch (Exception e) {
			FLog.e("error selecting item on hierarchical spinner", e);
		}
	}
	
	@Override
	public void setValue(String value) {
		if (terms == null) {
			super.setValue(value);
			return;
		}
		
		if (value == null || "".equals(value)) {
			return;
		}
		
		// add terms to parent stack
		parentTerms.clear();
		VocabularyTerm parentTerm = vocabIdToParentTerm.get(value);
		while(parentTerm != null) {
			parentTerms.insertElementAt(parentTerm, 0);
			parentTerm = vocabIdToParentTerm.get(parentTerm.id);
		}
		
		// load terms
		loadTerms();
		
		// set selection using position in term list
		List<VocabularyTerm> terms = vocabIdToParentTerms.get(value);
		if (terms == null) {
			terms = this.terms;
		}
		
		int index = 0;
		for (VocabularyTerm t : terms) {
			if (t.id.equals(value)) {
				setSelectionItem(parentTerms.size() + index, false);
				break;
			}
			index++;
		}
		notifySave();
	}
	
	@Override
	public String getValue() {
		if (terms == null) {
			return super.getValue();
		}
		
		int index = getSelectedItemPosition();
		return currentTerms.get(index).id;
	}
	
	@Override
	public void reset() {
		if (terms == null) {
			super.reset();
			return;
		}
		dirty = false;
		dirtyReason = null;
		
		lastSelectedItem = null;
		
		parentTerms.clear();
		loadTerms();
		
		setSelectionItem(0, false);
		setCertainty(1);
		setAnnotation("");
		
		save();
	}
}
