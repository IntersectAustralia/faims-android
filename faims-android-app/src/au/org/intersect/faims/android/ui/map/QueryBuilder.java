package au.org.intersect.faims.android.ui.map;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QueryBuilder {
	
	public static class Parameter {
		
		public String name;
		public String defaultValue;
		
		public Parameter(String name, String defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}
		
	}

	private String name;
	private String sql;
	private ArrayList<Parameter> parameters;
	
	public QueryBuilder() {
		this.parameters = new ArrayList<Parameter>();
	}

	public QueryBuilder(String sql) {
		this.sql = sql;
		this.parameters = new ArrayList<Parameter>();
	}
	
	public QueryBuilder addParameter(String name, String defaultValue) {
		parameters.add(new Parameter(name, defaultValue));
		return this;
	}
	
	public String getSql() {
		return sql;
	}
	
	public List<Parameter> getParameters() {
		return parameters;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void saveToJSON(JSONObject json) throws JSONException {
		json.put("name", name);
		json.put("sql", sql);
		JSONArray parametersJSON = new JSONArray();
		for (Parameter p : parameters) {
			JSONObject paramJSON = new JSONObject();
			paramJSON.put("name", p.name);
			paramJSON.put("defaultValue", p.defaultValue);
			parametersJSON.put(paramJSON);
		}
		json.put("params", parametersJSON);
	}
	
	public void loadFromJSON(JSONObject json) throws JSONException {
		name = json.getString("name");
		sql = json.optString("sql");
		JSONArray parametersJSON = json.getJSONArray("params");
		for (int i = 0; i < parametersJSON.length(); i++) {
			JSONObject paramJSON = parametersJSON.getJSONObject(i);
			parameters.add(new Parameter(paramJSON.getString("name"), paramJSON.optString("defaultValue")));
		}
	}
	
}
