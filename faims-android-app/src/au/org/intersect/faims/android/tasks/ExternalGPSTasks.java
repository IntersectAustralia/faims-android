package au.org.intersect.faims.android.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.GGASentence;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public class ExternalGPSTasks implements Runnable {

	private BluetoothDevice gpsDevice;
	private Handler handler;
	private BluetoothActionListener actionListener;
    private String GGAMessage;
    private String BODMessage;
    private int gpsUpdateInterval;
    private BluetoothSocket bluetoothSocket;
    private InputStream in;
    private InputStreamReader isr;
    private BufferedReader br;

    public ExternalGPSTasks(BluetoothDevice gpsDevice, Handler handler, BluetoothActionListener actionListener, int gpsUpdateInterval){
    	this.gpsDevice = gpsDevice;
    	this.handler = handler;
    	this.actionListener = actionListener;
    	this.gpsUpdateInterval = gpsUpdateInterval;
		try {
			if(this.gpsDevice != null){
				initialiseBluetoothSocket();
			}
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		} catch (IOException e) {
			this.bluetoothSocket = null;
		}
    }

	@Override
	public void run() {
		try{
			handler.postDelayed(this, this.gpsUpdateInterval);
			readSentences();
			this.actionListener.handleGPSUpdates(this.GGAMessage, this.BODMessage);
		}catch(Exception e){
			Log.d("bluetooth-faims", "run method exception", e);
		}
	}

	public void closeBluetoothConnection(){
		if(this.bluetoothSocket != null){
    		try {
    			if(this.br != null){
    				br.close();
    			}
    			if(this.isr != null){
    				isr.close();
    			}
    			if(this.in != null){
    				in.close();
    			}
				this.bluetoothSocket.close();
			} catch (IOException e) {
				Log.d("bluetooth-faims", "close bluetooth connection exception", e);
			}
    	}
	}
	
	private void readSentences() {
        this.GGAMessage = null;
        this.BODMessage = null;
        InputStream in = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        if(this.gpsDevice != null){
	        try {
	            if(this.bluetoothSocket == null){
	            	initialiseBluetoothSocket();
	            }
	            long start = System.currentTimeMillis();
	            long end = start + 1000; // check for 1 seconds to get valid GPGGA message
	            in = bluetoothSocket.getInputStream();
	    		isr = new InputStreamReader(in);
	    		br = new BufferedReader(isr);
	            while (System.currentTimeMillis() < end){
	                String nmeaMessage = br.readLine();
	                if(nmeaMessage == null){
	                	break;
	                }
	                if (nmeaMessage.startsWith("$GPGGA")) {
	                    if(hasValidGGAMessage()){
	                    	Log.d("bluetooth-faims", "valid nmea message");
	                        break;
	                    }else{
	                        this.GGAMessage = nmeaMessage;
	                        if(!hasValidGGAMessage()){
	                        	this.GGAMessage = null;
	                        }
	                    }
		            } else if (nmeaMessage.startsWith("$GPBOD")) {
	                    this.BODMessage = nmeaMessage;
	                }
	            }
	            if(this.br != null){
    				br.close();
    			}
    			if(this.isr != null){
    				isr.close();
    			}
    			if(this.in != null){
    				in.close();
    			}
	        } catch (Exception e) {
	        	Log.d("bluetooth-faims", "init connection exception", e);
	        	if(this.bluetoothSocket != null){
	        		try {
	        			if(this.br != null){
	        				br.close();
	        			}
	        			if(this.isr != null){
	        				isr.close();
	        			}
	        			if(this.in != null){
	        				in.close();
	        			}
						this.bluetoothSocket.close();
						this.bluetoothSocket = null;
					} catch (IOException exception) {
						Log.d("bluetooth-faims", "closing streams exception", e);
					}
	        	}
			}
        }else{
        	Log.d("bluetooth-faims", "null gps device");
        }
    }

	private void initialiseBluetoothSocket() throws NoSuchMethodException,
			IllegalAccessException, InvocationTargetException, IOException {
		Method m = this.gpsDevice.getClass().getMethod("createRfcommSocket",
		        new Class[] { int.class });
		this.bluetoothSocket = (BluetoothSocket) m.invoke(
				this.gpsDevice, 1);
		this.bluetoothSocket.connect();
	}

	private boolean hasValidGGAMessage() {
        GGASentence sentence = null;
        try{
	        if (this.GGAMessage != null) {
	            sentence = (GGASentence) SentenceFactory.getInstance()
	                    .createParser(this.GGAMessage);
	        }
        	return this.GGAMessage != null && sentence != null && sentence.getPosition() != null;
        } catch (Exception e){
        	Log.d("bluetooth-faims", "wrong gga format sentence", e);
        	return false;
        }
    }

}
