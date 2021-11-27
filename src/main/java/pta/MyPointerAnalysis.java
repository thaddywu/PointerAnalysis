package pta;

import java.io.File;

import soot.PackManager;
import soot.Transform;

public class MyPointerAnalysis {
	
	// args[0] = "/root/workspace/code"
	// args[1] = "test.Hello"	
	static String entryClass ;
	public static void main(String[] args) {	
		entryClass = args[1];
		String classpath = args[0] 
				+ File.pathSeparator + args[0] + File.separator + "rt.jar"
				+ File.pathSeparator + args[0] + File.separator + "jce.jar";
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.mypta", new WholeProgramTransformer()));
		soot.Main.main(new String[] {
			"-w",
			"-p", "cg.spark", "enabled:true",
			"-p", "wjtp.mypta", "enabled:true",
			"-soot-class-path", classpath,
			args[1]				
		});
	}

}
