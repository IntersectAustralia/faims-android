package au.org.intersect.faims.android.nutiteq;

public class GeometryData {

	public String id;
	public String label;
	public GeometryStyle style;
	public int geomId;
	public int layerId;
	
	public GeometryData(String id, String label, int layerId) {
		this(id, label, null, layerId);
	}
	
	public GeometryData(String id, String label, GeometryStyle style, int layerId) {
		this.id = id;
		this.label = label;
		this.style = style;
		this.layerId = layerId;
	}
	
	public GeometryData(int geomId, GeometryStyle style, int layerId) {
		this.geomId = geomId;
		this.style = style;
		this.layerId = layerId;
	}
	
}
