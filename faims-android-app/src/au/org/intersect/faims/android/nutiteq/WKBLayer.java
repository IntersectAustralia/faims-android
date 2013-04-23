package au.org.intersect.faims.android.nutiteq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import android.os.AsyncTask;
import au.org.intersect.faims.android.log.FLog;

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
import com.nutiteq.ui.Label;
import com.nutiteq.utils.Const;
import com.nutiteq.utils.Quadtree;
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
			FLog.d("Reading file");
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
				FLog.e("Could not read .wkb file " + filename, e);
				return null;
			}

			Quadtree<Geometry> objects = new Quadtree<Geometry>(Const.UNIT_SIZE / 10000.0);

			FLog.d("Creating elements");
			Label pointLabel = new DefaultLabel("Point");
			Label lineLabel = new DefaultLabel("Line");
			Label polyLabel = new DefaultLabel("Polygon");
			while (true) {
				Geometry[] geoms = WkbRead.readWkb(byteIn, null);
				if (geoms == null) break;

				for (int i = 0; i < geoms.length; i++) {
					Geometry object = geoms[i];
					Geometry newObject = null;

					if(object instanceof Point){
						newObject = new Point(((Point) object).getMapPos(), pointLabel, layer.pointStyleSet, object.userData);
					}else if(object instanceof Line){
						newObject = new Line(((Line) object).getVertexList(), lineLabel, layer.lineStyleSet, object.userData);
					}else if(object instanceof Polygon){
						newObject = new Polygon(((Polygon) object).getVertexList(), ((Polygon) object).getHolePolygonList(), polyLabel, layer.polygonStyle, object.userData);
					}
					newObject.attachToLayer(layer);
					objects.insert(newObject.getInternalState().envelope, newObject);
				}

			}
			FLog.d("Finished creating elements");

			layer.objects = objects;
			if (layer.components != null) {
				layer.components.mapRenderers.getMapRenderer().frustumChanged();
			}

			return null;
		}

	}

	private StyleSet<PointStyle> pointStyleSet;
	private StyleSet<LineStyle> lineStyleSet;
	private StyleSet<PolygonStyle> polygonStyle;
	private int minZoom;
	private Quadtree<Geometry> objects;

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
		FLog.d(filename);

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

		if (objects != null) {
			List<Geometry> objList = objects.query(envelope);
			for (Geometry geom : objList) {
				geom.setActiveStyle(minZoom);
			}
			setVisibleElementsList(objList);
		}
	}

}
