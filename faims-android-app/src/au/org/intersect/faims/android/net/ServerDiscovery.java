package au.org.intersect.faims.android.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.LinkedList;

import android.app.Application;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import au.org.intersect.faims.util.JsonUtil;

import com.google.gson.JsonObject;

public class ServerDiscovery {
	
	public static final int SERVER_DISCOVERY_PORT = 4000;
	public static final int ANDROID_BROADCAST_PORT = 5000;
	public static final int ANDROID_RECEIVER_PORT = 6000;
	public static final String BROADCAST_ADDR = "255.255.255.255";
	
	public static final int PACKET_TIMEOUT = 1000;
	
	public interface ServerDiscoveryListener {
		
		void handleDiscoveryResponse(boolean success);
		
	}
	
	private static ServerDiscovery instance;
	
	private LinkedList<ServerDiscoveryListener> listenerList;
	private Application application;
	private boolean isFindingServer;
	private int timeout;
	
	private String serverIP;
	private String serverPort;
	
	public ServerDiscovery() {
		listenerList = new LinkedList<ServerDiscoveryListener>();
		isFindingServer = false;
		
		startHandlerThread();
	}
	
	public static ServerDiscovery getInstance() {
		if (instance == null) instance = new ServerDiscovery();
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
		return "http://" + serverIP + ":" + serverPort;
	}
	
	public void invalidateServerHost() {
		serverIP = null;
		serverPort = null;
	}
	
	private void startHandlerThread() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					while(true) {
						
						if (listenerList.isEmpty()) {
							Thread.sleep(1000);
						} else if (isFindingServer) {
							Thread.sleep(1000);
						} else {
							
							ServerDiscoveryListener listener = listenerList.pop();
							listener.handleDiscoveryResponse(isServerHostValid());
						}
						
						
					}
				} catch(InterruptedException e) {
					Log.d("debug", e.toString());
				}
			}
		}).start();
	}
	
	public void findServer(ServerDiscoveryListener handler, int attempts) {
		Log.d("debug", "ServerDiscovery.findServer");
		
		isFindingServer = true;
		timeout = (1 + attempts) * PACKET_TIMEOUT;
		
		listenerList.add(handler);
		
		// check if server is valid or look for server
		if (!isServerHostValid()) {
			startDiscovery();
		}
	}
	
	public boolean isServerHostValid() {
		return serverIP != null && serverPort != null;
	}
	
	private void startDiscovery() {
		Log.d("debug", "ServerDiscovery.startDiscovery");
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					
					sendBroadcast();
					waitForResponse();
					
				} catch(SocketException e) {
					Log.d("debug", e.toString());
				} catch(IOException e) {
					Log.d("debug", e.toString());
				} finally {
			        isFindingServer = false;
				}
			}
		}).start();
	}
	
	private void sendBroadcast() throws SocketException, IOException {
		Log.d("debug", "ServerDiscovery.sendBroadcast");
		DatagramSocket s = new DatagramSocket();
		try {
	    	s.setBroadcast(true);
	    	s.setSoTimeout(timeout);
	    	//s.bind(new InetSocketAddress(InetAddress.getLocalHost(), ANDROID_BROADCAST_PORT));
	        
	    	String packet = JsonUtil.serializeServerPacket(getIPAddress(), String.valueOf(ANDROID_RECEIVER_PORT));
	    	int length = packet.length();
	    	byte[] message = packet.getBytes();
	    	
	    	DatagramPacket p = new DatagramPacket(message, length, InetAddress.getByName(BROADCAST_ADDR), SERVER_DISCOVERY_PORT);
	    	
	    	s.send(p);
	    	Log.d("debug", "AndroidIP: " + getIPAddress());
	    	Log.d("debug", "AndroidPort: " + ANDROID_RECEIVER_PORT);
		} finally {
			s.close();
		}
	}
	
	private void waitForResponse() throws SocketException, IOException {
		Log.d("debug", "ServerDiscovery.waitForResponse");
		// receive packet
		DatagramSocket r = new DatagramSocket(ANDROID_RECEIVER_PORT);
		try {
			r.setSoTimeout(timeout);
			
	    	byte[] buffer = new byte[1024];
	    	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	    	
	        r.receive(packet);
	        
	        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(packet.getData()), Charset.forName("UTF-8"));
	        StringBuilder sb = new StringBuilder();
	        int value;
	        while((value = input.read()) > 0)
	            sb.append((char) value);
	        
	        JsonObject data = JsonUtil.deserializeServerPacket(sb.toString());
	        
	        serverIP = data.get("ip").getAsString();
	        serverPort = data.get("port").getAsString();
	        
	        Log.d("debug", "ServerIP: " + serverIP.toString());
	        Log.d("debug", "ServerPort: " + serverPort.toString());
		} finally {
			r.close();
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
	
	public void clearListeners() {
		listenerList.clear();
	}

}
