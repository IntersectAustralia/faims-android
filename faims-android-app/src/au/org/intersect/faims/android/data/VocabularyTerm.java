package au.org.intersect.faims.android.data;

import java.util.List;

import au.org.intersect.faims.android.ui.form.Arch16n;
import au.org.intersect.faims.android.ui.form.NameValuePair;

public class VocabularyTerm extends NameValuePair {
	
	private static final long serialVersionUID = 6760908313556456448L;
	
	public String id;
	public String name;
	public String description;
	public String pictureURL;
	public List<VocabularyTerm> terms;
	
	public VocabularyTerm(String id, String name,
			String description, String pictureURL) {
		super(name, id);
		this.id = id;
		this.name = name;
		this.description = description;
		this.pictureURL = pictureURL;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public String toString() {
		if (terms != null)
			return this.name + " ...";
		return this.name; 
	}

	public static void applyArch16n(List<VocabularyTerm> terms, Arch16n arch16n) {
		if (terms == null) return;
		for (VocabularyTerm term : terms) {
			term.name = arch16n.substituteValue(term.name);
			term.description = arch16n.substituteValue(term.description);
			if (term.terms != null) {
				applyArch16n(term.terms, arch16n);
			}
		}
	}

	public static void applyProjectDir(List<VocabularyTerm> terms,
			String projectDir) {
		if (terms == null) return;
		for (VocabularyTerm term : terms) {
			if (term.pictureURL != null) {
				term.pictureURL = projectDir + term.pictureURL;
			}
			if (term.terms != null) {
				applyProjectDir(term.terms, projectDir);
			}
		}
	}

}
