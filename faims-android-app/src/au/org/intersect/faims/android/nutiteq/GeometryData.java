package au.org.intersect.faims.android.nutiteq;

public class GeometryData {

	public String id;
	public String label;
	public GeometryStyle style;
	public int geomId;
	
	public GeometryData(String id, String label) {
		this(id, label, null);
	}
	
	public GeometryData(String id, String label, GeometryStyle style) {
		this.id = id;
		this.label = label;
		this.style = style;
	}
	
	public GeometryData(int geomId, GeometryStyle style) {
		this.geomId = geomId;
		this.style = style;
	}
	
}
