package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.View;
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
import au.org.intersect.faims.android.util.Arch16n;
import au.org.intersect.faims.android.util.Compare;

import com.google.inject.Inject;

public abstract class CustomFileList extends LinearLayout implements ICustomView {
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	@Inject
	BeanShellLinker linker;
	
	@Inject
	Arch16n arch16n;
	
	private String ref;
	private boolean dynamic;
	private boolean sync;
	
	protected List<NameValuePair> currentValues;
	protected List<String> currentCertainties;
	protected List<String> currentAnnotations;
	protected List<String> certainties;
	protected List<String> annotations;
	protected boolean dirty;
	protected String dirtyReason;
	protected boolean annotationEnabled;
	protected boolean certaintyEnabled;
	protected FormInputDef inputDef;
	
	private String focusCallback;
	private String blurCallback;
	
	protected List<ImageView> annotationIcons;
	protected List<ImageView> certaintyIcons;
	
	protected ViewFactory viewFactory;
	
	private List<NameValuePair> reloadPairs;
	
	public CustomFileList(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CustomFileList(Context context, FormInputDef inputDef, String ref, boolean dynamic, boolean sync) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		
		setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
	    setOrientation(LinearLayout.VERTICAL);
	    
		this.inputDef = inputDef;
		this.ref = ref;
		this.dynamic = dynamic;
		this.sync = sync;
		
		this.viewFactory = new ViewFactory(new WeakReference<ShowModuleActivity>(linker.getActivity()), arch16n);
		
	    reset();
	}
	
	public boolean getSync() {
		return this.sync;
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
	public String getClickCallback() {
		return null;
	}

	@Override
	public void setClickCallback(String code) {
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
	public String getAttributeName() {
		return inputDef.name;
	}

	@Override
	public String getAttributeType() {
		return inputDef.type;
	}

	@Override
	public String getValue() {
		return null;
	}

	@Override
	public void setValue(String value) {
	}

	@Override
	public float getCertainty() {
		return 0;
	}

	@Override
	public void setCertainty(float certainty) {
	}

	@Override
	public String getAnnotation() {
		return null;
	}

	@Override
	public void setAnnotation(String annotation) {
	}
	
	public void setAnnotations(List<String> annotations) {
		this.annotations = annotations;
	}
	
	public List<String> getAnnotations() {
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
	
	private void updateAnnotationIcon(int index) {
		if (index < annotations.size() && index < annotationIcons.size()
				&& annotationIcons.get(index) != null) {
			if (!FaimsSettings.DEFAULT_ANNOTATION.equals(annotations.get(index)) && annotations.get(index) != null) {
				annotationIcons.get(index).setImageResource(R.drawable.annotation_entered);
			} else {
				annotationIcons.get(index).setImageResource(R.drawable.annotation);
			}
		}
	}
	
	@Override
	public void setAnnotationIcon(ImageView annotationIcon) {
	}
	
	public List<String> getCertainties() {
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
	
	private void updateCertaintyIcon(int index) {
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
	public void setCertaintyIcon(ImageView certaintyIcon) {
	}
	
	public void setCertainties(List<String> certainties) {
		this.certainties= certainties;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean hasChanges() {
		return !(Compare.compareValues((List<NameValuePair>) getValues(), currentValues)) || 
				!(Compare.compareValues(currentAnnotations, annotations)) || 
				!(Compare.compareValues(currentCertainties, certainties));
	}

	protected void notifySave() {
		if (getAttributeName() != null && hasChanges()) {
			autoSaveManager.save();
		}
	}
	
	@Override
	public boolean hasAttributeChanges(
			HashMap<String, ArrayList<Attribute>> attributes) {
		return Compare.compareAttributeValues(this, attributes);
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
		dirtyReason = reason;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void save() {
		currentValues = (List<NameValuePair>) getValues();
		currentAnnotations = new ArrayList<String>(getAnnotations());
		currentCertainties = new ArrayList<String>(getCertainties());
	}

	@Override
	public void setValues(List<?> values) {
	}

	public List<NameValuePair> getPairs() {
		return null;
	}
	
	public void setPairs(List<NameValuePair> pairs) {
		populate(pairs);
	}
	
	protected void populate(List<NameValuePair> pairs) {
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

	public void addFile(String value, String annotation, String certainty) {
		if (annotations == null) {
			annotations = new ArrayList<String>();
		}
		annotations.add(annotation);
		updateAnnotationIcon(annotations.indexOf(annotation));
		if (certainties == null) {
			certainties = new ArrayList<String>();
		}
		certainties.add(certainty);
		updateCertaintyIcon(certainties.indexOf(certainty));
		notifySave();
	}
	
	public void addFile(String value) {
		File file = new File(value);
		if (file.exists()) {
			addFile(value, FaimsSettings.DEFAULT_ANNOTATION, String.valueOf(FaimsSettings.DEFAULT_CERTAINTY));
		} else {
			linker.showWarning("Logic Error", "Cannot find file " + value + " to attach to " + ref);
		}
	}
	
	public void setReloadPairs(List<NameValuePair> pairs) {
		this.reloadPairs = pairs;
	}
	
	public void updateIcons() {
		for(ImageView icon : annotationIcons) {
			updateAnnotationIcon(annotationIcons.indexOf(icon));
		}
		for(ImageView icon : certaintyIcons) {
			updateCertaintyIcon(certaintyIcons.indexOf(icon));
		}
	}
	
	public void reload() {
		if (reloadPairs == null) return;
		setPairs(reloadPairs);
		save();
		reloadPairs = null;
	}

	public boolean hasMultiAttributeChanges(Module module,
			HashMap<String, ArrayList<Attribute>> attributes) {
		return Compare.compareFileAttributeValues(this, attributes, module);
	}
	
}
