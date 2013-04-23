package au.org.intersect.faims.android.net;

import au.org.intersect.faims.android.data.FileInfo;


public class DownloadResult extends Result {
	
	public static final DownloadResult SUCCESS = new DownloadResult(FAIMSClientResultCode.SUCCESS);
	public static final DownloadResult INTERRUPTED = new DownloadResult(FAIMSClientResultCode.INTERRUPTED);
	public static final DownloadResult FAILURE = new DownloadResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.SERVER_ERROR);
	
	public FileInfo info;
	
	public DownloadResult(FAIMSClientResultCode resultCode) {
		super(resultCode);
	}
	
	public DownloadResult(FAIMSClientResultCode resultCode, FAIMSClientErrorCode errorCode) {
		super(resultCode, errorCode);
	}
	
	public DownloadResult(FAIMSClientResultCode resultCode, FAIMSClientErrorCode errorCode, FileInfo info) {
		super(resultCode, errorCode);
		this.info = info;
	}
	
}
