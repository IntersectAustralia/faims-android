package au.org.intersect.faims.android.data;

import java.io.Serializable;

import com.google.gson.JsonObject;

public class Project implements Serializable {

	private static final long serialVersionUID = 3213578850852582553L;

	public String name;
	public String key;
	public String season;
	public String description;
	public String permitNo;
	public String permitHolder;
	public String contactAndAddress;
	public String participants;
	public String version;
	public String timestamp;
	public String srid;
	public String permitIssuedBy;
	public String permitType;
	public String copyrightHolder;
	public String clientSponsor;
	public String landOwner;
	public String hasSensitiveData;
	
	public Project() {
		
	}
	
	public Project(String name, String key) {
		this.name = name;
		this.key = key;
	}
	
	public static Project fromJson(JsonObject object) {
		Project p = new Project();
		if (object.has("name"))	p.name = object.get("name").getAsString();
		if (object.has("key")) p.key = object.get("key").getAsString();
		if (object.has("season")) p.season = object.get("season").getAsString();
		if (object.has("description")) p.description = object.get("description").getAsString();
		if (object.has("permit_no")) p.permitNo = object.get("permit_no").getAsString();
		if (object.has("permit_holder")) p.permitHolder = object.get("permit_holder").getAsString();
		if (object.has("contact_address")) p.contactAndAddress = object.get("contact_address").getAsString();
		if (object.has("participant")) p.participants = object.get("participant").getAsString();
		if (object.has("version")) p.version = object.get("version").getAsString();
		if (object.has("timestamp")) p.timestamp = object.get("timestamp").getAsString();
		if (object.has("srid")) p.srid = object.get("srid").getAsString();
		if (object.has("permit_issued_by")) p.permitIssuedBy = object.get("permit_issued_by").getAsString();
		if (object.has("permit_type")) p.permitType = object.get("permit_type").getAsString();
		if (object.has("copyright_holder")) p.copyrightHolder = object.get("copyright_holder").getAsString();
		if (object.has("client_sponsor")) p.clientSponsor = object.get("client_sponsor").getAsString();
		if (object.has("land_owner")) p.landOwner = object.get("land_owner").getAsString();
		if (object.has("has_sensitive_data")){ p.hasSensitiveData = object.get("has_sensitive_data").isJsonNull() ? null : object.get("has_sensitive_data").getAsString();}
		return p;
	}

	public Object toJson() {
		JsonObject object = new JsonObject();
		if (name != null) object.addProperty("name", name);
		if (key != null) object.addProperty("key",  key);
		if (season != null) object.addProperty("season", season);
		if (description != null) object.addProperty("description", description);
		if (permitNo != null) object.addProperty("permit_no", permitNo);
		if (permitHolder != null) object.addProperty("permit_holder", permitHolder);
		if (contactAndAddress != null) object.addProperty("contact_address", contactAndAddress);
		if (participants != null) object.addProperty("participant", participants);
		if (version != null) object.addProperty("version", version);
		if (timestamp != null) object.addProperty("timestamp", timestamp);
		if (srid != null) object.addProperty("srid", srid);
		if (permitIssuedBy != null) object.addProperty("permit_issued_by", permitIssuedBy);
		if (permitType != null) object.addProperty("permit_type", permitType);
		if (copyrightHolder != null) object.addProperty("copyright_holder", copyrightHolder);
		if (clientSponsor != null) object.addProperty("client_sponsor", clientSponsor);
		if (landOwner != null) object.addProperty("land_owner", landOwner);
		if (hasSensitiveData != null) object.addProperty("has_sensitive_data", hasSensitiveData);
		return object;	
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
	
}
