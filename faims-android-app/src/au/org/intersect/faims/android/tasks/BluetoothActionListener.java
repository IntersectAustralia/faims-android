package au.org.intersect.faims.android.tasks;

public interface BluetoothActionListener {

	public void handleGPSUpdates(String GGAMessage, String BODMessage);
	
	public void bluetoothOff(String message);
}
