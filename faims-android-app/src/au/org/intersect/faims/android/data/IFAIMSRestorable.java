package au.org.intersect.faims.android.data;

import android.os.Bundle;

public interface IFAIMSRestorable {

	public void saveTo(Bundle savedInstanceState);

	public void restoreFrom(Bundle savedInstanceState);
}
