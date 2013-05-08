package au.org.intersect.faims.android.nutiteq;

import java.io.IOException;

import com.nutiteq.layers.vector.OgrLayer;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;

public class CustomOgrLayer extends OgrLayer {

	private String name;

	public CustomOgrLayer(String name, Projection proj, String fileName, String tableName,
			int maxObjects, StyleSet<PointStyle> pointStyleSet,
			StyleSet<LineStyle> lineStyleSet,
			StyleSet<PolygonStyle> polygonStyleSet) throws IOException {
		super(proj, fileName, tableName, maxObjects, pointStyleSet, lineStyleSet,
				polygonStyleSet);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

}
