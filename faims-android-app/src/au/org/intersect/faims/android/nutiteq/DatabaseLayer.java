package au.org.intersect.faims.android.nutiteq;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.form.ArchEntity;
import au.org.intersect.faims.android.ui.form.Relationship;

import com.nutiteq.components.Envelope;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;

public class DatabaseLayer extends CanvasLayer {
	
	public enum Type {
		ENTITY,
		RELATIONSHIP
	}

	private DatabaseTextLayer textLayer;
	private boolean textVisible;
	private String query;
	private DatabaseManager dbmgr;
	private StyleSet<PointStyle> pointStyleSet;
	private StyleSet<LineStyle> lineStyleSet;
	private StyleSet<PolygonStyle> polygonStyle;
	private int maxObjects;
	private int minZoom;
	private Type type;

	public DatabaseLayer(int layerId, String name, Projection projection, Type type, String query, DatabaseManager dbmgr,
			int maxObjects, StyleSet<PointStyle> pointStyleSet,
			StyleSet<LineStyle> lineStyleSet,
			StyleSet<PolygonStyle> polygonStyleSet) {
		super(layerId, name, projection);
		this.query = query;
		this.type = type;
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
		
		try {
			Vector<Geometry> objectTemp = new Vector<Geometry>();
			Vector<Geometry> objects = new Vector<Geometry>();
			
			if (type == Type.ENTITY) {
				Collection<List<String>> results = dbmgr.fetchEntityList(null);
				for (List<String> r : results) {
					String uuid = r.get(0);
					ArchEntity entity = (ArchEntity) dbmgr.fetchArchEnt(uuid);
					objectTemp.addAll(entity.getGeometryList());
				}
			} else if (type == Type.RELATIONSHIP) {
				Collection<List<String>> results = dbmgr.fetchRelationshipList(null);
				for (List<String> r : results) {
					String relationshipid = r.get(0);
					Relationship rel = (Relationship) dbmgr.fetchRel(relationshipid);
					objectTemp.addAll(rel.getGeometryList());
				}
			} else {
				throw new Exception("database layer has no type");
			}
			
		    // apply styles, create new objects for these
		    for(Geometry object: objectTemp){
		        
		        Geometry newObject = null;
		        
		        if(object instanceof Point){
		            newObject = new Point(((Point) object).getMapPos(), null, pointStyleSet, object.userData);
		        }else if(object instanceof Line){
		            newObject = new Line(((Line) object).getVertexList(), null, lineStyleSet, object.userData);
		        }else if(object instanceof Polygon){
		            newObject = new Polygon(((Polygon) object).getVertexList(), ((Polygon) object).getHolePolygonList(), null, polygonStyle, object.userData);
		        }
		        
		        newObject.attachToLayer(this);
		        newObject.setActiveStyle(zoom);
		        
		        objects.add(newObject);
		    }
		    
		    setVisibleElementsList(objects);
		} catch (Exception e) {
			FLog.e("error rendering database layer", e);
		}
	}

}
