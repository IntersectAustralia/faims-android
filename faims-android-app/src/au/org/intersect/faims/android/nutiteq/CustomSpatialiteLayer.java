package au.org.intersect.faims.android.nutiteq;

import com.nutiteq.layers.vector.SpatialiteLayer;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;

public class CustomSpatialiteLayer extends SpatialiteLayer {

	private String name;

	public CustomSpatialiteLayer(String name, Projection proj, String dbPath,
			String tableName, String geomColumnName, String[] userColumns,
			int maxObjects, StyleSet<PointStyle> pointStyleSet,
			StyleSet<LineStyle> lineStyleSet,
			StyleSet<PolygonStyle> polygonStyleSet) {
		super(proj, dbPath, tableName, geomColumnName, userColumns, maxObjects,
				pointStyleSet, lineStyleSet, polygonStyleSet);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String layerName) {
		this.name = layerName;
	}

}
