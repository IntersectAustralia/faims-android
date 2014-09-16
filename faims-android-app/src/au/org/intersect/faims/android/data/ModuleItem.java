package au.org.intersect.faims.android.data;

public class ModuleItem {
	
	private String name;
	private String key;
	private String version;
	private String host;
	private boolean local;
	private boolean server;
	
	public ModuleItem(String name, String key, String host, String version, boolean local, boolean server) {
		this.name = name;
		this.key = key;
		this.host = host;
		this.version = version;
		this.local = local;
		this.server = server;
	}
	
	public String getName() {
		return name;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getHost() {
		return host;
	}
	
	public String getVersion() {
		return version;
	}
	
	public boolean isLocal() {
		return local;
	}
	
	public boolean isServer() {
		return server;
	}

	public void setServer(boolean server) {
		this.server = server;  
	}

	public String getDescription() {
		String description = "";
		if (version != null && !version.isEmpty()) {
			description += "Version: " + version + "   ";
		}
		if (host != null && !host.isEmpty()) {
			description += "Server: " + host;
		}
		return description;
	}
}
