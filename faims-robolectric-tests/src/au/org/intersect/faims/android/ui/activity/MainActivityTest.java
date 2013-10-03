package au.org.intersect.faims.android.ui.activity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.view.MenuInflater;
import android.widget.ListView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.roblectric.FAIMSRobolectricTestRunner;
import au.org.intersect.faims.android.ui.form.NameValuePair;
import au.org.intersect.faims.android.util.TestModuleUtil;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import com.xtremelabs.robolectric.shadows.ShadowIntent;
import com.xtremelabs.robolectric.tester.android.view.TestMenu;
import com.xtremelabs.robolectric.tester.android.view.TestMenuItem;

@RunWith(FAIMSRobolectricTestRunner.class)
public class MainActivityTest {
	
	@Test
	public void readStoredModulesList() throws Exception {
		
		int count = 10;
		for (int i = 0; i < count; i++) {
			TestModuleUtil.createModule("Module " + i, "key" + i);
		}
		
		MainActivity activity = new MainActivity();
		activity.onCreate(null);
		activity.onStart();

		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);
		
		for (int i = 0; i < count; i++) {
			assertEquals("Module List Item " + i, "Module " + i, ((NameValuePair)moduleListView.getItemAtPosition(i)).getName());
		}
	}
	
	@Test
	public void fetchModuleMenuItemTest(){
		
		MainActivity activity = new MainActivity();
		activity.onCreate(null);
		
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
		
		MainActivity activity = new MainActivity();
		activity.onCreate(null);
		activity.onStart();
		
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
