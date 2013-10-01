package au.org.intersect.faims.android.ui.activity;


public class FAIMSLogicTestBase {

	protected String getNewModuleName(){
		
		return getNewModuleName("TestModule");
		
	}
	
	protected String getNewModuleName(String baseName){
		
		return baseName + "-" + System.currentTimeMillis();
		
	}
	
}
