package au.org.intersect.faims.android.roboguice;

import au.org.intersect.faims.android.projects.IProjectUtils;
import au.org.intersect.faims.android.projects.ProjectUtilsImpl;

import com.google.inject.Binder;
import com.google.inject.Module;

public class FAIMSModule implements Module {

	@Override
	public void configure(Binder binder) {
		
		binder.bind(IProjectUtils.class).to(ProjectUtilsImpl.class);
		
	}

}
