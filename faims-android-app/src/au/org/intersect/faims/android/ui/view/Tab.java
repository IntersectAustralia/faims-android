package au.org.intersect.faims.android.ui.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.javarosa.core.model.Constants;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
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
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.data.VocabularyTerm;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.MapLayout;
import au.org.intersect.faims.android.util.Arch16n;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;

public class Tab implements Parcelable {

	@Inject
	DatabaseManager databaseManager;

	private static final String FREETEXT = "freetext";

	private Arch16n arch16n;
	private BeanShellLinker beanShellLinker;
	private ViewFactory viewFactory;
	
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
	private String moduleDir;
	private View view;
	private String reference;
	private List<String> onLoadCommands;
	private List<String> onShowCommands;
	private boolean tabShown;
	
	public Tab(Parcel source){
		FAIMSApplication.getInstance().injectMembers(this);
		
		hidden = source.readBundle().getBoolean("hidden");
		reference = source.readString();
	}
	
	public Tab(ShowModuleActivity activity, String name, String label, boolean hidden, boolean scrollable, String reference) {
		FAIMSApplication.getInstance().injectMembers(this);
		
		this.arch16n = activity.getArch16n();
		this.viewFactory = new ViewFactory(new WeakReference<Context>(activity), arch16n);
		this.beanShellLinker = activity.getBeanShellLinker();
		
		this.name = name;
		this.label = arch16n.substituteValue(label);
		this.hidden = hidden;
		this.moduleDir = activity.getModuleDir();
		this.reference = reference;	
		
		this.onLoadCommands = new ArrayList<String>();
		this.onShowCommands = new ArrayList<String>();
		
		this.viewReference = new HashMap<String, String>();
		this.dirtyButtonMap = new HashMap<String, Button>();
		this.viewMap = new HashMap<String, List<View>>();
		this.viewList = new ArrayList<View>();	
		this.mapViewList = new ArrayList<CustomMapView>();
		
		this.linearLayout = new LinearLayout(activity);
        linearLayout.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);    
        linearLayout.setBackgroundColor(Color.WHITE);
		
        if (scrollable) {
        	this.scrollView = new ScrollView(this.linearLayout.getContext());
        	scrollView.addView(linearLayout);
        	this.view = scrollView;
        } else {
        	this.view = linearLayout;
        }
	}

	public static final Parcelable.Creator<Tab> CREATOR = new Parcelable.Creator<Tab>() {
		public Tab createFromParcel(Parcel source) {
			return new Tab(source);
		}

		public Tab[] newArray(int size) {
			return new Tab[size];
		}
	};
	
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

	public LinearLayout addChildContainer(LinearLayout containerLayout,
			List<Map<String, String>> styleMappings) {
		CustomLinearLayout linearLayout = new CustomLinearLayout(this.linearLayout.getContext(), styleMappings);
		if (containerLayout == null) {
			this.linearLayout.addView(linearLayout);
		} else {
			containerLayout.addView(linearLayout);
		}

		return linearLayout;
	}

	public View addCustomView(LinearLayout linearLayout, FormAttribute attribute, String ref, String viewName, 
			boolean isArchEnt, boolean isRelationship, List<Map<String, String>> styleMappings) {
		// check if root container
		if (linearLayout == null) {
			linearLayout = this.linearLayout;
		}
		
		viewReference.put(viewName, ref);
		View view = null;
		
		// check the control type to know the type of the question
        switch (attribute.controlType) {
            case Constants.CONTROL_INPUT:
            	switch (attribute.dataType) {
	                case Constants.DATATYPE_INTEGER:
	                	view = viewFactory.createIntegerTextField(attribute, ref);
	                	setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
	                    break;
	                case Constants.DATATYPE_DECIMAL:
	                	view = viewFactory.createDecimalTextField(attribute, ref);
	                	setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
	                    break;
	                case Constants.DATATYPE_LONG:
	                	view = viewFactory.createLongTextField(attribute, ref);
	                	setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
	                    break;
	                case Constants.DATATYPE_DATE:
	                	view = viewFactory.createDatePicker(attribute, ref);
	                	setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
	                    break;
	                case Constants.DATATYPE_TIME:
	                	view = viewFactory.createTimePicker(attribute, ref);
	    				setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
	                    break;
	                case Constants.DATATYPE_TEXT:
	                	view = viewFactory.createTextArea(attribute, ref);
	                	setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
	                    break;
	                default:
	                	// check for additional types
	                	if (attribute.map) {            		
	                		MapLayout mapLayout = viewFactory.createMapView(linearLayout);
	                		linearLayout.addView(mapLayout);
	                		mapViewList.add(mapLayout.getMapView());
	                		view = mapLayout.getMapView();
	                	} else if (attribute.table) {
	                		if (attribute.questionText != null && !attribute.questionText.isEmpty()) {
	                			TextView tableLabel = viewFactory.createLabel(attribute);
	                			linearLayout.addView(tableLabel);
	                		}
	                		view = viewFactory.createTableView();
	                		linearLayout.addView(view);
	                	} else {
	                		view = viewFactory.createTextField(-1, attribute, ref);
	                		setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
	                	}
	                    break;
            	}
                break;
            // create control for select one showing it as drop down
            case Constants.CONTROL_SELECT_ONE:
                switch (attribute.dataType) {
                    case Constants.DATATYPE_CHOICE:
                    	// Picture Gallery
                        if ("image".equalsIgnoreCase(attribute.questionType)) {
                            view = viewFactory.createPictureGallery(attribute, ref, false);
                            setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
                        }
                        // Radio Button
                        else if ("full".equalsIgnoreCase(attribute.questionAppearance)) {
                        	view = viewFactory.createRadioGroup(attribute, ref);
                        	setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
                        // List
                        } else if ("compact".equalsIgnoreCase(attribute.questionAppearance) ) {
                        	view = viewFactory.createList(attribute);
                            linearLayout.addView(view);
                        // DropDown (default)
                        } else {
                        	view = viewFactory.createDropDown(attribute, ref);
                        	setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
                        }
                        break;
                }
                break;
            // create control for multi select, showing it as checkbox
            case Constants.CONTROL_SELECT_MULTI:
                switch (attribute.dataType) {
                    case Constants.DATATYPE_CHOICE_LIST:
                    	if ("image".equalsIgnoreCase(attribute.questionType)) {
                            view = viewFactory.createPictureGallery(attribute, ref, true);
                            setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
                    	} else if ("camera".equalsIgnoreCase(attribute.questionType)) {
                    		view = viewFactory.createCameraPictureGallery(attribute, ref);
                            setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
                    	} else if ("video".equalsIgnoreCase(attribute.questionType)) {
                    		view = viewFactory.createVideoGallery(attribute, ref);
                            setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
                    	} else if ("file".equalsIgnoreCase(attribute.questionType)) {
                    		view = viewFactory.createFileListGroup(attribute, ref);
                    		setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
                        } else {
	                    	view = viewFactory.createCheckListGroup(attribute, ref);
	                    	setupView(linearLayout, view, attribute, ref, isArchEnt, isRelationship);
                        }
                }
                break;
            // create control for trigger showing as a button
            case Constants.CONTROL_TRIGGER:
                view = viewFactory.createTrigger(attribute);
                linearLayout.addView(view);
                break;
        }
        
        if(attribute.name != null){
        	addViewMappings(attribute.name, view);
        }
        
        return view;
	}

	private void setupView(LinearLayout linearLayout, View view, FormAttribute attribute, String ref, boolean isArchEnt, boolean isRelationship) {
		Button certaintyButton = null;
    	Button annotationButton = null;
    	Button dirtyButton = null;
    	Button infoButton = null;
		
    	// setup view buttons
		if (attribute.controlType != Constants.CONTROL_TRIGGER &&
				!(attribute.controlType == Constants.CONTROL_SELECT_MULTI && 
				"image".equalsIgnoreCase(attribute.questionType))) {
			if(attribute.questionText != null && !attribute.questionText.isEmpty()){
				LinearLayout fieldLinearLayout = new LinearLayout(this.linearLayout.getContext());
		    	fieldLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
		    	
	            TextView textView = viewFactory.createLabel(attribute);
	            fieldLinearLayout.addView(textView);
	            linearLayout.addView(fieldLinearLayout);
	                
	    		if(attribute.certainty && (isArchEnt || isRelationship)){
	    			certaintyButton = viewFactory.createCertaintyButton();
	    			setupCertaintyButtonClick(certaintyButton, view);
	    			fieldLinearLayout.addView(certaintyButton);
	    		}
	    		
	    		if(attribute.annotation && (isArchEnt || isRelationship) && !FREETEXT.equals(attribute.type)){
	    			annotationButton = viewFactory.createAnnotationButton();
	    			setupAnnotationButtonClick(annotationButton, view);
	    			fieldLinearLayout.addView(annotationButton);
	    		}
	    		
	    		if(attribute.info && attribute.name != null && hasAttributeDescription(attribute.name)){
	    			infoButton = viewFactory.createInfoButton();
	    			setupInfoButtonClick(infoButton, attribute.name);
	    			fieldLinearLayout.addView(infoButton);
	    		}
	    		
	    		if (isArchEnt || isRelationship) {
		    		dirtyButton = viewFactory.createDirtyButton();
		    		dirtyButton.setVisibility(View.GONE);
		    		setupDirtyButtonClick(dirtyButton, view);
		    		fieldLinearLayout.addView(dirtyButton);
		    		dirtyButtonMap.put(ref, dirtyButton);
	    		}
			}
        }
		
        linearLayout.addView(view);
        
        if (view instanceof ICustomView) {
        	ICustomView customView = (ICustomView) view;
        	customView.setCertaintyEnabled(certaintyButton != null);
        	customView.setAnnotationEnabled(annotationButton != null);
        }
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
	
	private void setupDirtyButtonClick(Button dirtyButton, final View view) {
		dirtyButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ScrollView scrollView = new ScrollView(linearLayout.getContext());
				EditText textView = new EditText(linearLayout.getContext());
				scrollView.addView(textView);
				textView.setEnabled(false);
				
				if (view instanceof ICustomView) {
					ICustomView customView = (ICustomView) view;
					setDirtyTextArea(textView, customView.getDirtyReason());
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(linearLayout.getContext());
				
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

	private void setupAnnotationButtonClick(Button annotationButton, final View view) {
		annotationButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final EditText editText = new EditText(linearLayout.getContext());
				
				if (view instanceof ICustomView) {
					ICustomView customView = (ICustomView) view;
					editText.setText(customView.getAnnotation());
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(linearLayout.getContext());
				
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

	private void setupCertaintyButtonClick(Button certaintyButton,final View view) {
		certaintyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				LinearLayout layout = new LinearLayout(linearLayout.getContext());
				layout.setOrientation(LinearLayout.VERTICAL);
				final SeekBar seekBar = new SeekBar(linearLayout.getContext());
				float certainty = 0;
				seekBar.setMax(100);
				seekBar.setMinimumWidth((int) ScaleUtil.getDip(linearLayout.getContext(), 400));
				
				if (view instanceof ICustomView) {
					ICustomView customView = (ICustomView) view;
					certainty = customView.getCertainty();
	        		seekBar.setProgress((int) (certainty * 100));
				}
				
				final TextView text = new TextView(linearLayout.getContext());
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
				AlertDialog.Builder builder = new AlertDialog.Builder(linearLayout.getContext());
				
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

	private void setupInfoButtonClick(Button infoButton, final String attributeName) {
		infoButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showDescriptionDialog(getAttributeDescription(attributeName));
			}

		});
	}
	
	private boolean hasAttributeDescription(String attributeName) {
		try {		
			String attributeDescription = databaseManager.attributeRecord().getAttributeDescription(attributeName);
			List<VocabularyTerm> terms = databaseManager.attributeRecord().getVocabularyTerms(attributeName);
			
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
			
			String attributeDescription = databaseManager.attributeRecord().getAttributeDescription(attributeName);
			
			if(attributeDescription != null && !"".equals(attributeDescription)){
				description.append("<p><i>Description:</i>");
				description.append("<br/>");
				description.append(arch16n.substituteValue(attributeDescription));
				description.append("</p>");
			}
			
			List<VocabularyTerm> terms = databaseManager.attributeRecord().getVocabularyTerms(attributeName);
			
			if(terms != null && !terms.isEmpty()){
				description.append("<p><i>Glossary:</i></p>");
				VocabularyTerm.applyArch16n(terms, arch16n);
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
		AlertDialog.Builder dialog = new AlertDialog.Builder(linearLayout.getContext());
		dialog.setTitle("Info");
		ScrollView scrollView = new ScrollView(linearLayout.getContext());
		LinearLayout layout = new LinearLayout(linearLayout.getContext());
		WebView webView = new WebView(linearLayout.getContext());
		webView.loadDataWithBaseURL("file:///" + moduleDir + "/", description, "text/html", null, null);
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
	
	public String getPath(String viewName){
		return this.viewReference.get(viewName);
	}

	public boolean hasView(String name){
		return this.viewMap.containsKey(name);
	}
	
	public Button getDirtyButton(String ref) {
		return dirtyButtonMap.get(ref);
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
	
	public List<CustomMapView> getMapViewList(){
		return mapViewList;
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
	
	protected ScrollView getScrollViewForTab(){
		return this.view instanceof ScrollView ? (ScrollView) this.view : null;
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
	
	public void addOnLoadCommand(String command){
		this.onLoadCommands.add(command);
	}
	
	public void addOnShowCommand(String command){
		this.onShowCommands.add(command);
	}
	
	private void executeCommands(List<String> commands){
		for(String command : commands){
			beanShellLinker.execute(command);	
		}
	}
}
