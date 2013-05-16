package au.org.intersect.faims.android.data;

import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;

public class GeometryStyle {
	
	public int pointColor;
	public int lineColor;
	public int polygonColor;
	public float size;
	public float pickingSize;
	public float width;
	public float pickingWidth;
	public boolean showPoints;
	public boolean showStroke;
	
	public PointStyle toPointStyle() {
		return PointStyle.builder().setSize(size).setPickingSize(pickingSize).setColor(pointColor).build();
	}
	
	public LineStyle toLineStyle() {
		LineStyle.Builder<?> builder = LineStyle.builder().setWidth(width).setPickingWidth(pickingWidth).setColor(lineColor);
		if (showPoints) {
			builder.setPointStyle(toPointStyle());
		}
		return builder.build();
	}
	
	public PolygonStyle toPolygonStyle() {
		PolygonStyle.Builder<?> builder = PolygonStyle.builder().setColor(polygonColor);
		if (showStroke) {
			builder.setLineStyle(toLineStyle());
		}
		return builder.build();
	}
	
	public StyleSet<PointStyle> toPointStyleSet() {
		return new StyleSet<PointStyle>(toPointStyle());
	}

	public StyleSet<LineStyle> toLineStyleSet() {
		return new StyleSet<LineStyle>(toLineStyle());
	}

	public StyleSet<PolygonStyle> toPolygonStyleSet() {
		return new StyleSet<PolygonStyle>(toPolygonStyle());
	}
	
}
