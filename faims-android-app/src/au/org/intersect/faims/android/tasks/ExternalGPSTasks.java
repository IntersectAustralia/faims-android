package au.org.intersect.faims.android.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.GGASentence;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

public class ExternalGPSTasks implements Runnable {

	private BluetoothDevice gpsDevice;
	private Handler handler;
	private BluetoothActionListener actionListener;
    private String GGAMessage;
    private String BODMessage;
    private int gpsUpdateInterval;

    public ExternalGPSTasks(BluetoothDevice gpsDevice, Handler handler, BluetoothActionListener actionListener, int gpsUpdateInterval){
    	this.gpsDevice = gpsDevice;
    	this.handler = handler;
    	this.actionListener = actionListener;
    	this.gpsUpdateInterval = gpsUpdateInterval;
    }

	@Override
	public void run() {
		System.out.println("Running the external GPS task");
		readSentences();
		this.actionListener.handleGPSUpdates(this.GGAMessage, this.BODMessage);
		handler.postDelayed(this, this.gpsUpdateInterval);
	}

	private void readSentences() {
        InputStream in = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        this.GGAMessage = null;
        this.BODMessage = null;
        if(this.gpsDevice != null){
	        try {
	            Method m = this.gpsDevice.getClass().getMethod("createRfcommSocket",
	                    new Class[] { int.class });
	            BluetoothSocket bluetoothSocket = (BluetoothSocket) m.invoke(
	            		this.gpsDevice, 1);
	            bluetoothSocket.connect();
	            in = bluetoothSocket.getInputStream();
	            isr = new InputStreamReader(in);
	            br = new BufferedReader(isr);
	
	            long start = System.currentTimeMillis();
	            long end = start + 500; // check for 0.5 seconds to get valid GPGGA message
	            while (System.currentTimeMillis() < end){
	                String nmeaMessage = br.readLine();
	                if (nmeaMessage.startsWith("$GPGGA")) {
	                    if(hasValidGGAMessage()){
	                        break;
	                    }else{
	                        this.GGAMessage = nmeaMessage;
	                    }
	                } else if (nmeaMessage.startsWith("$GPBOD")) {
	                    this.BODMessage = nmeaMessage;
	                }
	            }
	            br.close();
	            isr.close();
	            in.close();
	            bluetoothSocket.close();
	        } catch (IOException e) {
	        	this.gpsDevice = null;
	            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	            if( adapter != null){
	                Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
	                if (pairedDevices.size() > 0) {
	                    for (BluetoothDevice bluetoothDevice : pairedDevices) {
	                        this.gpsDevice = bluetoothDevice;
	                        break;
	                    }
	                }
	            }
	        } catch (NoSuchMethodException e) {
	        } catch (IllegalArgumentException e) {
	        } catch (IllegalAccessException e) {
	        } catch (InvocationTargetException e) {
	        }
        }else{
        	this.gpsDevice = null;
        	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if( adapter != null){
                Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice bluetoothDevice : pairedDevices) {
                        this.gpsDevice = bluetoothDevice;
                        break;
                    }
                }
            }
        }
    }

	private boolean hasValidGGAMessage() {
        GGASentence sentence = null;
        if (this.GGAMessage != null) {
            sentence = (GGASentence) SentenceFactory.getInstance()
                    .createParser(this.GGAMessage);
        }
        try{
        	return this.GGAMessage != null && sentence != null && sentence.getPosition() != null;
        } catch (DataNotAvailableException e){
        	return false;
        }
    }

}
