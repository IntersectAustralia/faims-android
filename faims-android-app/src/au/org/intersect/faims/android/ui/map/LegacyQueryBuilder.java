package au.org.intersect.faims.android.ui.map;

import org.json.JSONException;
import org.json.JSONObject;

public class LegacyQueryBuilder extends QueryBuilder {

	private String dbPath;
	private String tableName;
	
	public LegacyQueryBuilder() {
		super();
	}
	
	public LegacyQueryBuilder(String sql) {
		super(sql);
	}

	public String getDbPath() {
		return dbPath;
	}

	public void setDbPath(String dbPath) {
		this.dbPath = dbPath;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	@Override
	public void saveToJSON(JSONObject json) throws JSONException {
		super.saveToJSON(json);
		json.put("dbPath", dbPath);
		json.put("tableName", tableName);
	}
	
	@Override
	public void loadFromJSON(JSONObject json) throws JSONException {
		super.loadFromJSON(json);
		dbPath = json.getString("dbPath");
		tableName = json.getString("tableName");
	}

}
