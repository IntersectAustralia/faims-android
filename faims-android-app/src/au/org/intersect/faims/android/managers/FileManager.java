package au.org.intersect.faims.android.managers;

import java.io.File;

public class FileManager {

	private File selectedFile;
	
	public FileManager(){
		
	}

	public File getSelectedFile() {
		return selectedFile;
	}

	public void setSelectedFile(File selectedFile) {
		this.selectedFile = selectedFile;
	}

	public interface FileSelectionListener{
		public void onFileChangesListener(boolean isSpatialFile);
	}
}
