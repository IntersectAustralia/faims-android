package au.org.intersect.faims.android.ui.activity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.widget.Button;
import au.org.intersect.faims.android.util.TestMethodsUtil;

@Config(manifest="../faims-android-app/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class ShowModuleActivityFAIMS114Test {

	private String moduleBaseName = "FAIMS114";
	private String directoryName = "FAIMS114_Tests";
	
	@Test
	public void showModuleTest(){

		ShowModuleActivity activity = TestMethodsUtil.showModule(moduleBaseName, directoryName); 
		
		// There should now be a dialog asking if we want to render the module
		/*
		ShadowAlertDialog choiceDialog = Robolectric.shadowOf(activity.choiceDialog);
		
		assertTrue("Server Discovery Failure Dialog Showing",choiceDialog.isShowing());
		assertEquals("Dialog title", activity.getString(R.string.render_module_title), choiceDialog.getTitle());
		assertEquals("Dialog message", activity.getString(R.string.render_module_message), choiceDialog.getMessage());
		*/
		// We now tell the activity to render the module UI
		
		// We should have Field1, Field2 and a copy button
		
		String field1Ref = "tabgroup1/tab1/field1";
		String field2Ref = "tabgroup1/tab1/field2";
		String buttonRef = "tabgroup1/tab1/copy1";
		
		String expectedValueF1 = "This is a test string";
		String expectedValueF2 = "This is a different test string";
		
		// Test we can set Field 1 via BeanShell
		
		activity.beanShellLinker.setFieldValue(field1Ref, expectedValueF1);
		
		String actualValue = (String) activity.beanShellLinker.getFieldValue(field1Ref);
		assertEquals("Set + Get field One", expectedValueF1, actualValue);
		
		// Test we can set Field 2 via BeanShell
		
		activity.beanShellLinker.setFieldValue(field2Ref, expectedValueF2);
		
		actualValue = (String) activity.beanShellLinker.getFieldValue(field2Ref);
		assertEquals("Set + Get field Two", expectedValueF2, actualValue);
		
		// Test we can set Field Two via the button
		
		Button button = (Button) activity.uiRenderer.getViewByRef(buttonRef);
		button.performClick();
		
		actualValue = (String) activity.beanShellLinker.getFieldValue(field2Ref);
		assertEquals("Set field Two using button", expectedValueF1, actualValue);

	}

}
