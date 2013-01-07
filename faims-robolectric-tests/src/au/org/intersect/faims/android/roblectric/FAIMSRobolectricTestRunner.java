package au.org.intersect.faims.android.roblectric;

import java.io.File;

import org.junit.runners.model.InitializationError;

import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;

public class FAIMSRobolectricTestRunner extends RobolectricTestRunner {

    /**
     * Call this constructor to specify the location of resources and AndroidManifest.xml.
     * 
     * @param testClass
     * @throws InitializationError
     */ 
    public FAIMSRobolectricTestRunner(@SuppressWarnings("rawtypes") Class testClass) throws InitializationError {
    	
        super(testClass, new RobolectricConfig(new File("../faims-android-app/AndroidManifest.xml"), new File("../faims-android-app/res")));
        
    }
    
}
