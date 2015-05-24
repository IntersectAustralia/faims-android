package au.org.intersect.faims.android.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import au.org.intersect.faims.android.log.FLog;

public class PersistentBundle implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6988351280460214034L;
	
	private HashMap<String, Object> objectMap;

	public PersistentBundle() {
		objectMap = new HashMap<String, Object>();
	}

	public static void save(String filename, PersistentBundle bundle) {
		FileOutputStream fileOut = null;
		ObjectOutputStream out = null;
		try {
			fileOut = new FileOutputStream(filename);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(bundle);
		} catch (Exception e) {
			FLog.e("could not save bundle", e);
		} finally {
			if (out != null) {
				try { 
					out.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (fileOut != null) {
				try { 
					fileOut.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}
	
	public static PersistentBundle load(String filename) {
		FileInputStream fileIn = null;
		ObjectInputStream in = null;
		try {
			fileIn = new FileInputStream(filename);
			in = new ObjectInputStream(fileIn);
			
			PersistentBundle bundle = (PersistentBundle) in.readObject();
			return bundle;
		} catch (Exception e) {
			FLog.e("could not load bundle", e);
		} finally {
			if (in != null) {
				try { 
					in.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (fileIn != null) {
				try { 
					fileIn.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return null;
	}

	public void putIntegerArrayList(String string, ArrayList<Integer> value) {
		objectMap.put(string, value);
	}
	
	@SuppressWarnings("unchecked")
	public List<Integer> getIntegerArrayList(String string) {
		return (List<Integer>) objectMap.get(string);
	}
	
	public void putSerializable(String string, Serializable value) {
		objectMap.put(string, value);
	}
	
	public Object getSerializable(String string) {
		return objectMap.get(string);
	}

	public void putString(String string, String value) {
		objectMap.put(string, value);
	}
	
	public String getString(String string) {
		return (String) objectMap.get(string);
	}

	public void putBoolean(String string, boolean value) {
		objectMap.put(string, value);
	}
	
	public boolean getBoolean(String string) {
		return (Boolean) objectMap.get(string);
	}
	
	public void putDouble(String string, Double value) {
		objectMap.put(string, value);
	}
	
	public Double getDouble(String string) {
		return (Double) objectMap.get(string);
	}
	
	public void putFloat(String string, float value) {
		objectMap.put(string, value);
	}
	
	public float getFloat(String string) {
		return (Float) objectMap.get(string);
	}

	public void putInt(String string, int value) {
		objectMap.put(string, value);
	}

	public int getInt(String string) {
		return (Integer) objectMap.get(string);
	}

}
