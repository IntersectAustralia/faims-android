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
	public String dir;
	
	public Project() {
		
	}
	
	public Project(String dir) {
		this.dir = dir;
	}
	
	public Project(String dir, String name, String key) {
		this.name = name;
		this.key = key;
	}
	
	public static Project fromJson(String dir, JsonObject object) {
		Project p = new Project(dir);
		if (object.has("name"))	p.name = object.get("name").getAsString();
		if (object.has("key")) p.key = object.get("key").getAsString();
		if (object.has("season")) p.season = object.get("season").getAsString();
		if (object.has("description")) p.description = object.get("description").getAsString();
		if (object.has("permit_no")) p.permitNo = object.get("permit_no").getAsString();
		if (object.has("permit_holder")) p.permitHolder = object.get("permit_holder").getAsString();
		if (object.has("contact_address")) p.contactAndAddress = object.get("contact_address").getAsString();
		if (object.has("participant")) p.participants = object.get("participant").getAsString();
		return p;
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
		if (p.name != null) p.dir = p.name.replace("\\s+", "_");
		return p;
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
	
}
