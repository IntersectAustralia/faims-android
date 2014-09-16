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
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowIntent;

import android.content.Intent;
import android.os.Message;
import android.widget.FrameLayout;
import android.widget.ListView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.net.TestFAIMSClient;
import au.org.intersect.faims.android.net.TestServerDiscovery;
import au.org.intersect.faims.android.roboguice.TestFAIMSModule;
import au.org.intersect.faims.android.services.DownloadModuleService;
import au.org.intersect.faims.android.services.TestDownloadModuleService;
import au.org.intersect.faims.android.ui.activity.MainActivity.ModuleItem;
import au.org.intersect.faims.android.util.ModuleUtil;
import au.org.intersect.faims.android.util.TestModuleUtil;

@Config(manifest="../faims-android-app/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {
	
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
	public void readStoredModulesList() throws Exception {
		
		int count = 10;
		for (int i = 0; i < count; i++) {
			TestModuleUtil.createModule("Module " + i, "key" + i);
		}
		
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();

		ListView moduleListView = (ListView) activity.findViewById(R.id.module_list);
		
		for (int i = 0; i < count; i++) {
			assertEquals("Module List Item " + i, "Module " + i, ((ModuleItem)moduleListView.getItemAtPosition(i)).getName());
		}
	}
	
	@Test
	public void loadStoredModuleTest() {
		String moduleName = "Test Module";
		String moduleKey = "123456789";
		
		TestModuleUtil.createModule(moduleName, moduleKey);
		
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
		
		ListView moduleListView = (ListView) activity.findViewById(R.id.module_list);
		
		assertEquals("Module List Item ", moduleName, ((ModuleItem)moduleListView.getItemAtPosition(0)).getName());
		
		FrameLayout listItem = (FrameLayout) ((ModuleListAdapter)moduleListView.getAdapter()).getView(0, null, moduleListView);
		listItem.getChildAt(0).performClick(); // click list item
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedActivity();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);

		assertEquals("New Activity launched ok", ShowModuleActivity.class.getName().toString(),shadowIntent.getComponent().getClassName());
		
		assertEquals("Show module key", moduleKey, startedIntent.getStringExtra("key"));
	}
	
	@Test
	public void testFetchModulesList() throws Exception {

		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
		
		// reset the server discovery
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.mockSharedPreferences(activity);
		discovery.setHostValid(true);
		
		// update test client
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
	
		int count = 10;
		client.setModulesCount(count);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		activity.readModules();
		
		ListView moduleListView = (ListView) activity.findViewById(R.id.module_list);

		for (int i = 0; i < count; i++) {
			assertEquals("Module " + i + " exists", "Module " + i,
					((ModuleItem)moduleListView.getItemAtPosition(i)).getName());
		}
	}

	@Test
	public void refreshListMenuItemTest(){
		
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
		
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.mockSharedPreferences(activity);
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
	
		int count = 10;
		client.setModulesCount(count);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		activity.readModules();
		
		ListView moduleListView = (ListView) activity
				.findViewById(R.id.module_list);

		for (int i = 0; i < count; i++) {
			assertEquals("Module " + i + " exists", "Module " + i,
					((ModuleItem)moduleListView.getItemAtPosition(i)).getName());
		}
		
		// update count and refresh list
		count += 5;
		client.setModulesCount(count);
		
		activity.readModules();
		
		for (int i = 0; i < count; i++) {
			assertEquals("Refreshed module " + i + " exists", "Module " + i,
					((ModuleItem)moduleListView.getItemAtPosition(i)).getName());
		}
	}
	
	@Test
	public void testServerDiscoveryFailure() throws Exception {
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
		
		// set invalid host
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.mockSharedPreferences(activity);
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		int count = 10;
		client.setModulesCount(count);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		// fetch modules list
		activity.readModules();
		
		discovery.setHostValid(false);
		
		ListView moduleListView = (ListView) activity.findViewById(R.id.module_list);
		FrameLayout listItem = (FrameLayout) ((ModuleListAdapter)moduleListView.getAdapter()).getView(0, null, moduleListView);
		listItem.getChildAt(0).performClick(); // click list item
		
		// TODO how to click yes on the dialog?
		activity.downloadModule(true);
		
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Server Discovery Failure Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.locate_server_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.locate_server_failure_message), choiceDialog.getMessage());
	}
	
	@Test
	public void testServerDiscoveryRetry() throws Exception {
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
		
		// set invalid host
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.mockSharedPreferences(activity);
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		int count = 10;
		client.setModulesCount(count);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		// fetch modules list
		activity.readModules();
		
		discovery.setHostValid(false);
		
		ListView moduleListView = (ListView) activity.findViewById(R.id.module_list);
		FrameLayout listItem = (FrameLayout) ((ModuleListAdapter)moduleListView.getAdapter()).getView(0, null, moduleListView);
		listItem.getChildAt(0).performClick(); // click list item
		
		// TODO how to click yes on the dialog?
		activity.downloadModule(true);
		
		// show failure dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Server Discovery Failure Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.locate_server_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.locate_server_failure_message), choiceDialog.getMessage());
		
		// set valid host
		discovery.setHostValid(true);
		
		// TODO how to click yes on the dialog?
		discovery.setHostValid(true);
		activity.downloadModule(true);
		
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
	public void showDownloadModuleDialog() {
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
		
		// reset the server discovery
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.mockSharedPreferences(activity);
		discovery.setHostValid(true);
		
		// update test client
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setModulesCount(1);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		
		// fetch module list
		activity.readModules();
		
		ListView moduleListView = (ListView) activity.findViewById(R.id.module_list);

		assertEquals("Module exists", "Module 0",((ModuleItem)moduleListView.getItemAtPosition(0)).getName());
		
		// select module
		FrameLayout listItem = (FrameLayout) ((ModuleListAdapter)moduleListView.getAdapter()).getView(0, null, moduleListView);
		listItem.getChildAt(0).performClick(); // click list item
		
		// show download module dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Download module dialog", ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.confirm_download_module_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.confirm_download_module_message) + " Module 0?", choiceDialog.getMessage());
	}
	
	@Test
	public void testDownloadModuleSuccess() throws Exception {
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
		
		// reset the server discovery
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.mockSharedPreferences(activity);
		discovery.setHostValid(true);
		
		// update test client
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setModulesCount(1);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.SUCCESS, null);
		
		// fetch module list
		activity.readModules();
		
		ListView moduleListView = (ListView) activity.findViewById(R.id.module_list);

		assertEquals("Module exists", "Module 0",((ModuleItem)moduleListView.getItemAtPosition(0)).getName());
		
//		// select module
		FrameLayout listItem = (FrameLayout) ((ModuleListAdapter)moduleListView.getAdapter()).getView(0, null, moduleListView);
		listItem.getChildAt(0).performClick(); // click list item
		
		// show download module dialog
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Download module dialog", ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.confirm_download_module_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.confirm_download_module_message) + " Module 0?", choiceDialog.getMessage());
		
		// download module
		activity.downloadModule(true);
		
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
		
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
		
		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);

		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setModulesCount(10);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.SERVER_ERROR);
		
		activity.downloadModule(true);
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service started", DownloadModuleService.class.getName().toString(), shadowIntent.getComponent().getClassName());
		
		// TODO assert no module has been downloaded
		
		Message msg = new Message();
		msg.obj = Result.FAILURE;
		activity.downloadHandler.handleMessage(msg);
		
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Could not download module Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.download_module_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.download_module_failure_message), choiceDialog.getMessage());

	}
	
	@Test
	public void testDownloadModuleCorrupted() throws Exception {
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();

		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setModulesCount(10);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.DOWNLOAD_CORRUPTED_ERROR);
		
		activity.downloadModule(true);
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service started", DownloadModuleService.class.getName().toString(), shadowIntent.getComponent().getClassName());
		
		// TODO assert no module has been downloaded
		
		Message msg = new Message();
		msg.obj = new Result(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.DOWNLOAD_CORRUPTED_ERROR);
		activity.downloadHandler.handleMessage(msg);
		
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Could not download module Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.download_module_failure_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.download_module_failure_message), choiceDialog.getMessage());
	}
	
	@Test
	public void testDownloadModuleTooBig() throws Exception {
		MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();

		TestServerDiscovery discovery = (TestServerDiscovery) activity.serverDiscovery;
		discovery.setHostValid(true);
		
		TestFAIMSClient client = (TestFAIMSClient) activity.faimsClient;
		client.setModulesCount(10);
		client.setModulesResultCode(FAIMSClientResultCode.SUCCESS);
		client.setDownloadResultCode(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.STORAGE_LIMIT_ERROR);
		
		activity.downloadModule(true);
		
		ShadowActivity shadowActivity = Robolectric.shadowOf(activity);
		Intent startedIntent = shadowActivity.getNextStartedService();
		ShadowIntent shadowIntent = Robolectric.shadowOf(startedIntent);
		
		assertEquals("Download service started", DownloadModuleService.class.getName().toString(), shadowIntent.getComponent().getClassName());
		
		// TODO assert no module has been downloaded
		
		Message msg = new Message();
		msg.obj = new Result(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.STORAGE_LIMIT_ERROR);
		activity.downloadHandler.handleMessage(msg);
		
		ShadowAlertDialog confirmDialog = Robolectric.shadowOf(activity.confirmDialog);
		
		assertTrue("Module is too big Dialog Showing",ShadowAlertDialog.getLatestAlertDialog().isShowing());
		assertEquals("Dialog title", activity.getString(R.string.download_module_error_title), confirmDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.download_module_error_message), confirmDialog.getMessage());
		
	}
	
}
