package au.org.intersect.faims.android.nutiteq;

import java.util.List;

import au.org.intersect.faims.android.data.GeometryStyle;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Line;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.StyleSet;

public class CustomLine extends Line {

	private int geomId;
	private GeometryStyle style;

	public CustomLine(int geomId, GeometryStyle style, List<MapPos> vertices) {
		super(vertices, null, createLineStyleSet(style), null);
		this.geomId = geomId;
		this.style = style;
	}

	public int getGeomId() {
		return geomId;
	}
	
	public GeometryStyle getStyle() {
		return style;
	}
	
	public static StyleSet<LineStyle> createLineStyleSet(GeometryStyle style) {
		StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>();
		LineStyle lineStyle;
		if (style.showPoints) {
			lineStyle = LineStyle.builder().setColor(style.lineColor).setWidth(style.width).setPickingWidth(style.pickingWidth).setPointStyle(createPointStyle(style)).build();
		} else {
			lineStyle = LineStyle.builder().setColor(style.lineColor).setWidth(style.width).setPickingWidth(style.pickingWidth).build();
		}
		lineStyleSet.setZoomStyle(0, lineStyle);
		return lineStyleSet;
	}
	
	private static PointStyle createPointStyle(GeometryStyle style) {
		return PointStyle.builder().setColor(style.pointColor).setSize(style.size).setPickingSize(style.pickingSize).build();
	}
	
	public List<MapPos> getPosList() {
		return GeometryUtil.convertToWgs84(this.getVertexList());
	}

}
