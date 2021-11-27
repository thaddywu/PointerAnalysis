package pta;

import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import java_cup.lalr_item;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.List;

import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.JastAddJ.AssignShiftExpr;
import soot.JastAddJ.IntegralType;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.ThisRef;
import soot.jimple.ParameterRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;

class NameManager {
	static TAI getIndetifier(SootMethod sm, Value v) {
		TAI identifier;
		if (v instanceof Local)
			identifier = new TAI(((Local) v).getName() + "@" + sm.getSignature(), null);
		else if (v instanceof ParameterRef)
			identifier = new TAI("$param" + ((ParameterRef) v).getIndex() + "@" + sm.getSignature(), null);
        else if (v instanceof ThisRef)
            identifier = new TAI("$this" + "@" + sm.getSignature(), null);
        else if (v instanceof ArrayRef) {
            Value base = ((ArrayRef) v).getBase();
            assert (base instanceof Local);
            identifier = new TAI(((Local) base).getName() + "@" + sm.getSignature() , "(ArrayItem)");
        }
		else if (v instanceof StaticFieldRef) {
			identifier = new TAI(((StaticFieldRef) v).getField().getName(), null);
            /*
			String fn = ((StaticFieldRef) v).getField().getName();
            System.out.println(((StaticFieldRef) v).getField());
			System.out.println(sm.getDeclaringClass());
			System.out.println(PolyManager.getFieldDecl(sm.getDeclaringClass(), fn));
			identifier = new TAI("static$" + fn + "@" + PolyManager.getFieldDecl(sm.getDeclaringClass(), fn).getName(), null);
            */
		}
		else if (v instanceof InstanceFieldRef) {
			InstanceFieldRef iv = (InstanceFieldRef) v;
			assert( iv.getBase() instanceof Local);
			identifier = new TAI(((Local) iv.getBase()).getName() + "@" + sm.getSignature(), iv.getField().getName());
		}
		else
			identifier = new TAI(null, null);
		return identifier;
	}
	static String getThisIdentifier(String sig)
		{ return "$this@" + sig; }
	static String getParamIdentifier(String sig, Integer index)
		{ return "$param" + index + "@" + sig; }
	static String getReturnIdentifier(String sig) 
		{ return "$ret@" + sig;	}
	static String getHeapIdentifier(Integer heap, String field)
		{ return heap + "." + field; }
	static String getArrayItemIdentifier(Integer heap)
		{ return getHeapIdentifier(heap, "(ArrayItem)"); }
}

class PolyManager {
	/* locate method, when polymorphism exists */
	static Map<String, SootClass> methodDeclRecord = new TreeMap<>() ;
	static SootClass getMethodDecl(SootClass sm, String subsig) {
		String token = sm.toString() + subsig;
		if (methodDeclRecord.containsKey(token))
			return methodDeclRecord.get(token);
		
		SootClass mi = sm;
		while (mi != null && mi.getMethodUnsafe(subsig) == null)
			mi = mi.getSuperclassUnsafe();
		methodDeclRecord.put(token, mi);
		return mi;
	}
	/* locate method, when polymorphism exists */
	static Map<String, SootClass> staticDeclRecord = new TreeMap<>() ;
	static SootClass getFieldDecl(SootClass sm, String sf) {
		String token = sm.toString() + sf;
		if (staticDeclRecord.containsKey(token))
			return staticDeclRecord.get(token);
		
		SootClass mi = sm;
		while (mi != null && mi.getFieldByNameUnsafe(sf) == null)
			mi = mi.getSuperclassUnsafe();
            staticDeclRecord.put(token, mi);
		return mi;
	}
}