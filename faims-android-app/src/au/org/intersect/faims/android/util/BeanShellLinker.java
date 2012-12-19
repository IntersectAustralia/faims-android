package au.org.intersect.faims.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import bsh.EvalError;
import bsh.Interpreter;

public class BeanShellLinker {
	
	private Interpreter interpreter;

	private Activity activity;
	
	public BeanShellLinker(Activity activity) {
		this.activity = activity;
		interpreter = new Interpreter();
		try {
			interpreter.set("linker", this);
			sourceFromAssets("ui_logic.bsh");
		} catch (EvalError e) {
			FAIMSLog.log(e);
		}
	}
	
	public void sourceFromAssets(String file) {
		try {
			InputStream stream = activity.getAssets().open(file);
    		interpreter.eval(convertStreamToString(stream));
    	} catch (EvalError e) {
    		FAIMSLog.log(e); 
    	} catch (IOException e) {
    		FAIMSLog.log(e);
    	}
	}

	public void execute(String code) {
		try {
    		interpreter.eval(code);
    	} catch (EvalError e) {
    		FAIMSLog.log(e); 
    	}
	}
	
	public void bindViewToEvent(int id, String type, final String code) {
		View view = activity.findViewById(id);
		if (type == "click") {
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					execute(code);
				}
				
			});
		} else {
			FAIMSLog.log("Not implemented");
		}
	}
	
	private String convertStreamToString(InputStream stream) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(stream));
		
			StringBuilder sb = new StringBuilder();
		
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			} 
	
			return sb.toString();
		} catch (IOException e) {
			FAIMSLog.log(e);
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException e) {
				FAIMSLog.log(e);
			}
		}
		return null;
	}

}
