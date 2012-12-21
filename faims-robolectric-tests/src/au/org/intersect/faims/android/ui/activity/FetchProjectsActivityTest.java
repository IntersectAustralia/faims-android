package au.org.intersect.faims.android.ui.activity;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.MenuInflater;
import android.widget.ListView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.roblectric.FAIMSRobolectricTestRunner;
import au.org.intersect.faims.android.roboguice.TestFAIMSModule;
import au.org.intersect.faims.android.test.helper.TestFAIMSClient;
import au.org.intersect.faims.android.test.helper.TestServerDiscovery;

import com.xtremelabs.robolectric.tester.android.view.TestMenu;
import com.xtremelabs.robolectric.tester.android.view.TestMenuItem;

@RunWith(FAIMSRobolectricTestRunner.class)
public class FetchProjectsActivityTest {
	
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
	public void testFetchProjectList() throws Exception {

		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null); // this automatically fetches projects
		
		// reset the server discovery
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		// update test client
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
	
		int count = 10;
		client.setProjectsCount(count);
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		
		activity.fetchProjectsList();
		
		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);

		for (int i = 0; i < count; i++) {
			assertEquals("Project " + i + " exists", "Project " + i,
					projectListView.getItemAtPosition(i));
		}
	}

	@Test
	public void refreshListMenuItemTest(){
		
		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null);
		
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
	
		int count = 10;
		client.setProjectsCount(count);
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		
		activity.fetchProjectsList();
		
		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);

		for (int i = 0; i < count; i++) {
			assertEquals("Project " + i + " exists", "Project " + i,
					projectListView.getItemAtPosition(i));
		}
		
		// update count and refresh list
		count += 5;
		client.setProjectsCount(count);
		
		String itemTitle = "Refresh Project List";
		
		TestMenu mainMenu = new TestMenu(activity); 
		new MenuInflater(activity).inflate(R.menu.acitvity_fetch_projects, mainMenu); 
		TestMenuItem item = mainMenu.findMenuItem(itemTitle); 
		
		assertEquals("Menu item exists", itemTitle, item.getTitle().toString());
		
		activity.onOptionsItemSelected(item);
		
		for (int i = 0; i < count; i++) {
			assertEquals("Refreshed Project " + i + " exists", "Project " + i,
					projectListView.getItemAtPosition(i));
		}
	}
	
	@Test
	public void testServerDiscoveryFailure() throws Exception {
		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null);
		
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(false);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		
		activity.fetchProjectsList();
		
		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);
		assertEquals("No projects exist", projectListView.getChildCount(), 0);

		String dialogTitle = "Could not find Server";
		//assertEquals("Dialog exists",
		//		activity.choiceDialog.getActionBar().getTitle(), dialogTitle);
		
		assertEquals("Dialog showing",
				activity.choiceDialog.isShowing(), true);
		
		// TODO check how to test user interactions
	}
	
	@Test
	public void testFetchProjectsListFailure() throws Exception {

		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null);
		
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);

		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setProjectsResultCode(FAIMSClientResultCode.SERVER_FAILURE);
		
		activity.fetchProjectsList();
		
		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);
		assertEquals("No projects exist", projectListView.getChildCount(), 0);

		String dialogTitle = "Could not fetch projects";
		//assertEquals("Dialog exists",
		//		activity.choiceDialog.getActionBar().getTitle(), dialogTitle);
		
		assertEquals("Dialog showing",
				activity.choiceDialog.isShowing(), true);
		
		// TODO check how to test user interactions

	}
	
	@Test
	public void testDownloadProjectSuccess() throws Exception {
		
		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null);
		
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);

		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setProjectsCount(10);
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.SERVER_FAILURE);
		
		activity.fetchProjectsList();
		
		// TODO pick project form dialog
		activity.selectedProject = activity.projects.get(0);
		
		activity.downloadProjectArchive();

		// TODO assert file has been downloaded
	}
	
	@Test
	public void testDownloadProjectServerFailure() throws Exception {

		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null);
		
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);

		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setProjectsCount(10);
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.SERVER_FAILURE);
		
		activity.fetchProjectsList();
		
		// TODO pick project form dialog
		activity.selectedProject = activity.projects.get(0);
		
		activity.downloadProjectArchive();
		
		// TODO assert no project has been downloaded

		String dialogTitle = "Could not download project";
		//assertEquals("Dialog exists",
		//		activity.choiceDialog.getActionBar().getTitle(), dialogTitle);
		
		assertEquals("Dialog showing",
				activity.choiceDialog.isShowing(), true);

	}
	
	@Test
	public void testDownloadProjectCorrupted() throws Exception {
		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null);

		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setProjectsCount(10);
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.DOWNLOAD_CORRUPTED);
		
		activity.fetchProjectsList();
		
		// TODO pick project form dialog
		activity.selectedProject = activity.projects.get(0);
		
		activity.downloadProjectArchive();
		
		// TODO assert no project has been downloaded

		String dialogTitle = "Could not download project";
		//assertEquals("Dialog exists",
		//		activity.choiceDialog.getActionBar().getTitle(), dialogTitle);
		
		assertEquals("Dialog showing",
				activity.choiceDialog.isShowing(), true);
	}
	
	@Test
	public void testDownloadProjectTooBig() throws Exception {
		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null);

		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setProjectsCount(10);
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.STORAGE_LIMIT_ERROR);
		
		activity.fetchProjectsList();
		
		// TODO pick project form dialog
		activity.selectedProject = activity.projects.get(0);
		
		activity.downloadProjectArchive();
		
		// TODO assert no project has been downloaded

		String dialogTitle = "Project is too big";
		//assertEquals("Dialog exists",
		//		activity.confirmDialog.getActionBar().getTitle(), dialogTitle);
		
		assertEquals("Dialog showing",
				activity.confirmDialog.isShowing(), true);
	}

}
