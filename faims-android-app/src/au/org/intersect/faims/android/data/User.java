package au.org.intersect.faims.android.data;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import au.org.intersect.faims.android.log.FLog;

public class User implements Serializable {

	private static final long serialVersionUID = 1304366100190766914L;
	
	private String userId;
	private String firstName;
	private String lastName;
	private String email;
	
	public User(String userId, String firstName, String lastName, String email){
		this.userId = userId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}

	public String getUserId() {
		return userId;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}
	
	public String getEmail() {
		return email;
	}
	
	public static User loadUserFromJSON(JSONObject json) {
		User user = null;
		if (json.length() == 0) {
			return user;
		}
		try {
			String id = json.getString("userId");
			String firstname = json.getString("firstname");
			String lastname = json.getString("lastname");
			String email = json.getString("email");
			user = new User(id, firstname, lastname, email);
		} catch (JSONException e) {
			FLog.e("Couldn't load User from JSON", e);
		}
		return user;
	}
	
	public void saveToJSON(JSONObject json) {
		try {
			json.put("userId", userId);
			json.put("firstname", firstName);
			json.put("lastname", lastName);
			json.put("email", email);
		} catch (JSONException e) {
			FLog.e("Couldn't serialize User", e);
		}
	}
}
