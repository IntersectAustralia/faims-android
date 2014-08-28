package au.org.intersect.faims.android.ui.view;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javarosa.core.model.Constants;

import android.app.ActionBar.LayoutParams;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.data.VocabularyTerm;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.dialog.AttributeLabelDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.MapLayout;
import au.org.intersect.faims.android.util.Arch16n;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;
import com.nativecss.NativeCSS;

public class Tab {
	
	private static class Creator implements Serializable {
		private static final long serialVersionUID = -7695642524796470211L;
		
		protected String ref;
		
		public Creator() {
			
		}
	}
	
	private static class ContainerCreator extends Creator implements Serializable {
		private static final long serialVersionUID = -6316380182212646811L;
		
		private String style;
		private String parent;
		
		public ContainerCreator(String ref, String style, String parent) {
			this.ref = ref;
			this.style = style;
			this.parent = parent;
		}
	}
	
	private static class ViewCreator extends Creator implements Serializable {
		private static final long serialVersionUID = 127908031663861660L;
		
		private FormInputDef inputDef;
		private boolean isArchEnt;
		private boolean isRelationship;
		private String containerRef;
		
		public ViewCreator(String ref, FormInputDef inputDef, boolean isArchEnt, boolean isRelationship, String containerRef) {
			this.ref = ref;
			this.inputDef = inputDef;
			this.isArchEnt = isArchEnt;
			this.isRelationship = isRelationship;
			this.containerRef = containerRef;
		}
	}
	
	@SuppressWarnings("unused")
	private static class ViewData implements Serializable {
		private static final long serialVersionUID = -3601918620845812810L;
		
		private List<NameValuePair> pairs;
		private List<VocabularyTerm> terms;
		private String clickCallback;
		private String delayClickCallback;
		private String selectCallback;
		private String focusCallback;
		private String blurCallback;
		private String mapClickCallback;
		private String mapSelectCallback;
		private String toolCreateCallback;
		private String toolLoadCallback;
		private Object value;
		private Object annotation;
		private List<String> annotations;
		private Object certainty;
		private List<String> certainties;
		private String dirtyReason;
		
		public ViewData() {
			
		}
	}

	@Inject
	DatabaseManager databaseManager;
	
	@Inject
	BeanShellLinker beanShellLinker;
	
	@Inject
	Arch16n arch16n;
	
	private static final int PADDING = 15;

	private ViewFactory viewFactory;
	private ScrollView scrollView;
	private LinearLayout linearLayout;
	
	private List<View> viewList;
	private Map<String, List<View>> attributeViewMap;
	private List<CustomMapView> mapViewList;
	private Map<String, ImageView> dirtyButtonMap;
	private Map<String, FrameLayout> viewLayoutMap;
	private Map<String, CustomLinearLayout> containerMap;
	
	private List<Creator> viewCreators;

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
		this.dirtyButtonMap = new HashMap<String, ImageView>();
		this.viewLayoutMap = new HashMap<String, FrameLayout>();
		this.containerMap = new HashMap<String, CustomLinearLayout>();
		
		this.viewCreators = new ArrayList<Creator>();
		
		linearLayout = new LinearLayout(activityRef.get());
        linearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(Color.WHITE);
        int padding = (int) ScaleUtil.getDip(activityRef.get(), PADDING);
        linearLayout.setPadding(padding, padding, padding, padding);
		
        if (scrollable) {
        	scrollView = new ScrollView(this.linearLayout.getContext());
        	scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        	scrollView.setBackgroundColor(Color.WHITE);
        	scrollView.addView(linearLayout);
        	view = scrollView;
        } else {
        	view = linearLayout;
        }
        
        viewFactory = new ViewFactory(activityRef, arch16n);
	}
	
	public void addCustomContainer(String ref, String style, String parentRef) throws Exception {
		viewCreators.add(new ContainerCreator(ref, style, parentRef));
		CustomLinearLayout parentContainer = containerMap.get(parentRef);
		CustomLinearLayout containerLayout = new CustomLinearLayout(linearLayout.getContext(), beanShellLinker.getUIRenderer().getStyleMappings(style), true);
		addCustomContainer(ref, containerLayout, parentContainer);
	}

	public void addCustomContainer(String ref, CustomLinearLayout containerLayout, CustomLinearLayout parentLayout) throws Exception {
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
		int index = 0;
		for (Creator c : viewCreators) {
			if (c.ref.equals(ref)) {
				break;
			}
			index++;
		}
		viewCreators.remove(index);
		
		CustomLinearLayout layout = containerMap.get(ref);
		if (layout != null) {
			if (layout.isDynamic()) {
				ViewParent parent = layout.getParent();
				if (parent instanceof ViewGroup) {
					((ViewGroup) parent).removeView(layout);
					containerMap.remove(ref);
				} else {
					FLog.w("cannot remove container " + ref);
				}
			}
		} else {
			FLog.w("cannot find container " + ref);
		}
		
		refreshTab();
	}
	
	public View addCustomView(String ref, FormInputDef inputDef, boolean isArchEnt, boolean isRelationship, String containerRef) throws Exception {
		viewCreators.add(new ViewCreator(ref, inputDef, isArchEnt, isRelationship, containerRef));
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
	                			TextView mapLabel = viewFactory.createLabel(ref, inputDef);
	                			container.addView(mapLabel);
	                		}
	                		MapLayout mapLayout = viewFactory.createMapView(ref, dynamic);
	                		container.addView(mapLayout);
	                		mapViewList.add(mapLayout.getMapView());
	                		view = mapLayout.getMapView();
	                	} else if (inputDef.table) {
	                		if (inputDef.questionText != null && !inputDef.questionText.isEmpty()) {
	                			TextView tableLabel = viewFactory.createLabel(ref, inputDef);
	                			container.addView(tableLabel);
	                		}
	                		view = viewFactory.createTableView(ref, dynamic);
	                		container.addView(view);
	                	} else if (inputDef.web) {
	                		if (inputDef.questionText != null && !inputDef.questionText.isEmpty()) {
	                			TextView webLabel = viewFactory.createLabel(ref, inputDef);
	                			container.addView(webLabel);
	                		}
	                		view = viewFactory.createWebView(ref, dynamic);
	                		container.addView(view);
	                	} else {
	                		view = viewFactory.createTextField(-1, inputDef, ref, dynamic);
	                		NativeCSS.addCSSClass(view, "input-field");
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
	                			TextView listLabel = viewFactory.createLabel(ref, inputDef);
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
		int index = 0;
		for (Creator c : viewCreators) {
			if (c.ref.equals(ref)) {
				break;
			}
			index++;
		}
		viewCreators.remove(index);
		
		// remove from view list and attribute map
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
		ImageView dirtyButton = dirtyButtonMap.get(ref);
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
		FrameLayout layout = viewLayoutMap.get(ref);
		if (layout != null) {
			ViewParent parent = layout.getParent();
			if (parent instanceof ViewGroup) {
				((ViewGroup) parent).removeView(layout);
			} else {
				FLog.w("cannot remove view layout " + ref);
			}
			viewLayoutMap.remove(ref);
		}
		
		// remove view to ui renderer
        beanShellLinker.getUIRenderer().removeViewFromTab(ref);
        
        refreshTab();
	}
	
	private void refreshTab() {
		view.postInvalidate();
	}

	private void setupView(LinearLayout linearLayout, View view, FormInputDef attribute, String ref, boolean isArchEnt, boolean isRelationship) {
		ImageView certaintyImage = null;
		ImageView annotationImage = null;
		ImageView infoImage = null;
		ImageView dirtyImage = null;
		
		LinearLayout viewLayout = new LinearLayout(linearLayout.getContext());
		viewLayout.setOrientation(LinearLayout.VERTICAL);
		
    	// setup view buttons
		if (view instanceof ICustomView) {
			ICustomView customView = (ICustomView) view;
			if (attribute.controlType != Constants.CONTROL_TRIGGER) {
				if(attribute.questionText != null && !attribute.questionText.isEmpty()){
					FrameLayout fieldFrameLayout = new FrameLayout(this.linearLayout.getContext());
					
					Button buttonOverlay = new Button(fieldFrameLayout.getContext());
					buttonOverlay.setBackgroundColor(Color.TRANSPARENT);
					buttonOverlay.setBackgroundResource(R.drawable.label_selector);
					fieldFrameLayout.addView(buttonOverlay);
			    	
		            TextView textView = viewFactory.createLabel(ref, attribute);
		            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		    		params.gravity = Gravity.CENTER_VERTICAL;
		            fieldFrameLayout.addView(textView, params);
		            
		            LinearLayout icons = new LinearLayout(fieldFrameLayout.getContext());
		            
		            final AttributeLabelDialog labelDialog = new AttributeLabelDialog(linearLayout.getContext(), customView);
		            labelDialog.setTitle(attribute.questionText);
		    		
		    		if(attribute.annotation && (isArchEnt || isRelationship)){
		    			annotationImage = viewFactory.createAnnotationIcon();
		    			if (!(view instanceof CustomFileList)) {
		    				labelDialog.addAnnotationTab();
		    				icons.addView(annotationImage);
		    			}
		    		}
		    		
		    		if(attribute.certainty && (isArchEnt || isRelationship)){
		    			certaintyImage = viewFactory.createCertaintyIcon();
		    			if (!(view instanceof CustomFileList)) {
		    				labelDialog.addCertaintyTab();
		    				icons.addView(certaintyImage);
		    			}
		    		}
		    		
		    		if(attribute.info && attribute.name != null && hasAttributeDescription(attribute.name)){
		    			infoImage = viewFactory.createInfoIcon();
		    			labelDialog.addInfoTab(getAttributeDescription(attribute.name), moduleDir);
		    			icons.addView(infoImage);
		    		}
		    		
		    		if (isArchEnt || isRelationship) {
			    		dirtyImage = viewFactory.createDirtyIcon();
			    		dirtyImage.setVisibility(View.GONE);
			    		labelDialog.addDirtyTab();
			    		icons.addView(dirtyImage);
			    		dirtyButtonMap.put(ref, dirtyImage);
		    		}
		    		
		    		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		    		params.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
		    		fieldFrameLayout.addView(icons, params);
		    		buttonOverlay.setOnLongClickListener(new OnLongClickListener() {
		    			
		    			@Override
		    			public boolean onLongClick(View v) {
	    					labelDialog.show();
		    				return false;
		    			}
		    		});
		    		
		    		viewLayout.addView(fieldFrameLayout);
		    		viewLayoutMap.put(ref, fieldFrameLayout);
				}
	        }
		}
		
        viewLayout.addView(view);
        linearLayout.addView(viewLayout);
        
        if (view instanceof ICustomView) {
        	ICustomView customView = (ICustomView) view;
        	customView.setCertaintyEnabled(certaintyImage != null);
        	customView.setAnnotationEnabled(annotationImage != null);
        }
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
				ImageView dirtyButton = dirtyButtonMap.get(customView.getRef());
				if (dirtyButton != null) dirtyButton.setVisibility(View.GONE);
			} else if (v instanceof CustomFileList) {
				CustomFileList customFileList = (CustomFileList) v;
				customFileList.reset();
			}
		}
	}
	
	public List<CustomMapView> getMapViewList(){
		return mapViewList;
	}
	
	public ImageView getDirtyButton(String ref) {
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
		HashMap<String, ViewData> viewData = new HashMap<String, ViewData>();
		for (View view : viewList) {
			if (view instanceof IView) {
				IView iview = (IView) view;
				String ref = iview.getRef();
				
				ViewData data = new ViewData();
				viewData.put(ref, data);
				
				// store callbacks
				data.clickCallback = iview.getClickCallback();
				data.selectCallback = iview.getSelectCallback();
				data.focusCallback = iview.getFocusCallback();
				data.blurCallback = iview.getBlurCallback();
				
				if (iview instanceof CustomButton) {
					CustomButton button = (CustomButton) iview;
					data.delayClickCallback = button.getDelayClickCallback();
				}
				
				// store pairs
				if (iview instanceof HierarchicalSpinner) {
					HierarchicalSpinner spinner = (HierarchicalSpinner) iview;
					if (spinner.getTerms() == null) {
						data.pairs = spinner.getPairs();
					} else {
						data.terms = spinner.getTerms();
					}
				} else if (iview instanceof HierarchicalPictureGallery) {
					HierarchicalPictureGallery gallery = (HierarchicalPictureGallery) iview;
					if (gallery.getTerms() == null) {
						data.pairs = gallery.getPairs();
					} else {
						data.terms = gallery.getTerms();
					}
				} else if (iview instanceof CustomRadioGroup) {
					CustomRadioGroup radio = (CustomRadioGroup) iview;
					data.pairs = radio.getPairs();
				} else if (iview instanceof CustomCheckBoxGroup) {
					CustomCheckBoxGroup check = (CustomCheckBoxGroup) iview;
					data.pairs = check.getPairs();
				} else if (iview instanceof CustomListView) {
					CustomListView list = (CustomListView) iview;
					data.pairs = list.getPairs();
				} else if (iview instanceof CustomFileList) {
					CustomFileList fileList = (CustomFileList) iview;
					data.pairs = fileList.getPairs();
					data.annotations = fileList.getAnnotations();
					data.certainties = fileList.getCertainties();
				}
				
				// store values
				if (iview instanceof ICustomView) {
					data.value = beanShellLinker.getFieldValue(ref);
					data.annotation = beanShellLinker.getFieldAnnotation(ref);
					data.certainty = beanShellLinker.getFieldCertainty(ref);
					data.dirtyReason = beanShellLinker.getFieldDirty(ref);
				} 
				
				// store map and callbacks
				if (iview instanceof CustomMapView) {
					CustomMapView map = (CustomMapView) iview;
					map.saveTo(savedInstanceState);
					data.mapClickCallback = map.getMapClickCallback();
					data.mapSelectCallback = map.getMapSelectCallback();
					data.toolCreateCallback = map.getCreateCallback();
					data.toolLoadCallback = map.getLoadCallback();
				}
			}
		}
		savedInstanceState.putSerializable(getRef() + ":viewData", (Serializable) viewData);
		savedInstanceState.putSerializable(getRef() + ":viewCreators", (Serializable) viewCreators);
		savedInstanceState.putBoolean(getRef() + ":tabShown", tabShown);
		savedInstanceState.putBoolean(getRef() + ":hidden", hidden);
	}

	@SuppressWarnings("unchecked")
	public void restoreFrom(Bundle savedInstanceState) {
		HashMap<String, ViewData> viewData = (HashMap<String, ViewData>) savedInstanceState.getSerializable(getRef() + ":viewData");
		try {
			ArrayList<Creator> viewCreators = (ArrayList<Creator>) savedInstanceState.getSerializable(getRef() + ":viewCreators");
			if (viewCreators != null) {
				for (Creator c : viewCreators) {
					if (c instanceof ContainerCreator) {
						ContainerCreator cc = (ContainerCreator) c;
						addCustomContainer(cc.ref, cc.style, cc.parent);
					} else if (c instanceof ViewCreator) {
						ViewCreator vc = (ViewCreator) c;
						addCustomView(vc.ref, vc.inputDef, vc.isArchEnt, vc.isRelationship, vc.containerRef);
					}
				}
			}
		} catch (Exception e) {
			beanShellLinker.reportError("Error trying to recover dynamic views", e);
		}
		for(View view : viewList){
			if (view instanceof IView) {
				IView iview = (IView) view;
				String ref = ((IView) view).getRef();
				
				ViewData data = viewData.get(ref);
				
				// load callbacks
				iview.setClickCallback(data.clickCallback);
				iview.setSelectCallback(data.selectCallback);
				iview.setFocusBlurCallbacks(data.focusCallback, data.blurCallback);
				
				// load pairs
				if (iview instanceof HierarchicalSpinner) {
					HierarchicalSpinner spinner = (HierarchicalSpinner) iview;
					if (data.terms == null) {
						spinner.setPairs(data.pairs);
					} else {
						spinner.setTerms(data.terms);
					}
				} else if (iview instanceof HierarchicalPictureGallery) {
					HierarchicalPictureGallery gallery = (HierarchicalPictureGallery) iview;
					if (data.terms == null) {
						gallery.setPairs(data.pairs);
					} else {
						gallery.setTerms(data.terms);
					}
				} else if (iview instanceof CustomRadioGroup) {
					CustomRadioGroup radio = (CustomRadioGroup) iview;
					radio.setPairs(data.pairs);
				} else if (iview instanceof CustomCheckBoxGroup) {
					CustomCheckBoxGroup check = (CustomCheckBoxGroup) iview;
					check.setPairs(data.pairs);
				} else if (iview instanceof CustomListView) {
					CustomListView list = (CustomListView) iview;
					list.setPairs(data.pairs);
				} else if (iview instanceof CustomFileList) {
					CustomFileList fileList = (CustomFileList) iview;
					fileList.setPairs(data.pairs);
					fileList.setAnnotations(data.annotations);
					fileList.setCertainties(data.certainties);
				}
				
				// load values
				if (iview instanceof ICustomView) {
					beanShellLinker.setFieldValue(ref, data.value);
					beanShellLinker.setFieldCertainty(ref, data.certainty);
					beanShellLinker.setFieldAnnotation(ref, data.annotation);
					beanShellLinker.setFieldDirty(ref, data.dirtyReason != null, data.dirtyReason);
				}
				
				// load map
				if (iview instanceof CustomMapView) {
					CustomMapView map = (CustomMapView) iview;
					map.restoreFrom(savedInstanceState);
					map.setMapCallbacks(data.mapClickCallback, data.mapSelectCallback);
					map.setCreateCallback(data.toolCreateCallback);
					map.setLoadCallback(data.toolLoadCallback);
				}
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
		cssHandler.postDelayed(new Runnable() {

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
			
		}, 1);
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

	public void invalidate() {
		view.invalidate();
	}
}
