package au.org.intersect.faims.android.data;

import java.io.Serializable;

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
}
