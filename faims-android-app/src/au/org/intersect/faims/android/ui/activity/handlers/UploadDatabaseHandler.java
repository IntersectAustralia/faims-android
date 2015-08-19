package au.org.intersect.faims.android.ui.activity.handlers;

import com.google.inject.Inject;

import android.os.Message;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

public class UploadDatabaseHandler extends ShowModuleActivityHandler {

	@Inject
	BeanShellLinker beanShellLinker;
	
	private String callback;

	public UploadDatabaseHandler(ShowModuleActivity activity, String callback) {
		super(activity);
		FAIMSApplication.getInstance().injectMembers(this);
		this.callback = callback;
	}

	@Override
	public void handleMessageSafe(ShowModuleActivity activity, Message message) {
		activity.hideBusyDialog();

		Result result = (Result) message.obj;
		if (result != null) {			
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				beanShellLinker.execute(callback);
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				activity.showUploadDatabaseFailureDialog(callback);
			} else {
				// ignore
			}
		}
	}

}
