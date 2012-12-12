package au.org.intersect.faims.android.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Application;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class DiscoveryServer implements Runnable {
	
	public static final int SERVER_DISCOVERY_PORT = 4000;
	public static final int ANDROID_RECEIVER_PORT = 5432;
	public static final int ANDROID_BROADCAST_PORT = 6543;
	public static final String BROADCAST_ADDR = "255.255.255.255";
	
	public interface ServerFoundHandler {
		
		void handleServerFound(boolean success);
		
	}
	
	private static DiscoveryServer instance;
	
	private LinkedList<ServerFoundHandler> handlerList;
	private Application application;
	private ReentrantLock lock;
	
	private String serverIP;
	private String serverPort;
	
	public DiscoveryServer() {
		handlerList = new LinkedList<ServerFoundHandler>();
		lock = new ReentrantLock();
	}
	
	public static DiscoveryServer getInstance() {
		if (instance == null) instance = new DiscoveryServer();
		return instance;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}
	
	public String getServerIP() {
		return serverIP;
	}
	
	public String getServerPort() {
		return String.valueOf(serverPort);
	}
	
	public String getServerHost() {
		return "http://" + serverIP + ":" + String.valueOf(serverPort);
	}
	
	public void findServer(ServerFoundHandler handler) {
		// make sure only one discovery thread is executing at a time
		
		handlerList.add(handler);
		
		Log.d("debug", "DiscoveryServer.findServer");
		
		// check if server is valid or look for server
		if (isSeverIPValid()) {
			
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
	
	private void foundServer(boolean success) {
		Log.d("debug", "DiscoveryServer.foundServer: " + String.valueOf(success));
		LinkedList<ServerFoundHandler> callbacks = new LinkedList<ServerFoundHandler>();
		
		while(!handlerList.isEmpty()) {
			callbacks.push(handlerList.pop());
		}
		
		while(!callbacks.isEmpty()) {
			ServerFoundHandler handler = callbacks.pop(); // note: maybe shift to execute in correct order
			handler.handleServerFound(success);
		}
	}
	
	@Override
	public void run() {
		try {
			DatagramSocket s = new DatagramSocket(ANDROID_BROADCAST_PORT);
	    	s.setBroadcast(true);
	    	s.setSoTimeout(1000);

	    	JsonObject object = new JsonObject();
	    	object.addProperty("ip", getIPAddress());
	    	object.addProperty("port", ANDROID_RECEIVER_PORT);
	    	
	    	// broadcast discovery packet using current network details
	    	InetAddress local = InetAddress.getByName(BROADCAST_ADDR);
	    	Log.d("debug", object.toString());
	    	Log.d("debug", local.getHostAddress().toString());
	    	
	    	int msg_length = object.toString().length();
	    	byte[] message = object.toString().getBytes();
	    	DatagramPacket p = new DatagramPacket(message, msg_length, local, SERVER_DISCOVERY_PORT);
	    	s.send(p);
	    	s.close();
	    	s.disconnect();
	    	
	    	Log.d("debug", "sent broadcast");
	    	
	    	// receive packet
	    	byte[] buffer = new byte[1024];
	    	DatagramSocket r = new DatagramSocket(ANDROID_RECEIVER_PORT);
	    	r.setSoTimeout(1000);
	        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	        r.receive(packet);
	        r.close();
	        r.disconnect();
	        
	        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(packet.getData()), Charset.forName("UTF-8"));
	        StringBuilder str = new StringBuilder();
	        int value;
	        while((value = input.read()) > 0)
	            str.append((char) value);
	        
	        JsonReader reader = new JsonReader(new StringReader(str.toString()));
	        JsonParser parser = new JsonParser();
	        JsonObject data = parser.parse(reader).getAsJsonObject();
	        
	        serverIP = data.get("ip").getAsString();
	        serverPort = data.get("port").getAsString();
	        
	        Log.d("debug", "JsonObject: " + data.toString());
	        Log.d("debug", "ServerIP: " + serverIP.toString());
	        
	        foundServer(true);
	        
		} catch(SocketException e) {
			Log.d("debug", e.toString());
			foundServer(false);
		} catch(IOException e) {
			Log.d("debug", e.toString());
			foundServer(false);
		} catch(JsonIOException e) {
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
