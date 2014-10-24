package au.org.intersect.faims.android.ui.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.dialog.CheckBoxGroupLabelDialog;
import au.org.intersect.faims.android.util.Arch16n;
import au.org.intersect.faims.android.util.Compare;

import com.google.inject.Inject;
import com.nativecss.NativeCSS;

public class CustomCheckBoxGroup extends LinearLayout implements ICustomView {
	
	class CheckBoxGroupOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if(listener != null) {
				listener.onClick(v);
			}
			notifySave();
		}
		
	}
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	@Inject
	BeanShellLinker linker;
	
	@Inject
	Arch16n arch16n;
	
	private String ref;
	private boolean dynamic;
	
	protected List<NameValuePair> currentValues;
	private List<String> currentCertainties;
	private List<String> currentAnnotations;
	private List<String> certainties;
	private List<String> annotations;
	
	protected boolean dirty;
	protected String dirtyReason;
	protected boolean annotationEnabled;
	protected boolean certaintyEnabled;
	protected FormInputDef inputDef;

	protected OnClickListener listener;
	protected CheckBoxGroupOnClickListener customListener;

	private String clickCallback;
	private String focusCallback;
	private String blurCallback;
	
	private List<ImageView> annotationIcons;
	private List<ImageView> certaintyIcons;
	
	private ViewFactory viewFactory;

	public CustomCheckBoxGroup(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.customListener = new CheckBoxGroupOnClickListener();
	}
	
	public CustomCheckBoxGroup(Context context, FormInputDef inputDef, String ref, boolean dynamic) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		
		setLayoutParams(new LayoutParams(
	                LayoutParams.MATCH_PARENT,
	                LayoutParams.MATCH_PARENT));
	    setOrientation(LinearLayout.VERTICAL);
	    
		this.inputDef = inputDef;
		this.ref = ref;
		this.dynamic = dynamic;		
	    this.customListener = new CheckBoxGroupOnClickListener();
	    NativeCSS.addCSSClass(this, "checkbox-group");
	    
	    this.viewFactory = new ViewFactory(new WeakReference<ShowModuleActivity>(linker.getActivity()), arch16n);
	    reset();
	}

	@Override
	public String getAttributeName() {
		return inputDef.name;
	}

	@Override
	public String getAttributeType() {
		return inputDef.type;
	}

	@Override
	public String getRef() {
		return ref;
	}
	
	@Override
	public boolean isDynamic() {
		return dynamic;
	}
	
	@Override
	public String getValue() {
		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			if (view instanceof FrameLayout) {
				CustomCheckBox checkbox = getItemCheckBoxView((FrameLayout) view);
				if (checkbox.isChecked()) {
					return checkbox.getValue();
				}
			}
		}
		return null;
	}

	@Override
	public void setValue(String value) {
		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			if (view instanceof FrameLayout) {
				CustomCheckBox checkbox = getItemCheckBoxView((FrameLayout) view);
				if (checkbox.getValue().toString().equalsIgnoreCase(value)) {
					checkbox.setChecked(true);
					break;
				}
			}
		}
		notifySave();
	}

	@Override
	public float getCertainty() {
		return 0;
	}

	@Override
	public void setCertainty(float certainty) {
	}
	
	@Override
	public void setCertaintyIcon(ImageView certaintyIcon) {
	}
	
	public void setCertainties(List<String> certainties) {
		this.certainties= certainties;
	}
	
	public List<String> getCertainties() {
		if (certainties == null) {
			certainties = new ArrayList<String>();
			return certainties;
		}
		List<String> values = new ArrayList<String>();

		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			
			if (view instanceof FrameLayout) {
				CustomCheckBox cb = getItemCheckBoxView((FrameLayout) view);
				if (cb.isChecked()) {
					values.add(certainties.get(i));
				}
			}
		}
		return values;
	}
	
	public List<String> getAllCertainties() {
		if (certainties == null) {
			certainties = new ArrayList<String>();
		}
		return certainties;
	}
	
	public void setCertainty(String certainty, int index) {
		certainties.set(index, certainty);
		updateCertaintyIcon(index);
		notifySave();
	}
	
	protected void updateCertaintyIcon(int index) {
		if (index < certainties.size() && index < certaintyIcons.size()
				&& certaintyIcons.get(index) != null) {
			if (!String.valueOf(FaimsSettings.DEFAULT_CERTAINTY).equals(certainties.get(index))
					&& certainties.get(index) != null) {
				certaintyIcons.get(index).setImageResource(R.drawable.certainty_entered);
			} else {
				certaintyIcons.get(index).setImageResource(R.drawable.certainty);
			}
		}
	}

	@Override
	public String getAnnotation() {
		return null;
	}

	@Override
	public void setAnnotation(String annotation) {
	}
	
	@Override
	public void setAnnotationIcon(ImageView annotationIcon) {
	}
	
	public void setAnnotations(List<String> annotations) {
		this.annotations = annotations;
	}
	
	public List<String> getAnnotations() {
		if (annotations == null) {
			annotations = new ArrayList<String>();
			return annotations;
		}
		List<String> values = new ArrayList<String>();

		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			
			if (view instanceof FrameLayout) {
				CustomCheckBox cb = getItemCheckBoxView((FrameLayout) view);
				if (cb.isChecked()) {
					values.add(annotations.get(i));
				}
			}
		}
		return values;
	}
	
	public List<String> getAllAnnotations() {
		if (annotations == null) {
			annotations = new ArrayList<String>();
		}
		return annotations;
	}
	
	public void setAnnotation(String annotation, int index) {
		annotations.set(index, annotation);
		updateAnnotationIcon(index);
		notifySave();
	}
	
	protected void updateAnnotationIcon(int index) {
		if (index < annotations.size() && index < annotationIcons.size()
				&& annotationIcons.get(index) != null) {
			if (!FaimsSettings.DEFAULT_ANNOTATION.equals(annotations.get(index)) && annotations.get(index) != null) {
				annotationIcons.get(index).setImageResource(R.drawable.annotation_entered);
			} else {
				annotationIcons.get(index).setImageResource(R.drawable.annotation);
			}
		}
	}
	
	public void setCheckBoxValue(String value, String annotation, String certainty) {
		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			
			if (view instanceof FrameLayout) {
				CustomCheckBox cb = getItemCheckBoxView((FrameLayout) view);
				if (cb.getValue().toString().equalsIgnoreCase(value)) {
					cb.setChecked(true);
					annotations.set(i, annotation);
					updateAnnotationIcon(i);
					certainties.set(i, certainty);
					updateCertaintyIcon(i);
					notifySave();
					break;
				}
			}
		}
	}

	@Override
	public void reset() {
		dirty = false;
		dirtyReason = null;
		annotations = new ArrayList<String>();
		certainties = new ArrayList<String>();
		
		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			if (view instanceof FrameLayout) {
				CustomCheckBox checkbox = getItemCheckBoxView((FrameLayout) view);
				checkbox.setChecked(false);
				annotations.add("");
				annotationIcons.get(i).setImageResource(R.drawable.annotation);
				certainties.add("1.0");
				certaintyIcons.get(i) .setImageResource(R.drawable.certainty);
			}
		}
		
		save();
	}
	
	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	@Override
	public String getDirtyReason() {
		return dirtyReason;
	}

	@Override
	public void setDirtyReason(String reason) {
		this.dirtyReason = reason;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean hasChanges() {
		return !(Compare.compareValues((List<NameValuePair>) getValues(), currentValues)) || 
				!(Compare.compareValues(currentAnnotations, annotations)) || 
				!(Compare.compareValues(currentCertainties, certainties));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void save() {
		currentValues = (List<NameValuePair>) getValues();
		currentCertainties = getCertainties();
		currentAnnotations = getAnnotations();
	}

	@Override
	public List<?> getValues() {
		List<NameValuePair> values = new ArrayList<NameValuePair>();

		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			
			if (view instanceof FrameLayout) {
				CustomCheckBox cb = getItemCheckBoxView((FrameLayout) view);
				if (cb.isChecked()) {
					values.add(new NameValuePair(cb.getValue(), "true"));
				}
			}
		}
		
		return values;
	}
	
	private CustomCheckBox getItemCheckBoxView(FrameLayout frameLayout) {
		for (int i = 0; i < frameLayout.getChildCount(); ++i) {
			View view = frameLayout.getChildAt(i);
			if (view instanceof LinearLayout) {
				LinearLayout layout = (LinearLayout) view;
				for (int j = 0; j < layout.getChildCount(); ++j) {
					View layoutView = layout.getChildAt(j);
					if (layoutView instanceof LinearLayout) {
						LinearLayout checkContainer = (LinearLayout) layoutView;
						for (int k = 0; k < checkContainer.getChildCount(); ++k) {
							View check = checkContainer.getChildAt(j);
							if (check instanceof CustomCheckBox) {
								return (CustomCheckBox) check;
							}
						}
					}
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setValues(List<?> values) {
		if (values == null) return;
		List<NameValuePair> pairs = (List<NameValuePair>) values;
		for (NameValuePair pair : pairs) {
			for (int i = 0; i < getChildCount(); ++i) {
				View view = getChildAt(i);
				
				if (view instanceof FrameLayout) {
					CustomCheckBox cb = getItemCheckBoxView((FrameLayout) view);
					if (cb.getValue()
							.toString()
							.equalsIgnoreCase(pair.getName())) {
						cb.setChecked("true".equals(pair.getValue()));
						break;
					}
				}
			}
		}
		notifySave();
	}
	
	public List<NameValuePair> getPairs() {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			
			if (view instanceof FrameLayout) {
				CustomCheckBox cb = getItemCheckBoxView((FrameLayout) view);
				String name = cb.getText().toString();
				String value = cb.getValue();
				pairs.add(new NameValuePair(name, value));
			}
		}
		return pairs;
	}
	
	public void updateIcons() {
		for(ImageView icon : annotationIcons) {
			updateAnnotationIcon(annotationIcons.indexOf(icon));
		}
		for(ImageView icon : certaintyIcons) {
			updateCertaintyIcon(certaintyIcons.indexOf(icon));
		}
	}

	public void setPairs(List<NameValuePair> pairs) {
		populate(pairs);
	}
	
	public void populate(List<NameValuePair> pairs) {
		if (pairs == null) return;
		annotations = new ArrayList<String>();
		annotationIcons = new ArrayList<ImageView>();
		certainties = new ArrayList<String>();
		certaintyIcons = new ArrayList<ImageView>();
		removeAllViews();
		for (NameValuePair pair : pairs) {
			annotations.add("");
			certainties.add("1.0");
			FrameLayout layout = createGroupItem(pair, pairs.indexOf(pair));
			addView(layout);
		}
	}
	
	private FrameLayout createGroupItem(NameValuePair item, int index) {
		FrameLayout layout = new FrameLayout(getContext());
		
		Button buttonOverlay = new Button(getContext());
		buttonOverlay.setBackgroundColor(Color.TRANSPARENT);
		buttonOverlay.setBackgroundResource(R.drawable.label_selector);
		layout.addView(buttonOverlay);
		
		LinearLayout innerLayout = new LinearLayout(getContext());

		CustomCheckBox checkBox = new CustomCheckBox(this.getContext());
		checkBox.setText(item.getName());
		checkBox.setValue(item.getValue());
		checkBox.setOnClickListener(customListener);
		
		LinearLayout checkContainer =  new LinearLayout(getContext());
		checkContainer.addView(checkBox);
		LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		checkParams.weight = 0.80F;
		innerLayout.addView(checkContainer, checkParams);
		
		final CheckBoxGroupLabelDialog dialog = new CheckBoxGroupLabelDialog(getContext(), this, index);
		if (annotationEnabled) {
			ImageView annotationImage = viewFactory.createAnnotationIcon();
			innerLayout.addView(annotationImage, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			dialog.addAnnotationTab();
			annotationIcons.add(annotationImage);
		}
		if (certaintyEnabled) {
			ImageView certaintyImage = viewFactory.createCertaintyIcon();
			innerLayout.addView(certaintyImage, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			dialog.addCertaintyTab();
			certaintyIcons.add(certaintyImage);
		}
		layout.addView(innerLayout);
		
		buttonOverlay.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				dialog.show();
				return false;
			}
		});
		
		return layout;
	}

	@Override
	public boolean getAnnotationEnabled() {
		return annotationEnabled;
	}

	@Override
	public void setAnnotationEnabled(boolean enabled) {
		annotationEnabled = enabled;
	}

	@Override
	public boolean getCertaintyEnabled() {
		return certaintyEnabled;
	}

	@Override
	public void setCertaintyEnabled(boolean enabled) {
		certaintyEnabled = enabled;
	}
	
	@Override
	public void setOnClickListener(OnClickListener l)
	{
		this.listener = l;
	}
	
	protected void notifySave() {
		if (getAttributeName() != null && hasChanges()) {
			autoSaveManager.save();
		}
	}

	public boolean hasMultiAttributeChanges(Module module,
			HashMap<String, ArrayList<Attribute>> attributes) {
		return Compare.compareMultiAttributeValues(this, attributes, module);
	}

	@Override
	public String getClickCallback() {
		return clickCallback;
	}

	@Override
	public void setClickCallback(String code) {
		if (code == null) return;
		clickCallback = code;
		setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				linker.execute(clickCallback);
			}
		});
	}
	
	@Override
	public String getSelectCallback() {
		return null;
	}

	@Override
	public void setSelectCallback(String code) {
	}

	@Override
	public String getFocusCallback() {
		return focusCallback;
	}
	
	@Override
	public String getBlurCallback() {
		return blurCallback;
	}
	
	@Override
	public void setFocusBlurCallbacks(String focusCode, String blurCode) {
		if (focusCode == null && blurCode == null) return;
		focusCallback = focusCode;
		blurCallback = blurCode;
		setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					linker.execute(focusCallback);
				} else {
					linker.execute(blurCallback);
				}
			}
		});
	}

	@Override
	public boolean hasAttributeChanges(
			HashMap<String, ArrayList<Attribute>> attributes) {
		return Compare.compareAttributeValues(this, attributes);
	}

}
