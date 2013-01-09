package au.org.intersect.faims.android.ui.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.TimePicker;

public class Tab {

	private Context context;
	private ScrollView scrollView;
	private LinearLayout linearLayout;
	private Map<String, String> viewReference;
	private String name;
	private String label;

	public Tab(Context context, String name, String label) {
		this.context = context;
		this.name = name;
		this.label = label;
		
		this.linearLayout = new LinearLayout(context);
		this.viewReference = new HashMap<String, String>();
        linearLayout.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        
        linearLayout.setBackgroundColor(Color.WHITE);
		
		this.scrollView = new ScrollView(this.context);
        scrollView.addView(linearLayout);
        
	}

	public View addInput(FormEntryPrompt input,String path, String viewName) {
		if (input.getControlType() != Constants.CONTROL_TRIGGER) {
            TextView textView = new TextView(this.context);
            textView.setText(input.getQuestionText());
            linearLayout.addView(textView);
        }
		
		viewReference.put(viewName, path);
		View view = null;
		// check the control type to know the type of the question
        switch (input.getControlType()) {
            case Constants.CONTROL_INPUT:
                // check the data type of question of type input
                switch (input.getDataType()) {
                // set input type as number
                    case Constants.DATATYPE_INTEGER:
                        view = new EditText(this.context);
                        ((TextView) view)
                                .setInputType(InputType.TYPE_CLASS_NUMBER);
                        linearLayout.addView(view);
                        break;
                    case Constants.DATATYPE_DECIMAL:
                        view = new EditText(this.context);
                        ((TextView) view)
                                .setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        linearLayout.addView(view);
                        break;
                    case Constants.DATATYPE_LONG:
                        view = new EditText(this.context);
                        ((TextView) view)
                                .setInputType(InputType.TYPE_CLASS_NUMBER);
                        linearLayout.addView(view);
                        break;
                    // set input type as date picker
                    case Constants.DATATYPE_DATE:
                        view = new DatePicker(this.context);
                        linearLayout.addView(view);
                        break;
                    // get the text area
                    case Constants.DATATYPE_TEXT:
                        view = new EditText(this.context);
                        ((TextView) view).setLines(5);
                        linearLayout.addView(view);
                        break;
                    // set input type as time picker
                    case Constants.DATATYPE_TIME:
                        view = new TimePicker(this.context);
                        linearLayout.addView(view);
                        break;
                    // default is edit text
                    default:
                        view = new EditText(this.context);
                        linearLayout.addView(view);
                        break;
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
                            //renderImageSliderForSingleSelection(layout, input);
                            // check if the type if image to create image slider
                        }
                        // Radio Button
                        else if ("full".equalsIgnoreCase(qd.getAppearanceAttr()) ) {
                            LinearLayout selectLayout = new LinearLayout(this.context);
                            selectLayout.setLayoutParams(new LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.MATCH_PARENT));
                            selectLayout.setOrientation(LinearLayout.VERTICAL);
                            RadioGroup radioGroupLayout = new RadioGroup(this.context);
                            radioGroupLayout.setOrientation(LinearLayout.HORIZONTAL);
                            int rbId = 0;
                            for (final SelectChoice selectChoice : input.getSelectChoices()) {
                            	CustomRadioButton radioButton = new CustomRadioButton(this.context);
                                radioButton.setId(rbId++);
                                radioButton.setText(selectChoice.getLabelInnerText());
                                radioButton.setValue(selectChoice.getValue());
                                radioGroupLayout.addView(radioButton);
                            }
                            selectLayout.addView(radioGroupLayout);
                            view = selectLayout;
                            linearLayout.addView(selectLayout);
                        // Default is single select dropdown
                        } else {
                        	Spinner spinner = new Spinner(this.context);
                            List<NameValuePair> choices = new ArrayList<NameValuePair>();
                            for (final SelectChoice selectChoice : input
                                    .getSelectChoices()) {
                            	NameValuePair pair = new NameValuePair(selectChoice.getLabelInnerText(), selectChoice.getValue());
                                choices.add(pair);
                            }
                            ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
                                    this.context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    choices);
                            spinner.setAdapter(arrayAdapter);
                            view = spinner;
                            linearLayout.addView(spinner);
                        }
                        break;
                }
                break;
            // create control for multi select, showing it as checkbox
            case Constants.CONTROL_SELECT_MULTI:
                switch (input.getDataType()) {
                    case Constants.DATATYPE_CHOICE_LIST:
                        LinearLayout selectLayout = new LinearLayout(
                                this.context);
                        selectLayout.setLayoutParams(new LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.MATCH_PARENT));
                        selectLayout.setOrientation(LinearLayout.VERTICAL);
                        for (final SelectChoice selectChoice : input
                                .getSelectChoices()) {
                        	CustomCheckBox checkBox = new CustomCheckBox(this.context);
                            checkBox.setText(selectChoice.getLabelInnerText());
                            checkBox.setValue(selectChoice.getValue());
                            selectLayout.addView(checkBox);
                        }
                        view = selectLayout;
                        linearLayout.addView(selectLayout);
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

	public TabSpec createTabSpec(TabHost tabHost) {
		TabSpec tabSpec = tabHost.newTabSpec(name);
		
		tabSpec.setContent(new TabContentFactory() {

            @Override
            public View createTabContent(String tag) {
            	return scrollView;
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

	public boolean hasView(String viewName){
		return this.viewReference.containsKey(viewName);
	}

	public String getPath(String viewName){
		return this.viewReference.get(viewName);
	}

	/**
     * Rendering image slide for select one
     * 
     * @param layout
     * @param questionPrompt
     */
	/*
    private void renderImageSliderForSingleSelection(LinearLayout layout,
            final FormEntryPrompt questionPrompt) {
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(
                this.context);
        RadioGroup radioGroupLayout = new RadioGroup(this.context);
        radioGroupLayout.setOrientation(LinearLayout.HORIZONTAL);
        final List<ImageView> images = new ArrayList<ImageView>();
        for (final SelectChoice selectChoice : questionPrompt
                .getSelectChoices()) {
            final ImageView gallery = new ImageView(this.context);
            String uri = selectChoice.getValue();
            gallery.setImageURI(Uri.parse(uri));
            gallery.setMinimumHeight(400);
            gallery.setMinimumWidth(400);
            gallery.setBackgroundColor(Color.GREEN);
            gallery.setPadding(10, 10, 10, 10);
            images.add(gallery);
            RadioButton button = new RadioButton(this.context);
            button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    for (ImageView view : images) {
                        if (view.equals(gallery)) {
                            view.setBackgroundColor(Color.RED);
                        } else {
                            view.setBackgroundColor(Color.GREEN);
                        }
                    }

                }
            });
            radioGroupLayout.addView(gallery);
            radioGroupLayout.addView(button);
        }
        horizontalScrollView.addView(radioGroupLayout);
        layout.addView(horizontalScrollView);

        HorizontalScrollView horizontalScrollView2 = new HorizontalScrollView(
                this.context);
        LinearLayout galleryLayout = new RadioGroup(this.context);
        galleryLayout.setOrientation(LinearLayout.HORIZONTAL);
        final List<ImageView> galleryImages = new ArrayList<ImageView>();
        for (final SelectChoice selectChoice : questionPrompt
                .getSelectChoices()) {
            final ImageView gallery = new ImageView(this.context);
            String uri = selectChoice.getValue();
            gallery.setImageURI(Uri.parse(uri));
            gallery.setBackgroundColor(Color.GREEN);
            gallery.setMinimumHeight(400);
            gallery.setMinimumWidth(400);
            gallery.setPadding(10, 10, 10, 10);
            gallery.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    ImageView selectedImageView = (ImageView) v;
                    for (ImageView view : galleryImages) {
                        if (view.equals(selectedImageView)) {
                            view.setBackgroundColor(Color.RED);
                        } else {
                            view.setBackgroundColor(Color.GREEN);
                        }
                    }

                }
            });
            galleryImages.add(gallery);
            galleryLayout.addView(gallery);
        }
        horizontalScrollView2.addView(galleryLayout);
        layout.addView(horizontalScrollView2);
    }

    public ImageView getCurrentImageView() {
        return this.imageView;
    }

    public void clearCurrentImageView() {
        this.imageView = null;
    }
    */

}
