package au.org.intersect.faims.android.nutiteq;

import java.util.List;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

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
import com.nutiteq.vectorlayers.TextLayer;

public class SpatialiteTextLayer extends TextLayer {
	
	@Inject
	DatabaseManager databaseManager;

	private GeometryTextStyle textStyle;
	private int minZoom;
	private CustomSpatialiteLayer spatialiteLayer;
	private String[] userColumns;
	private Vector<Text> objects;
	
	private boolean renderOnce;

	public SpatialiteTextLayer(Projection projection, CustomSpatialiteLayer layer, String[] userColumns, GeometryTextStyle textStyle) {
		super(projection);

		FAIMSApplication.getInstance().injectMembers(this);
		
		this.spatialiteLayer = layer;
		this.userColumns = userColumns;
		this.textStyle = textStyle;
		if (textStyle != null) {
			this.minZoom = textStyle.toStyleSet().getFirstNonNullZoomStyleZoom();
		}
	}
	
	public void renderOnce() {
		renderOnce = true;
	}
	
	public void calculateVisibleElements(Envelope envelope, int zoom) {
		if (zoom < minZoom) {
			setVisibleElementsList(null);
			return;
		}

		if (userColumns[0] == null) return;

		if (renderOnce) {
			renderOnce = false;
			
			List<Geometry> geometries = spatialiteLayer.getVisibleElements();
			if (geometries == null || geometries.size() == 0) {
				setVisibleElementsList(null);
				return;
			}
	
			objects = new Vector<Text>();
	
			for(Geometry geom: geometries){
	
				GeometryData userData = (GeometryData) geom.userData;
				String name = userData.label;
	
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
	
				Text newText = new Text(topRight, name, textStyle.toStyleSet(), null);
	
				newText.attachToLayer(this);
				newText.setActiveStyle(zoom);
	
				objects.add(newText);
			}
	
			setVisibleElementsList(objects);
		}

	}

	public void saveToJSON(JSONObject json) {
		try {
			JSONObject style = new JSONObject();
			textStyle.saveToJSON(style);
			json.put("textStyle", style);
			json.put("visible", isVisible());
		} catch (JSONException e) {
			FLog.e("Couldn't serialize SpatialiteTextLayer", e);
		}
	}

}
