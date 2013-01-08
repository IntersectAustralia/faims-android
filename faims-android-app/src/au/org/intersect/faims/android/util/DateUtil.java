package au.org.intersect.faims.android.util;

import android.widget.DatePicker;
import android.widget.TimePicker;

public class DateUtil {
	
	public static void setDatePicker(DatePicker date, String value) {
		String[] tokens = value.split("/");
		date.updateDate(Integer.valueOf(tokens[2]), Integer.valueOf(tokens[1]) - 1, Integer.valueOf(tokens[0]));
	}
	
	public static void setTimePicker(TimePicker time, String value) {
		String[] tokens = value.split(":");
		time.setCurrentHour(Integer.valueOf(tokens[1]));
		time.setCurrentMinute(Integer.valueOf(tokens[0]));
	}
	
	public static String getDate(DatePicker date) {
		return "" + date.getDayOfMonth() + "/" + (date.getMonth() + 1) + "/" + date.getYear();
	}
	
	public static String getTime(TimePicker time) {
		return "" + time.getCurrentHour() + ":" + time.getCurrentMinute();
	}

}
