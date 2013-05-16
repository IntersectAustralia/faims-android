package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;

import au.org.intersect.faims.android.data.GeometryStyle;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;

public class CustomPolygon extends Polygon {

	private int geomId;
	private GeometryStyle style;

	public CustomPolygon(int geomId, GeometryStyle style, List<MapPos> vertices) {
		super(vertices, new ArrayList<List<MapPos>>(), null, createPolygonStyle(style), null);
		this.geomId = geomId;
		this.style = style;
	}

	public int getGeomId() {
		return geomId;
	}
	
	public GeometryStyle getStyle() {
		return style;
	}
	
	public static StyleSet<PolygonStyle> createPolygonStyle(GeometryStyle style) {
		StyleSet<PolygonStyle> polygonStyleSet = new StyleSet<PolygonStyle>();
		PolygonStyle polygonStyle;
		if (style.showStroke) {
			polygonStyle = PolygonStyle.builder().setColor(style.polygonColor).setLineStyle(createLineStyle(style)).build();
		} else {
			polygonStyle = PolygonStyle.builder().setColor(style.polygonColor).build();
		}
		polygonStyleSet.setZoomStyle(0, polygonStyle);
		return polygonStyleSet;
	}
	
	private static LineStyle createLineStyle(GeometryStyle style) {
		if (style.showPoints) {
			return LineStyle.builder().setColor(style.lineColor).setWidth(style.width).setPickingWidth(style.pickingWidth).setPointStyle(createPointStyle(style)).build();
		} else {
			return LineStyle.builder().setColor(style.lineColor).setWidth(style.width).setPickingWidth(style.pickingWidth).build();
		}
	}
	
	private static PointStyle createPointStyle(GeometryStyle style) {
		return PointStyle.builder().setColor(style.pointColor).setSize(style.size).setPickingSize(style.pickingSize).build();
	}
	
	public List<MapPos> getPosList() {
		return GeometryUtil.convertToWgs84(this.getVertexList());
	}

}
