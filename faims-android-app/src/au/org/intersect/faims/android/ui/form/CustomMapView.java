package au.org.intersect.faims.android.ui.form;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import au.org.intersect.faims.android.R;

import com.nutiteq.MapView;
import com.nutiteq.components.Components;
import com.nutiteq.components.Options;
import com.nutiteq.layers.vector.WKBLayer;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.GeometryLayer;
import com.nutiteq.vectorlayers.VectorLayer;

public class CustomMapView extends MapView {
	
	private static int cacheId = 9991;
	
	private HashMap<Integer, GeometryLayer> vectorMap;
	
	private int vectorId = 1;

	public CustomMapView(Context context) {
		super(context);
		
        this.setComponents(new Components());
		
		// Activate some mapview options to make it smoother - optional
		this.getOptions().setPreloading(true);
		this.getOptions().setSeamlessHorizontalPan(true);
		this.getOptions().setTileFading(true);
		this.getOptions().setKineticPanning(true);
		this.getOptions().setDoubleClickZoomIn(true);
		this.getOptions().setDualClickZoomOut(true);
		
		 // set sky bitmap - optional, default - white
		this.getOptions().setSkyDrawMode(Options.DRAW_BITMAP);
		this.getOptions().setSkyOffset(4.86f);
		this.getOptions().setSkyBitmap(
                UnscaledBitmapLoader.decodeResource(getResources(),
                        R.drawable.sky_small));
		
		// Map background, visible if no map tiles loaded - optional, default - white
		this.getOptions().setBackgroundPlaneDrawMode(Options.DRAW_BITMAP);
		this.getOptions().setBackgroundPlaneBitmap(
                UnscaledBitmapLoader.decodeResource(getResources(),
                        R.drawable.background_plane));
		this.getOptions().setClearColor(Color.WHITE);
		
		// configure texture caching - optional, suggested 
        this.getOptions().setTextureMemoryCacheSize(40 * 1024 * 1024);
        this.getOptions().setCompressedMemoryCacheSize(8 * 1024 * 1024);
        
        //this.getOptions().setPersistentCachePath(activity.getDatabasePath("mapcache").getPath());
        // set persistent raster cache limit to 100MB
        //this.getOptions().setPersistentCacheSize(100 * 1024 * 1024);
        
        vectorMap = new HashMap<Integer, GeometryLayer>();
	}

	public static int nextId() {
		return cacheId;
	}
	
	public int addVectorLayer(GeometryLayer layer) {
		this.getLayers().addLayer(layer);
		vectorMap.put(vectorId, layer);
		return vectorId++;
	}

	public void removeVectorLayer(int id) {
		this.getLayers().removeLayer(vectorMap.remove(id));
	}
	
}
