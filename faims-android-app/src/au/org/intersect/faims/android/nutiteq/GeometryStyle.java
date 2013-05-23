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

	public GeometryStyle cloneStyle() {
		GeometryStyle g = new GeometryStyle(minZoom);
		g.pointColor = pointColor;
		g.lineColor = lineColor;
		g.polygonColor = polygonColor;
		g.size = size;
		g.pickingSize = pickingSize;
		g.width = width;
		g.pickingWidth = pickingWidth;
		g.showPoints = showPoints;
		g.showStroke = showStroke;
		return g;
	}
	
	public static GeometryStyle defaultPointStyle() {
		GeometryStyle style = new GeometryStyle(12);
		style.pointColor = 0xAAFF0000;
		style.size = 0.2f;
		style.pickingSize = 0.6f;
		return style;
	}
	
	public static GeometryStyle defaultLineStyle() {
		GeometryStyle style = new GeometryStyle(12);
		style.pointColor = 0xAA00FF00;
		style.lineColor = 0xAA00FF00;
		style.size = 0.2f;
		style.pickingSize = 0.6f;
		style.width = 0.05f;
		style.pickingWidth = 0.3f;
		style.showPoints = false;
		return style;
	}
	
	public static GeometryStyle defaultPolygonStyle() {
		GeometryStyle style = new GeometryStyle(12);
		style.pointColor = 0XAA0000FF;
		style.lineColor = 0XAA0000FF;
		style.polygonColor = 0x440000FF;
		style.size = 0.2f;
		style.pickingSize = 0.6f;
		style.width = 0.05f;
		style.pickingWidth = 0.3f;
		style.showPoints = false;
		style.showStroke = true;
		return style;
	}
	
}
