package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import android.content.Context;
import android.view.View;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.data.VocabularyTerm;
import au.org.intersect.faims.android.log.FLog;

public class HierarchicalPictureGallery extends PictureGallery {
	
	private List<VocabularyTerm> terms;
	
	private Stack<VocabularyTerm> parentTerms;
	
	private List<VocabularyTerm> currentTerms;

	private List<Picture> currentPictures;

	private HashMap<String, VocabularyTerm> vocabIdToParentTerm;
	private HashMap<String, List<VocabularyTerm>> vocabIdToParentTerms;

	public HierarchicalPictureGallery(Context context) {
		super(context);
	}
	
	public HierarchicalPictureGallery(Context context, FormAttribute attribute, String ref) {
		super(context, attribute, ref, false);
	}
	
	private void mapVocabToParent() {
		this.vocabIdToParentTerm = new HashMap<String, VocabularyTerm>();
		this.vocabIdToParentTerms = new HashMap<String, List<VocabularyTerm>>();
		
		for (VocabularyTerm term : terms) {
			vocabIdToParentTerm.put(term.id, null);
			vocabIdToParentTerms.put(term.id, null);
			if (term.terms != null) {
				mapVocabToParent(term);
			}
		}
	}
	
	private void mapVocabToParent(VocabularyTerm parentTerm) {
		for (VocabularyTerm term : parentTerm.terms) {
			vocabIdToParentTerm.put(term.id, parentTerm);
			vocabIdToParentTerms.put(term.id, parentTerm.terms);
			if (term.terms != null) {
				mapVocabToParent(term);
			}
		}
	}

	public void setTerms(List<VocabularyTerm> terms) {
		this.terms = terms;
		
		mapVocabToParent();
		
		this.parentTerms = new Stack<VocabularyTerm>();
		
		loadTerms();
	}
	
	private List<Picture> termsToPictures(List<VocabularyTerm> terms) {
		ArrayList<Picture> pictures = new ArrayList<Picture>();
		if (terms == null) return pictures;
		for (VocabularyTerm term : terms) {
			pictures.add(termToPicture(term));
		}
		return pictures;
	}
	
	private Picture termToPicture(VocabularyTerm term) {
		return new Picture(term.id, term.toString(), term.pictureURL);
	}
	
	private void loadTerms() {
		if (parentTerms.size() == 0) { 
			currentTerms = terms;
			currentPictures = termsToPictures(terms);
			
			populate(currentPictures);
		} else {
			VocabularyTerm selectedTerm = parentTerms.peek();
			currentTerms = new ArrayList<VocabularyTerm>();
			currentPictures = new ArrayList<Picture>();
			for (VocabularyTerm term : parentTerms) {
				currentTerms.add(term);
				currentPictures.add(new Picture("", "Back to: " + term.name, term.pictureURL));
			}
			currentTerms.addAll(selectedTerm.terms);
			currentPictures.addAll(termsToPictures(selectedTerm.terms));
			
			populate(currentPictures);
		}
		this.setScrollX(0);	
	}
	
	@Override
	protected void updateImageListeners() {
		if (galleryImages == null || galleryImages.size() == 0) return;
		
		// picture galleries
		if (terms == null) {
			for (CustomImageView image : galleryImages) {
				// Set listener if not already selected, otherwise remove
				if(selectedImages == null || (selectedImages != null && !selectedImages.contains(image))) {
					image.setOnClickListener(this.customListener);
				} else {
					image.setOnClickListener(null);
				}
	        }
		} else {
			// hierarchical picture galleries
			for(VocabularyTerm term : currentTerms) {
				if(currentTerms.indexOf(term) < galleryImages.size()) {
					CustomImageView galleryImage = galleryImages.get(currentTerms.indexOf(term));
					boolean selected = selectedImages != null && selectedImages.contains(galleryImage);
					boolean isParentTerm = term.terms != null;
					if(!isParentTerm && selected) {
						// Remove listener if a selected leaf
						galleryImages.get(currentTerms.indexOf(term)).setOnClickListener(null);						
					} else if(isParentTerm && selected) {
						// set only internal listener if a parent term and is selected
						galleryImages.get(currentTerms.indexOf(term)).setOnClickListener(this.internalListener);
					} else {
						galleryImages.get(currentTerms.indexOf(term)).setOnClickListener(this.customListener);
					}
				}
			}
		}
	}
	
	@Override
	protected void selectImage(View v) {
		if (terms == null) {
			super.selectImage(v);
			return;
		}
		
		setSelectionItem(galleryImages.indexOf(v));
	}
	
	@Override
	public void setSelectionItem(int position) {
		if (terms == null) {
			super.setSelectionItem(position);
			return;
		}
		
		try {
			VocabularyTerm selectedTerm = currentTerms.get(position);
			
			if (position >= parentTerms.size()) {
				if (selectedTerm.terms != null) {
					parentTerms.push(selectedTerm);
					loadTerms();
					super.setSelectionItem(parentTerms.size() - 1);
				} else {
					super.setSelectionItem(position);
				}
			} else {
				VocabularyTerm parentTerm = null;
				while(parentTerms.size() > position) {
					parentTerm = parentTerms.pop();
				}
				loadTerms();
				if (parentTerms.size() == 0) {
					super.setSelectionItem(terms.indexOf(parentTerm));
				} else {
					super.setSelectionItem(parentTerms.peek().terms.indexOf(parentTerm) + parentTerms.size());
				}
			}
		} catch (Exception e) {
			FLog.e("error selecting item on hierarchical picture gallery", e);
		}
	}
	
	@Override
	public void setValue(String value) {
		if (terms == null) {
			super.setValue(value);
			return;
		}
		
		if (value == null || "".equals(value)) {
			return;
		}
		
		// add terms to parent stack
		parentTerms.clear();
		VocabularyTerm parentTerm = vocabIdToParentTerm.get(value);
		while(parentTerm != null) {
			parentTerms.insertElementAt(parentTerm, 0);
			parentTerm = vocabIdToParentTerm.get(parentTerm.id);
		}
		
		// load terms
		loadTerms();
		
		// set selection using position in term list
		List<VocabularyTerm> terms = vocabIdToParentTerms.get(value);
		if (terms == null) {
			terms = this.terms;
		}
		
		int index = 0;
		for (VocabularyTerm t : terms) {
			if (t.id.equals(value)) {
				setSelectionItem(parentTerms.size() + index);
				break;
			}
			index++;
		}
		notifySave();
	}
	
	@Override
	public String getValue() {
		if (terms == null) {
			return super.getValue();
		}
		
		int index = getSelectionItem();
		if (index < 0) return null;
		
		return currentTerms.get(index).id;
	}
	
	@Override
	public void reset() {
		if (terms == null) {
			super.reset();
			return;
		}
		
		dirty = false;
		dirtyReason = null;
		
		parentTerms.clear();
		loadTerms();
		
		removeSelectedImages();
		setCertainty(1);
		setAnnotation("");
		
		save();
	}

}
