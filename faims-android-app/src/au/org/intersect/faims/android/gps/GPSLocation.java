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

	public double getLatitude() {
		return this.latitude;
	}

	public long getTimeStamp() {
		return this.timeStamp;
	}
	
	public String toString() {
		return "(" + longitude + "," + latitude + ")";
	}
}
