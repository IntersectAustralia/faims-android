package au.org.intersect.faims.android.database;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jsqlite.Stmt;
import au.org.intersect.faims.android.data.VocabularyTerm;

public class AttributeRecord extends Database {

	public AttributeRecord(File dbFile) {
		super(dbFile);
	}
	
	public String getAttributeDescription(String name) throws Exception {
		jsqlite.Database db = null;
		Stmt stmt = null;
		try {
			db = openDB();
			
			String description = null;
			String query = DatabaseQueries.GET_ATTRIBUTE_DESCRIPTION;
			stmt = db.prepare(query);
			stmt.bind(1, name);
			if(stmt.step()){
				description = stmt.column_string(0);
			}
			stmt.close();
			stmt = null;
			
			return description;
		} finally {
			closeStmt(stmt);
			closeDB(db);
		}
	}

	public List<VocabularyTerm> getVocabularyTerms(String attributeName) throws Exception {
		jsqlite.Database db = null;
		Stmt stmt = null;
		try {
			db = openDB();
			
			HashMap<String, VocabularyTerm> vocabIdToTerm = new HashMap<String, VocabularyTerm>();
			HashMap<String, List<VocabularyTerm>> parentIdToTerms = new HashMap<String, List<VocabularyTerm>>();
			
			String query = DatabaseQueries.GET_VOCABULARIES_TERM_DESCRIPTION;
			stmt = db.prepare(query);
			stmt.bind(1, attributeName);
			while(stmt.step()){
				VocabularyTerm term = new VocabularyTerm(stmt.column_string(0), stmt.column_string(1), stmt.column_string(2), stmt.column_string(3));
				
				vocabIdToTerm.put(term.id, term);
				
				String parentId = stmt.column_string(4);
				
				if (parentIdToTerms.get(parentId) == null) {
					parentIdToTerms.put(parentId, new ArrayList<VocabularyTerm>());
				}
				
				List<VocabularyTerm> terms = parentIdToTerms.get(parentId);
				
				terms.add(term);
			}
			stmt.close();
			stmt = null;
			
			// map parent terms to child terms
			for (String parentId : parentIdToTerms.keySet()) {
				if (parentId != null) {
					VocabularyTerm term = vocabIdToTerm.get(parentId);
					term.terms = parentIdToTerms.get(parentId);
				}
			}
			
			return parentIdToTerms.get(null);
		} finally {
			closeStmt(stmt);
			closeDB(db);
		}
	}

}
