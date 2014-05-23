package au.org.intersect.faims.android.ui.view;

public class Compare {
	
	public static boolean equal(String s1, String s2) {
		return (s1 == null && s2 == null) || (s1 != null && s2 != null && s1.equals(s2));
	}
	
	public static boolean equal(float f1, float f2) {
		return f1 == f2;
	}
	
}
