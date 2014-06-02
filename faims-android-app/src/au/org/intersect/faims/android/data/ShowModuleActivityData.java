package au.org.intersect.faims.android.data;

import android.os.Bundle;

public class ShowModuleActivityData implements ActivityData {
	
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
	public void saveTo(Bundle savedInstanceState) {
		savedInstanceState.putBoolean("syncEnabled", syncEnabled);
		savedInstanceState.putBoolean("fileSyncEnabled", fileSyncEnabled);
		savedInstanceState.putString("userId", userId);
		savedInstanceState.putFloat("syncMaxInterval", syncMaxInterval);
		savedInstanceState.putFloat("syncMinInterval", syncMinInterval);
		savedInstanceState.putFloat("syncDelay", syncDelay);
		savedInstanceState.putInt("copyFileCount", copyFileCount);
	}

	@Override
	public void restoreFrom(Bundle savedInstancestate) {
		setSyncEnabled(savedInstancestate.getBoolean("syncEnabled"));
		setFileSyncEnabled(savedInstancestate.getBoolean("fileSyncEnabled"));
		setUserId(savedInstancestate.getString("userId"));
		setSyncMaxInterval(savedInstancestate.getFloat("syncMaxInterval"));
		setSyncMinInterval(savedInstancestate.getFloat("syncMinInterval"));
		setSyncDelay(savedInstancestate.getFloat("syncDelay"));
		setCopyFileCount(savedInstancestate.getInt("copyFileCount"));
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
