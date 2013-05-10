package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import au.org.intersect.faims.android.log.FLog;

import com.nutiteq.components.Components;
import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.utils.Const;
import com.nutiteq.utils.Quadtree;
import com.nutiteq.vectorlayers.GeometryLayer;

public class CanvasLayer extends GeometryLayer {

	private String name;
	private Quadtree<Geometry> objects;
	private Stack<Geometry> geomBuffer;
	private int layerId;
	
	public CanvasLayer(int layerId, String name, Projection projection) {
		super(projection);
		this.name = name;
		this.layerId = layerId;
		objects = new Quadtree<Geometry>(Const.UNIT_SIZE / 10000.0);
		geomBuffer = new Stack<Geometry>();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String layerName) {
		this.name = layerName;
	}

	public int getLayerId() {
		return this.layerId;
	}
	
	@Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
		if (objects != null) {
			clearGeometryBuffer();
			
			List<Geometry> objList = objects.query(envelope);
			for (Geometry geom : objList) {
				geom.setActiveStyle(zoom);
			}
			setVisibleElementsList(objList);
		}
	}
	
	private void updateRenderList(Geometry geom) {
		List<Geometry> oldVisibleElementsList = getVisibleElements();
		List<Geometry> newVisibleElementsList = (oldVisibleElementsList != null ? new ArrayList<Geometry>(oldVisibleElementsList) : new ArrayList<Geometry>());
		newVisibleElementsList.add(geom);
		
		for (Geometry g : newVisibleElementsList) {
			g.setActiveStyle(0);
		}
		
		setVisibleElementsList(newVisibleElementsList); 
		
		// Update renderer
		Components components = getComponents();
		if (components != null) {
		  components.mapRenderers.getMapRenderer().frustumChanged();
		}
	}

	public CustomPoint addPoint(int geomId, MapPos point, StyleSet<PointStyle> styleSet) {		
		CustomPoint p = new CustomPoint(geomId, projection.fromWgs84(point.x, point.y), null, styleSet, null);
		addGeometry(p);
		return p;
	}
	
	public CustomLine addLine(int geomId, List<MapPos> points, StyleSet<LineStyle> styleSet) {
        List<MapPos> vertices = new ArrayList<MapPos>();
        for (MapPos p : points) {
        	vertices.add(projection.fromWgs84(p.x, p.y));
        }
		CustomLine l = new CustomLine(geomId, vertices, null, styleSet, null);
		addGeometry(l);
		return l;
	}
	
	public CustomPolygon addPolygon(int geomId, List<MapPos> points, StyleSet<PolygonStyle> styleSet) {		
		List<MapPos> vertices = new ArrayList<MapPos>();
        for (MapPos p : points) {
        	vertices.add(projection.fromWgs84(p.x, p.y));
        }
		CustomPolygon p = new CustomPolygon(geomId, vertices, new ArrayList<List<MapPos>>(), null, styleSet, null);
		
		addGeometry(p);
		return p;
	}
	
	public void addGeometry(Geometry geom) {
		geom.attachToLayer(this);
		
		objects.insert(geom.getInternalState().envelope, geom);
		
		updateRenderList(geom);
		
		FLog.d(geom.toString());
	}
	
	public void removeGeometry(Geometry geom) {
		objects.remove(geom.getInternalState().envelope, geom);
		
		geomBuffer.add(geom); // Issue with removing objects when object is still 
							  // in visible list so buffering objects to be removed later
	}
	
	private void clearGeometryBuffer() {
		while(geomBuffer.size() > 0) {
			Geometry geom = geomBuffer.pop();
			this.remove(geom);
		}
	}

	public List<Geometry> getGeometryList() {
		return objects.getAll();
	}
}
