package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.util.SparseArray;
import au.org.intersect.faims.android.log.FLog;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.Style;
import com.nutiteq.style.StyleSet;
import com.nutiteq.utils.Const;
import com.nutiteq.utils.Quadtree;
import com.nutiteq.vectorlayers.GeometryLayer;

public class CanvasLayer extends GeometryLayer {

	private String name;
	private Quadtree<Geometry> objects;
	private SparseArray<Geometry> objectMap;
	private static int geomId = 1;
	private Stack<Geometry> geomBuffer;
	
	public CanvasLayer(String name, Projection projection) {
		super(projection);
		this.name = name;
		objects = new Quadtree<Geometry>(Const.UNIT_SIZE / 10000.0);
		objectMap = new SparseArray<Geometry>();
		geomBuffer = new Stack<Geometry>();
	}
	
	public CanvasLayer(Projection projection) {
		this(null, projection);
	}
	
	public String getName() {
		return name;
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
	
	public int addPoint(MapPos point, StyleSet<PointStyle> styleSet) {
		return addPoint(point, styleSet, geomId++);
	}

	public int addPoint(MapPos point, StyleSet<PointStyle> styleSet, int id) {		
		Point p = new Point(projection.fromWgs84(point.x, point.y), null, styleSet, null);
		p.attachToLayer(this);
		
		objects.insert(p.getInternalState().envelope, p);
		
		objectMap.put(id, p);
		
		FLog.d(p.toString());
		
		return id;
	}
	
	public int addLine(List<MapPos> points, StyleSet<LineStyle> styleSet) {
		return addLine(points, styleSet, geomId++);
	}
	
	public int addLine(List<MapPos> points, StyleSet<LineStyle> styleSet, int id) {
        List<MapPos> vertices = new ArrayList<MapPos>();
        for (MapPos p : points) {
        	vertices.add(projection.fromWgs84(p.x, p.y));
        }
		Line l = new Line(vertices, null, styleSet, null);
		l.attachToLayer(this);
		
		objects.insert(l.getInternalState().envelope, l);
		
		objectMap.put(id, l);
		
		FLog.d(l.toString());
		
		return id;
	}

	public int addPolygon(List<MapPos> points, StyleSet<PolygonStyle> styleSet) {
		return addPolygon(points, styleSet, geomId++);
	}
	
	public int addPolygon(List<MapPos> points, StyleSet<PolygonStyle> styleSet, int id) {		
		List<MapPos> vertices = new ArrayList<MapPos>();
        for (MapPos p : points) {
        	vertices.add(projection.fromWgs84(p.x, p.y));
        }
		Polygon p = new Polygon(vertices, new ArrayList<List<MapPos>>(), null, styleSet, null);
		p.attachToLayer(this);
		
		objects.insert(p.getInternalState().envelope, p);
		
		objectMap.put(id, p);
		
		FLog.d(p.toString());
		
		return id;
	}
	
	public void removeGeometry(int geomId) {
		Geometry geom = objectMap.get(geomId);
		
		objects.remove(geom.getInternalState().envelope, geom);
		
		objectMap.remove(geomId);
		
		geomBuffer.add(geom); // Issue with removing objects when object is still 
							  // in visible list so buffering objects to be removed later
	}
	
	private void clearGeometryBuffer() {
		while(geomBuffer.size() > 0) {
			Geometry geom = geomBuffer.pop();
			this.remove(geom);
		}
	}
	
	public void updateRenderer() {
		if (components != null) {
			components.mapRenderers.getMapRenderer().frustumChanged();
		}
	}
	
	public Geometry getTransformedGeometry(int geomId) {
		return transformGeometry(objectMap.get(geomId));
	}

	public List<Geometry> getTransformedGeometryList() {
		return transformGeometryList(objects.getAll());
	}
	
	private Geometry transformGeometry(Geometry geom) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(transformVertex(p.getMapPos()), null, (StyleSet<PointStyle>) p.getStyleSet(), null);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(transformVertices(l.getVertexList()), null, (StyleSet<LineStyle>) l.getStyleSet(), null);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(transformVertices(p.getVertexList()), new ArrayList<List<MapPos>>(), null, (StyleSet<PolygonStyle>) p.getStyleSet(), null);
		}
		return null;
	}
	
	private List<Geometry> transformGeometryList(List<Geometry> geomList) {
		if (geomList.size() == 0) return null;
		
		List<Geometry> newGeomList = new ArrayList<Geometry>();
		for (Geometry geom : geomList) {
			newGeomList.add(transformGeometry(geom));
		}
		return newGeomList;
	}
	
	private MapPos transformVertex(MapPos v) {
		return projection.toWgs84(v.x, v.y);
	}
	
	private List<MapPos> transformVertices(List<MapPos> vertices) {
		List<MapPos> newVertices = new ArrayList<MapPos>();
		for (MapPos v : vertices) {
			newVertices.add(projection.toWgs84(v.x, v.y));
		}
		return newVertices;
	}

	public int getGeometryId(Geometry geometry) {
		for(int i = 0; i < objectMap.size(); i++) {
		   int key = objectMap.keyAt(i);
		   Geometry object = objectMap.get(key);
		   if (object == geometry)
			   return key;
		}
		return 0;
	}
	
	public Geometry getGeometry(int geomId) {
		return objectMap.get(geomId);
	}
	
	public int addGeometry(Geometry geom, StyleSet<? extends Style> styleSet) {
		return addGeometry(geom, styleSet, geomId++);
	}

	@SuppressWarnings("unchecked")
	public int addGeometry(Geometry geom, StyleSet<? extends Style> styleSet, int id) {
		if (geom instanceof Point) {
			return addPoint(((Point) geom).getMapPos(), (StyleSet<PointStyle>) styleSet, id);
		} else if  (geom instanceof Line) {
			return addLine(((Line) geom).getVertexList(), (StyleSet<LineStyle>) styleSet, id);
		} else if (geom instanceof Polygon) {
			return addPolygon(((Polygon) geom).getVertexList(), (StyleSet<PolygonStyle>) styleSet, id);
		}
		return 0;
	}

	public void replaceGeometry(int geomId, Geometry geom) {
		removeGeometry(geomId);
		
		geom.attachToLayer(this);
		
		objects.insert(geom.getInternalState().envelope, geom);
		
		objectMap.put(geomId, geom);
	}

	public void setName(String layerName) {
		this.name = layerName;
	}

}
