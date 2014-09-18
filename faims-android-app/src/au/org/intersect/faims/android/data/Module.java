package au.org.intersect.faims.android.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.util.FileUtil;

public class Module implements Serializable {

	private static final long serialVersionUID = 3213578850852582553L;

	public String name;
	public String host;
	public String key;
	public String version;
	public String season;
	public String description;
	public String permitNo;
	public String permitHolder;
	public String contactAndAddress;
	public String participants;
	public String dbVersion;
	public String timestamp;
	public String fileSyncTimeStamp;
	public String srid;
	public String permitIssuedBy;
	public String permitType;
	public String copyrightHolder;
	public String clientSponsor;
	public String landOwner;
	public String hasSensitiveData;
	
	public Module() {
		
	}
	
	public Module(String name, String key) {
		this.name = name;
		this.key = key;
	}
	
	public File getDirectoryPath() {
		return getDirectoryPath(null);
	}
	
	public File getDirectoryPath(String path) {
		String directoryPath = Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + this.key;
		if (path != null) {
			directoryPath += '/' + path;
		}
		return new File(directoryPath);
	}
	

	public File getLocalPath(File uploadFile) {
		String fullpath = uploadFile.getAbsolutePath();
		String basepath = getDirectoryPath().getAbsolutePath() + '/';
		return new File(fullpath.replace(basepath, ""));
	}
	
	public String getCSS() {
		String cssFilePath = getDirectoryPath("style.css").getPath();
		return FileUtil.readFileIntoString(cssFilePath);
	}
	
	public static Module fromJson(JSONObject object) throws JSONException {
		Module p = new Module();
		if (object.has("name"))	p.name = object.getString("name");
		if (object.has("host"))	p.host = object.getString("host");
		if (object.has("key")) p.key = object.getString("key");
		if (object.has("version")) p.version = object.getString("version");
		if (object.has("season")) p.season = object.getString("season");
		if (object.has("description")) p.description = object.getString("description");
		if (object.has("permit_no")) p.permitNo = object.getString("permit_no");
		if (object.has("permit_holder")) p.permitHolder = object.getString("permit_holder");
		if (object.has("contact_address")) p.contactAndAddress = object.getString("contact_address");
		if (object.has("participant")) p.participants = object.getString("participant");
		if (object.has("dbVersion")) p.dbVersion = object.getString("dbVersion");
		if (object.has("timestamp")) p.timestamp = object.getString("timestamp");
		if (object.has("file_sync_timestamp")) p.fileSyncTimeStamp = object.getString("file_sync_timestamp");
		if (object.has("srid")) p.srid = object.getString("srid");
		if (object.has("permit_issued_by")) p.permitIssuedBy = object.getString("permit_issued_by");
		if (object.has("permit_type")) p.permitType = object.getString("permit_type");
		if (object.has("copyright_holder")) p.copyrightHolder = object.getString("copyright_holder");
		if (object.has("client_sponsor")) p.clientSponsor = object.getString("client_sponsor");
		if (object.has("land_owner")) p.landOwner = object.getString("land_owner");
		if (object.has("has_sensitive_data")) p.hasSensitiveData = object.getString("has_sensitive_data");
		return p;
	}

	public Object toJson() throws JSONException {
		JSONObject object = new JSONObject();
		if (name != null) object.put("name", name);
		if (host != null) object.put("host", host);
		if (key != null) object.put("key",  key);
		if (version != null) object.put("version", version);
		if (season != null) object.put("season", season);
		if (description != null) object.put("description", description);
		if (permitNo != null) object.put("permit_no", permitNo);
		if (permitHolder != null) object.put("permit_holder", permitHolder);
		if (contactAndAddress != null) object.put("contact_address", contactAndAddress);
		if (participants != null) object.put("participant", participants);
		if (dbVersion != null) object.put("dbVersion", dbVersion);
		if (timestamp != null) object.put("timestamp", timestamp);
		if (fileSyncTimeStamp != null) object.put("file_sync_timestamp", fileSyncTimeStamp);
		if (srid != null) object.put("srid", srid);
		if (permitIssuedBy != null) object.put("permit_issued_by", permitIssuedBy);
		if (permitType != null) object.put("permit_type", permitType);
		if (copyrightHolder != null) object.put("copyright_holder", copyrightHolder);
		if (clientSponsor != null) object.put("client_sponsor", clientSponsor);
		if (landOwner != null) object.put("land_owner", landOwner);
		if (hasSensitiveData != null) object.put("has_sensitive_data", hasSensitiveData);
		return object;	
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getSeason() {
		return season;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	public void setSeason(String season) {
		this.season = season;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPermitNo() {
		return permitNo;
	}

	public void setPermitNo(String permitNo) {
		this.permitNo = permitNo;
	}

	public String getPermitHolder() {
		return permitHolder;
	}

	public void setPermitHolder(String permitHolder) {
		this.permitHolder = permitHolder;
	}

	public String getContactAndAddress() {
		return contactAndAddress;
	}

	public void setContactAndAddress(String contactAndAddress) {
		this.contactAndAddress = contactAndAddress;
	}

	public String getParticipants() {
		return participants;
	}

	public void setParticipants(String participants) {
		this.participants = participants;
	}

	public String getSrid() {
		return srid;
	}
	
	public void setSrid(String srid) {
		this.srid = srid;
	}

	public String getPermitIssuedBy() {
		return permitIssuedBy;
	}

	public String getPermitType() {
		return permitType;
	}

	public String getCopyrightHolder() {
		return copyrightHolder;
	}

	public String getClientSponsor() {
		return clientSponsor;
	}

	public String getLandOwner() {
		return landOwner;
	}

	public String hasSensitiveData() {
		return hasSensitiveData;
	}

	public ArrayList<NameValuePair> getStaticData() {
		ArrayList<NameValuePair> data = new ArrayList<NameValuePair>();
		data.add(new NameValuePair("Module SRID", srid));
		data.add(new NameValuePair("Module Year", season));
		data.add(new NameValuePair("Permit No.", permitNo));
		data.add(new NameValuePair("Permit Holder", permitHolder));
		data.add(new NameValuePair("Permit Issued By", permitIssuedBy));
		data.add(new NameValuePair("Permit Type", permitType));
		data.add(new NameValuePair("Contact and Address", contactAndAddress));
		data.add(new NameValuePair("Participants", participants));
		data.add(new NameValuePair("Copyright Holder", copyrightHolder));
		data.add(new NameValuePair("Client/Sponsor", clientSponsor));
		data.add(new NameValuePair("Land Owner", landOwner));	
		data.add(new NameValuePair("Has Sensitive Data", hasSensitiveData));
		return data;
	}
}