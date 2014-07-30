package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.json.JSONException;
import org.json.JSONObject;

import au.org.intersect.faims.android.log.FLog;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.projections.Projection;
import com.nutiteq.utils.Const;
import com.nutiteq.utils.Quadtree;
import com.nutiteq.vectorlayers.GeometryLayer;

public class CanvasLayer extends GeometryLayer {

	protected String name;
	private Quadtree<Geometry> objects;
	private Stack<Geometry> geomBuffer;
	protected int layerId;
	
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
			g.setActiveStyle(g.getStyleSet().getFirstNonNullZoomStyleZoom());
		}
		
		setVisibleElementsList(newVisibleElementsList);
	}

	public Point addPoint(int geomId, MapPos point, GeometryStyle style) {		
		Point p = new Point(projection.fromWgs84(point.x, point.y), null, style.toPointStyleSet(), new GeometryData(geomId, style, layerId));
		addGeometry(p);
		return p;
	}
	
	public Line addLine(int geomId, List<MapPos> points, GeometryStyle style) {
        List<MapPos> vertices = new ArrayList<MapPos>();
        for (MapPos p : points) {
        	vertices.add(projection.fromWgs84(p.x, p.y));
        }
        Line l = new Line(vertices, null, style.toLineStyleSet(), new GeometryData(geomId, style, layerId));
		addGeometry(l);
		return l;
	}
	
	public Polygon addPolygon(int geomId, List<MapPos> points, GeometryStyle style) {		
		List<MapPos> vertices = new ArrayList<MapPos>();
        for (MapPos p : points) {
        	vertices.add(projection.fromWgs84(p.x, p.y));
        }
        Polygon p = new Polygon(vertices, new ArrayList<List<MapPos>>(), null, style.toPolygonStyleSet(), new GeometryData(geomId, style, layerId));
		
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

	public void saveToJSON(JSONObject json) {
		try {
			json.put("name", getName());
			json.put("type", "CanvasLayer");
			json.put("visible", isVisible());
		} catch (JSONException e) {
			FLog.e("Couldn't serialize CanvasLayer", e);
		}
	}
}
