package au.org.intersect.faims.android.data;


public class ShowModuleActivityData implements ActivityData {
	
	private static final String TAG = "activitydata:";
	
	private float syncMinInterval;
	private float syncMaxInterval;
	private float syncDelay;

	private boolean syncEnabled;

	private boolean fileSyncEnabled;

	private String userId;
	
	private int copyFileCount;

	public boolean isSyncEnabled() {
		return syncEnabled;
	}

	public void setSyncEnabled(boolean syncEnabled) {
		this.syncEnabled = syncEnabled;
	}

	public boolean isFileSyncEnabled() {
		return fileSyncEnabled;
	}

	public void setFileSyncEnabled(boolean fileSyncEnabled) {
		this.fileSyncEnabled = fileSyncEnabled;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getUserId() {
		return this.userId;
	}

	@Override
	public void saveTo(PersistentBundle bundle) {
		bundle.putBoolean(TAG + "syncEnabled", syncEnabled);
		bundle.putBoolean(TAG + "fileSyncEnabled", fileSyncEnabled);
		bundle.putString(TAG + "userId", userId);
		bundle.putFloat(TAG + "syncMaxInterval", syncMaxInterval);
		bundle.putFloat(TAG + "syncMinInterval", syncMinInterval);
		bundle.putFloat(TAG + "syncDelay", syncDelay);
		bundle.putInt(TAG + "copyFileCount", copyFileCount);
	}

	@Override
	public void restoreFrom(PersistentBundle bundle) {
		setSyncEnabled(bundle.getBoolean(TAG + "syncEnabled"));
		setFileSyncEnabled(bundle.getBoolean(TAG + "fileSyncEnabled"));
		setUserId(bundle.getString(TAG + "userId"));
		setSyncMaxInterval(bundle.getFloat(TAG + "syncMaxInterval"));
		setSyncMinInterval(bundle.getFloat(TAG + "syncMinInterval"));
		setSyncDelay(bundle.getFloat(TAG + "syncDelay"));
		setCopyFileCount(bundle.getInt(TAG + "copyFileCount"));
	}

	public float getSyncMinInterval() {
		return syncMinInterval;
	}

	public void setSyncMinInterval(float syncMinInterval) {
		this.syncMinInterval = syncMinInterval;
	}

	public float getSyncMaxInterval() {
		return syncMaxInterval;
	}

	public void setSyncMaxInterval(float syncMaxInterval) {
		this.syncMaxInterval = syncMaxInterval;
	}

	public float getSyncDelay() {
		return syncDelay;
	}

	public void setSyncDelay(float syncDelay) {
		this.syncDelay = syncDelay;
	}

	public int getCopyFileCount() {
		return copyFileCount;
	}

	public void setCopyFileCount(int copyFileCount) {
		this.copyFileCount = copyFileCount;
	}

	@Override
	public void resume() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void destroy() {
	}

}
