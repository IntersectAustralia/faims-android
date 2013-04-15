package au.org.intersect.faims.android.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import au.org.intersect.faims.android.data.MessageResult;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

public abstract class MessageIntentService extends IntentService {

	public MessageIntentService(String name) {
		super(name);
	}

	protected void sendMessage(Intent intent, MessageType type, FAIMSClientResultCode result) throws Exception {
		Bundle extras = intent.getExtras();
		Messenger messenger = (Messenger) extras.get("MESSENGER");
		Message msg = Message.obtain();
		msg.obj = new MessageResult(type, result);
		messenger.send(msg);
	}

}
