package au.org.intersect.faims.android.data;

import java.io.Serializable;

import com.google.gson.JsonObject;

public class Project implements Serializable {

	private static final long serialVersionUID = 3601959914551529855L;

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
	
}
