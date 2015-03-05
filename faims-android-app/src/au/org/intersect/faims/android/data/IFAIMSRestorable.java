package au.org.intersect.faims.android.data;

import android.os.Bundle;

public interface IFAIMSRestorable {
	public void saveTo(Bundle savedInstanceState) throws Exception;
	public void restoreFrom(Bundle savedInstanceState);
	public void resume();
	public void pause();
	public void destroy();
}
