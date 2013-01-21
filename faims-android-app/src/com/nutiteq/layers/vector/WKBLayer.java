package com.nutiteq.layers.vector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

import android.os.AsyncTask;
import android.util.Log;

import com.nutiteq.components.Envelope;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.DefaultLabel;
import com.nutiteq.utils.WkbRead;
import com.nutiteq.vectorlayers.GeometryLayer;

public class WKBLayer extends GeometryLayer {
	
	private static class WKBParser extends AsyncTask<String, Void, Void> {
		
		private WKBLayer layer;
		
		public WKBParser(WKBLayer layer) {
			this.layer = layer;
		}

		@Override
		protected Void doInBackground(String... params) {
			String filename = params[0];
			
			ByteArrayInputStream byteIn;
			try {
				FileInputStream is = new FileInputStream(filename);
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				byte[] buffer = new byte[4096];
				int size;
				while ((size = is.read(buffer)) != -1) {
					byteOut.write(buffer, 0, size);
				}
				is.close();
				byteIn = new ByteArrayInputStream(byteOut.toByteArray());
			} catch (IOException e) {
				Log.e("FAIMS", "Could not read .wkb file " + filename, e);
				return null;
			}
			
			Vector<Geometry> objects = new Vector<Geometry>();
			
			while (true) {
				Geometry[] geoms = WkbRead.readWkb(byteIn, null);
				if (geoms == null) break;
				
				for (int i = 0; i < geoms.length; i++) {
					Geometry object = geoms[i];
					Log.d("FAIMS", object.toString());
					
					Geometry newObject = null;
			        
			        if(object instanceof Point){
			            newObject = new Point(((Point) object).getMapPos(), new DefaultLabel("Point"), layer.pointStyleSet, object.userData);
			        }else if(object instanceof Line){
			            newObject = new Line(((Line) object).getVertexList(), new DefaultLabel("Line"), layer.lineStyleSet, object.userData);
			        }else if(object instanceof Polygon){
			            newObject = new Polygon(((Polygon) object).getVertexList(), ((Polygon) object).getHolePolygonList(), new DefaultLabel("Polygon"), layer.polygonStyle, object.userData);
			        }
			        newObject.attachToLayer(layer);
			        newObject.setActiveStyle(layer.minZoom);
			        objects.add(newObject);
				}
				
			}
			
			layer.objects = objects;
			
			return null;
		}
		
	}

	private StyleSet<PointStyle> pointStyleSet;
	private StyleSet<LineStyle> lineStyleSet;
	private StyleSet<PolygonStyle> polygonStyle;
	private int minZoom;
	private Vector<Geometry> objects;

	public WKBLayer(Projection projection, String filename,
			StyleSet<PointStyle> pointStyleSet, StyleSet<LineStyle> lineStyleSet, StyleSet<PolygonStyle> polygonStyleSet) throws Exception {
		super(projection);
		
		this.pointStyleSet = pointStyleSet;
	    this.lineStyleSet = lineStyleSet;
	    this.polygonStyle = polygonStyleSet;
	    
	    if (pointStyleSet != null) {
	      minZoom = pointStyleSet.getFirstNonNullZoomStyleZoom();
	    }
	    
	    if (lineStyleSet != null) {
	      minZoom = Math.min(minZoom, lineStyleSet.getFirstNonNullZoomStyleZoom());
	    }
	    
	    if (polygonStyleSet != null) {
	      minZoom = Math.min(minZoom, polygonStyleSet.getFirstNonNullZoomStyleZoom());
	    }
		
		readFile(filename);
	}
	
	private void readFile(String filename) throws Exception {
		Log.d("FAIMS", filename);
		
		File file = new File(filename);
		if (!file.exists()) {
			throw new IOException("WKB file " + filename + " does not exist");
		}
		
		new WKBParser(this).execute(filename);
	}
	
	@Override
	  public void calculateVisibleElements(Envelope envelope, int zoom) {
				
		if (zoom < minZoom) {
	        setVisibleElementsList(null);
	      return;
		}
		
		setVisibleElementsList(objects);
	}

}
