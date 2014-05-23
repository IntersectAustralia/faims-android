package au.org.intersect.faims.android.util;

import android.location.Location;

import com.nutiteq.components.MapPos;

public class SpatialiteUtil {
	
	public static float computeAngleBetween(MapPos v1, MapPos v2) {
		float angle = (float) (Math.acos(dot(v1, v2) / (length(v1) * length(v2))) * 180 / Math.PI);
		if (v2.x < 0) return -angle;
		return angle;
	}
	
	public static  float dot(MapPos p1, MapPos p2) {
		return (float) (p1.x * p2.x + p1.y * p2.y);
	}
	
	public static float length(MapPos p) {
		return (float) Math.sqrt(p.x * p.x + p.y * p.y);
	}
	
	public static float computeAzimuth(MapPos p1, MapPos p2) {
		Location l1 = new Location("");
		l1.setLatitude(p1.y);
		l1.setLongitude(p1.x);
		
		Location l2 = new Location("");
		l2.setLatitude(p2.y);
		l2.setLongitude(p2.x);
		
		return (l1.bearingTo(l2) + 360) % 360;
	}
	
}
