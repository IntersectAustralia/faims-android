package au.org.intersect.faims.android.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.widget.DatePicker;
import android.widget.TimePicker;
import au.org.intersect.faims.android.log.FLog;

public class DateUtil {
	
	public static void setDatePicker(DatePicker date, String value) {
		String[] tokens = value.split("/");
		if (tokens.length == 3) {
			date.updateDate(Integer.valueOf(tokens[2]), Integer.valueOf(tokens[1]) - 1, Integer.valueOf(tokens[0]));
		} else {
			FLog.e("cannot set datepicker with value " + value);
		}
	}
	
	public static void setDatePicker(DatePicker date) {
		String currentDate = getCurrentTimestampGMT("dd/MM/yyyy");
		setDatePicker(date, currentDate);
	}
	
	public static void setTimePicker(TimePicker time, String value) {
		String[] tokens = value.split(":");
		if (tokens.length == 2) {
			time.setCurrentHour(Integer.valueOf(tokens[0]));
			time.setCurrentMinute(Integer.valueOf(tokens[1]));
		} else {
			FLog.e("cannot set timepicker with value " + value);
		}
	}
	
	public static void setTimePicker(TimePicker time) {
		String currentTime = getCurrentTimestampGMT("HH:mm");
		setTimePicker(time, currentTime);
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

	public static Date convertToDateGMT(String date){
		try{
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
			formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
			return formatter.parse(date);
		}catch(Exception e){
			FLog.e("Can not convert string to date", e);
			return null;
		}
	}
}
