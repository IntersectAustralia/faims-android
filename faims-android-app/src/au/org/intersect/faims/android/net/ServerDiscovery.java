package au.org.intersect.faims.android.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.JsonUtil;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;

@Singleton
public class ServerDiscovery {
	
	public interface ServerDiscoveryListener {
		
		public void handleDiscoveryResponse(boolean success);
	}
	
	private static ServerDiscovery instance;
	
	private LinkedList<ServerDiscoveryListener> listenerList;
	private Application application;
	
	private boolean isFindingServer;
	
	private String serverIP;
	private String serverPort;

	private Timer timer;

	private boolean serverFixed;
	
	public ServerDiscovery() {
		listenerList = new LinkedList<ServerDiscoveryListener>();
		isFindingServer = false;
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
	
	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}

	public void setServerPort(String serverPort) {
		this.serverPort = serverPort;
	}
	
	public void setServerHostFixed(boolean fixed) {
		this.serverFixed = fixed;
	}

	public String getServerHost() {
		return "http://" + serverIP + ":" + serverPort;
	}
	
	public void invalidateServerHost() {
		if (serverFixed) return;
		serverIP = null;
		serverPort = null;
	}
	
	public boolean isServerHostValid() {
		return serverIP != null && serverPort != null;
	}
	
	public synchronized void stopDiscovery() {
		killThreads();
	}
	
	public synchronized void startDiscovery(ServerDiscoveryListener listener) {
		if (isServerHostValid()) {
			FLog.w("server is already valid");
			listener.handleDiscoveryResponse(true);
			return ;
		}
		
		listenerList.add(listener);
		
		if (isFindingServer) return; // already looking for server
		
		isFindingServer = true;
		
		startReceiverThread();
		startBroadcastThread();
		
		// wait for discovery time before killing search
		if (timer != null) {
			FLog.w("already looking for server");
			return ;
		}
		
		timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				killThreads();
			}
			
		}, getDiscoveryTime());
	}
	
	private void killThreads() {
		
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		
		// TODO check if this list needs to be synchronized using Collections.synchronizedList
		synchronized(listenerList) {
			while(!listenerList.isEmpty()) {
				ServerDiscoveryListener listener = listenerList.pop();
				listener.handleDiscoveryResponse(isServerHostValid());
			}
		}
		
		isFindingServer = false;
	}
	
	private void startReceiverThread() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				DatagramSocket socket = null;
				DatagramChannel channel = null;
				try {
					channel = DatagramChannel.open();
					socket = channel.socket();
					socket.setReuseAddress(true);
					socket.bind(new InetSocketAddress(getDevicePort()));
					
					while(isFindingServer) {
						receivePacket(socket);
						
						if (isServerHostValid()) {
							killThreads();
						}
					}
						
				} catch(Exception e) {
					FLog.e("error listening for packets", e);
				} finally {
					if (socket != null) socket.close();
					try {
						if (channel != null) channel.close();
					} catch (IOException e) {
						FLog.e("error closing channel", e);
					}
				}
			}
		}).start();
		
	}
	
	private void startBroadcastThread() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					
					while(isFindingServer) {
						sendBroadcast();
						Thread.sleep(1000);
					}
						
				} catch(Exception e) {
					FLog.e("error broadcasting packets", e);
				}
			}
		}).start();
	}
	
	private void sendBroadcast() throws SocketException, IOException {
		
		DatagramSocket s = new DatagramSocket();
		try {
	    	s.setBroadcast(true);
	    	
	    	String packet = JsonUtil.serializeServerPacket(getIPAddress(), String.valueOf(getDevicePort()));
	    	int length = packet.length();
	    	byte[] message = packet.getBytes();
	    	
	    	DatagramPacket p = new DatagramPacket(message, length, InetAddress.getByName(getBroadcastAddr()), getDiscoveryPort());
	    	
	    	s.send(p);
	    	
	    	FLog.d("AndroidIP: " + getIPAddress());
	    	FLog.d("AndroidPort: " + getDevicePort());
		} finally {
			s.close();
		}
	}
	
	private void receivePacket(DatagramSocket r) throws SocketException, IOException {
		
		try {
			r.setSoTimeout(getPacketTimeout());
			
	    	byte[] buffer = new byte[1024];
	    	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	    	
	        r.receive(packet);
	       
	        JsonObject data = JsonUtil.deserializeServerPacket(getPacketDataAsString(packet));
	        
	        if (data.has("server_ip"))
	        	serverIP = data.get("server_ip").getAsString();
	        if (data.has("server_port"))
	        	serverPort = data.get("server_port").getAsString();
	        
	        FLog.d("ServerIP: " + serverIP);
	        FLog.d("ServerPort: " + serverPort);
		} finally {
			
		}
	}
	
	private String getIPAddress() throws IOException {
		WifiManager wifiManager = (WifiManager) application.getSystemService(Application.WIFI_SERVICE);
    	DhcpInfo myDhcpInfo = wifiManager.getDhcpInfo();
    	if (myDhcpInfo == null) {
    		FLog.d("could not determine device ip");
    		return null;
    	}
    	int broadcast = myDhcpInfo.ipAddress;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
		quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads).getHostAddress();
    }
	
	private int getDiscoveryTime() {
		return application.getResources().getInteger(R.integer.discovery_time) * 1000;
	}
	
	private int getPacketTimeout() {
		return application.getResources().getInteger(R.integer.packet_timeout) * 1000;
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

	public void initiateServerIPAndPort(SharedPreferences preferences) {
		String serverIP = preferences.getString("pref_server_ip", null);
        String serverPort = preferences.getString("pref_server_port", null);
        serverIP = serverIP == null || serverIP.isEmpty() ? null : serverIP;
        serverPort = serverPort == null || serverPort.isEmpty() || serverIP == null || serverIP.isEmpty() ? null : serverPort;
        setServerIP(serverIP);
        setServerPort(serverPort);
        setServerHostFixed(isServerHostValid());
	}

	public boolean isServerHostFixed() {
		return serverFixed;
	}

}
