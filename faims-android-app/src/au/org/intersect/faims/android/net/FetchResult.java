package au.org.intersect.faims.android.net;


public class FetchResult extends Result {
	
	public static final FetchResult SUCCESS = new FetchResult(FAIMSClientResultCode.SUCCESS);
	public static final FetchResult INTERRUPTED = new FetchResult(FAIMSClientResultCode.INTERRUPTED);
	public static final FetchResult FAILURE = new FetchResult(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.SERVER_ERROR);

	public Object data;
	
	public FetchResult(FAIMSClientResultCode resultCode) {
		super(resultCode);
	}
	
	public FetchResult(FAIMSClientResultCode resultCode, FAIMSClientErrorCode errorCode) {
		super(resultCode, errorCode);
	}
	
	public FetchResult(FAIMSClientResultCode resultCode, FAIMSClientErrorCode errorCode, Object data) {
		super(resultCode, errorCode);
		this.data = data;
	}
	
}
