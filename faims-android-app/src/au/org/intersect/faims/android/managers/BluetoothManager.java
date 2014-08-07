package au.org.intersect.faims.android.managers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.data.IFAIMSRestorable;
import au.org.intersect.faims.android.gps.GPSDataManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BluetoothManager implements IFAIMSRestorable {
	
	private static final String TAG = "bluetooth:";
	
	public interface BluetoothListener {
		public void onConnect();
		public void onInput(String input);
		public void onDisconnect();
	}
	
	public enum BluetoothStatus {
		ACTIVE, CONNECTED, DISCONNECTED, ERROR;

		public static BluetoothStatus toBluetoothStatus(String bluetoothStatusString) {
			return valueOf(bluetoothStatusString);
		}
	}
	
	class BluetoothInputRunnable implements Runnable {
		
		@Override
		public void run() {
			readInput();
			
	        if (handler != null && repeatable) {
	        	handler.postDelayed(inputRunnable, postInterval);
	        }
	    }
		
	}
	
	class BluetoothOutputRunnable implements Runnable {

		@Override
		public void run() {
			writeOutput(outputMessage);
		}
		
	}
	
	@Inject
	BeanShellLinker beanShellLinker;
	
	@Inject
	GPSDataManager gpsDataManager;
	
	private WeakReference<ShowModuleActivity> activityRef;
	
	private HandlerThread handlerThread;
	private BluetoothDevice bluetoothDevice;
	private BluetoothSocket bluetoothSocket;
	
	private Handler handler;
	private BluetoothInputRunnable inputRunnable;
	private BluetoothOutputRunnable outputRunnable;

	private BluetoothListener listener;
	
	private int postInterval;
	private boolean repeatable;
	private String outputMessage;
	private boolean clearInputOnRead;
	private boolean isBluetoothConnected;
	private BluetoothStatus bluetoothStatus;
	
	public void init(ShowModuleActivity activity) {
		FAIMSApplication.getInstance().injectMembers(this);
		activityRef = new WeakReference<ShowModuleActivity>(activity);
		postInterval = 0;
		repeatable = false;
		outputMessage = null;
		clearInputOnRead = false;
		isBluetoothConnected = false;
		bluetoothStatus = BluetoothStatus.DISCONNECTED;
	}
	
	public void showConnectionDialog() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null) {
			beanShellLinker.showToast("Bluetooth is not supported on this device");
		} else if (!adapter.isEnabled()) {
			beanShellLinker.showToast("Please enable bootooth");
		} else {
			// show dialog to connect to device
			final Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
			if (pairedDevices.size() > 0) {
				final ArrayList<CharSequence> sequences = new ArrayList<CharSequence>();
				
				for (BluetoothDevice bluetoothDevice : pairedDevices) {
					sequences.add(bluetoothDevice.getName());
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(activityRef.get());
				builder.setTitle("Select bluetooth to connect");
				builder.setItems(
						sequences.toArray(new CharSequence[sequences.size()]),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								for (BluetoothDevice bluetoothDevice : pairedDevices) {
									if (bluetoothDevice.getName().equals(sequences.get(item))) {
										BluetoothManager.this.bluetoothDevice = bluetoothDevice;
										break;
									}
								}
								createConnection();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}
		}
	}
	
	public void resetConnection() {
		destroyConnection();
		this.bluetoothDevice = null;
	}
	
	public void createConnection(BluetoothListener listener, int interval) {
		this.listener = listener;
		this.postInterval = interval;
		this.repeatable = interval > 0;
		
		createConnection();
	}
	
	private void createConnection() {
		if (!isBluetoothConnected) {
			if (bluetoothDevice == null) {
				showConnectionDialog();
				return;
			}	
			createBluetoothHandlerThread();
			isBluetoothConnected = true;
			updateBluetoothStatus(BluetoothStatus.CONNECTED);
			beanShellLinker.showToast("bluetooth connection established");
			
			if (repeatable) {
				readMessage();
			}
		}
	}
	
	public void destroyConnection() {
		if (isBluetoothConnected) {
			pauseConnection();
			updateBluetoothStatus(BluetoothStatus.DISCONNECTED);
			isBluetoothConnected = false;
			beanShellLinker.showToast("bluetooth connection destroyed");
		}
	}
	
	public void pauseConnection() {
		destroyBluetoothHandlerThread();
		destroyBluetoothSocket();
	}
	
	public void readMessage() {
		if (handler ==  null) {
			beanShellLinker.showToast("no bluetooth connection found");
		} else {
			handler.postDelayed(inputRunnable, 0);
		}
	}
	
	public void writeMessage(String message) {
		if (handler ==  null) {
			beanShellLinker.showToast("no bluetooth connection found");
		} else {
			outputMessage = message;
			handler.postDelayed(outputRunnable, 0);
		}
	}
	
	public void clearMessages() {
		clearInputOnRead = true;
	}
	
	private void createBluetoothHandlerThread() {
		if (handlerThread == null) {
			 handlerThread = new HandlerThread("BluetoothHandler");
			 handlerThread.start();
		}
		
		if (inputRunnable == null) {
			inputRunnable = new BluetoothInputRunnable();
		}
		
		if (outputRunnable == null) {
			outputRunnable = new BluetoothOutputRunnable();
		}
		
		if (handler == null ) {
			handler = new Handler(handlerThread.getLooper());
		}
	}
	
	private void destroyBluetoothHandlerThread() {
		if (handlerThread != null) {
			 handlerThread.quit();
			 handlerThread = null;
		}
		
		if (handler != null) {
			handler.removeCallbacks(inputRunnable);
			handler.removeCallbacks(outputRunnable);
			handler = null;
		}
		
		inputRunnable = null;
		outputRunnable = null;
	}
	
	private void createBluetoothSocket() {
		if (bluetoothDevice != null) {
			if (bluetoothSocket == null) {
				try {
					Method m = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
					bluetoothSocket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
					bluetoothSocket.connect();
					updateBluetoothStatus(BluetoothStatus.ACTIVE);
					if (listener != null) {
						listener.onConnect();
					}
				} catch (Exception e) {
					FLog.e("error trying to create bluetooth socket", e);
					
					if (bluetoothSocket != null && !bluetoothSocket.isConnected()) {
						bluetoothSocket = null;
					}
					updateBluetoothStatus(BluetoothStatus.ERROR);
				}
			}
		} else {
			FLog.d("no device found");
		}
	}
	
	private void destroyBluetoothSocket() {
		if (bluetoothSocket != null) {
			try {
				bluetoothSocket.close();
				bluetoothSocket = null;
				updateBluetoothStatus(BluetoothStatus.CONNECTED);
				if (listener != null) {
					listener.onDisconnect();
				}
			} catch (Exception e) {
				FLog.e("error trying to destroy bluetooth socket", e);
				updateBluetoothStatus(BluetoothStatus.ERROR);
			}
    	} else {
    		FLog.d("no connection found");
    	}
	}
	
	private void readInput() {
		createBluetoothSocket();
		
		if (bluetoothSocket != null) {
	        try {
	        	// clear the input and wait for new line
	        	if (clearInputOnRead) {
	        		skipBufferredInputs(bluetoothSocket.getInputStream());
	        		clearInputOnRead = false;
	        	}
	        	
	    		String input = readStringFromInput(bluetoothSocket.getInputStream()); 		
	    		if (listener != null && input != null && !input.isEmpty()) {
	    			listener.onInput(input);
	    		}
	        } catch (Exception e) {
	        	FLog.e("error trying to read input", e);
	        	destroyBluetoothSocket();
			}
        } else {
        	FLog.d("no connection found");
        }
	}
	
	private void writeOutput(String output) {
		createBluetoothSocket();
		
		if (bluetoothSocket != null) {
			try {
				writeStringToOutput(output + "\r", bluetoothSocket.getOutputStream());
			} catch (Exception e) {
				FLog.e("error trying to write output", e);
				destroyBluetoothSocket();
			}
		} else {
        	FLog.d("no connection found");
        }
	}
	
	private String readStringFromInput(InputStream inputStream) throws IOException {
		StringBuilder sb = new StringBuilder();
		int c;
		boolean first = true;
		while((c = inputStream.read()) != -1) {
			// check for end of line
			if (isEndOfLine((char) c)) {
				if (first) {
					first = false;
					continue;
				} else {
					break;
				}
			}
			sb.append((char) c); // ascii char
		}
		return sb.toString();
	}

	private void writeStringToOutput(String output, OutputStream outputStream) throws IOException {
		outputStream.write(output.getBytes());
	}
	
	private void skipBufferredInputs(InputStream inputStream) throws IOException {
		inputStream.skip(inputStream.available());
		int c;
		while((c = inputStream.read()) != -1) {
			if (isEndOfLine((char) c)) {
				break;
			}
		}
	}

	@Override
	public void saveTo(Bundle savedInstanceState) {
		savedInstanceState.putBoolean(TAG + "isBluetoothConnected", isBluetoothConnected);
	}

	@Override
	public void restoreFrom(Bundle savedInstanceState) {
		isBluetoothConnected = savedInstanceState.getBoolean(TAG + "isBluetoothConnected");
	}

	@Override
	public void resume() {
		if (isBluetoothConnected) {
			createConnection();
		}
	}

	@Override
	public void pause() {
		if (!(gpsDataManager.isTrackingStarted() && gpsDataManager.isExternalGPSStarted())) {
			pauseConnection();
		}
	}

	@Override
	public void destroy() {
		destroyConnection();
	}
	
	public boolean isEndOfLine(char c) {
		return c == Character.LINE_SEPARATOR || c == '\n';
	}
	
	public boolean isBluetoothConnected() {
		return isBluetoothConnected;
	}
	
	public boolean isSocketConnected() {
		return bluetoothSocket != null;
	}
	
	public BluetoothStatus getBluetoothStatus() {
		return bluetoothStatus;
	}
	
	public void setBluetoothStatus(BluetoothStatus bluetoothStatus) {
		this.bluetoothStatus = bluetoothStatus;
	}
	
	private void updateBluetoothStatus(BluetoothStatus status) {
		if (isBluetoothConnected) {
			setBluetoothStatus(status);
			activityRef.get().updateStatusBar();
		}
	}

}
