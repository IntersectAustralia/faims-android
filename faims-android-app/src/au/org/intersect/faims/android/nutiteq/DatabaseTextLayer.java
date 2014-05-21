package au.org.intersect.faims.android.nutiteq;

import java.util.List;
import java.util.Vector;

import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.google.inject.Inject;
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

public class DatabaseTextLayer extends TextLayer {
	
	@Inject
	DatabaseManager databaseManager;
	
	private DatabaseLayer databaseLayer;
	private StyleSet<TextStyle> styleSet;
	private int minZoom;
	private Vector<Text> objects;
	
	private boolean renderOnce;

	public DatabaseTextLayer(Projection projection, DatabaseLayer databaseLayer, StyleSet<TextStyle> styleSet) {
		super(projection);

		FAIMSApplication.getInstance().injectMembers(this);
		
		this.databaseLayer = databaseLayer;
		this.styleSet = styleSet;
		if (styleSet != null) {
			this.minZoom = styleSet.getFirstNonNullZoomStyleZoom();
		}
	}
	
	public void renderOnce() {
		renderOnce = true;
	}
	
	@Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
		objects = new Vector<Text>();
		
		if (zoom < minZoom) {
	        setVisibleElementsList(null);
	      return;
	    }
		
		if (renderOnce) {
			renderOnce = false;

		    List<Geometry> geometries = databaseLayer.getVisibleElements();
		    if (geometries == null || geometries.size() == 0) {
		    	setVisibleElementsList(null);
		    	return;
		    }
		    
		    for(Geometry geom: geometries){
		        
				GeometryData userData = (GeometryData) geom.userData;
		        String label = userData.label;
		        
		        MapPos topRight = null;
		        if (geom instanceof Point) {
		        	topRight = ((Point) geom).getMapPos();
		        } else if (geom instanceof Line) {
		        	topRight = ((Line) geom).getVertexList().get(0);
		        } else if (geom instanceof Polygon) {
		        	try {
		        		MapPos center = databaseManager.spatialRecord().computeCentroid((Polygon) GeometryUtil.convertGeometryToWgs84(geom));
		        		topRight = GeometryUtil.convertFromWgs84(center);
		        	} catch (Exception e) {
		        		topRight = new MapPos(0, 0);
		        		FLog.e("error computing centroid of polygon", e);
		        	}
		        } else {
		        	FLog.e("invalid geometry type");
		        }
		        
		        Text newText = new Text(topRight, label, styleSet, null);
		        
		        newText.attachToLayer(this);
		        newText.setActiveStyle(zoom);
		        
		        objects.add(newText);
		    }
		    
		    setVisibleElementsList(objects);
		    
		}
	}

}
