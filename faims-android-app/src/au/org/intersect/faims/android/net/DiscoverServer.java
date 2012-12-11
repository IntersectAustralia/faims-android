package au.org.intersect.faims.android.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class DiscoverServer implements Runnable {
	
	public static final int SEVER_PORT = 4000;
	public static final String BROADCAST_ADDR = "255.255.255.255";
	
	public interface ServerFoundHandler {
		
		void handleServerFound(boolean success);
		
	}
	
	private static DiscoverServer instance;
	
	private LinkedList<ServerFoundHandler> handlerList;
	private Application application;
	private ReentrantLock lock;
	
	private String serverIP;
	
	public DiscoverServer() {
		handlerList = new LinkedList<ServerFoundHandler>();
		lock = new ReentrantLock();
	}
	
	public static DiscoverServer getInstance() {
		if (instance == null) instance = new DiscoverServer();
		return instance;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}
	
	public String getServerIP() {
		return serverIP;
	}
	
	public String getServerPort() {
		return String.valueOf(SEVER_PORT);
	}
	
	public String getServerHost() {
		return "http://" + serverIP + ":" + String.valueOf(SEVER_PORT);
	}
	
	public void findServer(ServerFoundHandler handler) {
		// make sure only one discovery thread is executing at a time
		lock.lock();
		
		handlerList.add(handler);
		
		// check if server is valid or look for server
		if (isSeverIPValid()) {
			lock.unlock();
			foundServer(true);
		} else {
			startDiscovery();
		}
	}
	
	// todo: serverIP should only be valid for a certain period
	private boolean isSeverIPValid() {
		return serverIP != null;
	}
	
	private void startDiscovery() {
		Thread discoveryThread = new Thread(this);
		discoveryThread.start();
	}
	
	private synchronized void foundServer(boolean success) {
		while(!handlerList.isEmpty()) {
			ServerFoundHandler handler = handlerList.pop(); // note: maybe shift to execute in correct order
			handler.handleServerFound(success);
		}
		
		lock.unlock();
	}
	
	@Override
	public void run() {
		try {
			DatagramSocket s = new DatagramSocket();
	    	s.setBroadcast(true);
	    	s.setSoTimeout(200);
	    	int androidPort = s.getLocalPort();
	    	
	    	JSONObject object = new JSONObject();
	    	try {
	    		object.put("ip", getIPAddress());
	    		object.put("port", androidPort);
	    	} catch (JSONException e) {
	    		Log.d("debug", e.toString());
	    	}
	    	
	    	// broadcast discovery packet using current network details
	    	InetAddress local = InetAddress.getByName(BROADCAST_ADDR);
	    	Log.d("debug", object.toString());
	    	Log.d("debug", local.getHostAddress().toString());
	    	
	    	int msg_length = object.toString().length();
	    	byte[] message = object.toString().getBytes();
	    	DatagramPacket p = new DatagramPacket(message, msg_length, local, SEVER_PORT);
	    	s.send(p);
	    	s.close();
	    	
	    	// receive packet
	    	byte[] buffer = new byte[1024];
	    	DatagramSocket r = new DatagramSocket(androidPort);
	        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	        r.receive(packet);
	        
	        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(packet.getData()), Charset.forName("UTF-8"));

	        StringBuilder str = new StringBuilder();
	        int value;
	        while((value = input.read()) != -1)
	            str.append((char) value);
	        
	        Log.d("debug", str.toString());
	        
	        foundServer(true);
	        
		} catch(SocketException e) {
			Log.d("debug", e.toString());
			foundServer(false);
		} catch(IOException e) {
			Log.d("debug", e.toString());
			foundServer(false);
		}
	}
	
	private String getIPAddress() throws IOException {
		WifiManager wifiManager = (WifiManager) application.getSystemService(Application.WIFI_SERVICE);
    	DhcpInfo myDhcpInfo = wifiManager.getDhcpInfo();
    	if (myDhcpInfo == null) {
    		Log.d("debug", "Could not get broadcast address");
    		return null;
    	}
    	int broadcast = myDhcpInfo.ipAddress;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
		quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads).getHostAddress();
    }

}
