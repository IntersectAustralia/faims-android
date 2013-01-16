package au.org.intersect.faims.android.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.GGASentence;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;

public class ExternalGPSTasks implements Runnable {

	private BluetoothDevice gpsDevice;
	private Handler handler;
	private BluetoothActionListener actionListener;
    private String GGAMessage;
    private String BODMessage;

    public ExternalGPSTasks(BluetoothDevice gpsDevice, Handler handler, Context context){
    	this.gpsDevice = gpsDevice;
    	this.handler = handler;
    	this.actionListener = (BluetoothActionListener) context;
    }

	@Override
	public void run() {
		readSentences(this.gpsDevice);
		this.actionListener.handleGPSUpdates(this.GGAMessage, this.BODMessage);
		handler.postDelayed(this, 10000);
	}

	private void readSentences(BluetoothDevice device) {
        InputStream in = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        this.GGAMessage = null;
        this.BODMessage = null;
        try {
            Method m = device.getClass().getMethod("createRfcommSocket",
                    new Class[] { int.class });
            BluetoothSocket bluetoothSocket = (BluetoothSocket) m.invoke(
                    device, 1);
            bluetoothSocket.connect();
            in = bluetoothSocket.getInputStream();
            isr = new InputStreamReader(in);
            br = new BufferedReader(isr);

            while (true) {
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
            bluetoothSocket.close();
            br.close();
            isr.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private boolean hasValidGGAMessage() {
        GGASentence sentence = null;
        if (this.GGAMessage != null) {
            sentence = (GGASentence) SentenceFactory.getInstance()
                    .createParser(this.GGAMessage);
        }
        return this.GGAMessage != null && sentence != null && sentence.getPosition() != null;
    }

}
