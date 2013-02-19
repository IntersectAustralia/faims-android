package au.org.intersect.faims.android.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.widget.DatePicker;
import android.widget.TimePicker;

public class DateUtil {
	
	public static void setDatePicker(DatePicker date, String value) {
		String[] tokens = value.split("/");
		date.updateDate(Integer.valueOf(tokens[2]), Integer.valueOf(tokens[1]) - 1, Integer.valueOf(tokens[0]));
	}
	
	public static void setTimePicker(TimePicker time, String value) {
		String[] tokens = value.split(":");
		time.setCurrentHour(Integer.valueOf(tokens[0]));
		time.setCurrentMinute(Integer.valueOf(tokens[1]));
	}
	
	public static String getDate(DatePicker date) {
		return "" + toFixed(date.getDayOfMonth(), 2) + "/" + toFixed((date.getMonth() + 1), 2) + "/" + date.getYear();
	}
	
	public static String getTime(TimePicker time) {
		return "" + toFixed(time.getCurrentHour(),2) + ":" + toFixed(time.getCurrentMinute(),2);
	}
	
	public static String toFixed(int i, int length) {
		String s = String.valueOf(i);
		while (s.length() < length) {
			s = "0" + s;
		}
		return s;
	}
	
	public static String getCurrentTimestampGMT(String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.ENGLISH);
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		return formatter.format(new Date());
	}
	
	public static String getCurrentTimestampGMT() {
		return getCurrentTimestampGMT("yyyy-MM-dd HH:mm:ss");
	}

}
