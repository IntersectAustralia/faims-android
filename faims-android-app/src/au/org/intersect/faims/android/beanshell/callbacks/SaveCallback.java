package au.org.intersect.faims.android.beanshell.callbacks;

public interface SaveCallback extends IBeanShellCallback {
	
	public void onSave(String uuid, boolean newRecord);
	public void onSaveAssociation(String entityId, String relationshpId);
}
