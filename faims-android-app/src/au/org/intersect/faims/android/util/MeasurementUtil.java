package au.org.intersect.faims.android.util;

import java.text.DecimalFormat;

public class MeasurementUtil {
	
	public static String convertToDegrees(double value) {
		double degrees = value < 0 ? Math.ceil(value) : Math.floor(value);
		double totalSeconds = Math.floor(Math.abs(value - degrees) * 3600);
		double minutes = Math.floor((totalSeconds / 60));
		double seconds = totalSeconds - (minutes * 60);
		return Integer.toString((int) degrees) + ":" + toFixed((int) minutes, 2) + ":" + toFixed((int) seconds, 2);  
	}
	
	private static String toFixed(int value, int n) {
		String s = Integer.toString(value);
		while(s.length() < n) {
			s = "0" + s;
		}
		return s;
	}
	
	public static String displayAsCoord(double value) {
		return new DecimalFormat("#.0000").format(value);
	}
	
	public static String displayAsMeters(double value) {
		return new DecimalFormat("###,###,###,###.0000").format(value) + "m";
	}
	
	public static String displayAsKiloMeters(double value) {
		return new DecimalFormat("###,###,###,###.0000").format(value) + "km";
	}

	public static String displayAsDegrees(float value) {
		return new DecimalFormat("###.0000").format(value % 360) + "¡";
	}

}
