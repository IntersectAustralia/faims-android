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
	private int geomId = 1;
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
		StyleSet<PointStyle> pointStyleSet = new StyleSet<PointStyle>();
		Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(activity.getResources(), R.drawable.point);
		PointStyle pointStyle = PointStyle.builder().setBitmap(pointMarker).setSize(0.1f).setColor(color).build();
		pointStyleSet.setZoomStyle(0, pointStyle);
		
		Point p = new Point(projection.fromWgs84(point.x, point.y), null, pointStyleSet, null);
		p.attachToLayer(this);
		
		objects.insert(p.getInternalState().envelope, p);
		
		objectMap.put(geomId, p);
		
		Log.d("FAIMS", p.toString());
		
		return geomId++;
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
		StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>();
        lineStyleSet.setZoomStyle(0, LineStyle.builder().setWidth(0.1f).setColor(color).build());
        
        List<MapPos> vertices = new ArrayList<MapPos>();
        for (MapPos p : points) {
        	vertices.add(projection.fromWgs84(p.x, p.y));
        }
		Line l = new Line(vertices, null, lineStyleSet, null);
		l.attachToLayer(this);
		
		objects.insert(l.getInternalState().envelope, l);
		
		objectMap.put(geomId, l);
		
		Log.d("FAIMS", l.toString());
		
		return geomId++;
	}

	public int addPolygon(List<MapPos> points, int color) {
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
		
		objectMap.put(geomId, p);
		
		Log.d("FAIMS", p.toString());
		
		return geomId++;
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

}
