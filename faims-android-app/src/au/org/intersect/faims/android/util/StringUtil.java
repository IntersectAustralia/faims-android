package au.org.intersect.faims.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StringUtil {
	
	public static String streamToString(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream);
        StringBuilder sb = new StringBuilder();
        int value;
        while((value = reader.read()) > 0)
            sb.append((char) value);
        return sb.toString();
	}

}
