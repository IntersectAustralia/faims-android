package au.org.intersect.faims.android.nutiteq;

import java.util.List;
import java.util.Vector;

import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.SpatialiteUtil;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.Text;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.StyleSet;
import com.nutiteq.style.TextStyle;
import com.nutiteq.vectorlayers.TextLayer;

public class SpatialiteTextLayer extends TextLayer {

	private StyleSet<TextStyle> styleSet;
	private int minZoom;
	private CustomSpatialiteLayer spatialiteLayer;
	private String[] userColumns;

	public SpatialiteTextLayer(Projection projection, CustomSpatialiteLayer layer, String[] userColumns, StyleSet<TextStyle> styleSet) {
		super(projection);
		this.spatialiteLayer = layer;
		this.userColumns = userColumns;
		this.styleSet = styleSet;
		if (styleSet != null) {
			this.minZoom = styleSet.getFirstNonNullZoomStyleZoom();
		}
		
	}
	
	@Override
	  public void calculateVisibleElements(Envelope envelope, int zoom) {
	    
	    if (zoom < minZoom) {
	        setVisibleElementsList(null);
	      return;
	    }
	    
	    if (userColumns[0] == null) return;

	    List<Geometry> geometries = spatialiteLayer.getVisibleElements();
	    if (geometries == null || geometries.size() == 0) return;
	    
	    Vector<Text> objects = new Vector<Text>();
	    
	    for(Geometry geom: geometries){
	        
			String[] userData = (String[]) geom.userData;
	        String name = userData[1];
	        
	        MapPos topRight = null;
	        if (geom instanceof Point) {
	        	topRight = ((Point) geom).getMapPos();
	        } else if (geom instanceof Line) {
	        	topRight = ((Line) geom).getVertexList().get(0);
	        } else if (geom instanceof Polygon) {
	        	try {
	        		MapPos center = SpatialiteUtil.computeCentroid((Polygon) GeometryUtil.convertGeometryToWgs84(geom));
	        		topRight = GeometryUtil.convertFromWgs84(center);
	        	} catch (Exception e) {
	        		topRight = new MapPos(0, 0);
	        		FLog.e("error computing centroid of polygon", e);
	        	}
	        } else {
	        	FLog.e("invalid geometry type");
	        }
	        
	        Text newText = new Text(topRight, name, styleSet, null);
	        
	        newText.attachToLayer(this);
	        newText.setActiveStyle(zoom);
	        
	        objects.add(newText);
	    }
	    
	    setVisibleElementsList(objects);

	  }

}
