package au.org.intersect.faims.android.beanshell.callbacks;

public interface ToggleActionButtonCallback extends ActionButtonCallback{

	public boolean isActionOff();
	
	public String actionOffLabel();
	public void actionOff();
	
}
