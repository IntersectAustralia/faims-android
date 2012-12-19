package au.org.intersect.faims.android.ui.activity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.widget.ListView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.roblectric.FAIMSRobolectricTestRunner;
import au.org.intersect.faims.android.roboguice.TestFAIMSModule;
import au.org.intersect.faims.android.test.helper.TestFAIMSClient;
import au.org.intersect.faims.android.test.helper.TestServerDiscovery;

import com.google.inject.Binder;

@RunWith(FAIMSRobolectricTestRunner.class)
public class FetchProjectsActivityTest {

	@Test
	public void testServerDiscoveryFailure() throws Exception {
		TestFAIMSModule module = new TestFAIMSModule() {

			@Override
			public void configure(Binder binder) {
				binder.bind(ServerDiscovery.class).toProvider(TestServerDiscovery.createProvider(false));
				binder.bind(FAIMSClient.class).to(TestFAIMSClient.class);
			}

		};
		TestFAIMSModule.setUp(this, module);

		FetchProjectsActivity activity = new FetchProjectsActivity();

		activity.onCreate(null);

		Thread.sleep(100);

		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);
		assertEquals("Project List Count", projectListView.getChildCount(), 0);

		assertEquals("Server Discovery Failure Dialog Showing",
				activity.choiceDialog.isShowing(), true);
		
		// TODO assert dialog title
		
		// TODO check how to test user interactions
		
		TestFAIMSModule.tearDown();

	}
	
	@Test
	public void testFetchProjectList() throws Exception {
		final int projectsCount = 10;
		
		TestFAIMSModule module = new TestFAIMSModule() {

			@Override
			public void configure(Binder binder) {
				binder.bind(ServerDiscovery.class).toProvider(TestServerDiscovery.createProvider(true));
				binder.bind(FAIMSClient.class).toProvider(TestFAIMSClient.createProvider(projectsCount, 
						FAIMSClientResultCode.SUCCESS,
						FAIMSClientResultCode.SUCCESS));
			}
		};
		TestFAIMSModule.setUp(this, module);

		FetchProjectsActivity activity = new FetchProjectsActivity();

		activity.onCreate(null);

		Thread.sleep(100);

		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);

		for (int i = 0; i < projectsCount; i++) {
			assertEquals("Project List Item " + i, "Project " + i,
					projectListView.getItemAtPosition(i));
		}

		TestFAIMSModule.tearDown();

	}
	
	@Test
	public void testFetchProjectsListFailure() throws Exception {
		
		TestFAIMSModule module = new TestFAIMSModule() {

			@Override
			public void configure(Binder binder) {
				binder.bind(ServerDiscovery.class).toProvider(TestServerDiscovery.createProvider(true));
				binder.bind(FAIMSClient.class).toProvider(TestFAIMSClient.createProvider(0, 
						FAIMSClientResultCode.SERVER_FAILURE,
						FAIMSClientResultCode.SUCCESS));
			}

		};
		TestFAIMSModule.setUp(this, module);

		FetchProjectsActivity activity = new FetchProjectsActivity();

		activity.onCreate(null);

		Thread.sleep(100);

		ListView projectListView = (ListView) activity
				.findViewById(R.id.project_list);
		assertEquals("Project List Count", projectListView.getChildCount(), 0);

		assertEquals("Fetch Projects Failure Dialog Showing",
				activity.choiceDialog.isShowing(), true);
		
		// TODO assert dialog title
		
		// TODO check how to test user interactions
		
		TestFAIMSModule.tearDown();

	}
	
	@Test
	public void testDownloadProjectSuccess() throws Exception {
		final int projectsCount = 10;
		
		TestFAIMSModule module = new TestFAIMSModule() {

			@Override
			public void configure(Binder binder) {
				binder.bind(ServerDiscovery.class).toProvider(TestServerDiscovery.createProvider(true));
				binder.bind(FAIMSClient.class).toProvider(TestFAIMSClient.createProvider(projectsCount, 
						FAIMSClientResultCode.SUCCESS,
						FAIMSClientResultCode.SUCCESS));
			}

		};
		TestFAIMSModule.setUp(this, module);

		FetchProjectsActivity activity = new FetchProjectsActivity();

		activity.onCreate(null);
		
		Thread.sleep(100);
		
		activity.setSelectedProject(0);
		
		activity.downloadProjectArchive();

		// TODO assert file has been downloaded
		
		TestFAIMSModule.tearDown();

	}
	
	@Test
	public void testDownloadProjectServerFailure() throws Exception {
		final int projectsCount = 10;
		
		TestFAIMSModule module = new TestFAIMSModule() {

			@Override
			public void configure(Binder binder) {
				binder.bind(ServerDiscovery.class).toProvider(TestServerDiscovery.createProvider(true));
				binder.bind(FAIMSClient.class).toProvider(TestFAIMSClient.createProvider(projectsCount, 
						FAIMSClientResultCode.SUCCESS,
						FAIMSClientResultCode.SERVER_FAILURE));
			}

		};
		TestFAIMSModule.setUp(this, module);

		FetchProjectsActivity activity = new FetchProjectsActivity();

		activity.onCreate(null);
		
		Thread.sleep(100);
		
		activity.setSelectedProject(0);
		
		activity.downloadProjectArchive();

		assertEquals("Download Projects Failure Dialog Showing",
				activity.choiceDialog.isShowing(), true);
		
		TestFAIMSModule.tearDown();

	}
	
	@Test
	public void testDownloadProjectCorrupted() throws Exception {
		final int projectsCount = 10;
		
		TestFAIMSModule module = new TestFAIMSModule() {

			@Override
			public void configure(Binder binder) {
				binder.bind(ServerDiscovery.class).toProvider(TestServerDiscovery.createProvider(true));
				binder.bind(FAIMSClient.class).toProvider(TestFAIMSClient.createProvider(projectsCount, 
						FAIMSClientResultCode.SUCCESS,
						FAIMSClientResultCode.DOWNLOAD_CORRUPTED));
			}

		};
		TestFAIMSModule.setUp(this, module);

		FetchProjectsActivity activity = new FetchProjectsActivity();

		activity.onCreate(null);
		
		Thread.sleep(100);
		
		activity.setSelectedProject(0);
		
		activity.downloadProjectArchive();

		assertEquals("Download Projects Failure Dialog Showing",
				activity.choiceDialog.isShowing(), true);
		
		TestFAIMSModule.tearDown();

	}
	
	@Test
	public void testDownloadProjectTooBig() throws Exception {
		final int projectsCount = 10;
		
		TestFAIMSModule module = new TestFAIMSModule() {

			@Override
			public void configure(Binder binder) {
				binder.bind(ServerDiscovery.class).toProvider(TestServerDiscovery.createProvider(true));
				binder.bind(FAIMSClient.class).toProvider(TestFAIMSClient.createProvider(projectsCount, 
						FAIMSClientResultCode.SUCCESS,
						FAIMSClientResultCode.STORAGE_LIMIT_ERROR));
			}

		};
		TestFAIMSModule.setUp(this, module);

		FetchProjectsActivity activity = new FetchProjectsActivity();

		activity.onCreate(null);
		
		Thread.sleep(100);
		
		activity.setSelectedProject(0);
		
		activity.downloadProjectArchive();

		assertEquals("Download Projects Error Dialog Showing",
				activity.confirmDialog.isShowing(), true);
		
		TestFAIMSModule.tearDown();

	}

}
