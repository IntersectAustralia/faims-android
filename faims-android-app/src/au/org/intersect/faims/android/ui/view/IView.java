package au.org.intersect.faims.android.ui.view;

public interface IView {

	public String getRef();
	public boolean isDynamic();

	public String getClickCallback();
	public void setClickCallback(String code);
	public String getSelectCallback();
	public void setSelectCallback(String code);
	public String getFocusCallback();
	public String getBlurCallback();
	public void setFocusBlurCallbacks(String focusCode, String blurCode);
}
