package com.nutiteq.layers.vector;

import java.io.File;
import java.io.IOException;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import android.util.Log;
import au.org.intersect.faims.android.util.FileUtil;

import com.nutiteq.projections.Projection;
import com.nutiteq.vectorlayers.GeometryLayer;

import java.awt.geom.Rectangle2D;

public class ShapeLayer extends GeometryLayer {

	public ShapeLayer(Projection projection, String layerPath) throws Exception {
		super(projection);
		readLayer(layerPath);
	}
	
	private void readLayer(String layerPath) throws Exception {
		String projfilePath = layerPath + ".prj";
		String shapefilePath = layerPath + ".shp";
		
		Log.d("FAIMS", projfilePath);
		Log.d("FAIMS", shapefilePath);
		
		if (!new File(projfilePath).exists()) {
			throw new IOException("Proj file " + projfilePath + " does not exist");
		}
		
		if (!new File(shapefilePath).exists()) {
			throw new IOException("Shape file " + shapefilePath + " does not exist");
		}
		
		String wkt = FileUtil.readFileIntoString(projfilePath);

		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
		
		Log.d("FAIMS", sourceCRS.toString());
	}

}
