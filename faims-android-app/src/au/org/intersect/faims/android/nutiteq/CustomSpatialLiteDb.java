package au.org.intersect.faims.android.nutiteq;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import jsqlite.Callback;
import jsqlite.Exception;
import au.org.intersect.faims.android.log.FLog;

import com.nutiteq.db.DBLayer;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.layers.vector.SpatialLiteDb;
import com.nutiteq.utils.Utils;
import com.nutiteq.utils.WkbRead;

public class CustomSpatialLiteDb extends SpatialLiteDb {

	private Geometry result = null;

	public CustomSpatialLiteDb(String dbPath) {
		super(dbPath);
	}

	public Geometry getBoundariesFromDataBase(final DBLayer dbLayer) {
        result = null;
        Callback cb = new Callback() {
            
            @Override
            public void columns(String[] coldata) {
                FLog.d("columns" + Arrays.toString(coldata));
            }

            @Override
            public void types(String[] types) {
            }

            @Override
            public boolean newrow(String[] rowdata) {

            	Geometry[] gs = WKBUtil.cleanGeometry(WkbRead.readWkb(
	                    new ByteArrayInputStream(Utils
	                            .hexStringToByteArray(rowdata[0])), (Object) null));
				if (gs != null) {
					result = GeometryUtil.fromGeometry(gs[0]);
				}

                return false;
            }
        };
        String geomCol = dbLayer.geomColumn;

        if (dbLayer.srid != SDK_SRID) {
            FLog.d("SpatialLite: Data must be transformed from " + SDK_SRID
                    + " to " + dbLayer.srid);
            geomCol = "Transform(" + dbLayer.geomColumn + "," + dbLayer.srid + ")";

        }
        
        
        String qry = "SELECT HEX(AsBinary(extent(" + geomCol + "))) "
                    + " from " + dbLayer.table;

        FLog.d(qry);
        try {
            db.exec(qry, cb);
        } catch (Exception e) {
            FLog.e("SpatialLite: Failed to query data! ", e);
        }

        return result;
    }
}
