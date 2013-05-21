package au.org.intersect.faims.android.nutiteq;

import au.org.intersect.faims.android.database.DatabaseManager;

import com.nutiteq.components.Envelope;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;

public class DatabaseLayer extends CanvasLayer {

	private DatabaseTextLayer textLayer;
	private boolean textVisible;
	private String query;
	private DatabaseManager dbmgr;
	private StyleSet<PointStyle> pointStyleSet;
	private StyleSet<LineStyle> lineStyleSet;
	private StyleSet<PolygonStyle> polygonStyle;
	private int maxObjects;
	private int minZoom;

	public DatabaseLayer(int layerId, String name, Projection projection, String query, DatabaseManager dbmgr,
			int maxObjects, StyleSet<PointStyle> pointStyleSet,
			StyleSet<LineStyle> lineStyleSet,
			StyleSet<PolygonStyle> polygonStyleSet) {
		super(layerId, name, projection);
		this.query = query;
		this.dbmgr = dbmgr;
		this.pointStyleSet = pointStyleSet;
	    this.lineStyleSet = lineStyleSet;
	    this.polygonStyle = polygonStyleSet;
	    this.maxObjects = maxObjects;
		if (pointStyleSet != null) {
	      minZoom = pointStyleSet.getFirstNonNullZoomStyleZoom();
	    }
	    if (lineStyleSet != null) {
	      minZoom = lineStyleSet.getFirstNonNullZoomStyleZoom();
	    }
	    if (polygonStyleSet != null) {
	      minZoom = polygonStyleSet.getFirstNonNullZoomStyleZoom();
	    }
	}
	
	public DatabaseTextLayer getTextLayer() {
		return textLayer;
	}

	public void setTextLayer(DatabaseTextLayer textLayer) {
		this.textLayer = textLayer;
		this.textVisible = textLayer.isVisible();
	}

	public boolean getTextVisible() {
		return textVisible;
	}
	
	public void setTextVisible(boolean visible) {
		this.textVisible = visible;
		updateTextLayer();
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		updateTextLayer();
	}
	
	private void updateTextLayer() {
		if (this.isVisible() && this.textVisible) {
			this.textLayer.setVisible(true);
		} else {
			this.textLayer.setVisible(false);
		}
	}
	
	@Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
		if (zoom < minZoom) {
	        setVisibleElementsList(null);
	        return;
	    }
	}

}
