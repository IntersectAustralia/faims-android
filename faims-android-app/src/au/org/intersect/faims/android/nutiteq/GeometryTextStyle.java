package au.org.intersect.faims.android.nutiteq;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;
import android.graphics.Typeface;
import au.org.intersect.faims.android.log.FLog;

import com.nutiteq.style.StyleSet;
import com.nutiteq.style.TextStyle;

public class GeometryTextStyle {
	
	public int color;
	public int size;
	public Typeface font;
	public int minZoom;
	
	public GeometryTextStyle(int minZoom) {
		this.minZoom = minZoom;
	}
	
	public TextStyle toStyle() {
		return TextStyle.builder().setColor(color).setSize(size).setFont(font).build();
	}
	
	public StyleSet<TextStyle> toStyleSet() {
		StyleSet<TextStyle> styleSet = new StyleSet<TextStyle>();
		styleSet.setZoomStyle(minZoom, toStyle());
		return styleSet;
	}

	public static GeometryTextStyle defaultStyle() {
		GeometryTextStyle textStyle = new GeometryTextStyle(12);
		textStyle.color = Color.WHITE;
		textStyle.size = 40;
		textStyle.font = Typeface.DEFAULT;
		return textStyle;
	}

	public static GeometryTextStyle loadGeometryStyleFromJSON(JSONObject json) {
		GeometryTextStyle style = null;
		if (json.length() == 0) {
			return style;
		}
		try {
			style = new GeometryTextStyle(json.getInt("minZoom"));
			style.color = json.getInt("color");
			style.size = json.getInt("size");
			style.font = Typeface.create((String) null, json.getInt("font"));
		} catch (JSONException e) {
			FLog.e("Couldn't load GeometryStyle from JSON", e);
		}
		return style;
	}
	
	public void saveToJSON(JSONObject json) {
		try {
			json.put("color", color);
			json.put("size", size);
			json.put("font", font.getStyle());
			json.put("minZoom", minZoom);
		} catch (JSONException e) {
			FLog.e("Couldn't serialize GeometryTextStyle", e);
		}
	}

}
