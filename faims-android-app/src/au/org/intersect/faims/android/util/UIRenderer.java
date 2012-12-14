package au.org.intersect.faims.android.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.TimePicker;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;

/**
 * Class that reads the ui defintion file and render the UI
 * 
 * @author danielt
 * 
 */
public class UIRenderer {

    private FormEntryController fem;

    private TabHost tabHost;

    private Context context;

    private ImageView imageView;

    public UIRenderer(FormEntryController fem, TabHost tabHost, Context context) {
        this.fem = fem;
        this.tabHost = tabHost;
        this.context = context;
    }

    /**
     * Render the tabs and questions inside the tabs
     * 
     */
    public void render() {
        Map<FormEntryCaption, FormEntryPrompt[]> questionPrompts = getQuestionPrompts();
        for (Entry<FormEntryCaption, FormEntryPrompt[]> entry : questionPrompts
                .entrySet()) {
            TabSpec tabSpec = this.tabHost.newTabSpec(entry.getKey()
                    .getQuestionText());

            final ScrollView scrollView = new ScrollView(this.context);
            LinearLayout linearLayout = new LinearLayout(this.context);
            linearLayout.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            for (FormEntryPrompt questionPrompt : entry.getValue()) {
                renderLayout(linearLayout, questionPrompt);
            }
            scrollView.addView(linearLayout);
            tabSpec.setContent(new TabContentFactory() {

                @Override
                public View createTabContent(String tag) {
                    return scrollView;
                }
            });

            tabSpec.setIndicator(entry.getKey().getQuestionText());
            this.tabHost.addTab(tabSpec);
        }
    }

    /**
     * Obtain a map of groups and questions inside the group
     * 
     * @return
     * @throws RuntimeException
     */
    private Map<FormEntryCaption, FormEntryPrompt[]> getQuestionPrompts()
            throws RuntimeException {

        FormIndex currentIndex = this.fem.getModel().getFormIndex();

        if (currentIndex.isBeginningOfFormIndex()) {
            currentIndex = this.fem.getModel().incrementIndex(currentIndex,
                    true);
        }

        Map<FormEntryCaption, FormEntryPrompt[]> map = new LinkedHashMap<FormEntryCaption, FormEntryPrompt[]>();

        IFormElement element = this.fem.getModel().getForm()
                .getChild(currentIndex);
        if (element instanceof GroupDef) {
            GroupDef gd = (GroupDef) element;
            // descend into group
            FormIndex idxChild = this.fem.getModel().incrementIndex(
                    currentIndex, true);

            for (int i = 0; i < gd.getChildren().size(); i++) {
                FormEntryCaption group = this.fem.getModel().getCaptionPrompt(
                        idxChild);
                IFormElement groupElement = this.fem.getModel().getForm()
                        .getChild(idxChild);
                if (groupElement instanceof GroupDef) {
                    GroupDef childGroup = (GroupDef) groupElement;
                    idxChild = this.fem.getModel().incrementIndex(idxChild,
                            true);
                    List<FormEntryPrompt> questionPrompts = new ArrayList<FormEntryPrompt>();
                    for (int j = 0; j < childGroup.getChildren().size(); j++) {
                        FormEntryPrompt questionPrompt = this.fem.getModel()
                                .getQuestionPrompt(idxChild);
                        questionPrompts.add(questionPrompt);
                        idxChild = this.fem.getModel().incrementIndex(idxChild,
                                false);
                    }
                    map.put(group,
                            questionPrompts
                                    .toArray(new FormEntryPrompt[questionPrompts
                                            .size()]));
                }
            }

        }

        return map;
    }

    /**
     * Rendering layout for each question inside the tab
     * 
     * @param layout
     * @param questionPrompt
     */
    private void renderLayout(LinearLayout layout,
            final FormEntryPrompt questionPrompt) {
        if (questionPrompt.getControlType() != Constants.CONTROL_TRIGGER) {
            TextView textView = new TextView(this.context);
            textView.setText(questionPrompt.getQuestionText());
            layout.addView(textView);
        }

        // check the control type to know the type of the question
        switch (questionPrompt.getControlType()) {
            case Constants.CONTROL_INPUT:
                View view;
                // check the data type of question of type input
                switch (questionPrompt.getDataType()) {
                // set input type as number
                    case Constants.DATATYPE_INTEGER:
                    case Constants.DATATYPE_DECIMAL:
                    case Constants.DATATYPE_LONG:
                        view = new EditText(this.context);
                        ((TextView) view)
                                .setInputType(InputType.TYPE_CLASS_NUMBER);
                        layout.addView(view);
                        break;
                    // set input type as date picker
                    case Constants.DATATYPE_DATE:
                        view = new DatePicker(this.context);
                        layout.addView(view);
                        break;
                    // get the text area
                    case Constants.DATATYPE_TEXT:
                        view = new EditText(this.context);
                        ((TextView) view).setLines(5);
                        layout.addView(view);
                        break;
                    // set input type as time picker
                    case Constants.DATATYPE_TIME:
                        view = new TimePicker(this.context);
                        layout.addView(view);
                        break;
                    // default is edit text
                    default:
                        view = new EditText(this.context);
                        layout.addView(view);
                        break;
                }
                break;
            // uploading image by using camera
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
            // create control for select one showing it as drop down
            case Constants.CONTROL_SELECT_ONE:
                switch (questionPrompt.getDataType()) {
                    case Constants.DATATYPE_CHOICE:
                        // check if the type if image to create image slider
                        if (questionPrompt.getQuestion()
                                .getAdditionalAttributes().size() != 0
                                && questionPrompt.getQuestion()
                                .getAdditionalAttribute(null, "type")
                                .equals("image")) {
                            renderImageSliderForSingleSelection(layout,
                                    questionPrompt);
                        } else {
                            Spinner spinner = new Spinner(this.context);
                            List<String> choices = new ArrayList<String>();
                            for (final SelectChoice selectChoice : questionPrompt
                                    .getSelectChoices()) {
                                choices.add(selectChoice.getValue());
                            }
                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                                    this.context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    choices);
                            spinner.setAdapter(arrayAdapter);
                            layout.addView(spinner);
                        }
                        break;
                }
                break;
            // create control for multi select, showing it as checkbox
            case Constants.CONTROL_SELECT_MULTI:
                switch (questionPrompt.getDataType()) {
                    case Constants.DATATYPE_CHOICE_LIST:
                        LinearLayout selectLayout = new LinearLayout(
                                this.context);
                        selectLayout.setLayoutParams(new LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.MATCH_PARENT));
                        selectLayout.setOrientation(LinearLayout.VERTICAL);
                        for (final SelectChoice selectChoice : questionPrompt
                                .getSelectChoices()) {
                            CheckBox checkBox = new CheckBox(this.context);
                            checkBox.setText(selectChoice.getValue());
                            selectLayout.addView(checkBox);
                        }
                        layout.addView(selectLayout);
                }
                break;
            // create control for trigger showing as a button
            case Constants.CONTROL_TRIGGER:
                Button button = new Button(this.context);
                button.setText(questionPrompt.getQuestionText());
                layout.addView(button);
                break;
        }
    }

    /**
     * Rendering image slide for select one
     * 
     * @param layout
     * @param questionPrompt
     */
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
}