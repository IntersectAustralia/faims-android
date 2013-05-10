package au.org.intersect.faims.android.nutiteq;

import java.util.List;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Line;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.Label;

public class CustomLine extends Line {

	private int geomId;

	public CustomLine(int geomId, List<MapPos> vertices, Label label, LineStyle lineStyle,
			Object userData) {
		super(vertices, label, lineStyle, userData);
		this.geomId = geomId;
	}
	
	public CustomLine(int geomId, List<MapPos> vertices, Label label,
			StyleSet<LineStyle> styleSet, Object userData) {
		super(vertices, label, styleSet, userData);
		this.geomId = geomId;
	}

	public int getGeomId() {
		return geomId;
	}

}
