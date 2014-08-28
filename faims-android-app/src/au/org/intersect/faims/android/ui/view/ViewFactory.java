package au.org.intersect.faims.android.ui.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.text.InputType;
import android.text.format.Time;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.map.MapLayout;
import au.org.intersect.faims.android.util.Arch16n;

import com.nativecss.NativeCSS;

public class ViewFactory {

	public static final int MARGIN = 10;
	public static final int TEXT_SIZE = 10;
	public static final int BUTTON_SIZE = 34;
	public static final int TEXT_AREA_SIZE = 5;
	public static final float LABEL_ICON_SCALE = 0.6F;
	
	class DefaultLayoutParams extends LayoutParams {
		
		public DefaultLayoutParams(int arg0, int arg1) {
			super(arg0, arg1);
			this.topMargin = MARGIN;
		}
		
	}
	
	private WeakReference<ShowModuleActivity> contextRef;
	private Arch16n arch16n;
	
	public ViewFactory(WeakReference<ShowModuleActivity> contextRef, Arch16n arch16n) {
		this.contextRef = contextRef;
		this.arch16n = arch16n;
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	protected TextView createLabel(String ref, FormInputDef attribute) {
		TextView textView = new TextView(context());
        String inputText = attribute.questionText;
        inputText = arch16n.substituteValue(inputText);
        textView.setText(inputText);
        NativeCSS.addCSSClass(textView, "label");
        NativeCSS.setCSSId(textView, ref + "-label");
        if (attribute.styleClass != null) {
        	NativeCSS.addCSSClass(textView, attribute.styleClass + "-label");
        }
        return textView;
	}
	
	protected ImageView createCertaintyIcon() {
		return createLabelIcon(R.drawable.certainty);
	}
	
	protected ImageView createAnnotationIcon() {
		return createLabelIcon(R.drawable.annotation);
	}
	
	protected ImageView createDirtyIcon() {
		return createLabelIcon(R.drawable.dirty);
	}

	protected ImageView createInfoIcon() {
		return createLabelIcon(R.drawable.info);
	}
	
	private ImageView createLabelIcon(int drawableResource) {
		ImageView image = new ImageView(context());
		image.setScaleType(ScaleType.FIT_CENTER);
		image.setScaleX(LABEL_ICON_SCALE);
		image.setScaleY(LABEL_ICON_SCALE);
		image.setImageResource(drawableResource);
		return image;
	}
	
	protected Table createTableView(String ref, boolean dynamic) {
		return new Table(context(), ref, dynamic);
	}
	
	protected CustomWebView createWebView(String ref, boolean dynamic) {
		return new CustomWebView(context(), ref, dynamic);
	}

	protected CustomEditText createTextField(int type, FormInputDef attribute, String ref, boolean dynamic) {
		CustomEditText text = new CustomEditText(context(), attribute, ref, dynamic);
    	if (attribute.readOnly) {
    		text.setEnabled(false);
    	}
    	if (type >= 0) {
    		text.setInputType(type);
    		NativeCSS.addCSSClass(text, "input-field");
    	}
    	return text;
	}
	
	protected CustomEditText createIntegerTextField(FormInputDef attribute, String ref, boolean dynamic) {
    	return createTextField(InputType.TYPE_CLASS_NUMBER, attribute, ref, dynamic);
	}
	
	protected CustomEditText createDecimalTextField(FormInputDef attribute, String ref, boolean dynamic) {
        return createTextField(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, attribute, ref, dynamic);
	}
	
	protected CustomEditText createLongTextField(FormInputDef attribute, String ref, boolean dynamic) {
        return createTextField(InputType.TYPE_CLASS_NUMBER, attribute, ref, dynamic);
	}
	
	protected CustomEditText createTextArea(FormInputDef attribute, String ref, boolean dynamic) {
		CustomEditText text = createTextField(-1, attribute, ref, dynamic);
    	text.setLines(TEXT_AREA_SIZE);
    	NativeCSS.addCSSClass(text, "text-area");
    	return text;
	}
	
	protected CustomDatePicker createDatePicker(FormInputDef attribute, String ref, boolean dynamic) {
		CustomDatePicker date = new CustomDatePicker(context(), attribute, ref, dynamic);
    	Time now = new Time();
		now.setToNow();
		date.updateDate(now.year, now.month, now.monthDay);
		if (attribute.readOnly) {
    		date.setEnabled(false);
    	}
    	return date;
	}
	
	protected CustomTimePicker createTimePicker(FormInputDef attribute, String ref, boolean dynamic) {
		CustomTimePicker time = new CustomTimePicker(context(), attribute, ref, dynamic);
    	Time timeNow = new Time();
        timeNow.setToNow();
		time.setCurrentHour(timeNow.hour);
		time.setCurrentMinute(timeNow.minute);
		if (attribute.readOnly) {
    		time.setEnabled(false);
    	}
		return time;
	}
	
	protected CustomRadioGroup createRadioGroup(FormInputDef attribute, String ref, boolean dynamic) {
		CustomRadioGroup radioGroup = new CustomRadioGroup(context(), attribute, ref, dynamic);
		
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		if (attribute.selectChoices != null && !attribute.selectChoices.isEmpty()) {
			for (final NameValuePair selectChoice : attribute.selectChoices) {
	        	String innerText = selectChoice.getName();
	        	innerText = arch16n.substituteValue(innerText);
	        	pairs.add(new NameValuePair(innerText, selectChoice.getValue()));
	        }
			radioGroup.populate(pairs);
		}
		
		return radioGroup;  
	}
	
	protected HierarchicalSpinner createDropDown(FormInputDef attribute, String ref, boolean dynamic) {
		HierarchicalSpinner spinner = new HierarchicalSpinner(context(), attribute, ref, dynamic);
		
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        if (attribute.selectChoices != null && !attribute.selectChoices.isEmpty()) {
	        for (final NameValuePair selectChoice : attribute.selectChoices) {
	        	String innerText = selectChoice.getName();
	        	innerText = arch16n.substituteValue(innerText);
	        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
	            choices.add(pair);
	        }
	        
	        ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
	                context(),
	                R.layout.multiline_spinner_dropdown_item,
	                choices);
	        spinner.setAdapter(arrayAdapter, true);
	        spinner.reset();
        }
        
        return spinner;
	}
	
	protected CustomCheckBoxGroup createCheckListGroup(FormInputDef attribute, String ref, boolean dynamic) {
		CustomCheckBoxGroup checkboxGroup = new CustomCheckBoxGroup(
                context(), attribute, ref, dynamic);
        
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        if (attribute.selectChoices != null && !attribute.selectChoices.isEmpty()) {
	        for (final NameValuePair selectChoice : attribute.selectChoices) {
	        	String innerText = selectChoice.getName();
	        	innerText = arch16n.substituteValue(innerText);
	        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
	            choices.add(pair);
	        }
        }
        
        return checkboxGroup;
	}
	
	protected FileListGroup createFileListGroup(FormInputDef attribute, String ref, boolean dynamic) {
		FileListGroup audioListGroup = new FileListGroup(
                context(), attribute, attribute.sync, ref, dynamic);
        return audioListGroup;
	}
	
	protected CustomListView createList(FormInputDef attribute, String ref, boolean dynamic) {
		CustomListView list = new CustomListView(context(), ref, dynamic);
		
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        if (attribute.selectChoices != null && !attribute.selectChoices.isEmpty()) {
	        for (final NameValuePair selectChoice : attribute.selectChoices) {
	        	String innerText = selectChoice.getName();
	        	innerText = arch16n.substituteValue(innerText);
	        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
	            choices.add(pair);
	        }
	        
	        ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
	                context(),
	                android.R.layout.simple_list_item_1,
	                choices);
	        list.setAdapter(arrayAdapter);
        }
        
        return list;
	}
	
	protected Button createTrigger(FormInputDef attribute, String ref, boolean dynamic) {
		 CustomButton button = new CustomButton(context(), ref, dynamic);
         String questionText = arch16n.substituteValue(attribute.questionText);
         button.setText(questionText);
         return button;
	}
	
	protected PictureGallery createPictureGallery(FormInputDef attribute, String ref, boolean dynamic, boolean isMulti) {
    	if (isMulti) {
    		return new PictureGallery(context(), attribute, ref, dynamic, isMulti);
    	}
    	return new HierarchicalPictureGallery(context(), attribute, ref, dynamic);
	}
	    
	protected CameraPictureGallery createCameraPictureGallery(FormInputDef attribute, String ref, boolean dynamic) {
		return new CameraPictureGallery(context(), attribute, attribute.sync, ref, dynamic);
	}
	    
	protected VideoGallery createVideoGallery(FormInputDef attribute, String ref, boolean dynamic) {
		return new VideoGallery(context(), attribute, attribute.sync, ref, dynamic);
	}
	 
	protected MapLayout createMapView(String ref, boolean dynamic) {
		return new MapLayout(context(), ref, dynamic);
	}
	
	private Context context() {
		return contextRef.get();
	}
	
}
