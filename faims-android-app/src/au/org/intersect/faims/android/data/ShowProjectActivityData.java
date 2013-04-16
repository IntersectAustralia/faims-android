package au.org.intersect.faims.android.data;

import android.os.Bundle;

public class ShowProjectActivityData implements ActivityData {

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
	}

	@Override
	public void restoreFrom(Bundle savedInstancestate) {
		setSyncEnabled(savedInstancestate.getBoolean("syncEnabled"));
		setFileSyncEnabled(savedInstancestate.getBoolean("fileSyncEnabled"));
		setUserId(savedInstancestate.getString("userId"));
		setExternalGPSStarted(savedInstancestate.getBoolean("isExternalGPSStarted"));
		setInternalGPSStarted(savedInstancestate.getBoolean("isInternalGPSStarted"));
		setGpsUpdateInterval(savedInstancestate.getInt("gpsUpdateInterval"));
	}

}
