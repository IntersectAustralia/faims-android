package au.org.intersect.faims.android.ui.map;

public class LegacyQueryBuilder extends QueryBuilder {

	private String dbPath;
	private String tableName;
	
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

}
