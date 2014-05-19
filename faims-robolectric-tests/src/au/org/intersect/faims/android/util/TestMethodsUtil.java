package au.org.intersect.faims.android.util;

import java.util.UUID;

import org.robolectric.Robolectric;

import android.content.Intent;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

public class TestMethodsUtil
{
	public static ShowModuleActivity showModule(String moduleName, String directoryName) {
		String name = getNewModuleName(moduleName);
		String moduleKey = UUID.randomUUID().toString();
		
		Intent intent = new Intent(Robolectric.application, ShowModuleActivity.class);
		intent.putExtra("key", moduleKey);
		
		TestModuleUtil.createModuleFrom(name, moduleKey, directoryName);
		
		ShowModuleActivity activity = Robolectric.buildActivity(ShowModuleActivity.class).withIntent(intent).create().get();
		
		return activity;
	}
	
	public static String getNewModuleName(String baseName) {
		return baseName + "-" + System.currentTimeMillis();
	}
}
