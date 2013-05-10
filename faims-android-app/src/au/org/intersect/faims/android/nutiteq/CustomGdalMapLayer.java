package au.org.intersect.faims.android.nutiteq;

import java.io.IOException;

import com.nutiteq.MapView;
import com.nutiteq.layers.raster.GdalMapLayer;
import com.nutiteq.projections.Projection;

public class CustomGdalMapLayer extends GdalMapLayer {

	private String name;
	private int layerId;

	public CustomGdalMapLayer(int layerId, String name, Projection projection, int minZoom, int maxZoom,
			int id, String gdalSource, MapView mapView, boolean reproject)
			throws IOException {
		super(projection, minZoom, maxZoom, id, gdalSource, mapView, reproject);
		this.name = name;
		this.layerId = layerId;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String layerName) {
		this.name = layerName;
	}

	public int getLayerId() {
		return layerId;
	}

}
