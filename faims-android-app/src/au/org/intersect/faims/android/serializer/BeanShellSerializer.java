package au.org.intersect.faims.android.serializer;

import java.io.Serializable;
import java.util.HashMap;

import au.org.intersect.faims.android.log.FLog;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.This;

public class BeanShellSerializer {

	private static final String BEANSHELLLINKER = "linker";
	
	private Interpreter interpreter;

	public BeanShellSerializer(Interpreter interpreter) {
		this.interpreter = interpreter;
	}
	
	public HashMap<String, Serializable> getVariables() throws Exception {
		HashMap<String, Serializable> vars = new HashMap<String, Serializable>();
		String[] variables = (String[]) interpreter.eval("this.variables");
		for (String var: variables) {
			if (isBeanShellLinker(var)) {
				continue;
			}
			
			Object obj = interpreter.get(var);
			if (isBeanShellScriptedObject(obj)) {
				This thisObj = (This) obj;
				NameSpace namespace = thisObj.getNameSpace();
				namespace.prune();
				vars.put(var, thisObj);
			} else if (isSerializable(obj)) {
				vars.put(var, (Serializable) obj);
			} else {
				FLog.d("Cannot serialize " + obj);
			}
		}
		return vars;
	}

	public void setVariables(HashMap<String, Serializable> vars) throws Exception {
		for (String var: vars.keySet()) {
			Object obj = vars.get(var);
			if (isBeanShellScriptedObject(obj)) {
				This thisObj = (This) obj;
				NameSpace namespace = thisObj.getNameSpace();
				This.bind(thisObj, namespace, interpreter);
				interpreter.set(var, thisObj);
			} else if (isSerializable(obj)) {
				interpreter.set(var, obj);
			} else {
				FLog.d("Cannot deserialize " + var);
			}
		}
	}

	private boolean isBeanShellLinker(String var) {
		return BEANSHELLLINKER.equals(var);
	}

	private boolean isBeanShellScriptedObject(Object obj) {
		return obj instanceof This;
	}

	private boolean isSerializable(Object obj) {
		return obj instanceof Serializable;
	}

}
