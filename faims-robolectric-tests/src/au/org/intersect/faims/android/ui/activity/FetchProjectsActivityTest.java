package au.org.intersect.faims.android.ui.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.os.Message;
import android.view.MenuInflater;
import android.widget.ListView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.TestFAIMSClient;
import au.org.intersect.faims.android.net.TestServerDiscovery;
import au.org.intersect.faims.android.roblectric.FAIMSRobolectricTestRunner;
import au.org.intersect.faims.android.roboguice.TestFAIMSModule;
import au.org.intersect.faims.android.services.DownloadProjectService;
import au.org.intersect.faims.android.services.TestDownloadProjectService;
import au.org.intersect.faims.android.util.ProjectUtil;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import com.xtremelabs.robolectric.shadows.ShadowAlertDialog;
import com.xtremelabs.robolectric.shadows.ShadowIntent;
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
		
		// set invalid host
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(false);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		int count = 10;
		client.setProjectsCount(count);
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		
		// fetch projects list
		activity.fetchProjectsList();
		
		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);
		assertEquals("No projects exist", projectListView.getChildCount(), 0);
		
		// show failure dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Server Discovery Failure Dialog Showing",choiceDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.locate_server_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.locate_server_failure_message), choiceDialog.getMessage());
	}
	
	@Test
	public void testServerDiscoveryRetry() throws Exception {
		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null);
		
		// set invalid host
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(false);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		int count = 10;
		client.setProjectsCount(count);
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		
		// fetch projects list
		activity.fetchProjectsList();
		
		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);
		assertEquals("No projects exist", projectListView.getChildCount(), 0);
		
		// show failure dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Server Discovery Failure Dialog Showing",choiceDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.locate_server_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.locate_server_failure_message), choiceDialog.getMessage());
		
		// set valid host
		discovery.setHostValid(true);
		
		// TODO how to click yes on the dialog?
		activity.fetchProjectsList();
		
		// show projects list
		for (int i = 0; i < count; i++) {
			assertEquals("Project " + i + " exists", "Project " + i,
					projectListView.getItemAtPosition(i));
		}
	}
	
	@Test
	public void testFetchProjectsListFailure() throws Exception {

		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null);
		
		// set valid host
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);

		// set fetch failure
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		int count = 10;
		client.setProjectsCount(count);
		client.setProjectsResultCode(FAIMSClientResultCode.FAILURE);
		
		// fetch projects list
		activity.fetchProjectsList();
		
		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);
		assertEquals("No projects exist", projectListView.getChildCount(), 0);		
		
		// show fetch failure dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Could not fetch projects Dialog Showing",choiceDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.fetch_projects_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.fetch_projects_failure_message), choiceDialog.getMessage());
	}
	
	@Test
	public void testFetchProjectsListRetry() throws Exception {

		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null);
		
		// set valid host
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);

		// set fetch failure
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		int count = 10;
		client.setProjectsCount(count);
		client.setProjectsResultCode(FAIMSClientResultCode.FAILURE);
		
		// fetch projects list
		activity.fetchProjectsList();
		
		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);
		assertEquals("No projects exist", projectListView.getChildCount(), 0);		
		
		// show fetch failure dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Could not fetch projects Dialog Showing",choiceDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.fetch_projects_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.fetch_projects_failure_message), choiceDialog.getMessage());
		
		// set fetch success
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		
		// TODO how to click yes on the dialog?
		activity.fetchProjectsList();
		
		// show projects list
		for (int i = 0; i < count; i++) {
			assertEquals("Project " + i + " exists", "Project " + i,
					projectListView.getItemAtPosition(i));
		}
	}

	@Test
	public void showDownloadProjectDialog() {
		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null); // this automatically fetches projects
		
		// reset the server discovery
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		// update test client
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setProjectsCount(1);
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		
		// fetch project list
		activity.fetchProjectsList();
		
		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);

		assertEquals("Project exists", "Project 0",
				projectListView.getItemAtPosition(0));
		
		// select project
		projectListView.performItemClick(null, 0, 0);
		
		// show download project dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Download project dialog", choiceDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.confirm_download_project_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.confirm_download_project_message) + " Project 0?", choiceDialog.getMessage());
	}
	
	@Test
	public void testDownloadProjectSuccess() throws Exception {
		FetchProjectsActivity activity = new FetchProjectsActivity();
		
		activity.onCreate(null); // this automatically fetches projects
		
		// reset the server discovery
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		// update test client
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setProjectsCount(1);
		client.setProjectsResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.SUCCESS, null);
		
		// fetch project list
		activity.fetchProjectsList();
		
		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);

		assertEquals("Project exists", "Project 0",
				projectListView.getItemAtPosition(0));
		
		// select project
		projectListView.performItemClick(null, 0, 0);
		
		// show download project dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Download project dialog", choiceDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.confirm_download_project_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.confirm_download_project_message) + " Project 0?", choiceDialog.getMessage());
		
		// download project
		activity.downloadProjectArchive();
		
		// download service
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service launched ok", DownloadProjectService.class.getName().toString(),shadowIntent.getComponent().getClassName());
		
		// start service
		TestDownloadProjectService downloadService = new TestDownloadProjectService();
		downloadService.onCreate();
		downloadService.setFaimsClient(activity.faimsClient);	// manual roboguice injection

		downloadService.onHandleIntent(startedIntent);
		
		// assert file has been downloaded
		List<Project> projects = ProjectUtil.getProjects();
		assertEquals("Project downloaded", "Project 0", projects.get(0).name);
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
		client.setDownloadResultCode(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.SERVER_ERROR);
		
		activity.fetchProjectsList();
		
		activity.selectedProject = activity.projects.get(0);
		
		activity.downloadProjectArchive();
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service started", DownloadProjectService.class.getName().toString(), shadowIntent.getComponent().getClassName());
		
		// TODO assert no project has been downloaded
		
		Message msg = new Message();
		msg.obj = DownloadResult.FAILURE;
		activity.handler.handleMessage(msg);
		
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Could not download project Dialog Showing",choiceDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.download_project_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.download_project_failure_message), choiceDialog.getMessage());

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
		client.setDownloadResultCode(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.DOWNLOAD_CORRUPTED_ERROR);
		
		activity.fetchProjectsList();
		
		// TODO pick project form dialog
		activity.selectedProject = activity.projects.get(0);
		
		activity.downloadProjectArchive();
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service started", DownloadProjectService.class.getName().toString(), shadowIntent.getComponent().getClassName());
		
		// TODO assert no project has been downloaded
		
		Message msg = new Message();
		msg.obj = new DownloadResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.DOWNLOAD_CORRUPTED_ERROR);
		activity.handler.handleMessage(msg);
		
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Could not download project Dialog Showing",choiceDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.download_project_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.download_project_failure_message), choiceDialog.getMessage());
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
		client.setDownloadResultCode(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.STORAGE_LIMIT_ERROR);
		
		activity.fetchProjectsList();
		
		// TODO pick project form dialog
		activity.selectedProject = activity.projects.get(0);
		
		activity.downloadProjectArchive();
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service started", DownloadProjectService.class.getName().toString(), shadowIntent.getComponent().getClassName());
		
		// TODO assert no project has been downloaded
		
		Message msg = new Message();
		msg.obj = new DownloadResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.STORAGE_LIMIT_ERROR);
		activity.handler.handleMessage(msg);
		
		ShadowAlertDialog confirmDialog = Robolectric.shadowOf(activity.confirmDialog);
		
		assertTrue("Project is too big Dialog Showing",confirmDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.download_project_error_title), confirmDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.download_project_error_message), confirmDialog.getMessage());
		
	}

}
