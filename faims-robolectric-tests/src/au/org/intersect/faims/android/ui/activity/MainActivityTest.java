package au.org.intersect.faims.android.ui.activity;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import com.xtremelabs.robolectric.shadows.ShadowIntent;
import com.xtremelabs.robolectric.tester.android.view.TestMenu;
import com.xtremelabs.robolectric.tester.android.view.TestMenuItem;

import android.content.Intent;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.roblectric.FAIMSRobolectricTestRunner;

@RunWith(FAIMSRobolectricTestRunner.class)
public class MainActivityTest {

	@Test
	public void pageSetupTest() {
		
		MainActivity activity = new MainActivity();
		activity.onCreate(null);
		
		TextView tv = (TextView) activity.findViewById(R.id.welcome_text);
		String welcomeText = activity.getResources().getString (R.string.welcome);
		
		assertEquals("Text is visible", View.VISIBLE, tv.getVisibility());
		assertEquals("Text is correct", welcomeText, tv.getText().toString());

	}

	@Test
	public void fetchProjectMenuItemTest(){
		
		MainActivity activity = new MainActivity();
		activity.onCreate(null);
		
		String itemTitle = "Fetch Project List";
		
		TestMenu mainMenu = new TestMenu(activity); 
		new MenuInflater(activity).inflate(R.menu.activity_main, mainMenu); 
		TestMenuItem item = mainMenu.findMenuItem(itemTitle); 
		
		assertEquals("Menu item exists", itemTitle, item.getTitle().toString());
		
		activity.onOptionsItemSelected(item);
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedActivity();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);

		assertEquals("New Activity launched ok", FetchProjectsActivity.class.getName().toString(),shadowIntent.getComponent().getClassName());
	}
	
}
