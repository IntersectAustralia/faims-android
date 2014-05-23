package au.org.intersect.faims.android.net;

import au.org.intersect.faims.android.data.Module;

public class Request {
	
	public static final String PROJECT_LIST = "/android/modules";
	
	public static String SETTINGS_INFO_REQUEST(Module module) {
		return "/android/module/" + module.key + "/settings_info";
	}
	
	public static String SETTINGS_DOWNLOAD_REQUEST(Module module) {
		return "/android/module/" + module.key + "/settings_download?";
	}
	
	public static String DATABASE_INFO_REQUEST(Module module) {
		return "/android/module/" + module.key + "/db_info";
	}
	
	public static String DATABASE_DOWNLOAD_REQUEST(Module module) {
		return "/android/module/" + module.key + "/db_download?";
	}
	
	public static String DATABASE_INFO_REQUEST(Module module, int version) {
		return "/android/module/" + module.key + "/db_info?version=" + version;
	}
	
	public static String DATABASE_DOWNLOAD_REQUEST(Module module, int version) {
		return "/android/module/" + module.key + "/db_download?version=" + version;
	}
	
	public static String DATABASE_UPLOAD_REQUEST(Module module) {
		return "/android/module/" + module.key + "/db_upload";
	}
	
	public static String DATA_FILES_INFO_REQUEST(Module module) {
		return "/android/module/" + module.key + "/data_files_info";
	}
	
	public static String DATA_FILE_DOWNLOAD_REQUEST(Module module) {
		return "/android/module/" + module.key + "/data_file_download?";
	}
	
	public static String APP_FILES_INFO_REQUEST(Module module) {
		return "/android/module/" + module.key + "/app_files_info";
	}
	
	public static String APP_FILE_DOWNLOAD_REQUEST(Module module) {
		return "/android/module/" + module.key + "/app_file_download?";
	}
	
	public static String APP_FILE_UPLOAD_REQUEST(Module module) {
		return "/android/module/" + module.key + "/app_file_upload";
	}
	
	public static String SERVER_FILES_INFO_REQUEST(Module module) {
		return "/android/module/" + module.key + "/server_files_info";
	}
	
	public static String SERVER_FILE_UPLOAD_REQUEST(Module module) {
		return "/android/module/" + module.key + "/server_file_upload?";
	}
}
