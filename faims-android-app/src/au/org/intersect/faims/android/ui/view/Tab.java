package au.org.intersect.faims.android.ui.view;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javarosa.core.model.Constants;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
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
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.data.VocabularyTerm;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.MapLayout;
import au.org.intersect.faims.android.util.Arch16n;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;
import com.nativecss.NativeCSS;

public class Tab {

	@Inject
	DatabaseManager databaseManager;
	
	@Inject
	BeanShellLinker beanShellLinker;
	
	@Inject
	Arch16n arch16n;

	public static final String FREETEXT = "freetext";

	private ViewFactory viewFactory;
	private ScrollView scrollView;
	private LinearLayout linearLayout;
	
	private List<View> viewList;
	private Map<String, List<View>> attributeViewMap;
	private List<CustomMapView> mapViewList;
	private Map<String, Button> dirtyButtonMap;
	private Map<String, LinearLayout> viewLayoutMap;
	
	private Map<String, LinearLayout> containerMap;

	private String ref;
	private String name;
	private String label;
	private String moduleDir;
	
	private View view;
	private List<String> onLoadCommands;
	private List<String> onShowCommands;
	
	// restorable
	private boolean tabShown;
	private boolean hidden;
	
	public Tab(String ref, String name, String label, boolean hidden, boolean scrollable, WeakReference<ShowModuleActivity> activityRef) {
		FAIMSApplication.getInstance().injectMembers(this);
		
		this.ref = ref;
		this.name = name;
		this.label = arch16n.substituteValue(label);
		this.moduleDir = activityRef.get().getModule().getDirectoryPath().getPath();
		
		this.hidden = hidden;
		this.tabShown = false;
		
		this.onLoadCommands = new ArrayList<String>();
		this.onShowCommands = new ArrayList<String>();
		
		this.viewList = new ArrayList<View>();	
		this.mapViewList = new ArrayList<CustomMapView>();
		this.attributeViewMap = new HashMap<String, List<View>>();
		this.dirtyButtonMap = new HashMap<String, Button>();
		this.viewLayoutMap = new HashMap<String, LinearLayout>();
		this.containerMap = new HashMap<String, LinearLayout>();
		
		linearLayout = new LinearLayout(activityRef.get());
        linearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);    
        linearLayout.setBackgroundColor(Color.WHITE);
		
        if (scrollable) {
        	scrollView = new ScrollView(this.linearLayout.getContext());
        	scrollView.addView(linearLayout);
        	view = scrollView;
        } else {
        	view = linearLayout;
        }
        
        viewFactory = new ViewFactory(activityRef, arch16n);
	}
	
	public void addCustomContainer(String ref, String style, String parentRef) throws Exception {
		LinearLayout parentContainer = containerMap.get(parentRef);
		CustomLinearLayout containerLayout = new CustomLinearLayout(linearLayout.getContext(), beanShellLinker.getUIRenderer().getStyleMappings(style), true);
		addCustomContainer(ref, containerLayout, parentContainer);
	}

	public void addCustomContainer(String ref, LinearLayout containerLayout, LinearLayout parentLayout) throws Exception {
		// check if container already exists
		if (containerMap.get(ref) != null) {
			throw new Exception("container already exists " + ref);
		}
		
		if (parentLayout == null) {
			linearLayout.addView(containerLayout);
		} else {
			parentLayout.addView(containerLayout);
		}
		containerMap.put(ref, containerLayout);
		refreshTab();
	}
	
	public void removeCustomContainer(String ref) {
		LinearLayout layout = containerMap.get(ref);
		if (layout != null) {
			ViewParent parent = layout.getParent();
			if (parent instanceof ViewGroup) {
				((ViewGroup) parent).removeView(layout);
				containerMap.remove(ref);
			} else {
				FLog.w("cannot remove container " + ref);
			}
		} else {
			FLog.w("cannot find container " + ref);
		}
		
		refreshTab();
	}
	
	public View addCustomView(String ref, FormInputDef inputDef, boolean isArchEnt, boolean isRelationship, String containerRef) throws Exception {
		return addCustomView(ref, inputDef, isArchEnt, isRelationship, containerMap.get(containerRef), true);
	}
	
	public View addCustomView(String ref, FormInputDef inputDef, boolean isArchEnt, boolean isRelationship, LinearLayout container) throws Exception {
		return addCustomView(ref, inputDef, isArchEnt, isRelationship, container, false);
	}

	public View addCustomView(String ref, FormInputDef inputDef, boolean isArchEnt, boolean isRelationship, LinearLayout container, boolean dynamic) throws Exception {
		// check if ref already exists
		if (beanShellLinker.getUIRenderer().getViewByRef(ref) != null) {
			throw new Exception("view already exists " + ref);
		}
		
		// check if root container
		if (container == null) {
			container = this.linearLayout;
		}
		
		View view = null;
		
		// check the control type to know the type of the question
        switch (inputDef.controlType) {
            case Constants.CONTROL_INPUT:
            	switch (inputDef.dataType) {
	                case Constants.DATATYPE_INTEGER:
	                	view = viewFactory.createIntegerTextField(inputDef, ref, dynamic);
	                	setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
	                    break;
	                case Constants.DATATYPE_DECIMAL:
	                	view = viewFactory.createDecimalTextField(inputDef, ref, dynamic);
	                	setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
	                    break;
	                case Constants.DATATYPE_LONG:
	                	view = viewFactory.createLongTextField(inputDef, ref, dynamic);
	                	setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
	                    break;
	                case Constants.DATATYPE_DATE:
	                	view = viewFactory.createDatePicker(inputDef, ref, dynamic);
	                	setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
	                    break;
	                case Constants.DATATYPE_TIME:
	                	view = viewFactory.createTimePicker(inputDef, ref, dynamic);
	    				setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
	                    break;
	                case Constants.DATATYPE_TEXT:
	                	view = viewFactory.createTextArea(inputDef, ref, dynamic);
	                	setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
	                    break;
	                default:
	                	// check for additional types
	                	if (inputDef.map) {
	                		if (inputDef.questionText != null && !inputDef.questionText.isEmpty()) {
	                			TextView mapLabel = viewFactory.createLabel(inputDef);
	                			container.addView(mapLabel);
	                		}
	                		MapLayout mapLayout = viewFactory.createMapView(ref, dynamic);
	                		container.addView(mapLayout);
	                		mapViewList.add(mapLayout.getMapView());
	                		view = mapLayout.getMapView();
	                	} else if (inputDef.table) {
	                		if (inputDef.questionText != null && !inputDef.questionText.isEmpty()) {
	                			TextView tableLabel = viewFactory.createLabel(inputDef);
	                			container.addView(tableLabel);
	                		}
	                		view = viewFactory.createTableView(ref, dynamic);
	                		container.addView(view);
	                	} else if (inputDef.web) {
	                		if (inputDef.questionText != null && !inputDef.questionText.isEmpty()) {
	                			TextView webLabel = viewFactory.createLabel(inputDef);
	                			container.addView(webLabel);
	                		}
	                		view = viewFactory.createWebView(ref, dynamic);
	                		container.addView(view);
	                	} else {
	                		view = viewFactory.createTextField(-1, inputDef, ref, dynamic);
	                		setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
	                	}
	                    break;
            	}
                break;
            // create control for select one showing it as drop down
            case Constants.CONTROL_SELECT_ONE:
                switch (inputDef.dataType) {
                    case Constants.DATATYPE_CHOICE:
                    	// Picture Gallery
                        if (FormInputDef.IMAGE_TYPE.equalsIgnoreCase(inputDef.questionType)) {
                            view = viewFactory.createPictureGallery(inputDef, ref, dynamic, false);
                            setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
                        }
                        // Radio Button
                        else if (FormInputDef.RADIO_TYPE.equalsIgnoreCase(inputDef.questionAppearance)) {
                        	view = viewFactory.createRadioGroup(inputDef, ref, dynamic);
                        	setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
                        // List
                        } else if (FormInputDef.LIST_TYPE.equalsIgnoreCase(inputDef.questionAppearance) ) {
                        	if (inputDef.questionText != null && !inputDef.questionText.isEmpty()) {
	                			TextView listLabel = viewFactory.createLabel(inputDef);
	                			container.addView(listLabel);
	                		}
                        	view = viewFactory.createList(inputDef, ref, dynamic);
                        	container.addView(view);
                        // DropDown (default)
                        } else {
                        	view = viewFactory.createDropDown(inputDef, ref, dynamic);
                        	setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
                        }
                        break;
                }
                break;
            // create control for multi select, showing it as checkbox
            case Constants.CONTROL_SELECT_MULTI:
                switch (inputDef.dataType) {
                    case Constants.DATATYPE_CHOICE_LIST:
                    	if (FormInputDef.IMAGE_TYPE.equalsIgnoreCase(inputDef.questionType)) {
                            view = viewFactory.createPictureGallery(inputDef, ref, dynamic, true);
                            setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
                    	} else if (FormInputDef.CAMERA_TYPE.equalsIgnoreCase(inputDef.questionType)) {
                    		view = viewFactory.createCameraPictureGallery(inputDef, ref, dynamic);
                            setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
                    	} else if (FormInputDef.VIDEO_TYPE.equalsIgnoreCase(inputDef.questionType)) {
                    		view = viewFactory.createVideoGallery(inputDef, ref, dynamic);
                            setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
                    	} else if (FormInputDef.FILE_TYPE.equalsIgnoreCase(inputDef.questionType)) {
                    		view = viewFactory.createFileListGroup(inputDef, ref, dynamic);
                    		setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
                        } else {
	                    	view = viewFactory.createCheckListGroup(inputDef, ref, dynamic);
	                    	setupView(container, view, inputDef, ref, isArchEnt, isRelationship);
                        }
                }
                break;
            // create control for trigger showing as a button
            case Constants.CONTROL_TRIGGER:
                view = viewFactory.createTrigger(inputDef, ref, dynamic);
                container.addView(view);
                break;
        }
        
        viewList.add(view);
        
        if (inputDef.styleClass != null) {
			NativeCSS.addCSSClass(view, inputDef.styleClass);
		}
		NativeCSS.setCSSId(view, ref);
        
        if(inputDef.name != null){
        	addViewMappings(inputDef.name, view);
        }
        
        // add view to ui renderer
        beanShellLinker.getUIRenderer().addViewToTab(ref, view, this);
        
        refreshTab();
        
        return view;
	}
	
	public void removeCustomView(String ref) {
		// remove from view list
		for (int i = 0; i < viewList.size(); i++) {
			View view = viewList.get(i);
			if (view instanceof IView) {
				IView iview = (IView) view;
				if (iview.getRef().equals(ref)) {
					if (!iview.isDynamic()) {
						FLog.w("can only remove dynamic views " + ref);
						return ;
					}
					
					ViewParent parent = view.getParent();
					viewList.remove(i);
					
					if (parent instanceof ViewGroup) {
						((ViewGroup) parent).removeView(view);
					} else {
						FLog.w("could not remove view " + iview.getRef());
					}
					
					if (view instanceof ICustomView) {
						ICustomView customView = (ICustomView) view;
						
						String name = customView.getAttributeName();
						if (attributeViewMap.get(name) != null) {
							attributeViewMap.get(name).remove(customView);
							if (attributeViewMap.get(name).isEmpty()) {
								attributeViewMap.remove(name);
							}
						}
					}
					break;
				}
			}
		}
		
		// remove from map list and map layout
		for (int i = 0; i < mapViewList.size(); i++) {
			CustomMapView mapView = mapViewList.get(i);
			if (mapView.getRef().equals(ref)) {
				mapViewList.remove(i);
				mapView.removeLayout();
				break;
			}
		}
		
		// remove from dirty button map
		Button dirtyButton = dirtyButtonMap.get(ref);
		if (dirtyButton != null) {
			ViewParent parent = dirtyButton.getParent();
			if (parent instanceof ViewGroup) {
				((ViewGroup) parent).removeView(dirtyButton);
			} else {
				FLog.w("cannot remove dirty button " + ref);
			}
			dirtyButtonMap.remove(ref);
		}
		
		// remove from layout button map
		LinearLayout layout = viewLayoutMap.get(ref);
		if (layout != null) {
			ViewParent parent = layout.getParent();
			if (parent instanceof ViewGroup) {
				((ViewGroup) parent).removeView(layout);
			} else {
				FLog.w("cannot remove view layout " + ref);
			}
			viewLayoutMap.remove(ref);
		}
		
		// add view to ui renderer
        beanShellLinker.getUIRenderer().removeViewFromTab(ref);
        
        refreshTab();
	}
	
	private void refreshTab() {
		view.postInvalidate();
	}

	private void setupView(LinearLayout linearLayout, View view, FormInputDef attribute, String ref, boolean isArchEnt, boolean isRelationship) {
		Button certaintyButton = null;
    	Button annotationButton = null;
    	Button dirtyButton = null;
    	Button infoButton = null;
		
    	// setup view buttons
		if (attribute.controlType != Constants.CONTROL_TRIGGER) {
			if(attribute.questionText != null && !attribute.questionText.isEmpty()){
				LinearLayout fieldLinearLayout = new LinearLayout(this.linearLayout.getContext());
		    	fieldLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
		    	
	            TextView textView = viewFactory.createLabel(attribute);
	            fieldLinearLayout.addView(textView);
	                
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
	    		
	    		linearLayout.addView(fieldLinearLayout);
	    		viewLayoutMap.put(ref, fieldLinearLayout);
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
	
	public String getRef() {
		return ref;
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
	
	private void addViewMappings(String name, View view){
		if(this.attributeViewMap.containsKey(name)){
			this.attributeViewMap.get(name).add(view);
		}else{
			List<View> views = new ArrayList<View>();
			views.add(view);
			this.attributeViewMap.put(name, views);
		}
	}

	public boolean hasView(String name){
		return this.attributeViewMap.containsKey(name);
	}
	
	public List<View> getViews(){
		return viewList;
	}
	
	public List<View> getAttributeViews(){
		ArrayList<View> attributeViews = new ArrayList<View>();
		for (String attribute : attributeViewMap.keySet()) {
			attributeViews.addAll(attributeViewMap.get(attribute));
		}
		return attributeViews;
	}

	public List<View> getAttributeViews(String name) {
		return attributeViewMap.get(name);
	}

	public void clearViews() {
		List<View> views = new ArrayList<View>();
		views.addAll(viewList);
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
	
	public Button getDirtyButton(String ref) {
		return dirtyButtonMap.get(ref);
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
		refreshCSS();
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
	
	public void saveTo(Bundle savedInstanceState) {
		HashMap<String, Object> viewPairs = new HashMap<String, Object>();
		HashMap<String, Object> viewValues = new HashMap<String, Object>(); 
		HashMap<String, Object> viewCertainties = new HashMap<String, Object>(); 
		HashMap<String, Object> viewAnnotations = new HashMap<String, Object>(); 
		HashMap<String, Object> viewDirtyReasons = new HashMap<String, Object>();
		for (View view : viewList) {
			if (view instanceof ICustomView) {
				String ref = ((ICustomView) view).getRef();
				if (view instanceof FileListGroup) {
					FileListGroup fileList = (FileListGroup) view;
					viewPairs.put(ref, fileList.getPairs());
				} else if (view instanceof CameraPictureGallery) {
					CameraPictureGallery cameraGallery = (CameraPictureGallery) view;
					viewPairs.put(ref, cameraGallery.getPairs());
				} else if (view instanceof VideoGallery) {
					VideoGallery videoGallery = (VideoGallery) view;
					viewPairs.put(ref, videoGallery.getPairs());
				}
				viewValues.put(ref, beanShellLinker.getFieldValue(ref));
				viewCertainties.put(ref, beanShellLinker.getFieldCertainty(ref));
				viewAnnotations.put(ref, beanShellLinker.getFieldAnnotation(ref));
				viewDirtyReasons.put(ref, beanShellLinker.getFieldDirty(ref));
			} else if (view instanceof CustomMapView) {
				CustomMapView map = (CustomMapView) view;
				map.saveTo(savedInstanceState);
			}
		}
		savedInstanceState.putSerializable(getRef() + ":viewPairs", (Serializable) viewPairs);
		savedInstanceState.putSerializable(getRef() + ":viewValues", (Serializable) viewValues);
		savedInstanceState.putSerializable(getRef() + ":viewCertainties", (Serializable) viewCertainties);
		savedInstanceState.putSerializable(getRef() + ":viewAnnotations", (Serializable) viewAnnotations);
		savedInstanceState.putSerializable(getRef() + ":viewDirtyReasons", (Serializable) viewDirtyReasons);
		savedInstanceState.putBoolean(getRef() + ":tabShown", tabShown);
		savedInstanceState.putBoolean(getRef() + ":hidden", hidden);
	}

	@SuppressWarnings("unchecked")
	public void restoreFrom(Bundle savedInstanceState) {
		HashMap<String, Object> viewPairs = (HashMap<String, Object>) savedInstanceState.getSerializable(getRef() + ":viewPairs");
		HashMap<String, Object> viewValues = (HashMap<String, Object>) savedInstanceState.getSerializable(getRef() + ":viewValues");
		HashMap<String, Object> viewCertainties = (HashMap<String, Object>) savedInstanceState.getSerializable(getRef() + ":viewCertainties");
		HashMap<String, Object> viewAnnotations = (HashMap<String, Object>) savedInstanceState.getSerializable(getRef() + ":viewAnnotations");
		HashMap<String, Object> viewDirtyReasons = (HashMap<String, Object>) savedInstanceState.getSerializable(getRef() + ":viewDirtyReasons");
		for(View view : viewList){
			if (view instanceof ICustomView) {
				String ref = ((ICustomView) view).getRef();
				if (view instanceof FileListGroup) {
					FileListGroup fileList = (FileListGroup) view;
					fileList.setPairs((List<NameValuePair>) viewPairs.get(ref));
				} else if (view instanceof CameraPictureGallery) {
					CameraPictureGallery cameraGallery = (CameraPictureGallery) view;
					cameraGallery.setPairs((List<NameValuePair>) viewPairs.get(ref));
				} else if (view instanceof VideoGallery) {
					VideoGallery videoGallery = (VideoGallery) view;
					videoGallery.setPairs((List<NameValuePair>) viewPairs.get(ref));
				}
				beanShellLinker.setFieldValue(ref, viewValues.get(ref));
				beanShellLinker.setFieldCertainty(ref, viewCertainties.get(ref));
				beanShellLinker.setFieldAnnotation(ref, viewAnnotations.get(ref));
				beanShellLinker.setFieldDirty(ref, viewDirtyReasons.get(ref) != null, (String) viewDirtyReasons.get(ref));
			} else if (view instanceof CustomMapView) {
				((CustomMapView) view).restoreFrom(savedInstanceState);
			}
		}
		tabShown = savedInstanceState.getBoolean(getRef() + ":tabShown");
		hidden = savedInstanceState.getBoolean(getRef() + ":hidden");
	}

	public void keepChanges() {
		List<View> views = getAttributeViews();
		for (View v : views) {
			if (v instanceof ICustomView) {
				ICustomView customView = (ICustomView) v;
				customView.save();
			}
		}
	}
	
	public boolean hasChanges() {
		List<View> views = getAttributeViews();
		for (View v : views) {
			if (v instanceof ICustomView) {
				ICustomView customView = (ICustomView) v;
				if (customView.hasChanges()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void refreshCSS() {
		Handler cssHandler = new Handler(beanShellLinker.getActivity().getMainLooper());
		cssHandler.post(new Runnable() {

			@Override
			public void run() {
				if (beanShellLinker.getActivity() != null) {
					NativeCSS.refreshCSSStyling(view);
					for (View v : getViews()) {
						if (v instanceof PictureGallery) {
							((PictureGallery) v).updateImages();
						}
					}
				}
			}
			
		});
	}
	
	public void removeCustomViews() {
		List<String> dynamicViews = new ArrayList<String>();
		for (View view : viewList) {
			if (view instanceof IView) {
				IView iview = (IView) view;
				if (iview.isDynamic()) {
					dynamicViews.add(iview.getRef());
				}
			}
		}
		for (String ref : dynamicViews) {
			removeCustomView(ref);
		}
	}
	
	public void removeCustomContainers() {
		ArrayList<String> dynamicLayouts = new ArrayList<String>();
		for (String ref : containerMap.keySet()) {
			LinearLayout layout = containerMap.get(ref);
			if (layout instanceof CustomLinearLayout) {
				CustomLinearLayout customLayout = (CustomLinearLayout) layout;
				if (customLayout.isDynamic()) {
					dynamicLayouts.add(ref);
				}
			}
		}
		for (String ref : dynamicLayouts) {
			removeCustomContainer(ref);
		}
	}
}
