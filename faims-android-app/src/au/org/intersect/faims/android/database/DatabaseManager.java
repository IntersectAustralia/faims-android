package au.org.intersect.faims.android.database;

import java.io.File;
import java.util.ArrayList;

import com.google.inject.Singleton;

@Singleton
public class DatabaseManager {

	protected File dbFile;
	protected String userId;

	private ArrayList<DatabaseChangeListener> listeners;

	private FetchRecord fetchRecord;
	private MergeRecord mergeRecord;
	private EntityRecord entityRecord;
	private RelationshipRecord relationshipRecord;
	private QueryRecord queryRecord;
	private AttributeRecord attributeRecord;
	private SpatialRecord spatialRecord;
	private SharedRecord sharedRecord;

	public void init(File dbFile) {
		this.dbFile = dbFile;
		this.listeners = new ArrayList<DatabaseChangeListener>();
		this.fetchRecord = new FetchRecord(dbFile);
		this.mergeRecord = new MergeRecord(dbFile);
		this.entityRecord = new EntityRecord(dbFile);
		this.relationshipRecord = new RelationshipRecord(dbFile);
		this.sharedRecord = new SharedRecord(dbFile);
		this.queryRecord = new QueryRecord(dbFile);
		this.attributeRecord = new AttributeRecord(dbFile);
		this.spatialRecord = new SpatialRecord(dbFile);
	}

	public void setUserId(String userId) {
		this.userId = userId;
		this.entityRecord.setUserId(userId);
		this.relationshipRecord.setUserId(userId);
		this.sharedRecord.setUserId(userId);
	}

	public String getUserId() {
		return this.userId;
	}

	public void addListener(DatabaseChangeListener listener) {
		this.listeners.add(listener);
	}

	public void notifyListeners() {
		for (int i = 0; i < listeners.size(); i++) {
			this.listeners.get(i).onDatabaseChange();
		}
	}

	public FetchRecord fetchRecord() {
		return this.fetchRecord;
	}

	public MergeRecord mergeRecord() {
		return this.mergeRecord;
	}

	public EntityRecord entityRecord() {
		return this.entityRecord;
	}

	public RelationshipRecord relationshipRecord() {
		return this.relationshipRecord;
	}

	public SharedRecord sharedRecord() {
		return this.sharedRecord;
	}

	public QueryRecord queryRecord() {
		return this.queryRecord;
	}

	public AttributeRecord attributeRecord() {
		return this.attributeRecord;
	}

	public SpatialRecord spatialRecord() {
		return this.spatialRecord;
	}

	public void interrupt() {
		this.fetchRecord().interrupt();
		this.mergeRecord().interrupt();
		this.entityRecord().interrupt();
		this.relationshipRecord().interrupt();
		this.sharedRecord().interrupt();
		this.queryRecord().interrupt();
		this.attributeRecord().interrupt();
		this.spatialRecord().interrupt();
	}

}
