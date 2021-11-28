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
import soot.jimple.CastExpr;

class NameManager {
	static TAI getIndetifier(SootMethod sm, Value v, Integer ctx) throws Exception {
		TAI identifier;
		if (v instanceof Local)
			identifier = new TAI(((Local) v).getName() + "@" + sm.getSignature() + "#" + ctx.toString(), null);
		else if (v instanceof CastExpr)
			identifier = getIndetifier(sm, ((CastExpr) v).getOp(), ctx);
		else if (v instanceof ParameterRef)
			identifier = new TAI("$param" + ((ParameterRef) v).getIndex() + "@" + sm.getSignature() + "#" + ctx.toString(), null);
        else if (v instanceof ThisRef)
            identifier = new TAI("$this" + "@" + sm.getSignature() + "#" + ctx.toString(), null);
        else if (v instanceof ArrayRef) {
            Value base = ((ArrayRef) v).getBase();
            Myassert.myassert (base instanceof Local);
            identifier = new TAI(((Local) base).getName() + "@" + sm.getSignature() + "#" + ctx.toString() , "(ArrayItem)");
        }
		else if (v instanceof StaticFieldRef) {
			SootField sf = ((StaticFieldRef) v).getField();
			identifier = new TAI(sf.getName() + "@" + sf.getDeclaringClass(), null);
		}
		else if (v instanceof InstanceFieldRef) {
			InstanceFieldRef iv = (InstanceFieldRef) v;
			SootField sf = iv.getField();
			Myassert.myassert (!sf.isStatic() && iv.getBase() instanceof Local);
			identifier = new TAI(((Local) iv.getBase()).getName() + "@" + sm.getSignature() + "#" + ctx.toString(), iv.getField().getName());
		}
		else
			identifier = new TAI(null, null);
		return identifier;
	}
	static String getThisIdentifier(SootMethod sm, Integer ctx)
		{ return "$this@" + sm.getSignature() + "#" + ctx.toString(); }
	static String getParamIdentifier(SootMethod sm, Integer index, Integer ctx)
		{ return "$param" + index + "@" + sm.getSignature() + "#" + ctx.toString(); }
	static String getReturnIdentifier(SootMethod sm, Integer ctx) 
		{ return "$ret@" + sm.getSignature() + "#" + ctx.toString();	}
	static String getHeapIdentifier(Integer heap, String field)
		{ return heap + "." + field; }
	static String getArrayItemIdentifier(Integer heap)
		{ return getHeapIdentifier(heap, "(ArrayItem)"); }
	static String getCollectionItemIdentifier(Integer heap)
		{ return getHeapIdentifier(heap, getCollectionItemField()); }
	static String getCollectionItemField()
		{ return "(CollectionItem)"; }
}

class PolyManager {
	/* locate method, when polymorphism exists */
	static Map<String, SootMethod> methodDeclRecord = new TreeMap<>() ;
	static SootMethod getVirtualMethod(SootClass sc, SootMethod sm) {
		/*
		String token = sm.toString() + sm.getSubSignature();
		if (methodDeclRecord.containsKey(token))
			return methodDeclRecord.get(token);
		*/
		
		String sig = sm.getSubSignature();
		SootClass mi = sc;
		while (mi != null && mi.getMethodUnsafe(sig) == null)
			mi = mi.getSuperclassUnsafe();
		// methodDeclRecord.put(token, mi);
		SootMethod ret = null;
		if (mi != null) ret = mi.getMethodUnsafe(sig);
		return ret;
	}
}

class Myassert {
	static void myassert(boolean condition) throws Exception {
		// if (!condition) assert (false);
		if (!condition) throw new Exception("Asseation failed...");
	}
}