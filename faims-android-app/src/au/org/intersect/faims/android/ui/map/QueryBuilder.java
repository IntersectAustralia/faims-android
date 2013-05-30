package au.org.intersect.faims.android.ui.map;

import java.util.ArrayList;
import java.util.List;

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
	
}
