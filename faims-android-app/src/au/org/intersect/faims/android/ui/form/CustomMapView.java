package au.org.intersect.faims.android.ui.form;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseArray;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;

import com.nutiteq.MapView;
import com.nutiteq.components.Components;
import com.nutiteq.components.Constraints;
import com.nutiteq.components.Options;
import com.nutiteq.components.Range;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.ui.MapListener;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.GeometryLayer;

public class CustomMapView extends MapView {
	
	public static class CustomMapListener extends MapListener {

		@Override
		public void onDrawFrameAfter3D(GL10 arg0, float arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDrawFrameBefore3D(GL10 arg0, float arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onLabelClicked(VectorElement arg0, boolean arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onMapClicked(double arg0, double arg1, boolean arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onMapMoved() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSurfaceChanged(GL10 arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onVectorElementClicked(VectorElement arg0, double arg1,
				double arg2, boolean arg3) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private static int cacheId = 9991;
	
	private SparseArray<GeometryLayer> vectorMap;
	
	private int vectorId = 1;

	private DrawView drawView;

	private Geometry overlayGeometry;
	
	public CustomMapView(Context context, DrawView drawView) {
		this(context);
		this.drawView = drawView;
	}
	
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
        
        vectorMap = new SparseArray<GeometryLayer>();
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
		this.getLayers().removeLayer(vectorMap.get(id));
		vectorMap.remove(id);
	}

	public GeometryLayer getVectorLayer(int layerId) {
		return vectorMap.get(layerId);
	}

	public void setLayerVisible(int layerId, boolean visible) {
		GeometryLayer layer = vectorMap.get(layerId);
		layer.setVisible(visible);
	}

	public void setViewLocked(boolean lock) {
		if (lock) {
			this.getConstraints().setTiltRange(new Range(90.0f, 90.0f));
			this.setTilt(90.0f);
		} else {
			this.getConstraints().setTiltRange(Constraints.DEFAULT_TILT_RANGE);
		}
	}

	public int getGeometryId(Geometry geometry) {
		for(int i = 0; i < vectorMap.size(); i++) {
			int key = vectorMap.keyAt(i);
			GeometryLayer layer = vectorMap.get(key);
			if (layer instanceof CanvasLayer) {
				int geomId = ((CanvasLayer) layer).getGeometryId(geometry);
				if (geomId > 0) return geomId;
			}
		}
		return 0;
	}

	public Geometry getGeometry(int geomId) {
		for(int i = 0; i < vectorMap.size(); i++) {
			int key = vectorMap.keyAt(i);
			GeometryLayer layer = vectorMap.get(key);
			if (layer instanceof CanvasLayer) {
				Geometry geom = ((CanvasLayer) layer).getGeometry(geomId);
				if (geom != null) return geom;
			}
		}
		return null;
	}

	public void drawGeometrOverlay(Geometry geom) {
		if (geom == null) {
			this.overlayGeometry = null;
			drawView.drawGeometry(null);
		} else {
			this.overlayGeometry = GeometryUtil.worldToScreen(geom, this);
			drawView.drawGeometry(overlayGeometry);
		}
	}
	
	public void replaceGeometryOverlay(int geomId) {
		for(int i = 0; i < vectorMap.size(); i++) {
			int key = vectorMap.keyAt(i);
			GeometryLayer layer = vectorMap.get(key);
			if (layer instanceof CanvasLayer) {
				CanvasLayer canvas = (CanvasLayer) layer;
				Geometry geom = canvas.getGeometry(geomId);
				if (geom != null) {
					canvas.replaceGeometry(geomId, GeometryUtil.screenToWorld(overlayGeometry, this));
					canvas.updateRenderer();
				}
			}
		}
	}
	
}
