package au.org.intersect.faims.android.nutiteq;

import java.io.IOException;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;

import au.org.intersect.faims.android.exceptions.MapException;

import com.nutiteq.MapView;
import com.nutiteq.components.Envelope;
import com.nutiteq.layers.raster.GdalMapLayer;
import com.nutiteq.log.Log;
import com.nutiteq.projections.Projection;

public class CustomGdalMapLayer extends GdalMapLayer {

	private String name;
	private int layerId;
	private String gdalSource;

	private static final double VRT_MAXERROR = 0.125;
    private static final int VRT_RESAMPLER = gdalconst.GRA_NearestNeighbour;
	private static final String EPSG_3785_WKT = "PROJCS[\"Google Maps Global Mercator\",    GEOGCS[\"WGS 84\",        DATUM[\"WGS_1984\",            SPHEROID[\"WGS 84\",6378137,298.257223563,                AUTHORITY[\"EPSG\",\"7030\"]],            AUTHORITY[\"EPSG\",\"6326\"]],        PRIMEM[\"Greenwich\",0,            AUTHORITY[\"EPSG\",\"8901\"]],        UNIT[\"degree\",0.01745329251994328,            AUTHORITY[\"EPSG\",\"9122\"]],        AUTHORITY[\"EPSG\",\"4326\"]],    PROJECTION[\"Mercator_2SP\"],    PARAMETER[\"standard_parallel_1\",0],    PARAMETER[\"latitude_of_origin\",0],    PARAMETER[\"central_meridian\",0],    PARAMETER[\"false_easting\",0],    PARAMETER[\"false_northing\",0],    UNIT[\"Meter\",1],    EXTENSION[\"PROJ4\",\"+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs\"],    AUTHORITY[\"EPSG\",\"3785\"]]";
	private static final double WORLD_WIDTH = 20037508.3428; // width of EPSG:3785

	public CustomGdalMapLayer(int layerId, String name, Projection projection, int minZoom, int maxZoom,
			int id, String gdalSource, MapView mapView, boolean reproject)
			throws IOException {
		super(projection, minZoom, maxZoom, id, gdalSource, mapView, reproject);
		this.name = name;
		this.layerId = layerId;
		this.gdalSource = gdalSource;
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

	public String getGdalSource() {
		return gdalSource;
	}
	
	private double[][] boundsWgs84(Dataset data,SpatialReference layerProjection) {
        double[][] corners= new double[4][2];
        
        corners[0] = corner(data, layerProjection, 0.0, 0.0);
        corners[1] = corner(data, layerProjection, 0.0, data
                .getRasterYSize());
        corners[2] = corner(data,layerProjection, data
                .getRasterXSize(), 0.0);
        corners[3] = corner(data, layerProjection, data
                .getRasterXSize(), data.getRasterYSize());
        
        return corners; 
    }

	public double[][] getBoundary() {
		Dataset originalData = gdal.Open(this.getGdalSource(), gdalconstConstants.GA_ReadOnly);
		// get original bounds in Wgs84
		SpatialReference hLatLong = new SpatialReference(osr.SRS_WKT_WGS84);
		SpatialReference layerProjection = new SpatialReference(EPSG_3785_WKT);
		Dataset openData = gdal.AutoCreateWarpedVRT(originalData,null, layerProjection.ExportToWkt(),VRT_RESAMPLER, VRT_MAXERROR);
		double[][] bounds = this.boundsWgs84(openData, hLatLong);
		return bounds;
	}
	
	public double getBestZoom() {
		Dataset originalData = gdal.Open(this.getGdalSource(), gdalconstConstants.GA_ReadOnly);
		// get original bounds in Wgs84
		SpatialReference layerProjection = new SpatialReference(EPSG_3785_WKT);
		Dataset openData = gdal.AutoCreateWarpedVRT(originalData,null, layerProjection.ExportToWkt(),VRT_RESAMPLER, VRT_MAXERROR);
		Envelope bbox = this.bounds(openData, layerProjection);
		return this.bestZoom(bbox.getWidth(),openData.getRasterXSize());
	}
	
    // calculate "best" (native) zoom for given raster
    private double bestZoom(double boundWidth, double pixWidth){
        return Math.log(((pixWidth * WORLD_WIDTH) / (boundWidth * 256.0))) / (Math.log(2));
    }
    
	private Envelope bounds(Dataset data,SpatialReference layerProjection) {
        double[][] corner= new double[4][2];
        if(data == null){
            Log.error("data null");
            return null;
        }
        corner[0] = corner(data, layerProjection, 0.0, 0.0);
        corner[1] = corner(data, layerProjection, 0.0, data
                .getRasterYSize());
        corner[2] = corner(data,layerProjection, data
                .getRasterXSize(), 0.0);
        corner[3] = corner(data, layerProjection, data
                .getRasterXSize(), data.getRasterYSize());
        
        return new Envelope(corner[1][0],corner[2][0],corner[1][1],corner[2][1]);
    }

	/**
     * Calculate corner coordinates of dataset, in layerProjection
     * @param data
     * @param dstProj
     * @param x coordinates of bounds
     * @param y coordinates of bounds 
     * @return
     */
    static double[] corner(Dataset data, SpatialReference dstProj, double x, double y)

    {
        double dfGeoX, dfGeoY;
        String dataProjection;
        double[] adfGeoTransform = new double[6];
        CoordinateTransformation hTransform = null;

        /* -------------------------------------------------------------------- */
        /*      Transform the point into georeferenced coordinates.             */
        /* -------------------------------------------------------------------- */
        data.GetGeoTransform(adfGeoTransform);
        
        {
            dataProjection = data.GetProjectionRef();
            if(dataProjection.equals("")){
                dataProjection = data.GetGCPProjection();
            }
//Log.debug("dataProjection "+dataProjection);
            dfGeoX = adfGeoTransform[0] + adfGeoTransform[1] * x
                    + adfGeoTransform[2] * y;
            dfGeoY = adfGeoTransform[3] + adfGeoTransform[4] * x
                    + adfGeoTransform[5] * y;
        }
        
        SpatialReference dataProj = new SpatialReference(dataProjection);
        
        // is reprojection needed?
       // Log.debug("dataProj "+dataProj.GetAuthorityCode(null)+ " layerProj "+layerProj.GetAuthorityCode(null));
        if(dstProj == null || (dataProj.GetAuthorityCode(null) != null && dataProj.GetAuthorityCode(null).equals(dstProj.GetAuthorityCode(null)))){
            return new double[]{dfGeoX, dfGeoY};
        }
        
        if (adfGeoTransform[0] == 0 && adfGeoTransform[1] == 0
                && adfGeoTransform[2] == 0 && adfGeoTransform[3] == 0
                && adfGeoTransform[4] == 0 && adfGeoTransform[5] == 0) {
            return null;
        }

        if (dataProjection != null && dataProjection.length() > 0) {

            if (dstProj != null) {
                gdal.PushErrorHandler( "CPLQuietErrorHandler" );
                hTransform = new CoordinateTransformation(dataProj, dstProj);
                gdal.PopErrorHandler();
//                layerProj.delete();
                if (gdal.GetLastErrorMsg().indexOf("Unable to load PROJ.4 library") != -1){
                    Log.error(gdal.GetLastErrorMsg());
                    hTransform = null;
                }
            }

//            if (dataProj != null)
//                dataProj.delete();
        }

        double[] transPoint = new double[3];
        if (hTransform != null) {
            hTransform.TransformPoint(transPoint, dfGeoX, dfGeoY, 0);
        }

        if (hTransform != null)
            hTransform.delete();

        return new double[]{transPoint[0], transPoint[1]};
        
    }

	public void raiseInvalidLayer() throws MapException {
		try {
			getBoundary();
		} catch (Exception e) {
			throw new MapException("Invalid raster map file");
		}
	}
}
