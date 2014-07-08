package au.org.intersect.faims.android.data;

import bsh.EvalError;
import android.os.Bundle;

public interface IFAIMSRestorable {
	public void saveTo(Bundle savedInstanceState) throws EvalError;
	public void restoreFrom(Bundle savedInstanceState);
	public void resume();
	public void pause();
	public void destroy();
}
