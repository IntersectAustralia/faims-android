package au.org.intersect.faims.android.ui.form;

import java.util.ArrayList;
import java.util.List;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
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
	private String name;

	public Tab(Context context, String name) {
		this.context = context;
		this.name = name;
		
		this.linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
		
		this.scrollView = new ScrollView(this.context);
        scrollView.addView(linearLayout);
        
	}

	public View addInput(FormEntryPrompt input) {
		if (input.getControlType() != Constants.CONTROL_TRIGGER) {
            TextView textView = new TextView(this.context);
            textView.setText(input.getQuestionText());
            linearLayout.addView(textView);
        }
		
		View view = null;
		// check the control type to know the type of the question
        switch (input.getControlType()) {
            case Constants.CONTROL_INPUT:
                // check the data type of question of type input
                switch (input.getDataType()) {
                // set input type as number
                    case Constants.DATATYPE_INTEGER:
                    case Constants.DATATYPE_DECIMAL:
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
                        // check if the type if image to create image slider
                        if (input.getQuestion()
                                .getAdditionalAttributes().size() != 0
                                && input.getQuestion()
                                .getAdditionalAttribute(null, "type")
                                .equals("image")) {
                            //renderImageSliderForSingleSelection(layout, input);
                        } else {
                            Spinner spinner = new Spinner(this.context);
                            List<String> choices = new ArrayList<String>();
                            for (final SelectChoice selectChoice : input
                                    .getSelectChoices()) {
                                choices.add(selectChoice.getValue());
                            }
                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
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
                            CheckBox checkBox = new CheckBox(this.context);
                            checkBox.setText(selectChoice.getValue());
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
        
        tabSpec.setIndicator(name);
        
		return tabSpec;
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
