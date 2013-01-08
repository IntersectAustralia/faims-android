package au.org.intersect.faims.android.ui.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.os.Environment;

public class FAIMSLogicTestBase {

	protected String getNewProjectName(){
		
		return getNewProjectName("TestProject");
		
	}
	
	protected String getNewProjectName(String baseName){
		
		return baseName + "-" + System.currentTimeMillis();
		
	}
	
	protected void createTestData(String projectName, String directoryName) {
		
		String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
		
		System.out.println("Dir= " + baseDir + "/faims/projects/" + projectName);
		
		File newDir = new File(baseDir + "/faims/projects/" + projectName);
		newDir.mkdirs();
		try{
			copyFile(new File("test_data/" + directoryName + "/data_schema.xml"), 
					new File(newDir.getAbsolutePath() + "/data_schema.xml"));
			
			copyFile(new File("test_data/" + directoryName + "/ui_schema.xml"), 
					new File(newDir.getAbsolutePath() + "/ui_schema.xml"));
			
			copyFile(new File("test_data/" + directoryName + "/ui_logic.bsh"), 
					new File(newDir.getAbsolutePath() + "/ui_logic.bsh"));
		}
		catch(Exception e){
			// continue
		}

		
	}
	
	protected void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
}
