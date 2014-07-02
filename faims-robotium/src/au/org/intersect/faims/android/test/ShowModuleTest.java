package au.org.intersect.faims.android.test;

import android.content.Intent;
import android.content.res.AssetManager;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.CheckedTextView;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.view.CustomCheckBox;
import au.org.intersect.faims.android.ui.view.CustomImageView;
import au.org.intersect.faims.android.ui.view.CustomRadioButton;
import au.org.intersect.faims.android.ui.view.HierarchicalSpinner;
import au.org.intersect.faims.android.util.TestModuleUtil;

import com.robotium.solo.Solo;

public class ShowModuleTest extends ActivityInstrumentationTestCase2<ShowModuleActivity>
{
	private Solo solo;

	public ShowModuleTest() {
		 super(ShowModuleActivity.class);
	}
	
	public void setUp() throws Exception {
		super.setUp();
		
		//TODO: Extract module generation into global setup task
		AssetManager assetManager = getInstrumentation().getContext().getAssets();
		String name = TestModuleUtil.getNewModuleName("Sync Example");
		String moduleKey = "sync";
		TestModuleUtil.createModuleFrom(name, moduleKey, "Sync Example", assetManager);
		
		Intent i = new Intent();
		i.putExtra("key", "sync");
		setActivityIntent(i);
		solo = new Solo(getInstrumentation(), getActivity());
	}
	
	public void testDropdownClickEvent() throws Exception {
		solo.assertCurrentActivity("wrong activity", ShowModuleActivity.class);
		solo.waitForDialogToClose();
		// Check click event doesn't fire on tab load
		assertFalse(solo.searchText("Dropdown select event"));
		
		solo.clickOnText("Faims Admin");
		solo.clickOnText("Create Entity");
		solo.clickOnText("Load Entity");
		solo.clickOnView(solo.getView(HierarchicalSpinner.class, 0));
		solo.clickOnView(solo.getView(CheckedTextView.class, 1));
		
		assertTrue(solo.searchText("Dropdown select event"));
	}
	
	public void testHierarchicalDropdownClickEvent() throws Exception {
		solo.assertCurrentActivity("wrong activity", ShowModuleActivity.class);
		solo.waitForDialogToClose();
		// Check click event doesn't fire on tab load
		assertFalse(solo.searchText("Hierarchical dropdown select event"));
		
		solo.clickOnText("Faims Admin");
		solo.clickOnText("Create Entity");
		solo.clickOnView(solo.getView(HierarchicalSpinner.class, 0));
		solo.clickOnView(solo.getView(CheckedTextView.class, 2));
		solo.clickOnView(solo.getView(CheckedTextView.class, 2));
		
		assertTrue(solo.searchText("Hierarchical dropdown select event"));
	}
	
	public void testPictureGalleryClickEvent() throws Exception {
		solo.assertCurrentActivity("wrong activity", ShowModuleActivity.class);
		solo.waitForDialogToClose();
		// Check click event doesn't fire on tab load
		assertFalse(solo.searchText("Gallery select event"));
		
		solo.clickOnText("Faims Admin");
		solo.clickOnText("Create Entity");
		solo.clickOnView(solo.getView(CustomImageView.class, 1));
		
		assertTrue(solo.searchText("Gallery select event"));
	}
	
	public void testHierarchicalPictureGalleryClickEvent() throws Exception {
		solo.assertCurrentActivity("wrong activity", ShowModuleActivity.class);
		solo.waitForDialogToClose();
		// Check click event doesn't fire on tab load
		assertFalse(solo.searchText("Hierarchical gallery select event"));
		
		solo.clickOnText("Faims Admin");
		solo.clickOnText("Create Entity");
		solo.clickOnView(solo.getView(CustomImageView.class, 21));
		
		assertTrue(solo.searchText("Hierarchical gallery select event"));
	}
	
	public void testCheckboxClickEvent() throws Exception {
		solo.assertCurrentActivity("wrong activity", ShowModuleActivity.class);
		solo.waitForDialogToClose();
		
		solo.clickOnText("Faims Admin");
		solo.clickOnText("Create Entity");
		solo.clickOnView(solo.getView(CustomCheckBox.class, 2));
		
		assertTrue(solo.searchText("Checkbox click event"));
	}
	
	public void testRadioGroupClickEvent() throws Exception {
		solo.assertCurrentActivity("wrong activity", ShowModuleActivity.class);
		solo.waitForDialogToClose();
		
		solo.clickOnText("Faims Admin");
		solo.clickOnText("Create Entity");
		solo.clickOnView(solo.getView(CustomRadioButton.class, 2));
		
		assertTrue(solo.searchText("Radiogroup select event"));
	}
	  
	@Override
	public void tearDown() throws Exception {
		solo.finishOpenedActivities();
	}
}
