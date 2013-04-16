package au.org.intersect.faims.android.ui.form;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.text.InputType;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.util.DateUtil;

public class Tab {

	private Context context;
	private ScrollView scrollView;
	private LinearLayout linearLayout;
	private Map<String, String> viewReference;
	private Map<String, List<View>> viewMap;
	private Map<String, Object> valueReference;
	private List<View> viewList;
	private String name;
	private String label;
	private boolean hidden;
	//private boolean scrollable;
	private View view;
	private Arch16n arch16n;
	private static final String FREETEXT = "freetext";

	public Tab(Context context, String name, String label, boolean hidden, boolean scrollable, Arch16n arch16n) {
		this.context = context;
		this.name = name;
		this.arch16n = arch16n;
		label = this.arch16n.substituteValue(label);
		this.label = label;
		this.hidden = hidden;
		//this.scrollable = scrollable;
		
		this.linearLayout = new LinearLayout(context);
		this.viewReference = new HashMap<String, String>();
		this.valueReference = new HashMap<String, Object>();
		this.viewMap = new HashMap<String, List<View>>();
		this.viewList = new ArrayList<View>();
        linearLayout.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        
        linearLayout.setBackgroundColor(Color.WHITE);
		
        if (scrollable) {
        	this.scrollView = new ScrollView(this.context);
        	scrollView.addView(linearLayout);
        	this.view = scrollView;
        } else {
        	this.view = linearLayout;
        }
	}

	public View addInput(FormEntryPrompt input,String path, String viewName, String directory, boolean isArchEnt, boolean isRelationship) {
		LinearLayout fieldLinearLayout = new LinearLayout(this.context);
    	fieldLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
		if (input.getControlType() != Constants.CONTROL_TRIGGER) {
            TextView textView = new TextView(this.context);
            String inputText = input.getQuestionText();
            inputText = arch16n.substituteValue(inputText);
            textView.setText(inputText);
            fieldLinearLayout.addView(textView);
            linearLayout.addView(fieldLinearLayout);
        }
		
		viewReference.put(viewName, path);
		View view = null;
		CustomEditText text = null;
		String attributeName = input.getQuestion().getAdditionalAttribute(null, "faims_attribute_name");
		String attributeType = input.getQuestion().getAdditionalAttribute(null, "faims_attribute_type");
		String certainty = input.getQuestion().getAdditionalAttribute(null, "faims_certainty");
		String annotation = input.getQuestion().getAdditionalAttribute(null, "faims_annotation");
		String readOnly = input.getQuestion().getAdditionalAttribute(null, "faims_read_only");
		boolean isReadOnly = (readOnly != null && readOnly.equals("true"));
		attributeType = (attributeType == null) ? "freetext" : attributeType;
		Button certaintyButton = new Button(this.context);
		certaintyButton.setBackgroundResource(R.drawable.square_button);
		int size = getDpi(30);
		LayoutParams layoutParams = new LayoutParams(size, size);
		layoutParams.topMargin = 10;
		certaintyButton.setLayoutParams(layoutParams);
		certaintyButton.setText("C");
		certaintyButton.setTextSize(10);
		if(isArchEnt || isRelationship){
			if(!"false".equals(certainty)){
				fieldLinearLayout.addView(certaintyButton);
			}
		}
		Button annotationButton = new Button(this.context);
		annotationButton.setBackgroundResource(R.drawable.square_button);
		annotationButton.setLayoutParams(layoutParams);
		annotationButton.setText("A");
		annotationButton.setTextSize(10);
		if(!FREETEXT.equals(attributeType)){
			if(!"false".equals(annotation)) {
				fieldLinearLayout.addView(annotationButton);
			}
		}
		// check the control type to know the type of the question
        switch (input.getControlType()) {
            case Constants.CONTROL_INPUT:
                // check the data type of question of type input
                switch (input.getDataType()) {
                // set input type as number
                    case Constants.DATATYPE_INTEGER:
                    	text = new CustomEditText(this.context, attributeName, attributeType, path);
                    	if (isReadOnly) {
                    		text.setEnabled(false);
                    	}
                    	view = text;
                        ((TextView) view)
                                .setInputType(InputType.TYPE_CLASS_NUMBER);
                        onCertaintyButtonClicked(certaintyButton, text);
                        onAnnotationButtonClicked(annotationButton, text);
                        linearLayout.addView(view);
                        valueReference.put(path, "");
                        break;
                    case Constants.DATATYPE_DECIMAL:
                    	text = new CustomEditText(this.context, attributeName, attributeType, path);
                        view = text;
                        if (isReadOnly) {
                    		text.setEnabled(false);
                    	}
                        ((TextView) view)
                                .setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        onCertaintyButtonClicked(certaintyButton, text);
                        onAnnotationButtonClicked(annotationButton, text);
                        linearLayout.addView(view);
                        valueReference.put(path, "");
                        break;
                    case Constants.DATATYPE_LONG:
                    	text = new CustomEditText(this.context, attributeName, attributeType, path);
                        view = text;
                        if (isReadOnly) {
                    		text.setEnabled(false);
                    	}
                        ((TextView) view)
                                .setInputType(InputType.TYPE_CLASS_NUMBER);
                        onCertaintyButtonClicked(certaintyButton, text);
                        onAnnotationButtonClicked(annotationButton, text);
                        linearLayout.addView(view);
                        break;
                    // set input type as date picker
                    case Constants.DATATYPE_DATE:
                    	CustomDatePicker date = new CustomDatePicker(this.context, attributeName, attributeType, path);
                    	Time now = new Time();
        				now.setToNow();
        				date.updateDate(now.year, now.month, now.monthDay);
        				if (isReadOnly) {
                    		date.setEnabled(false);
                    	}
                    	view = date;
                    	onCertaintyButtonClicked(certaintyButton, date);
                    	onAnnotationButtonClicked(annotationButton, date);
                    	linearLayout.addView(view);
                        valueReference.put(path, DateUtil.getDate(date));
                        break;
                    // get the text area
                    case Constants.DATATYPE_TEXT:
                    	text = new CustomEditText(this.context, attributeName, attributeType, path);
                    	if (isReadOnly) {
                    		text.setEnabled(false);
                    	}
                    	view = text;
                        ((TextView) view).setLines(5);
                        onCertaintyButtonClicked(certaintyButton, text);
                        onAnnotationButtonClicked(annotationButton, text);
                        linearLayout.addView(view);
                        valueReference.put(path, "");
                        break;
                    // set input type as time picker
                    case Constants.DATATYPE_TIME:
                    	CustomTimePicker time = new CustomTimePicker(this.context, attributeName, attributeType, path);
                    	Time timeNow = new Time();
                        timeNow.setToNow();
        				time.setCurrentHour(timeNow.hour);
        				time.setCurrentMinute(timeNow.minute);
        				view = time;
        				if (isReadOnly) {
                    		time.setEnabled(false);
                    	}
        				onCertaintyButtonClicked(certaintyButton, time);
        				onAnnotationButtonClicked(annotationButton, time);
        				linearLayout.addView(view);
        				valueReference.put(path, DateUtil.getTime(time));
                        break;
                    // default is edit text
                    default:
                    	// check if map type
                    	if ("true".equalsIgnoreCase(input.getQuestion().getAdditionalAttribute(null, "faims_map"))) {
                    		RelativeLayout mapLayout = new RelativeLayout(this.context);
                    		mapLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
                    		
                    		DrawView drawView = new DrawView(this.context);
                    		drawView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    		
                    		CustomMapView mapView = new CustomMapView(this.context, drawView);

                    		mapView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                    		mapView.startMapping();
                    		
                    		mapLayout.addView(mapView);
                    		mapLayout.addView(drawView);
                    		
                    		view = mapView;
                    		linearLayout.addView(mapLayout);
                    	} else {
                    		text = new CustomEditText(this.context, attributeName, attributeType, path);
                            view = text;
                            if (isReadOnly) {
                        		text.setEnabled(false);
                        	}
                            valueReference.put(path, "");
                            onCertaintyButtonClicked(certaintyButton, text);
                            linearLayout.addView(view);
                    	}
                        break;
                }
                if(attributeName != null){
                	addViewMappings(attributeName, view);
                }
                break;
            // uploading image by using camera
                /*
            case Constants.CONTROL_IMAGE_CHOOSE:
                Button imageButton = new Button(this.context);
                imageButton.setText("Choose Image");

                final ImageView imageView = new ImageView(this.context);
                imageButton.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent cameraIntent = new Intent(
                                MediaStore.ACTION_IMAGE_CAPTURE);
                        UIRenderer.this.imageView = imageView;
                        ((ShowProjectActivity) UIRenderer.this.context)
                                .startActivityForResult(cameraIntent,
                                        ShowProjectActivity.CAMERA_REQUEST_CODE);
                    }
                });
               
                layout.addView(imageButton);
                layout.addView(imageView);
                break;
                 */
            // create control for select one showing it as drop down
            case Constants.CONTROL_SELECT_ONE:
                switch (input.getDataType()) {
                    case Constants.DATATYPE_CHOICE:
                    	QuestionDef qd = input.getQuestion();
                        // check if the type if image to create image slider
                        if ("image".equalsIgnoreCase(input.getQuestion()
                                .getAdditionalAttribute(null, "type"))) {
                            view = renderImageSliderForSingleSelection(input, directory, attributeName, attributeType, path);
                            linearLayout.addView(view);
                            onCertaintyButtonClicked(certaintyButton, view);
                            onAnnotationButtonClicked(annotationButton, view);
                            valueReference.put(path, "");
                        }
                        // Radio Button
                        else if ("full".equalsIgnoreCase(qd.getAppearanceAttr()) ) {
                        	CustomLinearLayout selectLayout = new CustomLinearLayout(this.context, attributeName, attributeType, path);
                            selectLayout.setLayoutParams(new LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.MATCH_PARENT));
                            selectLayout.setOrientation(LinearLayout.VERTICAL);
                            RadioGroup radioGroupLayout = new RadioGroup(this.context);
                            radioGroupLayout.setOrientation(LinearLayout.HORIZONTAL);
                            for (final SelectChoice selectChoice : input.getSelectChoices()) {
                            	CustomRadioButton radioButton = new CustomRadioButton(this.context);
                            	String innerText = selectChoice.getLabelInnerText();
                            	innerText = arch16n.substituteValue(innerText);
                                radioButton.setText(innerText);
                                radioButton.setValue(selectChoice.getValue());
                                radioGroupLayout.addView(radioButton);
                            }
                            selectLayout.addView(radioGroupLayout);
                            view = selectLayout;
                            onCertaintyButtonClicked(certaintyButton, selectLayout);
                            onAnnotationButtonClicked(annotationButton, selectLayout);
                            linearLayout.addView(selectLayout);
                            valueReference.put(path, "");
                        // List
                        } else if ("compact".equalsIgnoreCase(qd.getAppearanceAttr()) ) {
                        	CustomListView list = new CustomListView(this.context);
                            List<NameValuePair> choices = new ArrayList<NameValuePair>();
                            for (final SelectChoice selectChoice : input
                                    .getSelectChoices()) {
                            	String innerText = selectChoice.getLabelInnerText();
                            	innerText = arch16n.substituteValue(innerText);
                            	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
                                choices.add(pair);
                            }
                            ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
                                    this.context,
                                    android.R.layout.simple_list_item_1,
                                    choices);
                            list.setAdapter(arrayAdapter);
                            view = list;
                            linearLayout.addView(list);
                        // Default is single select dropdown
                        } else {
                        	CustomSpinner spinner = new CustomSpinner(this.context, attributeName, attributeType, path);
                            List<NameValuePair> choices = new ArrayList<NameValuePair>();
                            for (final SelectChoice selectChoice : input
                                    .getSelectChoices()) {
                            	String innerText = selectChoice.getLabelInnerText();
                            	innerText = arch16n.substituteValue(innerText);
                            	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
                                choices.add(pair);
                            }
                            ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
                                    this.context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    choices);
                            spinner.setAdapter(arrayAdapter);
                            spinner.setSelection(0);
                            view = spinner;
                            onCertaintyButtonClicked(certaintyButton, view);
                            onAnnotationButtonClicked(annotationButton, view);
                            linearLayout.addView(spinner);
                            NameValuePair pair = (NameValuePair) spinner.getSelectedItem();
            				valueReference.put(path, pair.getValue());
                        }
                        break;
                }
                if(attributeName != null){
                	addViewMappings(attributeName, view);
                }
                break;
            // create control for multi select, showing it as checkbox
            case Constants.CONTROL_SELECT_MULTI:
                switch (input.getDataType()) {
                    case Constants.DATATYPE_CHOICE_LIST:
                    	CustomLinearLayout selectLayout = new CustomLinearLayout(
                                this.context, attributeName, attributeType, path);
                        selectLayout.setLayoutParams(new LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.MATCH_PARENT));
                        selectLayout.setOrientation(LinearLayout.VERTICAL);
                        for (final SelectChoice selectChoice : input
                                .getSelectChoices()) {
                        	CustomCheckBox checkBox = new CustomCheckBox(this.context);
                        	String innerText = selectChoice.getLabelInnerText();
                        	innerText = arch16n.substituteValue(innerText);
                            checkBox.setText(innerText);
                            checkBox.setValue(selectChoice.getValue());
                            selectLayout.addView(checkBox);
                        }
                        view = selectLayout;
                        linearLayout.addView(selectLayout);
                        onCertaintyButtonClicked(certaintyButton, selectLayout);
                        onAnnotationButtonClicked(annotationButton, selectLayout);
                        valueReference.put(path, new ArrayList<NameValuePair>());
                }
                if(attributeName != null){
                	addViewMappings(attributeName, view);
                }
                break;
            // create control for trigger showing as a button
            case Constants.CONTROL_TRIGGER:
                Button button = new Button(this.context);
                button.setText(input.getQuestionText());
                view = button;
                linearLayout.addView(button);
                break;
        }
        
        return view;
	}

	private void onAnnotationButtonClicked(Button annotationButton, final View view) {
		annotationButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final EditText editText = new EditText(v.getContext());
				if (view instanceof CustomEditText){
	        		CustomEditText customEditText = (CustomEditText) view;
	        		editText.setText(customEditText.getCurrentAnnotation());
	        	}else if (view instanceof CustomLinearLayout){
	        		CustomLinearLayout customLinearLayout = (CustomLinearLayout) view;
	        		editText.setText(customLinearLayout.getCurrentAnnotation());
	        	}else if (view instanceof CustomHorizontalScrollView){
	        		CustomHorizontalScrollView customHorizontalScrollView = (CustomHorizontalScrollView) view;
	        		editText.setText(customHorizontalScrollView.getCurrentAnnotation());
	        	}else if (view instanceof CustomSpinner){
	        		CustomSpinner customSpinner = (CustomSpinner) view;
	        		editText.setText(customSpinner.getCurrentAnnotation());
	        	}
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				
				builder.setTitle("Annotation");
				builder.setMessage("Set the annotation text for the field");
				builder.setView(editText);
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				        	if (view instanceof CustomEditText){
				        		CustomEditText customEditText = (CustomEditText) view;
				        		customEditText.setCurrentAnnotation(editText.getText().toString());
				        	}else if (view instanceof CustomLinearLayout){
				        		CustomLinearLayout customLinearLayout = (CustomLinearLayout) view;
				        		customLinearLayout.setCurrentAnnotation(editText.getText().toString());
				        	}else if (view instanceof CustomHorizontalScrollView){
				        		CustomHorizontalScrollView customHorizontalScrollView = (CustomHorizontalScrollView) view;
				        		customHorizontalScrollView.setCurrentAnnotation(editText.getText().toString());
				        	}else if (view instanceof CustomSpinner){
				        		CustomSpinner customSpinner = (CustomSpinner) view;
				        		customSpinner.setCurrentAnnotation(editText.getText().toString());
				        	}
				        }
				    });
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				            // User cancelled the dialog
				        }
				    });
				
				builder.create().show();
			}
		});
	}

	private void onCertaintyButtonClicked(Button certaintyButton,final View view) {
		certaintyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				LinearLayout layout = new LinearLayout(v.getContext());
				layout.setOrientation(LinearLayout.VERTICAL);
				final SeekBar seekBar = new SeekBar(v.getContext());
				float certainty = 0;
				seekBar.setMax(100);
				seekBar.setMinimumWidth(400);
				if (view instanceof CustomEditText){
	        		CustomEditText customEditText = (CustomEditText) view;
	        		certainty = customEditText.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}else if (view instanceof CustomDatePicker){
	        		CustomDatePicker customDatePicker = (CustomDatePicker) view;
	        		certainty = customDatePicker.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}else if (view instanceof CustomTimePicker){
	        		CustomTimePicker customTimePicker = (CustomTimePicker) view;
	        		certainty = customTimePicker.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}else if (view instanceof CustomLinearLayout){
	        		CustomLinearLayout customLinearLayout = (CustomLinearLayout) view;
	        		certainty = customLinearLayout.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}else if (view instanceof CustomHorizontalScrollView){
	        		CustomHorizontalScrollView customHorizontalScrollView = (CustomHorizontalScrollView) view;
	        		certainty = customHorizontalScrollView.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}else if (view instanceof CustomSpinner){
	        		CustomSpinner customSpinner = (CustomSpinner) view;
	        		certainty = customSpinner.getCurrentCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
	        	}
				
				final TextView text = new TextView(v.getContext());
				text.setText("    Certainty: " + certainty);
				seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}
					
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}
					
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
						text.setText("    Certainty: " + ((float) progress)/100);
					}
				});
				layout.addView(text);
				layout.addView(seekBar);
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				
				builder.setTitle("Certainty");
				builder.setMessage("Set the certainty value for the question");
				builder.setView(layout);
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				        	if (view instanceof CustomEditText){
				        		CustomEditText customEditText = (CustomEditText) view;
				        		customEditText.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}else if (view instanceof CustomDatePicker){
				        		CustomDatePicker customDatePicker = (CustomDatePicker) view;
				        		customDatePicker.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}else if (view instanceof CustomTimePicker){
				        		CustomTimePicker customTimePicker = (CustomTimePicker) view;
				        		customTimePicker.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}else if (view instanceof CustomLinearLayout){
				        		CustomLinearLayout customLinearLayout = (CustomLinearLayout) view;
				        		customLinearLayout.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}else if (view instanceof CustomHorizontalScrollView){
				        		CustomHorizontalScrollView customHorizontalScrollView = (CustomHorizontalScrollView) view;
				        		customHorizontalScrollView.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}else if (view instanceof CustomSpinner){
				        		CustomSpinner customSpinner = (CustomSpinner) view;
				        		customSpinner.setCurrentCertainty(((float)seekBar.getProgress())/100);
				        	}
				        }
				    });
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				            // User cancelled the dialog
				        }
				    });
				
				builder.create().show();
				
			}
		});
	}

	public TabSpec createTabSpec(TabHost tabHost) {
		TabSpec tabSpec = tabHost.newTabSpec(name);
		
		tabSpec.setContent(new TabContentFactory() {

            @Override
            public View createTabContent(String tag) {
            	return view;
            }
        });
        
        tabSpec.setIndicator(label);
        
		return tabSpec;
	}
	
	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}
	
	public boolean getHidden() {
		return hidden;
	}

	public boolean hasView(String name){
		return this.viewMap.containsKey(name);
	}

	public String getPath(String viewName){
		return this.viewReference.get(viewName);
	}

	private void addViewMappings(String name, View view){
		if(this.viewMap.containsKey(name)){
			this.viewMap.get(name).add(view);
		}else{
			List<View> views = new ArrayList<View>();
			views.add(view);
			this.viewMap.put(name, views);
		}
		viewList.add(view);
	}
	
	public List<View> getAllViews(){
		return this.viewList;
	}

	public List<View> getViews(String name) {
		return this.viewMap.get(name);
	}
	
	public Object getStoredValue(String ref){
		return this.valueReference.get(ref);
	}

	public void setValueReference(String ref, Object value){
		this.valueReference.put(ref, value);
	}

	/**
     * Rendering image slide for select one
     * 
     * @param layout
     * @param questionPrompt
	 * @param path2 
	 * @param attributeType 
	 * @param attributeName 
     */
    private CustomHorizontalScrollView renderImageSliderForSingleSelection(final FormEntryPrompt questionPrompt, String directory, String attributeName, String attributeType, String path) {
    	final CustomHorizontalScrollView horizontalScrollView = new CustomHorizontalScrollView(this.context, attributeName, attributeType, path);
        LinearLayout galleriesLayout = new LinearLayout(this.context);
        galleriesLayout.setOrientation(LinearLayout.HORIZONTAL);
        final List<CustomImageView> galleryImages = new ArrayList<CustomImageView>();
        for (final SelectChoice selectChoice : questionPrompt
                .getSelectChoices()) {
        	final String picturePath = Environment.getExternalStorageDirectory() + directory + "/" + selectChoice.getValue();
        	File pictureFolder = new File(picturePath);
        	if(pictureFolder.exists()){
	        	for(final String name : pictureFolder.list()){
	        		LinearLayout galleryLayout = new LinearLayout(this.context);
	        		galleryLayout.setOrientation(LinearLayout.VERTICAL);
	        		CustomImageView gallery = new CustomImageView(this.context);
	        		int size = getDpi(400);
	        		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(size, size);
	                gallery.setImageURI(Uri.parse(path+"/"+name));
	                gallery.setBackgroundColor(Color.RED);
	                gallery.setPadding(10, 10, 10, 10);
	                gallery.setLayoutParams(layoutParams);
	                gallery.setPicture(null);
	                gallery.setOnClickListener(new OnClickListener() {
	
	                    @Override
	                    public void onClick(View v) {
	                    	CustomImageView selectedImageView = (CustomImageView) v;
	                        horizontalScrollView.setSelectedImageView(selectedImageView);
	                        for (CustomImageView view : galleryImages) {
	                            if (view.equals(selectedImageView)) {
	                                view.setBackgroundColor(Color.GREEN);
	                            } else {
	                                view.setBackgroundColor(Color.RED);
	                            }
	                        }
	                    }
	                });
	                TextView textView = new TextView(this.context);
	                textView.setText(name);
	                textView.setGravity(Gravity.CENTER_HORIZONTAL);
	                textView.setTextSize(20);
	                galleryLayout.addView(textView);
	                galleryImages.add(gallery);
	                galleryLayout.addView(gallery);
	                galleriesLayout.addView(galleryLayout);
	        	}
	        	horizontalScrollView.setImageViews(galleryImages);
	        }
        }
        horizontalScrollView.addView(galleriesLayout);
        return horizontalScrollView;
    }

	/*
    public ImageView getCurrentImageView() {
        return this.imageView;
    }

    public void clearCurrentImageView() {
        this.imageView = null;
    }
    */

	public void clearViews() {
		for (View v : viewList) {
			if (v instanceof CustomEditText) {
				CustomEditText text = (CustomEditText) v;
				text.setText("");
				text.setCertainty(1);
				text.setAnnotation("");
				text.setCurrentCertainty(1);
				text.setCurrentAnnotation("");
				valueReference.put(text.getRef(), "");
			} else if (v instanceof CustomDatePicker) {
				CustomDatePicker date = (CustomDatePicker) v;
				Time now = new Time();
				now.setToNow();
				date.updateDate(now.year, now.month, now.monthDay);
				date.setCertainty(1);
				date.setCurrentCertainty(1);
				valueReference.put(date.getRef(), DateUtil.getDate(date));
			} else if (v instanceof CustomTimePicker) {
				CustomTimePicker time = (CustomTimePicker) v;
				Time now = new Time();
				now.setToNow();
				time.setCurrentHour(now.hour);
				time.setCurrentMinute(now.minute);
				time.setCertainty(1);
				time.setCurrentCertainty(1);
				valueReference.put(time.getRef(), DateUtil.getTime(time));
			} else if (v instanceof CustomLinearLayout) {
				CustomLinearLayout layout = (CustomLinearLayout) v;
				layout.setCertainty(1);
				layout.setAnnotation("");
				layout.setCurrentCertainty(1);
				layout.setCurrentAnnotation("");
				View child0 = layout.getChildAt(0);
				
				if (child0 instanceof RadioGroup){
					RadioGroup rg = (RadioGroup) child0;
					rg.clearCheck();
					valueReference.put(layout.getRef(), "");
				}else if (child0 instanceof CheckBox){
					for(int i = 0; i < layout.getChildCount(); ++i){
						View view = layout.getChildAt(i);
						if (view instanceof CustomCheckBox){
							CustomCheckBox cb = (CustomCheckBox) view;
							cb.setChecked(false);
						}
					}
					valueReference.put(layout.getRef(), new ArrayList<NameValuePair>());
				}
			} else if (v instanceof CustomSpinner) {
				CustomSpinner spinner = (CustomSpinner) v;
				spinner.setSelection(0);
				spinner.setCertainty(1);
				spinner.setAnnotation("");
				spinner.setCurrentCertainty(1);
				spinner.setCurrentAnnotation("");
				NameValuePair pair = (NameValuePair) spinner.getSelectedItem();
				valueReference.put(spinner.getRef(), pair.getValue());
			} else if(v instanceof CustomHorizontalScrollView){
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) v;
				horizontalScrollView.setCertainty(1);
				horizontalScrollView.setAnnotation("");
				horizontalScrollView.setCurrentCertainty(1);
				horizontalScrollView.setCurrentAnnotation("");
				for(CustomImageView customImageView : horizontalScrollView.getImageViews()){
					customImageView.setBackgroundColor(Color.RED);
				}
				horizontalScrollView.setSelectedImageView(null);
			}
		}
	}
	
	private int getDpi(int size) {
		return (size * context.getResources().getDisplayMetrics().densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
	}

}
