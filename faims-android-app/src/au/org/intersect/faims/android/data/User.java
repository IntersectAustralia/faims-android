package au.org.intersect.faims.android.data;

public class User {

	private String userId;
	
	private String firstName;
	
	private String lastName;

	public User(String userId, String firstName, String lastName){
		this.userId = userId;
		this.firstName = firstName;
		this.lastName = lastName;
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
}
