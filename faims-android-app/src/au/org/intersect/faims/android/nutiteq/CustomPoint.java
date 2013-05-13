package au.org.intersect.faims.android.nutiteq;

import au.org.intersect.faims.android.data.GeometryStyle;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Point;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.StyleSet;

public class CustomPoint extends Point {

	private int geomId;
	private GeometryStyle style;

	public CustomPoint(int geomId, GeometryStyle style, MapPos mapPos) {
		super(mapPos, null, createPointStyleSet(style), null);
		this.geomId = geomId;
		this.style = style;
	}

	public int getGeomId() {
		return geomId;
	}
	
	public GeometryStyle getStyle() {
		return style;
	}
	
	public static StyleSet<PointStyle> createPointStyleSet(GeometryStyle data) {
		StyleSet<PointStyle> pointStyleSet = new StyleSet<PointStyle>();
		PointStyle style = PointStyle.builder().setColor(data.pointColor).setSize(data.size).setPickingSize(data.pickingSize).build();
		pointStyleSet.setZoomStyle(0, style);
		return pointStyleSet;
	}

}
