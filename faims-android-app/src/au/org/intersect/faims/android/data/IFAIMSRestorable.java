package au.org.intersect.faims.android.data;


public interface IFAIMSRestorable {
	public void saveTo(PersistentBundle savedInstanceState) throws Exception;
	public void restoreFrom(PersistentBundle savedInstanceState);
	public void resume();
	public void pause();
	public void destroy();
}
