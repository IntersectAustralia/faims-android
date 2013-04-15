package au.org.intersect.faims.android.data;

import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.services.MessageType;

public class MessageResult {

	public MessageType type;
	public FAIMSClientResultCode result;
	
	public MessageResult(MessageType type, FAIMSClientResultCode result) {
		this.type = type;
		this.result = result;
	}
	
}
