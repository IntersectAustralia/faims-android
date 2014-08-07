package au.org.intersect.faims.android.managers;

import java.util.ArrayList;

import android.os.AsyncTask;

import com.google.inject.Singleton;

@Singleton
public class AsyncTaskManager {
	
	private ArrayList<AsyncTask<Void, Void, Void>> tasks;

	public void init() {
		tasks = new ArrayList<AsyncTask<Void,Void,Void>>();
	}
	
	public void destroy() {
		for (AsyncTask<Void,Void,Void> task : tasks) {
			if (!task.isCancelled()) {
				task.cancel(true);
			}
		}
		tasks = null;
	}
	
	public AsyncTask<Void,Void,Void> addAsyncTask(AsyncTask<Void,Void,Void> task) {
		tasks.add(task);
		return task;
	}

}
