package au.org.intersect.faims.android.ui.activity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.tester.android.view.TestMenu;
import org.robolectric.tester.android.view.TestMenuItem;

import android.content.Intent;
import android.view.MenuInflater;
import android.widget.ListView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.view.NameValuePair;
import au.org.intersect.faims.android.util.TestModuleUtil;

@Config(manifest="../faims-android-app/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {
	
	@Test
	public void readStoredModulesList() throws Exception {
		
		int count = 10;
		for (int i = 0; i < count; i++) {
			TestModuleUtil.createModule("Module " + i, "key" + i);
		}
		
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().get();

		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);
		
		for (int i = 0; i < count; i++) {
			assertEquals("Module List Item " + i, "Module " + i, ((NameValuePair)moduleListView.getItemAtPosition(i)).getName());
		}
	}
	
	@Test
	public void fetchModuleMenuItemTest(){
		
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
		
		String itemTitle = "Fetch Module List";
		
		TestMenu mainMenu = new TestMenu(activity); 
		new MenuInflater(activity).inflate(R.menu.activity_main, mainMenu); 
		TestMenuItem item = mainMenu.findMenuItem(itemTitle); 
		
		assertEquals("Menu item exists", itemTitle, item.getTitle().toString());
		
		activity.onOptionsItemSelected(item);
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedActivity();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);

		assertEquals("New Activity launched ok", FetchModulesActivity.class.getName().toString(),shadowIntent.getComponent().getClassName());
	}
	
	@Test
	public void loadStoredModuleTest() {
		String moduleName = "Test Module";
		String moduleKey = "123456789";
		
		TestModuleUtil.createModule(moduleName, moduleKey);
		
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().get();
		
		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);
		
		assertEquals("Module List Item ", moduleName, ((NameValuePair)moduleListView.getItemAtPosition(0)).getName());
		
		moduleListView.performItemClick(null, 0, 0);
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedActivity();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);

		assertEquals("New Activity launched ok", ShowModuleActivity.class.getName().toString(),shadowIntent.getComponent().getClassName());
		
		assertEquals("Show module key", moduleKey, startedIntent.getStringExtra("key"));
	}
	
}
