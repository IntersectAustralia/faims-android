package au.org.intersect.faims.android.ui.activity;


public class FAIMSLogicTestBase {

	protected String getNewProjectName(){
		
		return getNewProjectName("TestProject");
		
	}
	
	protected String getNewProjectName(String baseName){
		
		return baseName + "-" + System.currentTimeMillis();
		
	}
	
}
