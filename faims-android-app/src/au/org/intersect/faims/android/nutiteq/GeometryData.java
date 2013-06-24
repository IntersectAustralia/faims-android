package au.org.intersect.faims.android.nutiteq;

public class GeometryData {
	
	public enum Type {
		ENTITY,
		RELATIONSHIP,
		LEGACY
	}

	public String id;
	public String label;
	public GeometryStyle style;
	public int geomId;
	public int layerId;
	public Type type;
	
	public GeometryData(String id, Type type, String label, int layerId) {
		this(id, type, label, null, layerId);
	}
	
	public GeometryData(String id, Type type, String label, GeometryStyle style, int layerId) {
		this.id = id;
		this.type = type;
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
