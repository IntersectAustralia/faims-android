package au.org.intersect.faims.android.ui.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowAlertDialog;

import android.content.Intent;
import android.widget.Button;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.roblectric.FAIMSRobolectricTestRunner;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.DialogType;

@RunWith(FAIMSRobolectricTestRunner.class)
public class ShowProjectActivityFAIMS114Test extends FAIMSLogicTestBase {

	private String projectBaseName = "FAIMS114";
	private String directoryName = "FAIMS114_Tests";
	
	@Test
	public void showProjectTest(){
/**		
		String projectName = getNewProjectName(projectBaseName);
				
		ShowProjectActivity activity = new ShowProjectActivity();
		
		// We need name + directory in an Intent
		
		Intent intent = new Intent();
		intent.putExtra("name", projectName);
		intent.putExtra("directory", "/faims/projects/" + projectName);
		activity.setIntent(intent);
		
		// We need the UI xml and logic bsh files
		
		createTestData(projectName, this.directoryName);
		
		// Create the activity
		
		activity.onCreate(null);
		
		// There should now be a dialog asking if we want to render the project
		
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Server Discovery Failure Dialog Showing",choiceDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.render_project_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.render_project_message), choiceDialog.getMessage());
		
		// We now tell the activity to render the project UI
		
		activity.handleDialogResponse(DialogResultCode.SELECT_YES, null, DialogType.CONFIRM_RENDER_PROJECT, null);
		
		// We should have Field1, Field2 and a copy button
		
		String field1Ref = "/faims/tabgroup1/tab1/field1";
		String field2Ref = "/faims/tabgroup1/tab1/field2";
		String buttonRef = "/faims/tabgroup1/tab1/copy1";
		
		String expectedValueF1 = "This is a test string";
		String expectedValueF2 = "This is a different test string";
		
		// Test we can set Field 1 via BeanShell
		
		activity.getBeanShellLinker().setFieldValue(field1Ref, expectedValueF1);
		
		String actualValue = (String) activity.getBeanShellLinker().getFieldValue(field1Ref);
		assertEquals("Set + Get field One", expectedValueF1, actualValue);
		
		// Test we can set Field 2 via BeanShell
		
		activity.getBeanShellLinker().setFieldValue(field2Ref, expectedValueF2);
		
		actualValue = (String) activity.getBeanShellLinker().getFieldValue(field2Ref);
		assertEquals("Set + Get field Two", expectedValueF2, actualValue);
		
		// Test we can set Field Two via the button
		
		Button button = (Button) activity.getBeanShellLinker().getUIRenderer().getViewByRef(buttonRef);
		button.performClick();
		
		actualValue = (String) activity.getBeanShellLinker().getFieldValue(field2Ref);
		assertEquals("Set field Two using button", expectedValueF1, actualValue);

**/
	}

}
