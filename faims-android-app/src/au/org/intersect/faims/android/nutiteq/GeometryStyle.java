package au.org.intersect.faims.android.nutiteq;

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
	public int minZoom;
	
	public GeometryStyle(int minZoom) {
		this.minZoom = minZoom;
	}
	
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
		StyleSet<PointStyle> styleSet = new StyleSet<PointStyle>();
		styleSet.setZoomStyle(minZoom, toPointStyle());
		return styleSet;
	}

	public StyleSet<LineStyle> toLineStyleSet() {
		StyleSet<LineStyle> styleSet = new StyleSet<LineStyle>();
		styleSet.setZoomStyle(minZoom, toLineStyle());
		return styleSet;
	}

	public StyleSet<PolygonStyle> toPolygonStyleSet() {
		StyleSet<PolygonStyle> styleSet = new StyleSet<PolygonStyle>();
		styleSet.setZoomStyle(minZoom, toPolygonStyle());
		return styleSet;
	}
	
}
