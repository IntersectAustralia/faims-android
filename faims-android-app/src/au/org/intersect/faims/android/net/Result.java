package au.org.intersect.faims.android.net;


public class Result {
	
	public static final Result SUCCESS = new Result(FAIMSClientResultCode.SUCCESS);
	public static final Result INTERRUPTED = new Result(FAIMSClientResultCode.INTERRUPTED);
	public static final Result FAILURE = new Result(FAIMSClientResultCode.FAILURE, FAIMSClientErrorCode.SERVER_ERROR);
	
	public FAIMSClientResultCode resultCode;
	public FAIMSClientErrorCode errorCode;
	
	public Result(FAIMSClientResultCode resultCode) {
		this.resultCode = resultCode;
	}
	
	public Result(FAIMSClientResultCode resultCode, FAIMSClientErrorCode errorCode) {
		this.resultCode = resultCode;
		this.errorCode = errorCode;
	}
	
}
