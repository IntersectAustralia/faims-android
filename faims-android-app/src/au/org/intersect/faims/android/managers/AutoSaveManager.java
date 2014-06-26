package au.org.intersect.faims.android.managers;

import android.os.Bundle;
import android.os.Handler;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.data.IFAIMSRestorable;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AutoSaveManager implements IFAIMSRestorable {

	private class DelayAutoSave implements Runnable {

		@Override
		public void run() {
			flush(false);
		}
		
	}
	
	private static final String TAG = "autosave:";
	private static final int SAVE_AFTER_CHANGE_DELAY = 5000;
	
	@Inject
	BeanShellLinker linker;
	
	private String tabGroupRef;
	private String uuid;
	private boolean newRecord;

	private boolean enabled;
	private int pauseCounter;
	
	private Handler handler;
	private DelayAutoSave delayAutoSaveRunnable;

	public void init(ShowModuleActivity activity) {
		FAIMSApplication.getInstance().injectMembers(this);
		destroy();
		this.handler = new Handler(activity.getMainLooper());
		this.delayAutoSaveRunnable = new DelayAutoSave();
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
		this.newRecord = false;
		this.enabled = false;
		this.pauseCounter = 0;
		clearSaveCallbacks();
	}

	public void enable(String tabGroupRef, String uuid, boolean newRecord) {
		FLog.d("enable autosave on tabgroup " + tabGroupRef + " to uuid " + uuid);
		this.tabGroupRef = tabGroupRef;
		this.uuid = uuid;
		this.newRecord = newRecord;
		this.enabled = true;
		this.pauseCounter = 0;
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
		flush(true);
	}

	public void flush(boolean disableAutoSave) {
		if (enabled) {
			clearSaveCallbacks();
			
			if (disableAutoSave) {
				disable(tabGroupRef);
			}
			
			if (pauseCounter == 0) {
				FLog.d("autosaving...");
				linker.showToast("autosaving...");
				newRecord = false;
			} else {
				FLog.d("ignore autosave");
			}
		}
	}
	
	public void save() {
		if (enabled) {
			postSaveCallback(SAVE_AFTER_CHANGE_DELAY);
		}
	}
	
	private void postSaveCallback(long time) {
		if (handler != null) {
			handler.removeCallbacks(delayAutoSaveRunnable);
			handler.postDelayed(delayAutoSaveRunnable, time);
		}
	}
	
	private void clearSaveCallbacks() {
		if (handler != null) {
			handler.removeCallbacks(delayAutoSaveRunnable);
		}
	}
}
