package au.org.intersect.faims.android.managers;

import java.lang.ref.WeakReference;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.beanshell.callbacks.SaveCallback;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.IFAIMSRestorable;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nutiteq.geometry.Geometry;

@Singleton
public class AutoSaveManager implements IFAIMSRestorable {

	private class DelayAutoSave implements Runnable {

		@Override
		public void run() {
			autosave(false, false);
		}
		
	}
	
	public enum Status {
		ACTIVE, READY, SAVING, ERROR, INACTIVE
	}
	
	private static final String TAG = "autosave:";
	private static final int SAVE_AFTER_CHANGE_DELAY = 5000;
	
	@Inject
	BeanShellLinker linker;
	
	private String tabGroupRef;
	private String uuid;
	private List<Geometry> geometry;
	private List<? extends Attribute> attributes;
	private SaveCallback callback;
	private boolean newRecord;

	private boolean enabled;
	private int pauseCounter;
	
	private Handler handler;
	private DelayAutoSave delayAutoSaveRunnable;
	
	private Status status;
	private WeakReference<ShowModuleActivity> activityRef;

	public void init(ShowModuleActivity activity) {
		FAIMSApplication.getInstance().injectMembers(this);
		destroy();
		this.handler = new Handler(activity.getMainLooper());
		this.delayAutoSaveRunnable = new DelayAutoSave();
		this.activityRef = new WeakReference<ShowModuleActivity>(activity);
	}

	@Override
	public void saveTo(Bundle savedInstanceState) {
		savedInstanceState.putString(TAG + "tabGroupRef", tabGroupRef);
		savedInstanceState.putString(TAG + "uuid", uuid);
		savedInstanceState.putBoolean(TAG + "newRecord", newRecord);
	}

	@Override
	public void restoreFrom(Bundle savedInstanceState) {
		tabGroupRef = savedInstanceState.getString(TAG + "tabGroupRef");
		uuid = savedInstanceState.getString(TAG + "uuid");
		newRecord = savedInstanceState.getBoolean(TAG + "newRecord");
	}
 
	@Override
	public void resume() {
		if (enabled) {
			this.pauseCounter--;
			if (this.pauseCounter < 0) {
				FLog.d("autosave manager found to be in illegal state");
			}
		}
	}

	@Override
	public void pause() {
		if (enabled) {
			this.pauseCounter++;
		}
	}

	@Override
	public void destroy() {
		clear();
		this.handler = null;
		this.delayAutoSaveRunnable = null;
	}
	
	private void clear() {
		this.tabGroupRef = null;
		this.uuid = null;
		this.geometry = null;
		this.attributes = null;
		this.callback = null;
		this.newRecord = false;
		this.enabled = false;
		this.pauseCounter = 0;
		setStatus(Status.INACTIVE);
		clearSaveCallbacks();
	}

	public void enable(String tabGroupRef, String uuid, List<Geometry> geometry, List<? extends Attribute> attributes, SaveCallback callback, boolean newRecord) {
		FLog.d("enable autosave on tabgroup " + tabGroupRef + " to uuid " + uuid);
		this.tabGroupRef = tabGroupRef;
		this.uuid = uuid;
		this.geometry = geometry;
		this.attributes = attributes;
		this.callback = callback;
		this.newRecord = newRecord;
		this.enabled = true;
		this.pauseCounter = 0;
		setStatus(Status.ACTIVE);
	}
	
	public void disable(String tabGroupRef) {
		if (enabled) {
			FLog.d("disable autosave on " + tabGroupRef + " to uuid " + uuid);
			clear();
		} else {
			FLog.w("cannot disable autosave as it was not enabled");
		}
	}

	public void flush() {
		autosave(true, true);
	}
	
	public void flush(boolean disableAutoSave) {
		autosave(true, disableAutoSave);
	}

	public void autosave(boolean blocking, boolean disableAutoSave) {
		if (enabled) {
			setStatus(Status.SAVING);
			
			clearSaveCallbacks();
			
			if (pauseCounter == 0) {
				linker.autoSaveTabGroup(tabGroupRef, uuid, geometry, attributes, callback, newRecord, blocking);
				geometry = null;
				attributes = null;
				newRecord = false;
			} else {
				FLog.d("ignore autosave");
			}
			
			if (disableAutoSave) {
				disable(tabGroupRef);
			}
		}
	}
	
	public void save() {
		if (enabled) {
			postSaveCallback(SAVE_AFTER_CHANGE_DELAY);
		}
	}
	
	public void notifyError() {
		if (enabled) {
			setStatus(Status.ERROR);
		}
	}
	
	public void reportSaved() {
		if (enabled) {
			setStatus(Status.ACTIVE);
		}
	}
	
	private void postSaveCallback(long time) {
		if (handler != null) {
			setStatus(Status.READY);
			
			handler.removeCallbacks(delayAutoSaveRunnable);
			handler.postDelayed(delayAutoSaveRunnable, time);
		}
	}
	
	private void clearSaveCallbacks() {
		if (handler != null) {
			handler.removeCallbacks(delayAutoSaveRunnable);
		}
	}
	
	public void setStatus(Status status) {
		this.status = status;
		if (this.activityRef != null) {
			this.activityRef.get().updateActionBar();
		}
	}
	
	public Status getStatus() {
		return status;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
}
