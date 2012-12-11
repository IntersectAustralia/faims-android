package au.org.intersect.faims.android.net;

public interface FAIMSResponseHandler<T> {

	
	void handleResponse(boolean success, T content);
	
}
