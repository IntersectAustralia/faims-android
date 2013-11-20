package au.org.intersect.faims.android.ui.form;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.SelectChoice;

import roboguice.RoboGuice;
import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.InputType;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.data.VocabularyTerm;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.MapLayout;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;

public class Tab implements Parcelable{

	private WeakReference<ShowModuleActivity> activityRef;
	private ScrollView scrollView;
	private LinearLayout linearLayout;
	private Map<String, String> viewReference;
	private Map<String, List<View>> viewMap;
	private Map<String, Button> dirtyButtonMap;
	private List<View> viewList;
	private List<CustomMapView> mapViewList;
	private String name;
	private String label;
	private boolean hidden;
	private View view;
	private Arch16n arch16n;
	private String reference;
	private static final String FREETEXT = "freetext";
	private List<String> onLoadCommands;
	private List<String> onShowCommands;
	private boolean tabShown;

	@Inject
	DatabaseManager databaseManager;
	
	public Tab(Parcel source){
		hidden = source.readBundle().getBoolean("hidden");
		reference = source.readString();
	}
	
	public Tab(ShowModuleActivity activity, String name, String label, boolean hidden, boolean scrollable, Arch16n arch16n, String reference) {
		this.activityRef = new WeakReference<ShowModuleActivity>(activity);
		this.name = name;
		this.arch16n = arch16n;
		label = this.arch16n.substituteValue(label);
		this.label = label;
		this.hidden = hidden;
		this.reference = reference;
		
		onLoadCommands = new ArrayList<String>();
		onShowCommands = new ArrayList<String>();
		
		this.linearLayout = new LinearLayout(activity);
		this.viewReference = new HashMap<String, String>();
		this.dirtyButtonMap = new HashMap<String, Button>();
		this.viewMap = new HashMap<String, List<View>>();
		this.viewList = new ArrayList<View>();
		this.mapViewList = new ArrayList<CustomMapView>();
        linearLayout.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        
        linearLayout.setBackgroundColor(Color.WHITE);
		
        if (scrollable) {
        	this.scrollView = new ScrollView(this.activityRef.get());
        	scrollView.addView(linearLayout);
        	this.view = scrollView;
        } else {
        	this.view = linearLayout;
        }
        // inject faimsClient and serverDiscovery
        RoboGuice.getBaseApplicationInjector(this.activityRef.get().getApplication()).injectMembers(this);
	}

	public static final Parcelable.Creator<Tab> CREATOR = new Parcelable.Creator<Tab>() {
		public Tab createFromParcel(Parcel source) {
			return new Tab(source);
		}

		public Tab[] newArray(int size) {
			return new Tab[size];
		}
	};

	public LinearLayout addChildContainer(LinearLayout containerLayout,
			List<Map<String, String>> styleMappings) {
		CustomLinearLayout linearLayout = new CustomLinearLayout(this.activityRef.get(), styleMappings);
		if (containerLayout == null) {
			this.linearLayout.addView(linearLayout);
		} else {
			containerLayout.addView(linearLayout);
		}

		return linearLayout;
	}
	
	public Button getDirtyButton(String ref) {
		return dirtyButtonMap.get(ref);
	}

	public View addCustomView(LinearLayout linearLayout, FormAttribute attribute, String ref, String viewName, 
			boolean isArchEnt, boolean isRelationship, List<Map<String, String>> styleMappings) {
    	Button certaintyButton = null;
    	Button annotationButton = null;
    	Button dirtyButton = null;
    	Button infoButton = null;
		if (linearLayout == null) {
			linearLayout = this.linearLayout;
		}
    	
		if (attribute.controlType != Constants.CONTROL_TRIGGER &&
				!(attribute.controlType == Constants.CONTROL_SELECT_MULTI && "image".equalsIgnoreCase(attribute.questionType))) {
			if(attribute.questionText != null && !attribute.questionText.isEmpty()){
				LinearLayout fieldLinearLayout = new LinearLayout(this.activityRef.get());
		    	fieldLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
		    	
	            TextView textView = createLabel(attribute);
	            fieldLinearLayout.addView(textView);
	            linearLayout.addView(fieldLinearLayout);
	                
	    		if(attribute.certainty && (isArchEnt || isRelationship)){
	    			certaintyButton = createCertaintyButton();
	    			fieldLinearLayout.addView(certaintyButton);
	    		}
	    		
	    		if(attribute.annotation && (isArchEnt || isRelationship) && !FREETEXT.equals(attribute.type)){
	    			annotationButton = createAnnotationButton();
	    			fieldLinearLayout.addView(annotationButton);
	    		}
	    		
	    		if(attribute.info && attribute.name != null && hasAttributeDescription(attribute.name)){
	    			infoButton = createInfoButton();
	    			fieldLinearLayout.addView(infoButton);
	    		}
	    		
	    		if (isArchEnt || isRelationship) {
		    		dirtyButton = createDirtyButton();
		    		dirtyButton.setVisibility(View.GONE);
		    		fieldLinearLayout.addView(dirtyButton);
		    		dirtyButtonMap.put(ref, dirtyButton);
	    		}
			}
        }
		
		viewReference.put(viewName, ref);
		View view = null;
		
		// check the control type to know the type of the question
        switch (attribute.controlType) {
            case Constants.CONTROL_INPUT:
            	switch (attribute.dataType) {
	                case Constants.DATATYPE_INTEGER:
	                	view = createIntegerTextField(attribute, ref);
	                	setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref);
	                    break;
	                case Constants.DATATYPE_DECIMAL:
	                	view = createDecimalTextField(attribute, ref);
	                	setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref);
	                    break;
	                case Constants.DATATYPE_LONG:
	                	view = createLongTextField(attribute, ref);
	                	setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref);
	                    break;
	                // set input type as date picker
	                case Constants.DATATYPE_DATE:
	                	view = createDatePicker(attribute, ref);
	                	setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref, DateUtil.getDate((CustomDatePicker) view));
	                    break;
	                // get the text area
	                case Constants.DATATYPE_TEXT:
	                	view = createTextArea(attribute, ref);
	                	setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref);
	                    break;
	                // set input type as time picker
	                case Constants.DATATYPE_TIME:
	                	view = createTimePicker(attribute, ref);
	    				setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref, DateUtil.getTime((CustomTimePicker) view));
	                    break;
	                // default is edit text
	                default:
	                	// check if map type
	                	if (attribute.map) {
	                		MapLayout mapLayout = new MapLayout(this.activityRef.get());
	                		
	                		linearLayout.addView(mapLayout);
	                		
	                		mapViewList.add(mapLayout.getMapView());
	                		view = mapLayout.getMapView();
	                	} else {
	                		view = createTextField(-1, attribute, ref);
	                		setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref);
	                	}
	                    break;
            	}
                break;
            // create control for select one showing it as drop down
            case Constants.CONTROL_SELECT_ONE:
                switch (attribute.dataType) {
                    case Constants.DATATYPE_CHOICE:
                    	// check if the type if image to create image slider
                        if ("image".equalsIgnoreCase(attribute.questionType)) {
                            view = createPictureGallery(attribute, ref, false);
                            setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref);
                        }
                        // Radio Button
                        else if ("full".equalsIgnoreCase(attribute.questionAppearance)) {
                        	view = createRadioGroup(attribute, ref);
                        	setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref);
                        // List
                        } else if ("compact".equalsIgnoreCase(attribute.questionAppearance) ) {
                        	view = createList(attribute);
                            linearLayout.addView(view); // TODO does this need certainty and annotation buttons
                        // Default is single select dropdown
                        } else {
                        	view = createDropDown(attribute, ref);
                        	NameValuePair pair = (NameValuePair) ((CustomSpinner) view).getSelectedItem();
                        	setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref, pair.getValue());
                        }
                        break;
                }
                break;
            // create control for multi select, showing it as checkbox
            case Constants.CONTROL_SELECT_MULTI:
                switch (attribute.dataType) {
                    case Constants.DATATYPE_CHOICE_LIST:
                    	if ("image".equalsIgnoreCase(attribute.questionType)) {
                            view = createPictureGallery(attribute, ref, true);
                            setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref);
                    	} else if ("camera".equalsIgnoreCase(attribute.questionType)) {
                    		view = createCameraPictureGallery(attribute, ref);
                            setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref);
                    	} else if ("video".equalsIgnoreCase(attribute.questionType)) {
                    		view = createVideoGallery(attribute, ref);
                            setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref);
                    	} else if ("file".equalsIgnoreCase(attribute.questionType)) {
                    		view = createFileListGroup(attribute, ref);
                    		setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref, new ArrayList<NameValuePair>());
                        } else {
	                    	view = createCheckListGroup(attribute, ref);
	                    	setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attribute.name, ref, new ArrayList<NameValuePair>());
                        }
                }
                break;
            // create control for trigger showing as a button
            case Constants.CONTROL_TRIGGER:
                view = createTrigger(attribute);
                linearLayout.addView(view);
                break;
        }
        
        if(attribute.name != null){
        	addViewMappings(attribute.name, view);
        }
        
        return view;
	}
	
	private TextView createLabel(FormAttribute attribute) {
		TextView textView = new TextView(this.activityRef.get());
        String inputText = attribute.questionText;
        inputText = arch16n.substituteValue(inputText);
        textView.setText(inputText);
        return textView;
	}
	
	private Button createCertaintyButton() {
		Button button = new Button(this.activityRef.get());
		button.setBackgroundResource(R.drawable.square_button);
		int size = (int) ScaleUtil.getDip(this.activityRef.get(), 34);
		LayoutParams layoutParams = new LayoutParams(size, size);
		layoutParams.topMargin = 10;
		button.setLayoutParams(layoutParams);
		button.setText("C");
		button.setTextSize(10);
		return button;
	}
	
	private Button createAnnotationButton() {
		Button button = new Button(this.activityRef.get());
		button.setBackgroundResource(R.drawable.square_button);
		int size = (int) ScaleUtil.getDip(this.activityRef.get(), 34);
		LayoutParams layoutParams = new LayoutParams(size, size);
		layoutParams.topMargin = 10;
		button.setLayoutParams(layoutParams);
		button.setText("A");
		button.setTextSize(10);
		return button;
	}
	
	private Button createDirtyButton() {
		Button button = new Button(this.activityRef.get());
		button.setBackgroundResource(R.drawable.square_button);
		int size = (int) ScaleUtil.getDip(this.activityRef.get(), 34);
		LayoutParams layoutParams = new LayoutParams(size, size);
		layoutParams.topMargin = 10;
		button.setLayoutParams(layoutParams);
		button.setText("\u26A0");
		button.setTextSize(10);
		return button;
	}

	private Button createInfoButton() {
		Button button = new Button(this.activityRef.get());
		button.setBackgroundResource(R.drawable.square_button);
		int size = (int) ScaleUtil.getDip(this.activityRef.get(), 34);
		LayoutParams layoutParams = new LayoutParams(size, size);
		layoutParams.topMargin = 10;
		button.setLayoutParams(layoutParams);
		button.setText("?");
		button.setTextSize(10);
		return button;
	}

	private void setupView(LinearLayout linearLayout, View view, Button certaintyButton, Button annotationButton, Button dirtyButton, Button infoButton, String attributeName, String ref) {
		setupView(linearLayout, view, certaintyButton, annotationButton, dirtyButton, infoButton, attributeName, ref, "");
	}
	
	private void setupView(LinearLayout linearLayout,View view, Button certaintyButton, Button annotationButton, Button dirtyButton, Button infoButton, String attributeName, String ref, Object value) {
		if (certaintyButton != null) onCertaintyButtonClicked(certaintyButton, view);
        if (annotationButton != null) onAnnotationButtonClicked(annotationButton, view);
        if (dirtyButton != null) onDirtyButtonClicked(dirtyButton, view);
        if (infoButton != null) onInfoButtonClicked(infoButton, attributeName);
        linearLayout.addView(view);
        
        if (view instanceof ICustomView) {
        	ICustomView customView = (ICustomView) view;
        	customView.setCertaintyEnabled(certaintyButton != null);
        	customView.setAnnotationEnabled(annotationButton != null);
        }
	}
	

	private CustomEditText createTextField(int type, FormAttribute attribute, String ref) {
		CustomEditText text = new CustomEditText(this.activityRef.get(), attribute, ref);
    	if (attribute.readOnly) {
    		text.setEnabled(false);
    	}
    	if (type >= 0) text.setInputType(type);
    	return text;
	}
	
	private CustomEditText createIntegerTextField(FormAttribute attribute, String ref) {
    	return createTextField(InputType.TYPE_CLASS_NUMBER, attribute, ref);
	}
	
	private CustomEditText createDecimalTextField(FormAttribute attribute, String ref) {
        return createTextField(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, attribute, ref);
	}
	
	private CustomEditText createLongTextField(FormAttribute attribute, String ref) {
        return createTextField(InputType.TYPE_CLASS_NUMBER, attribute, ref);
	}
	
	private CustomDatePicker createDatePicker(FormAttribute attribute, String ref) {
		CustomDatePicker date = new CustomDatePicker(this.activityRef.get(), attribute, ref);
    	Time now = new Time();
		now.setToNow();
		date.updateDate(now.year, now.month, now.monthDay);
		if (attribute.readOnly) {
    		date.setEnabled(false);
    	}
    	return date;
	}
	
	private CustomEditText createTextArea(FormAttribute attribute, String ref) {
		CustomEditText text = new CustomEditText(this.activityRef.get(), attribute, ref);
    	if (attribute.readOnly) {
    		text.setEnabled(false);
    	}
    	text.setLines(5);
    	return text;
	}
	
	private CustomTimePicker createTimePicker(FormAttribute attribute, String ref) {
		CustomTimePicker time = new CustomTimePicker(this.activityRef.get(), attribute, ref);
    	Time timeNow = new Time();
        timeNow.setToNow();
		time.setCurrentHour(timeNow.hour);
		time.setCurrentMinute(timeNow.minute);
		if (attribute.readOnly) {
    		time.setEnabled(false);
    	}
		return time;
	}
	
	private CustomRadioGroup createRadioGroup(FormAttribute attribute, String ref) {
		CustomRadioGroup radioGroup = new CustomRadioGroup(this.activityRef.get(), attribute, ref);
		
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (final SelectChoice selectChoice : attribute.selectChoices) {
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
        	pairs.add(new NameValuePair(innerText, selectChoice.getValue()));
        }
		radioGroup.populate(pairs);
		
		return radioGroup;  
	}
	
	private CustomListView createList(FormAttribute attribute) {
		CustomListView list = new CustomListView(this.activityRef.get());
		
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
            choices.add(pair);
        }
        
        ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
                this.activityRef.get(),
                android.R.layout.simple_list_item_1,
                choices);
        list.setAdapter(arrayAdapter);
        return list;
	}
	
	private HierarchicalSpinner createDropDown(FormAttribute attribute, String ref) {
		HierarchicalSpinner spinner = new HierarchicalSpinner(this.activityRef.get(), attribute, ref);
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
            choices.add(pair);
        }
        ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
                this.activityRef.get(),
                R.layout.multiline_spinner_dropdown_item,
                choices);
        spinner.setAdapter(arrayAdapter);
        spinner.reset();
        return spinner;
	}
	
	private CustomCheckBoxGroup createCheckListGroup(FormAttribute attribute, String ref) {
		CustomCheckBoxGroup checkboxGroup = new CustomCheckBoxGroup(
                this.activityRef.get(), attribute, ref);
        
        List<NameValuePair> choices = new ArrayList<NameValuePair>();
        for (final SelectChoice selectChoice : attribute.selectChoices) {
        	String innerText = selectChoice.getLabelInnerText();
        	innerText = arch16n.substituteValue(innerText);
        	NameValuePair pair = new NameValuePair(innerText, selectChoice.getValue());
            choices.add(pair);
        }
        
        return checkboxGroup;
	}
	
	private FileListGroup createFileListGroup(FormAttribute attribute, String ref) {
		FileListGroup audioListGroup = new FileListGroup(
                this.activityRef.get(), attribute, attribute.sync, ref);
        return audioListGroup;
	}
	
	private Button createTrigger(FormAttribute attribute) {
		 CustomButton button = new CustomButton(this.activityRef.get());
         String questionText = arch16n.substituteValue(attribute.questionText);
         button.setText(questionText);
         return button;
	}
	
	private void setDirtyTextArea(EditText text, String value) {
		if (value == null || "".equals(value)) return;
		
		String[] lines = value.split(";");
		StringBuffer sb = new StringBuffer();
		int count = 0;
		for (String l : lines) {
			if (l.trim().equals("")) continue;
			sb.append(l);
			sb.append("\n");
			count++;
		}
		text.setLines(count);
		text.setText(sb.toString());
	}
	
	private void onDirtyButtonClicked(Button dirtyButton, final View view) {
		dirtyButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ScrollView scrollView = new ScrollView(activityRef.get());
				EditText textView = new EditText(activityRef.get());
				scrollView.addView(textView);
				textView.setEnabled(false);
				
				if (view instanceof ICustomView) {
					ICustomView customView = (ICustomView) view;
					setDirtyTextArea(textView, customView.getDirtyReason());
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				
				builder.setTitle("Annotation");
				builder.setMessage("Dirty Reason:");
				builder.setView(scrollView);
				builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				            // User cancelled the dialog
				        }
				    });
				
				builder.create().show();
			}
		});
	}

	private void onAnnotationButtonClicked(Button annotationButton, final View view) {
		annotationButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final EditText editText = new EditText(v.getContext());
				
				if (view instanceof ICustomView) {
					ICustomView customView = (ICustomView) view;
					editText.setText(customView.getAnnotation());
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				
				builder.setTitle("Annotation");
				builder.setMessage("Set the annotation text for the field");
				builder.setView(editText);
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int id) {
				        	
				        	if (view instanceof ICustomView) {
								ICustomView customView = (ICustomView) view;
								customView.setAnnotation(editText.getText().toString());
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
				seekBar.setMinimumWidth((int) ScaleUtil.getDip(Tab.this.activityRef.get(), 400));
				
				if (view instanceof ICustomView) {
					ICustomView customView = (ICustomView) view;
					certainty = customView.getCertainty();
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
				        	
				        	if (view instanceof ICustomView) {
								ICustomView customView = (ICustomView) view;
								customView.setCertainty(((float)seekBar.getProgress())/100);
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

	private void onInfoButtonClicked(Button infoButton, final String attributeName) {
		infoButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showDescriptionDialog(getAttributeDescription(attributeName));
			}

		});
	}
	
	private boolean hasAttributeDescription(String attributeName) {
		try {
			
			String attributeDescription = databaseManager.getAttributeDescription(attributeName);
			List<VocabularyTerm> terms = databaseManager.getVocabularyTerms(attributeName);
			
			boolean termsEmpty = terms == null || terms.isEmpty();
			boolean attributeDescriptionEmpty = attributeDescription == null || "".equals(attributeDescription);
			
			if(termsEmpty && attributeDescriptionEmpty) return false;
			
			return true;
		} catch (Exception e) {
			FLog.e("Cannot retrieve the description for attribute " + attributeName, e);
			return false;
		}
	}
	
	private String getAttributeDescription(String attributeName) {
		StringBuilder description = new StringBuilder();
		try {
			
			String attributeDescription = databaseManager.getAttributeDescription(attributeName);
			
			if(attributeDescription != null && !"".equals(attributeDescription)){
				description.append("<p><i>Description:</i>");
				description.append("<br/>");
				description.append(activityRef.get().getArch16n().substituteValue(attributeDescription));
				description.append("</p>");
			}
			
			List<VocabularyTerm> terms = databaseManager.getVocabularyTerms(attributeName);
			
			if(terms != null && !terms.isEmpty()){
				description.append("<p><i>Glossary:</i></p>");
				VocabularyTerm.applyArch16n(terms, activityRef.get().getArch16n());
				createVocabularyTermXML(description, terms);
			}
			
		} catch (Exception e) {
			FLog.e("Cannot retrieve the description for attribute " + attributeName, e);
		}
		return description.toString();
	}
	
	private void createVocabularyTermXML(StringBuilder sb, List<VocabularyTerm> terms) {
		sb.append("<ul>");
		
		for (VocabularyTerm term : terms) {
			sb.append("<li>");
			
			if(term.description != null && !"".equals(term.description)){
				sb.append("<p><b>");
				sb.append(term.name);
				sb.append("</b><br/>");
				sb.append(term.description);
				sb.append("</p>");
			} else {
				sb.append("<p><b>");
				sb.append(term.name);
				sb.append("</b></p>");
			}
			
			if(term.pictureURL != null && !"".equals(term.pictureURL)){
				sb.append("<img width=\"100\" height=\"100\" src=\"");
				sb.append(term.pictureURL);
				sb.append("\"/>");
			}
			
			if (term.terms != null){
				createVocabularyTermXML(sb, term.terms);
			}
			
			sb.append("</li>");
		}
		
		sb.append("</ul>");
	}
	
	private void showDescriptionDialog(String description) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this.activityRef.get());
		dialog.setTitle("Info");
		ScrollView scrollView = new ScrollView(this.activityRef.get());
		LinearLayout layout = new LinearLayout(this.activityRef.get());
		WebView webView = new WebView(this.activityRef.get());
		webView.loadDataWithBaseURL("file:///" + this.activityRef.get().getModuleDir() + "/", description, "text/html", null, null);
		layout.addView(webView);
		scrollView.addView(layout);
		dialog.setView(scrollView);
		dialog.setPositiveButton("Done", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// dismiss the dialog
			}
		});
		AlertDialog d = dialog.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
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
	
	public void setHidden(boolean hidden){
		this.hidden = hidden;
	}

	public String getReference() {
		return reference;
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
	
	public List<View> getAllChildrenViews(){
		Stack<LinearLayout> containers = new Stack<LinearLayout>(); 
		List<View> views = new ArrayList<View>();
		if (linearLayout == null) return views;
		containers.add(linearLayout);
		while (!containers.isEmpty()){
			LinearLayout layout = containers.pop();
			for(int i = 0; i < layout.getChildCount(); i++){
				View view = layout.getChildAt(i);
				if (view instanceof CustomLinearLayout){
					containers.add((LinearLayout) view);
				} else {
					views.add(view);
				}
			}
		}
		
		return views;
	}
	public List<View> getAllViews(){
		return this.viewList;
	}

	public List<View> getViews(String name) {
		return this.viewMap.get(name);
	}
	
	public Map<String, String> getViewReference() {
		return viewReference;
	}
	
    private PictureGallery createPictureGallery(FormAttribute attribute, String ref, boolean isMulti) {
    	if (isMulti) {
    		return new PictureGallery(this.activityRef.get(), attribute, ref, isMulti);
    	}
    	return new HierarchicalPictureGallery(this.activityRef.get(), attribute, ref);
	}
    
    private CameraPictureGallery createCameraPictureGallery(FormAttribute attribute, String ref) {
		return new CameraPictureGallery(this.activityRef.get(), attribute, ref);
	}
    
    private VideoGallery createVideoGallery(FormAttribute attribute, String ref) {
		return new VideoGallery(this.activityRef.get(), attribute, ref);
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
		List<View> views = getAllChildrenViews();
		for (View v : views) {
			if (v instanceof ICustomView) {
				ICustomView customView = (ICustomView) v;
				customView.reset();
				Button dirtyButton = dirtyButtonMap.get(customView.getRef());
				if (dirtyButton != null) dirtyButton.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Bundle tabBundle = new Bundle();
		tabBundle.putBoolean("hidden", hidden);
		dest.writeBundle(tabBundle);
		dest.writeString(reference);
	}

	public void onShowTab() {
		for (CustomMapView mapView : mapViewList) {
			mapView.restartThreads();
		}
		
		if (!tabShown) {
			tabShown = true;
			executeCommands(onLoadCommands);
		}
		
		executeCommands(onShowCommands);
	}

	public void onHideTab() {
		for (CustomMapView mapView : mapViewList) {
			mapView.killThreads();
		}
	}
	
	public List<CustomMapView> getMapViewList(){
		return mapViewList;
	}
	
	public void addOnLoadCommand(String command){
		this.onLoadCommands.add(command);
	}
	
	public void addOnShowCommand(String command){
		this.onShowCommands.add(command);
	}
	
	private void executeCommands(List<String> commands){

		BeanShellLinker linker = activityRef.get().getBeanShellLinker();
		
		for(String command : commands){
			linker.execute(command);	
		}
	}

	protected ScrollView getScrollViewForTab(){
		return this.view instanceof ScrollView ? (ScrollView) this.view : null;
	}
}
