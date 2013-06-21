package au.org.intersect.faims.android.gps;

public class GPSLocation {

	private double longitude;
	private double latitude;
	private long timeStamp;

	public GPSLocation(double longitude, double latitude, long timeStamp){
		this.longitude = longitude;
		this.latitude = latitude;
		this.timeStamp = timeStamp;
	}

	public double getLongitude() {
		return this.longitude;
	}
	
	public void setLongitude(double value) {
		this.longitude = value;
	}

	public double getLatitude() {
		return this.latitude;
	}
	
	public void setLatitude(double value) {
		this.latitude = value;
	}

	public long getTimeStamp() {
		return this.timeStamp;
	}
	
	public String toString() {
		return "(" + longitude + "," + latitude + ")";
	}
}
