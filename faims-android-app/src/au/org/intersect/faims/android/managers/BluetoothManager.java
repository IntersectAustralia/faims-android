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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.IFAIMSRestorable;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.view.BeanShellLinker;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BluetoothManager implements IFAIMSRestorable {
	
	public interface BluetoothListener {
		public void onInput(String input);
	}
	
	class BluetoothInputRunnable implements Runnable {
		
		@Override
		public void run() {
			readInput();
			
	        if (repeatable) {
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
	
	private WeakReference<Context> contextRef;
	private BluetoothDevice bluetoothDevice;
	private BluetoothSocket bluetoothSocket;
	
	private HandlerThread handlerThread;
	private Handler handler;
	private BluetoothInputRunnable inputRunnable;
	private BluetoothOutputRunnable outputRunnable;

	private BluetoothListener listener;
	
	private int postInterval;
	private boolean repeatable;
	private String outputMessage;
	private boolean clearInputOnRead;
	
	private boolean isBluetoothConnected;
	private boolean isBluetoothPaused;
	
	public void init(Context context) {
		FAIMSApplication.getInstance().injectMembers(this);
		contextRef = new WeakReference<Context>(context);
	}
	
	public BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
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
				
				AlertDialog.Builder builder = new AlertDialog.Builder(contextRef.get());
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
			
			if (repeatable) {
				readMessage();
			}
		}
	}
	
	public void destroyConnection() {
		if (isBluetoothConnected) {
			destroyBluetoothHandlerThread();
			destroyBluetoothSocket();
			isBluetoothConnected = false;
		}
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
		if (handler != null) {
			handler.removeCallbacks(inputRunnable);
			handler.removeCallbacks(outputRunnable);
			handler = null;
		}
		
		inputRunnable = null;
		outputRunnable = null;
		
		if (handlerThread != null) {
			handlerThread.quit();
			handlerThread = null;
		}
	}
	
	private void createBluetoothSocket() {
		if (bluetoothDevice != null) {
			if (bluetoothSocket == null) {
				try {
					Method m = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
					bluetoothSocket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
					bluetoothSocket.connect();
				} catch (Exception e) {
					FLog.e("error trying to create bluetooth socket", e);
					
					if (listener != null) {
						beanShellLinker.showToast("Waiting on valid bluetooth connection");
					}
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
			} catch (Exception e) {
				FLog.e("error trying to destroy bluetooth socket", e);
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
	    		if (listener != null) {
	    			listener.onInput(input);
	    		}
	        } catch (Exception e) {
	        	FLog.e("error trying to read input", e);
			}
        } else {
        	FLog.d("no connection found");
        }
	}
	
	private void writeOutput(String output) {
		createBluetoothSocket();
		
		if (bluetoothSocket != null) {
			try {
				writeStringToOutput(output, bluetoothSocket.getOutputStream());
			} catch (Exception e) {
				FLog.e("error trying to write output", e);
			}
		} else {
        	FLog.d("no connection found");
        }
	}
	
	private String readStringFromInput(InputStream inputStream) throws IOException {
		StringBuilder sb = new StringBuilder();
		int c;
		while((c = inputStream.read()) != -1) {
			if (c == Character.LINE_SEPARATOR) {
				break;
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
			if (c == Character.LINE_SEPARATOR) {
				break;
			}
		}
	}

	@Override
	public void saveTo(Bundle savedInstanceState) {
	}

	@Override
	public void restoreFrom(Bundle savedInstanceState) {
	}

	@Override
	public void resume() {
		if (isBluetoothPaused) {
			createConnection();
			isBluetoothPaused = false;
		}
	}

	@Override
	public void pause() {
		if (isBluetoothConnected) {
			destroyConnection();
			isBluetoothPaused = true;
		}
	}

	@Override
	public void destroy() {
		isBluetoothPaused = false;
	}

}
