package au.org.intersect.faims.android.tasks;

import android.os.AsyncTask;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.managers.AsyncTaskManager;

import com.google.inject.Inject;

public abstract class CancelableTask extends AsyncTask<Void,Void,Void> {
	
	@Inject
	AsyncTaskManager asyncTaskManager;
	
	public CancelableTask() {
		FAIMSApplication.getInstance().injectMembers(this);
		asyncTaskManager.addAsyncTask(this);
	}

}
