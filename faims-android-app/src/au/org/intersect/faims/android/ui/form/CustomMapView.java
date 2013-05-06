package au.org.intersect.faims.android.ui.form;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.util.SparseArray;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.util.Dpi;

import com.nutiteq.MapView;
import com.nutiteq.components.Components;
import com.nutiteq.components.Constraints;
import com.nutiteq.components.MapPos;
import com.nutiteq.components.Options;
import com.nutiteq.components.Range;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;
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
	
	private MapNorthView northView;
	
	private ScaleBarView scaleView;

	private Geometry overlayGeometry;
	
	private ArrayList<Runnable> runnableList;
	private ArrayList<Thread> threadList;

	private boolean canRunThreads;
	
	public CustomMapView(Context context, DrawView drawView, MapNorthView northView, ScaleBarView scaleView) {
		this(context);
		this.drawView = drawView;
		this.northView = northView;
		this.scaleView = scaleView;
		
		scaleView.setBarWidthRange(Dpi.getDpi(context, 40), Dpi.getDpi(context, 100));
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
        
        runnableList = new ArrayList<Runnable>();
        threadList = new ArrayList<Thread>();
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
	
	public static void registerLicense(Context context){
    	final String LICENSE = "XTUMwQ0ZIRklrbEZ2T0dIdkZ3QkRieVBtcWJqdjZ1RUtBaFVBa1RreXdabUIraER4UjFmZ01aUk5oay83a2hzPQoKcGFja2FnZU5hbWU9YXUub3JnLmludGVyc2VjdC5mYWltcy5hbmRyb2lkCndhdGVybWFyaz1jdXN0b20KCg==";
		CustomMapView.registerLicense(LICENSE, context);
        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        CustomMapView.setWatermark(logo, -1.0f, -1.0f, 0.2f);
	}
	
	public void updateOverlay() {
		northView.setMapRotation(this.getRotation());
		int width = this.getWidth();
		int height = this.getHeight();
		
		scaleView.setMapBoundary(this.getZoom(), width, height, 
				distance(convertToWgs84(this.screenToWorld(0,  height, 0)), convertToWgs84(this.screenToWorld(width, height, 0))));
	}
	
	private double distance(MapPos p1, MapPos p2) {
		float[] results = new float[3];
		Location.distanceBetween(p1.y, p1.x, p2.y, p2.x, results);
		return results[0] / 1000;
	}
	
	private MapPos convertToWgs84(MapPos p) {
		return (new EPSG3857()).toWgs84(p.x, p.y);
	}

	public void startThread(Runnable runnable) {
		runnableList.add(runnable);
		
		// Note: the runnable will need to handle stopping the thread
		Thread t = new Thread(runnable);
		threadList.add(t);
		t.start();
	}
	
	public void restartThreads() {
		try {
			// wait for all threads to finish
			canRunThreads = false;
			while(true) {
				boolean allThreadsTerminated = true;
				for (Thread t : threadList) {
					if (t.getState() != Thread.State.TERMINATED) {
						allThreadsTerminated = false;
						break;
					}
				}
				if (allThreadsTerminated) {
					break;
				}
				FLog.d("Waiting to start map threads");
				Thread.sleep(1000);
			}
			canRunThreads = true;
			threadList.clear();
			for (Runnable r : runnableList) {
				Thread t = new Thread(r);
				threadList.add(t);
				t.start();
			}
		} catch (Exception e) {
			FLog.e("error restarting map threads", e);
		}
	}

	public void killThreads() {
		canRunThreads = false;
	}
	
	public boolean canRunThreads() {
		return canRunThreads;
	}
	
}
