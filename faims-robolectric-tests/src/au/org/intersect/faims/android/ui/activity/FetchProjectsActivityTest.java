package au.org.intersect.faims.android.ui.activity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.widget.ListView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.roblectric.FAIMSRobolectricTestRunner;
import au.org.intersect.faims.android.roboguice.TestFAIMSModule;

@RunWith(FAIMSRobolectricTestRunner.class)
public class FetchProjectsActivityTest {

	@Test
	public void testFetchProjectList() throws Exception {
		TestFAIMSModule module = new TestFAIMSModule();
		TestFAIMSModule.setUp(this, module);
		
		FetchProjectsActivity activity = new FetchProjectsActivity();

		activity.onCreate(null);	
		
		Thread.sleep(1000);

		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);
		
		for (int i = 0; i < 10; i++) {
			assertEquals("Project List Item " + i, "Project " + i,
					projectListView.getItemAtPosition(i));
		}

		TestFAIMSModule.tearDown();
		
	}

}
