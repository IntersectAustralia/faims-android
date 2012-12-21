package au.org.intersect.faims.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.res.AssetManager;
import android.view.View;
import android.view.View.OnClickListener;
import bsh.EvalError;
import bsh.Interpreter;

public class BeanShellLinker {
	
	private Interpreter interpreter;

	private UIRenderer renderer;

	private AssetManager assets;
	
	public BeanShellLinker(AssetManager assets, UIRenderer renderer) {
		this.assets = assets;
		this.renderer = renderer;
		interpreter = new Interpreter();
		try {
			interpreter.set("linker", this);
		} catch (EvalError e) {
			FAIMSLog.log(e);
		}
	}
	
	public void source(String filename) {
		try {
    		interpreter.eval(convertStreamToString(assets.open(filename)));
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
	/*
	public void bindViewToEvent(String ref, String type, final String code) {
		View view = renderer.getViewByRef(ref);
		if (view ==  null) {
			FAIMSLog.log("Can't find view for " + ref);
			return;
		}
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
*/
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
