package pta;

import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import fj.test.reflect.Name;
import java_cup.lalr_item;
import polyglot.lex.Identifier;

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
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.ThisRef;
import soot.jimple.ParameterRef;
import soot.jimple.NullConstant;
import soot.jimple.Constant;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;


class TAI { /* Three address representation */
	public String var;
	public String field;
	public TAI(String var, String field)
		{this.var = var; this.field = field; }
	public Boolean isNull() { return var == null; }
	public Boolean hasField() { return field != null; }
	public String toString() { return "<" + var + ", " + field + ">"; }
}

public class WholeProgramTransformer extends SceneTransformer {
	static Map< Integer, String > Heap2Class = new TreeMap<>();
	static Map< Integer, Integer > Heap2Alloc = new TreeMap<>();
	static Map< Integer, TreeSet<String> > queries = new TreeMap<>();
	
	static Integer hashMod = 13;
	static Integer contextId = 0;
	static Integer allocId = -1;
	static Integer heapId = 0;
	
	static boolean shouldAnalysis(SootMethod sm) {
		SootClass sc = sm.getDeclaringClass();
		// if (sc.getName().equals("java.lang.Object")) return true;
		if (sc.getName().equals(MyPointerAnalysis.entryClass)) return true;
		if (sc.isJavaLibraryClass()) return false;
		return true;
	}
	
	public void buildCFG(SootMethod sm) {
		/* Inter procedural pointer analysis */
		if (!sm.hasActiveBody()) return ;
		if (!shouldAnalysis(sm)) return ;
		System.out.println();
		System.out.println(sm);
		for (Unit u : sm.getActiveBody().getUnits()) {
			System.out.println("\t" + u);

			String sig = sm.getSubSignature();

			if (u instanceof AssignStmt)
				assert (u instanceof DefinitionStmt);
				
			/* Instrumented */
			if (u instanceof InvokeStmt) {
				InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
				if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void alloc(int)>"))
					{ allocId = ((IntConstant)ie.getArg(0)).value; continue; }
				if (ie.getMethod().toString().equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")) {
					Integer queryId = ((IntConstant)ie.getArg(0)).value;
					TAI var = NameManager.getIndetifier(sm, ie.getArg(1));
					assert (!var.hasField() && !var.isNull());
					
					/* add node in super-CFG, inefficient implementation! */
					System.out.println("\t[Query] " + var.var + " " + queryId);
					TreeSet<String> varset;
					if (queries.containsKey(queryId))
						varset = queries.get(queryId);
					else
						varset = new TreeSet<>();
					varset.add(var.var);
					queries.put(queryId, varset);
					continue;
				}
				/* skip auxiliary function */
			} 

			if ((u instanceof DefinitionStmt) && (((DefinitionStmt) u).getRightOp() instanceof AnyNewExpr) ) {
				DefinitionStmt au = (DefinitionStmt) u;
				System.out.println("\t[New]" + au.getRightOp().getType() + " alloc:" + allocId + " heap:" + heapId);
				Heap2Class.put(heapId, au.getRightOp().getType().toString());
				if (allocId != -1) Heap2Alloc.put(heapId, allocId);
				if (au.getRightOp() instanceof NewArrayExpr || au.getRightOp() instanceof NewMultiArrayExpr) {
					/* Distinguish array and its item, allocate a heap space for its item */
					Anderson.passSingleton(heapId + 1, NameManager.getArrayItemIdentifier(heapId));
					if (allocId != -1) Heap2Alloc.put(heapId + 1, allocId);
					heapId += 1;
				}
				heapId = heapId + 1; allocId = -1;
				continue;
			}

			if (u instanceof DefinitionStmt) {
				DefinitionStmt au = (DefinitionStmt) u;
				TAI lop = NameManager.getIndetifier(sm, au.getLeftOp());
				TAI rop = NameManager.getIndetifier(sm, au.getRightOp());
				System.out.println("\t[Definition]" + lop + " " + rop);
				if (lop.isNull() || rop.isNull()) continue;
				if (lop.hasField()) /* get: a.f = x */
					Anderson.newPut(lop.var, rop.var, lop.field);
				else if (rop.hasField()) /* put: a = x.f */
					Anderson.newGet(lop.var, rop.var, rop.field);
				else /* a = x */
					Anderson.newEdge(rop.var, lop.var);
				continue ;
			}

			if (u instanceof InvokeStmt || (u instanceof DefinitionStmt && ((DefinitionStmt) u).getRightOp() instanceof InvokeExpr)) {
				InvokeExpr ie; Value rax = null; // return value -> rax
				if (u instanceof InvokeStmt)
					ie = ((InvokeStmt) u).getInvokeExpr();
				else {
					ie = (InvokeExpr) ((DefinitionStmt) u).getRightOp();
					rax = (InvokeExpr) ((DefinitionStmt) u).getLeftOp();
					assert (rax instanceof Local);
				}
				System.out.println("\t[Invoke]" + ie + " rax:" + rax);
				String m = ie.getMethod().getSubSignature();
				List<String> args = new ArrayList<>();
				for (Value v: ie.getArgs()) {
					if (v instanceof Constant) args.add(null);
					else if (v instanceof Local) args.add(NameManager.getIndetifier(sm, v).var);
					else assert(false);
				}
				Anderson.newCall(rax, o, m, args); /* need to fill */
			}
			if (u instanceof ReturnVoidStmt)
				continue;
			if (u instanceof ReturnStmt) {
				ReturnStmt re = ((ReturnStmt) u);
				System.out.println("\t[Return]" + re.getOp());
				if (re.getOp() instanceof NullConstant) continue; 
				assert(re.getOp() instanceof Local) ;
				TAI retvar = NameManager.getIndetifier(sm, re.getOp());
				Anderson.newEdge(retvar.var, NameManager.getReturnIdentifier(sig));
			}
		}
	}

	protected String fancyTransform() {
		/*
		QueueReader<Edge> qc = Scene.v().getCallGraph().listener();
		while (qc.hasNext()) {
			Edge edge = qc.next();
			if (!shouldAnalysis(edge.src())) continue;
			System.out.println(edge.src() + " -> " + edge.tgt());
			System.out.println(edge.srcStmt());
			System.out.println(edge.kind().name());
		}
		if (true) return "ha";
		*/
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();
		
		while (qr.hasNext()) {
			SootMethod sm = qr.next().method();
			buildCFG(sm);
		}
		Anderson.run();
		return this.generateOutput();
	}
	protected String generateOutput() {
		String answer = "";
		for (Entry<Integer, TreeSet<String> > q: queries.entrySet()) {
			answer += q.getKey() + ":";
			TreeSet<Integer> Alloc = new TreeSet<>();
			for (String var: q.getValue()) /* queries: queryId -> set{vars} */
			if (Anderson.Var2Heap.containsKey(var))
				for (Integer heap: Anderson.Var2Heap.get(var)) { /* Var2Heap: var -> heapId */
					assert (Heap2Alloc.containsKey(heap));
					Alloc.add(Heap2Alloc.get(heap));
				}
			
			for (Integer alloc: Alloc)
				answer += " " + alloc;
			answer += "\n";
		}
		return answer;
	}

	/* naive implementation */
	protected String naiveTransform() {
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> qr = reachableMethods.listener();
		List<Integer> allocList = new ArrayList<Integer>();
		List<Integer> queryList = new ArrayList<Integer>();
		while (qr.hasNext()) {
			SootMethod sm = qr.next().method();
			if (!sm.hasActiveBody()) continue;
			for (Unit u : sm.getActiveBody().getUnits()) {
				if (! (u instanceof InvokeStmt)) continue;
				InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
				String callee = ie.getMethod().toString();
				if (callee.equals("<benchmark.internal.BenchmarkN: void alloc(int)>"))
					allocList.add((Integer) ((IntConstant) ie.getArgs().get(0)).value);
				if (callee.equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>"))
					queryList.add((Integer) ((IntConstant) ie.getArgs().get(0)).value);
			}
		}
		String answer = "";
		for (Integer query: queryList) {
			answer += query + ":";
			for (Integer alloc: allocList)
				answer += " " + alloc;
			answer += "\n";
		}
		return answer;
	}
	
	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {
		String answer;
		answer = this.fancyTransform();
		try {
			// answer = this.fancyTransform();
		}
		catch (Exception e) {
			// answer = this.naiveTransform();
			System.out.println("Error !");
		}
		AnswerPrinter.printAnswer(answer);
	}
}
