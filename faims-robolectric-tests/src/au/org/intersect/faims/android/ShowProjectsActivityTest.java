package au.org.intersect.faims.android;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.widget.ListView;
import au.org.intersect.faims.android.roblectric.FAIMSRobolectricTestRunner;
import au.org.intersect.faims.android.roboguice.TestFAIMSModule;

@RunWith(FAIMSRobolectricTestRunner.class)
public class ShowProjectsActivityTest {

	
	@Before
	  public void setUp() {
	    TestFAIMSModule module = new TestFAIMSModule();
	    TestFAIMSModule.setUp(this, module);
	}
	
	@After
	public void tearDown() {
	  TestFAIMSModule.tearDown();
	}
	  
    @Test
    public void testProjectList() throws Exception {
    	
    	FetchProjectsActivity activity = new FetchProjectsActivity();
        
        activity.onCreate(null);
		 
        ListView projectListView = (ListView) activity.findViewById(R.id.project_list);
        
        assertEquals("Item count", "Test Project One", projectListView.getItemAtPosition(0));
        assertEquals("Item count", "Test Project Two", projectListView.getItemAtPosition(1));
        assertEquals("Item count", "Test Project Three", projectListView.getItemAtPosition(2));
    }
    
}
