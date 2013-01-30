package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;
import au.org.intersect.faims.android.R;

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
import com.nutiteq.style.StyleSet;
import com.nutiteq.utils.Const;
import com.nutiteq.utils.Quadtree;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.GeometryLayer;

public class CanvasLayer extends GeometryLayer {

	private Quadtree<Geometry> objects;
	private Activity activity;
	private SparseArray<Geometry> objectMap;
	private static int geomId = 1;
	private Stack<Geometry> geomBuffer;
	
	public CanvasLayer(Activity activity, Projection projection) {
		super(projection);
		this.activity = activity;
		objects = new Quadtree<Geometry>(Const.UNIT_SIZE / 10000.0);
		objectMap = new SparseArray<Geometry>();
		geomBuffer = new Stack<Geometry>();
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
	
	public int addPoint(MapPos point, int color) {
		return addPoint(point, color, geomId++);
	}

	public int addPoint(MapPos point, int color, int id) {
		StyleSet<PointStyle> pointStyleSet = new StyleSet<PointStyle>();
		Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(activity.getResources(), R.drawable.point);
		PointStyle pointStyle = PointStyle.builder().setBitmap(pointMarker).setSize(0.1f).setColor(color).setPickingSize(0.3f).build();
		pointStyleSet.setZoomStyle(0, pointStyle);
		
		Point p = new Point(projection.fromWgs84(point.x, point.y), null, pointStyleSet, null);
		p.attachToLayer(this);
		
		objects.insert(p.getInternalState().envelope, p);
		
		objectMap.put(id, p);
		
		Log.d("FAIMS", p.toString());
		
		return id;
	}
	
	public void removeGeometry(int geomId) {
		Geometry geom = objectMap.get(geomId);
		
		objects.remove(geom.getInternalState().envelope, geom);
		
		objectMap.remove(geomId);
		
		geomBuffer.add(geom); // Issue with removing objects when object is still in visible list so buffering objects to be removed later
		
		Log.d("FAIMS", "Removed: " + geom.toString());
	}
	
	private void clearGeometryBuffer() {
		while(geomBuffer.size() > 0) {
			Geometry geom = geomBuffer.pop();
			this.remove(geom);
			
			Log.d("FAIMS", "Cleared: " + geom.toString());
		}
	}
	
	public void updateRenderer() {

		if (components != null) {
			components.mapRenderers.getMapRenderer().frustumChanged();
		}
	}
	
	public int addLine(List<MapPos> points, int color) {
		return addLine(points, color, geomId++);
	}
	
	public int addLine(List<MapPos> points, int color, int id) {
		StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>();
        lineStyleSet.setZoomStyle(0, LineStyle.builder().setWidth(0.1f).setColor(color).setPickingWidth(0.3f).build());
        
        List<MapPos> vertices = new ArrayList<MapPos>();
        for (MapPos p : points) {
        	vertices.add(projection.fromWgs84(p.x, p.y));
        }
		Line l = new Line(vertices, null, lineStyleSet, null);
		l.attachToLayer(this);
		
		objects.insert(l.getInternalState().envelope, l);
		
		objectMap.put(id, l);
		
		Log.d("FAIMS", l.toString());
		
		return id;
	}

	public int addPolygon(List<MapPos> points, int color) {
		return addPolygon(points, color, geomId++);
	}
	
	public int addPolygon(List<MapPos> points, int color, int id) {
		PolygonStyle polygonStyle = PolygonStyle.builder().setColor(color).build();
        StyleSet<PolygonStyle> polygonStyleSet = new StyleSet<PolygonStyle>(null);
		polygonStyleSet.setZoomStyle(0, polygonStyle);
		
		List<MapPos> vertices = new ArrayList<MapPos>();
        for (MapPos p : points) {
        	vertices.add(projection.fromWgs84(p.x, p.y));
        }
		Polygon p = new Polygon(vertices, new ArrayList<List<MapPos>>(), null, polygonStyle, null);
		p.attachToLayer(this);
		
		objects.insert(p.getInternalState().envelope, p);
		
		objectMap.put(id, p);
		
		Log.d("FAIMS", p.toString());
		
		return id;
	}

	public List<Geometry> getGeometryList() {
		return transformGeometryList(objects.getAll());
	}
	
	@SuppressWarnings("unchecked")
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
	
	public int addGeometry(Geometry geom, int color) {
		return addGeometry(geom, color, geomId++);
	}

	public int addGeometry(Geometry geom, int color, int id) {
		if (geom instanceof Point) {
			return addPoint(((Point) geom).getMapPos(), color, id);
		} else if  (geom instanceof Line) {
			return addLine(((Line) geom).getVertexList(), color, id);
		} else if (geom instanceof Polygon) {
			return addPolygon(((Polygon) geom).getVertexList(), color, id);
		}
		return 0;
	}

	public void replaceGeometry(int geomId, Geometry geom) {
		removeGeometry(geomId);
		
		geom.attachToLayer(this);
		
		objects.insert(geom.getInternalState().envelope, geom);
		
		objectMap.put(geomId, geom);
		
		Log.d("FAIMS", geom.toString());
		
	}

}
