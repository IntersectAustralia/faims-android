package au.org.intersect.faims.android.data;

import android.os.Bundle;

public class ShowProjectActivityData implements ActivityData {
	
	private float syncMinInterval;
	private float syncMaxInterval;
	private float syncDelay;

	private boolean syncEnabled;

	private boolean fileSyncEnabled;

	private String userId;
	
	private boolean isExternalGPSStarted;

	private boolean isInternalGPSStarted;

	private int gpsUpdateInterval;

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

	public boolean isExternalGPSStarted() {
		return isExternalGPSStarted;
	}

	public void setExternalGPSStarted(boolean isExternalGPSStarted) {
		this.isExternalGPSStarted = isExternalGPSStarted;
	}

	public boolean isInternalGPSStarted() {
		return isInternalGPSStarted;
	}

	public void setInternalGPSStarted(boolean isInternalGPSStarted) {
		this.isInternalGPSStarted = isInternalGPSStarted;
	}

	public int getGpsUpdateInterval() {
		return gpsUpdateInterval;
	}

	public void setGpsUpdateInterval(int gpsUpdateInterval) {
		this.gpsUpdateInterval = gpsUpdateInterval;
	}

	@Override
	public void saveTo(Bundle savedInstanceState) {
		savedInstanceState.putBoolean("syncEnabled", syncEnabled);
		savedInstanceState.putBoolean("fileSyncEnabled", fileSyncEnabled);
		savedInstanceState.putString("userId", userId);
		savedInstanceState.putBoolean("isExternalGPSStarted", isExternalGPSStarted);
		savedInstanceState.putBoolean("isInternalGPSStarted", isInternalGPSStarted);
		savedInstanceState.putInt("gpsUpdateInterval", gpsUpdateInterval);
		savedInstanceState.putFloat("syncMaxInterval", syncMaxInterval);
		savedInstanceState.putFloat("syncMinInterval", syncMinInterval);
		savedInstanceState.putFloat("syncDelay", syncDelay);
	}

	@Override
	public void restoreFrom(Bundle savedInstancestate) {
		setSyncEnabled(savedInstancestate.getBoolean("syncEnabled"));
		setFileSyncEnabled(savedInstancestate.getBoolean("fileSyncEnabled"));
		setUserId(savedInstancestate.getString("userId"));
		setExternalGPSStarted(savedInstancestate.getBoolean("isExternalGPSStarted"));
		setInternalGPSStarted(savedInstancestate.getBoolean("isInternalGPSStarted"));
		setGpsUpdateInterval(savedInstancestate.getInt("gpsUpdateInterval"));
		setSyncMaxInterval(savedInstancestate.getFloat("syncMaxInterval"));
		setSyncMinInterval(savedInstancestate.getFloat("syncMinInterval"));
		setSyncDelay(savedInstancestate.getFloat("syncDelay"));
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

}
