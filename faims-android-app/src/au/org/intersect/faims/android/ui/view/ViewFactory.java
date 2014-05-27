package au.org.intersect.faims.android.ui.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.javarosa.core.model.SelectChoice;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.text.InputType;
import android.text.format.Time;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.ui.map.MapLayout;
import au.org.intersect.faims.android.util.Arch16n;
import au.org.intersect.faims.android.util.ScaleUtil;

public class ViewFactory {

	public static final int MARGIN = 10;
	public static final int TEXT_SIZE = 10;
	public static final int BUTTON_SIZE = 34;
	public static final int TEXT_AREA_SIZE = 5;
	
	class DefaultLayoutParams extends LayoutParams {
		
		public DefaultLayoutParams(int arg0, int arg1) {
			super(arg0, arg1);
			this.topMargin = MARGIN;
		}
		
	}
	
	private WeakReference<Context> contextRef;
	private Arch16n arch16n;
	
	public ViewFactory(WeakReference<Context> contextRef, Arch16n arch16n) {
		this.contextRef = contextRef;
		this.arch16n = arch16n;
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	protected TextView createLabel(FormAttribute attribute) {
		TextView textView = new TextView(context());
        String inputText = attribute.questionText;
        inputText = arch16n.substituteValue(inputText);
        textView.setText(inputText);
        return textView;
	}
	
	protected Button createCertaintyButton() {
		return createButton("C");
	}
	
	protected Button createAnnotationButton() {
		return createButton("A");
	}
	
	protected Button createDirtyButton() {
		return createButton("\u26A0");
	}

	protected Button createInfoButton() {
		return createButton("?");
	}
	
	protected Table createTableView() {
		return new Table(context());
	}

	protected CustomEditText createTextField(int type, FormAttribute attribute, String ref) {
		CustomEditText text = new CustomEditText(context(), attribute, ref);
    	if (attribute.readOnly) {
    		text.setEnabled(false);
    	}
    	if (type >= 0) text.setInputType(type);
    	return text;
	}
	
	protected CustomEditText createIntegerTextField(FormAttribute attribute, String ref) {
    	return createTextField(InputType.TYPE_CLASS_NUMBER, attribute, ref);
	}
	
	protected CustomEditText createDecimalTextField(FormAttribute attribute, String ref) {
        return createTextField(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, attribute, ref);
	}
	
	protected CustomEditText createLongTextField(FormAttribute attribute, String ref) {
        return createTextField(InputType.TYPE_CLASS_NUMBER, attribute, ref);
	}
	
	protected CustomEditText createTextArea(FormAttribute attribute, String ref) {
		CustomEditText text = createTextField(-1, attribute, ref);
    	text.setLines(TEXT_AREA_SIZE);
    	return text;
	}
	
	protected CustomDatePicker createDatePicker(FormAttribute attribute, String ref) {
		CustomDatePicker date = new CustomDatePicker(context(), attribute, ref);
    	Time now = new Time();
		now.setToNow();
		date.updateDate(now.year, now.month, now.monthDay);
		if (attribute.readOnly) {
    		date.setEnabled(false);
    	}
    	return date;
	}
	
	protected CustomTimePicker createTimePicker(FormAttribute attribute, String ref) {
		CustomTimePicker time = new CustomTimePicker(context(), attribute, ref);
    	Time timeNow = new Time();
        timeNow.setToNow();
		time.setCurrentHour(timeNow.hour);
		time.setCurrentMinute(timeNow.minute);
		if (attribute.readOnly) {
    		time.setEnabled(false);
    	}
		return time;
	}
	
	protected CustomRadioGroup createRadioGroup(FormAttribute attribute, String ref) {
		CustomRadioGroup radioGroup = new CustomRadioGroup(context(), attribute, ref);
		
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (final SelectChoice selectChoice : attribute.selectChoices) {
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
        	pairs.add(new NameValuePair(innerText, selectChoice.getValue()));
        }
		radioGroup.populate(pairs);
		
		return radioGroup;  
	}
	
	protected HierarchicalSpinner createDropDown(FormAttribute attribute, String ref) {
		HierarchicalSpinner spinner = new HierarchicalSpinner(context(), attribute, ref);
		
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
            choices.add(pair);
        }
        
        ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
                context(),
                R.layout.multiline_spinner_dropdown_item,
                choices);
        spinner.setAdapter(arrayAdapter);
        spinner.reset();
        
        return spinner;
	}
	
	protected CustomCheckBoxGroup createCheckListGroup(FormAttribute attribute, String ref) {
		CustomCheckBoxGroup checkboxGroup = new CustomCheckBoxGroup(
                context(), attribute, ref);
        
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
            choices.add(pair);
        }
        
        return checkboxGroup;
	}
	
	protected FileListGroup createFileListGroup(FormAttribute attribute, String ref) {
		FileListGroup audioListGroup = new FileListGroup(
                context(), attribute, attribute.sync, ref);
        return audioListGroup;
	}
	
	protected CustomListView createList(FormAttribute attribute) {
		CustomListView list = new CustomListView(context());
		
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
            choices.add(pair);
        }
        
        ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
                context(),
                android.R.layout.simple_list_item_1,
                choices);
        list.setAdapter(arrayAdapter);
        
        return list;
	}
	
	protected Button createTrigger(FormAttribute attribute) {
		 CustomButton button = new CustomButton(context());
         String questionText = arch16n.substituteValue(attribute.questionText);
         button.setText(questionText);
         return button;
	}
	
	protected PictureGallery createPictureGallery(FormAttribute attribute, String ref, boolean isMulti) {
    	if (isMulti) {
    		return new PictureGallery(context(), attribute, ref, isMulti);
    	}
    	return new HierarchicalPictureGallery(context(), attribute, ref);
	}
	    
	protected CameraPictureGallery createCameraPictureGallery(FormAttribute attribute, String ref) {
		return new CameraPictureGallery(context(), attribute, ref);
	}
	    
	protected VideoGallery createVideoGallery(FormAttribute attribute, String ref) {
		return new VideoGallery(context(), attribute, ref);
	}
	 
	protected MapLayout createMapView(LinearLayout linearLayout) {
		return new MapLayout(context());
	}
	
	private Button createButton(String label) {
		Button button = new Button(context());
		button.setBackgroundResource(R.drawable.square_button);
		int size = getDefaultSize();
		LayoutParams layoutParams = new DefaultLayoutParams(size, size);
		button.setLayoutParams(layoutParams);
		button.setText(label);
		button.setTextSize(TEXT_SIZE);
		return button;
	}
	
	private int getDefaultSize() {
		return (int) ScaleUtil.getDip(context(), BUTTON_SIZE);
	}
	
	private Context context() {
		return contextRef.get();
	}
	
}
