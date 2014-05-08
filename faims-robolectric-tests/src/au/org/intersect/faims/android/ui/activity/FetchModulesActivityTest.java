package au.org.intersect.faims.android.ui.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.tester.android.view.TestMenu;
import org.robolectric.tester.android.view.TestMenuItem;

import android.content.Intent;
import android.os.Message;
import android.view.MenuInflater;
import android.widget.ListView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.TestFAIMSClient;
import au.org.intersect.faims.android.net.TestServerDiscovery;
import au.org.intersect.faims.android.roboguice.TestFAIMSModule;
import au.org.intersect.faims.android.services.DownloadModuleService;
import au.org.intersect.faims.android.services.TestDownloadModuleService;
import au.org.intersect.faims.android.util.ModuleUtil;

@RunWith(RobolectricTestRunner.class)
public class FetchModulesActivityTest {
	
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
	public void testFetchModulesList() throws Exception {

		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();
		
		// reset the server discovery
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		// update test client
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
	
		int count = 10;
		client.setModulesCount(count);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		activity.fetchModulesList();
		
		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);

		for (int i = 0; i < count; i++) {
			assertEquals("Module " + i + " exists", "Module " + i,
					moduleListView.getItemAtPosition(count - i - 1));
		}
	}

	@Test
	public void refreshListMenuItemTest(){
		
		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();
		
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
	
		int count = 10;
		client.setModulesCount(count);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		activity.fetchModulesList();
		
		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);

		for (int i = 0; i < count; i++) {
			assertEquals("Module " + i + " exists", "Module " + i,
					moduleListView.getItemAtPosition(count - i - 1));
		}
		
		// update count and refresh list
		count += 5;
		client.setModulesCount(count);
		
		String itemTitle = "Refresh Module List";
		
		TestMenu mainMenu = new TestMenu(activity); 
		new MenuInflater(activity).inflate(R.menu.acitvity_fetch_modules, mainMenu); 
		TestMenuItem item = mainMenu.findMenuItem(itemTitle); 
		
		assertEquals("Menu item exists", itemTitle, item.getTitle().toString());
		
		activity.onOptionsItemSelected(item);
		
		for (int i = 0; i < count; i++) {
			assertEquals("Refreshed Module " + i + " exists", "Module " + i,
					moduleListView.getItemAtPosition(count - i - 1));
		}
	}
	
	@Test
	public void testServerDiscoveryFailure() throws Exception {
		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();
		
		// set invalid host
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(false);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		int count = 10;
		client.setModulesCount(count);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		// fetch modules list
		activity.fetchModulesList();
		
		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);
		assertEquals("No modules exist", moduleListView.getChildCount(), 0);
		
		// show failure dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Server Discovery Failure Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.locate_server_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.locate_server_failure_message), choiceDialog.getMessage());
	}
	
	@Test
	public void testServerDiscoveryRetry() throws Exception {
		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();
		
		// set invalid host
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(false);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		int count = 10;
		client.setModulesCount(count);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		// fetch modules list
		activity.fetchModulesList();
		
		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);
		assertEquals("No modules exist", moduleListView.getChildCount(), 0);
		
		// show failure dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Server Discovery Failure Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.locate_server_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.locate_server_failure_message), choiceDialog.getMessage());
		
		// set valid host
		discovery.setHostValid(true);
		
		// TODO how to click yes on the dialog?
		activity.fetchModulesList();
		
		// show modules list
		for (int i = 0; i < count; i++) {
			assertEquals("Module " + i + " exists", "Module " + i,
					moduleListView.getItemAtPosition(count - i - 1));
		}
	}
	
	@Test
	public void testFetchModulesListFailure() throws Exception {

		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();
		
		// set valid host
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);

		// set fetch failure
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		int count = 10;
		client.setModulesCount(count);
		client.setModulesResultCode(FAIMSClientResultCode.FAILURE);
		
		// fetch modules list
		activity.fetchModulesList();
		
		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);
		assertEquals("No modules exist", moduleListView.getChildCount(), 0);		
		
		// show fetch failure dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Could not fetch modules Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.fetch_modules_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.fetch_modules_failure_message), choiceDialog.getMessage());
	}
	
	@Test
	public void testFetchModulesListRetry() throws Exception {

		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();
		
		// set valid host
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);

		// set fetch failure
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		int count = 10;
		client.setModulesCount(count);
		client.setModulesResultCode(FAIMSClientResultCode.FAILURE);
		
		// fetch modules list
		activity.fetchModulesList();
		
		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);
		assertEquals("No modules exist", moduleListView.getChildCount(), 0);		
		
		// show fetch failure dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Could not fetch modules Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.fetch_modules_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.fetch_modules_failure_message), choiceDialog.getMessage());
		
		// set fetch success
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		// TODO how to click yes on the dialog?
		activity.fetchModulesList();
		
		// show modules list
		for (int i = 0; i < count; i++) {
			assertEquals("Module " + i + " exists", "Module " + i,
					moduleListView.getItemAtPosition(count - i - 1));
		}
	}

	@Test
	public void showDownloadModuleDialog() {
		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();
		
		// reset the server discovery
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		// update test client
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setModulesCount(1);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		// fetch module list
		activity.fetchModulesList();
		
		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);

		assertEquals("Module exists", "Module 0",
				moduleListView.getItemAtPosition(0));
		
		// select module
		moduleListView.performItemClick(null, 0, 0);
		
		// show download module dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Download module dialog", ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.confirm_download_module_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.confirm_download_module_message) + " Module 0?", choiceDialog.getMessage());
	}
	
	@Test
	public void testDownloadModuleSuccess() throws Exception {
		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();
		
		// reset the server discovery
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		// update test client
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setModulesCount(1);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.SUCCESS, null);
		
		// fetch module list
		activity.fetchModulesList();
		
		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);

		assertEquals("Module exists", "Module 0",
				moduleListView.getItemAtPosition(0));
		
		// select module
		moduleListView.performItemClick(null, 0, 0);
		
		// show download module dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Download module dialog", ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.confirm_download_module_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.confirm_download_module_message) + " Module 0?", choiceDialog.getMessage());
		
		// download module
		activity.downloadModuleArchive();
		
		// download service
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service launched ok", DownloadModuleService.class.getName().toString(),shadowIntent.getComponent().getClassName());
		
		// start service
		TestDownloadModuleService downloadService = new TestDownloadModuleService();
		downloadService.onCreate();
		downloadService.setFaimsClient(activity.faimsClient);	// manual roboguice injection

		downloadService.onHandleIntent(startedIntent);
		
		// assert file has been downloaded
		List<Module> modules = ModuleUtil.getModules();
		assertEquals("Module downloaded", "Module 0", modules.get(0).name);
	}
	
	@Test
	public void testDownloadModuleServerFailure() throws Exception {
		
		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();
		
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);

		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setModulesCount(10);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.SERVER_ERROR);
		
		activity.fetchModulesList();
		
		activity.selectedModule = activity.modules.get(0);
		
		activity.downloadModuleArchive();
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service started", DownloadModuleService.class.getName().toString(), shadowIntent.getComponent().getClassName());
		
		// TODO assert no module has been downloaded
		
		Message msg = new Message();
		msg.obj = DownloadResult.FAILURE;
		activity.handler.handleMessage(msg);
		
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Could not download module Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.download_module_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.download_module_failure_message), choiceDialog.getMessage());

	}
	
	@Test
	public void testDownloadModuleCorrupted() throws Exception {
		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();

		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setModulesCount(10);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.DOWNLOAD_CORRUPTED_ERROR);
		
		activity.fetchModulesList();
		
		// TODO pick module form dialog
		activity.selectedModule = activity.modules.get(0);
		
		activity.downloadModuleArchive();
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service started", DownloadModuleService.class.getName().toString(), shadowIntent.getComponent().getClassName());
		
		// TODO assert no module has been downloaded
		
		Message msg = new Message();
		msg.obj = new DownloadResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.DOWNLOAD_CORRUPTED_ERROR);
		activity.handler.handleMessage(msg);
		
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Could not download module Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.download_module_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.download_module_failure_message), choiceDialog.getMessage());
	}
	
	@Test
	public void testDownloadModuleTooBig() throws Exception {
		FetchModulesActivity activity = Robolectric.buildActivity(FetchModulesActivity.class).create().get();

		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setModulesCount(10);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.STORAGE_LIMIT_ERROR);
		
		activity.fetchModulesList();
		
		// TODO pick module form dialog
		activity.selectedModule = activity.modules.get(0);
		
		activity.downloadModuleArchive();
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service started", DownloadModuleService.class.getName().toString(), shadowIntent.getComponent().getClassName());
		
		// TODO assert no module has been downloaded
		
		Message msg = new Message();
		msg.obj = new DownloadResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.STORAGE_LIMIT_ERROR);
		activity.handler.handleMessage(msg);
		
		ShadowAlertDialog confirmDialog = Robolectric.shadowOf(activity.confirmDialog);
		
		assertTrue("Module is too big Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.download_module_error_title), confirmDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.download_module_error_message), confirmDialog.getMessage());
		
	}

}
