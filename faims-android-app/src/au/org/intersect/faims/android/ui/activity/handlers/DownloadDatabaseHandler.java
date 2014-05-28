package au.org.intersect.faims.android.ui.activity.handlers;

import android.os.Message;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.view.BeanShellLinker;

import com.google.inject.Inject;

public class DownloadDatabaseHandler extends ShowModuleActivityHandler {
	
	@Inject
	BeanShellLinker beanShellLinker;
	
	private String callback;
	
	public DownloadDatabaseHandler(ShowModuleActivity activity, String callback) {
		super(activity);
		FAIMSApplication.getInstance().injectMembers(this);
		this.callback = callback;
	}
	
	@Override
	public void handleMessageSafe(ShowModuleActivity activity, Message message) {
		activity.hideBusyDialog();
	
		Result result = (Result) message.obj;
		if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
			beanShellLinker.execute(callback);
		} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
			if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
				activity.showBusyErrorDialog();
			} else if (result.errorCode == FAIMSClientErrorCode.STORAGE_LIMIT_ERROR) {
				activity.showDownloadDatabaseErrorDialog(callback);
			} else {
				activity.showDownloadDatabaseFailureDialog(callback);
			}
		} else {
			// ignore
		}
	}

}