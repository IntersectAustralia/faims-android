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

import android.app.Application;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.util.FAIMSLog;
import au.org.intersect.faims.android.util.JsonUtil;

import com.google.gson.JsonObject;

public class ServerDiscovery {
	
	public interface ServerDiscoveryListener {
		
		void handleDiscoveryResponse(boolean success);
		
	}
	
	private static ServerDiscovery instance;
	
	private LinkedList<ServerDiscoveryListener> listenerList;
	private Application application;
	
	private boolean isFindingServer;
	private int currentTimeout;
	
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
	
	public boolean isServerHostValid() {
		return serverIP != null && serverPort != null;
	}
	
	private void startHandlerThread() {
		FAIMSLog.log();
		
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
					FAIMSLog.log(e);
				}
			}
		}).start();
	}
	
	public void findServer(ServerDiscoveryListener handler, int attempts) {
		FAIMSLog.log();
		
		isFindingServer = true;
		currentTimeout = (1 + attempts) * getPacketTimeout();
		
		listenerList.add(handler);
		
		// check if server is valid or look for server
		if (!isServerHostValid()) {
			startDiscovery();
		}
	}
	
	private void startDiscovery() {
		FAIMSLog.log();
		
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
		FAIMSLog.log();
		
		DatagramSocket s = new DatagramSocket();
		try {
	    	s.setBroadcast(true);
	    	s.setSoTimeout(currentTimeout);
	    	
	    	//s.bind(new InetSocketAddress(InetAddress.getLocalHost(), ANDROID_BROADCAST_PORT));
	        
	    	String packet = JsonUtil.serializeServerPacket(getIPAddress(), String.valueOf(getDevicePort()));
	    	int length = packet.length();
	    	byte[] message = packet.getBytes();
	    	
	    	DatagramPacket p = new DatagramPacket(message, length, InetAddress.getByName(getBroadcastAddr()), getDiscoveryPort());
	    	
	    	s.send(p);
	    	
	    	Log.d("debug", "AndroidIP: " + getIPAddress());
	    	Log.d("debug", "AndroidPort: " + getDevicePort());
		} finally {
			s.close();
		}
	}
	
	private void waitForResponse() throws SocketException, IOException {
		FAIMSLog.log();
		
		// receive packet
		DatagramSocket r = new DatagramSocket(getDevicePort());
		try {
			r.setSoTimeout(currentTimeout);
			
	    	byte[] buffer = new byte[1024];
	    	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	    	
	        r.receive(packet);
	       
	        JsonObject data = JsonUtil.deserializeServerPacket(getPacketDataAsString(packet));
	        
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
    		FAIMSLog.log("could not determine device ip");
    		return null;
    	}
    	int broadcast = myDhcpInfo.ipAddress;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
		quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads).getHostAddress();
    }
	
	private int getPacketTimeout() {
		return application.getResources().getInteger(R.integer.packet_timeout);
	}
	
	private int getDiscoveryPort() {
		return application.getResources().getInteger(R.integer.discovery_port);
	}
	
	private int getDevicePort() {
		return application.getResources().getInteger(R.integer.device_port);
	}
	
	private String getBroadcastAddr() {
		return application.getResources().getString(R.string.broadcast_addr);
	}
	
	private String getPacketDataAsString(DatagramPacket packet) throws IOException {
		FAIMSLog.log();
		
		InputStreamReader reader = null;
		try {
			 reader = new InputStreamReader(new ByteArrayInputStream(packet.getData()), Charset.forName("UTF-8"));
		     StringBuilder sb = new StringBuilder();
		     int value;
		     while((value = reader.read()) > 0)
		    	 sb.append((char) value);
		     
		     return sb.toString();
		 } finally {
			 if (reader != null) reader.close();
		 }
	}
	
	public void clearListeners() {
		listenerList.clear();
	}

}
