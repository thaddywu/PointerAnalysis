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
import soot.jimple.DynamicInvokeExpr;
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
	static Map< Integer, SootClass > Heap2Class = new TreeMap<>();
	static Map< Integer, Integer > Heap2Alloc = new TreeMap<>();
	static Map< Integer, TreeSet<String> > queries = new TreeMap<>();
	
	static Integer hashMod = 133;
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
	
	static TreeSet<String> cfgBuilt = new TreeSet<>();
	static void buildCFG(SootMethod sm, int smctx) throws Exception {
		/* Inter procedural pointer analysis */
		if (!sm.hasActiveBody()) return ;
		if (!shouldAnalysis(sm)) return ;
		
		String cfgName = sm.toString() + smctx;
		if (cfgBuilt.contains(cfgName)) return ;
		cfgBuilt.add(cfgName);

		// System.out.println();
		// System.out.println(sm);
		for (Unit u : sm.getActiveBody().getUnits()) {
			// System.out.println("\t" + u);

			String sig = sm.getSignature();
			int ctxId = (contextId++) % hashMod;

			if (u instanceof AssignStmt)
				Myassert.myassert (u instanceof DefinitionStmt);
				
			/* Instrumented */
			if (u instanceof InvokeStmt) {
				InvokeExpr ie = ((InvokeStmt) u).getInvokeExpr();
				String callee = ie.getMethod().toString();
				if (callee.equals("<benchmark.internal.BenchmarkN: void alloc(int)>") || callee.equals("<benchmark.internal.Benchmark: void alloc(int)>"))
					{ allocId = ((IntConstant)ie.getArg(0)).value; continue; }
				if (callee.equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>") || callee.equals("<benchmark.internal.Benchmark: void test(int,java.lang.Object)>")) {
					Integer queryId = ((IntConstant)ie.getArg(0)).value;
					TAI var = NameManager.getIndetifier(sm, ie.getArg(1), smctx);
					Myassert.myassert (!var.hasField() && !var.isNull());
					
					/* add node in super-CFG, inefficient implementation! */
					// System.out.println("\t[Query] " + var.var + " " + queryId);
					if (!queries.containsKey(queryId))
						queries.put(queryId, new TreeSet<>());
					queries.get(queryId).add(var.var);
					continue;
				}
				/* skip auxiliary function */
			} 

			if ((u instanceof DefinitionStmt) && (((DefinitionStmt) u).getRightOp() instanceof AnyNewExpr) ) {
				DefinitionStmt au = (DefinitionStmt) u;
				AnyNewExpr ae = (AnyNewExpr) au.getRightOp();
				// System.out.println("\t[New]" + ae.getType() + " alloc:" + allocId + " heap:" + heapId);
				Myassert.myassert (au.getLeftOp() instanceof Local);
				// System.out.println("\t[Alloc] " + NameManager.getIndetifier(sm, au.getLeftOp()).var);
				Anderson.passSingleton(heapId, NameManager.getIndetifier(sm, au.getLeftOp(), smctx).var);
				if (allocId != -1) Heap2Alloc.put(heapId, allocId);
				if (au.getRightOp() instanceof NewArrayExpr || au.getRightOp() instanceof NewMultiArrayExpr) {
					/* Distinguish array and its item, allocate a heap space for its item */
					Anderson.passSingleton(heapId + 1, NameManager.getArrayItemIdentifier(heapId));
					if (allocId != -1) Heap2Alloc.put(heapId + 1, allocId);
					heapId += 1;
				}
				else {
					Myassert.myassert (au.getRightOp() instanceof NewExpr);
					NewExpr ne = (NewExpr) au.getRightOp();
					// System.out.println("\t[NewExpr] " + ne.getBaseType().getSootClass());
					Heap2Class.put(heapId, ne.getBaseType().getSootClass() );
				}
				heapId = heapId + 1; allocId = -1;
				continue;
			}
			

			if (u instanceof InvokeStmt || (u instanceof DefinitionStmt && ((DefinitionStmt) u).getRightOp() instanceof InvokeExpr)) {
				InvokeExpr ie; Value rax = null; // return value -> rax
				if (u instanceof InvokeStmt)
					ie = ((InvokeStmt) u).getInvokeExpr();
				else {
					ie = (InvokeExpr) ((DefinitionStmt) u).getRightOp();
					rax = ((DefinitionStmt) u).getLeftOp();
					Myassert.myassert (rax instanceof Local);
				}

				String retvar = null;
				if (rax != null) retvar = NameManager.getIndetifier(sm, rax, smctx).var;

				Myassert.myassert (!(ie instanceof DynamicInvokeExpr));
				// System.out.println("\t[Invoke]" + ie + " rax:" + rax);
				// System.out.println("\t[method]" + m + " " + m.getDeclaringClass());
				// System.out.println("\t[type]" + (ie instanceof StaticInvokeExpr) + " " + (ie instanceof InstanceInvokeExpr) );
				
				/* args */
				List<String> args = new ArrayList<>();
				for (Value v: ie.getArgs()) {
					if (v instanceof Constant) args.add(null);
					else if (v instanceof Local) args.add(NameManager.getIndetifier(sm, v, smctx).var);
					else Myassert.myassert(false);
				}

				if (ie instanceof StaticInvokeExpr) {
					StaticInvokeExpr sie = (StaticInvokeExpr) ie;
					Anderson.newStaticCall(retvar, sie.getMethod(), args, ctxId);
				}
				else {
					InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
					Value base = iie.getBase();
					Myassert.myassert (base instanceof Local);
					Anderson.newCall(retvar, NameManager.getIndetifier(sm, base, smctx).var, iie.getMethod(), args, ctxId);
				}
				
				// Anderson.newCall(rax, o, m, args); /* need to fill */
				continue ;
			}

			if (u instanceof DefinitionStmt) {
				DefinitionStmt au = (DefinitionStmt) u;
				TAI lop = NameManager.getIndetifier(sm, au.getLeftOp(), smctx);
				TAI rop = NameManager.getIndetifier(sm, au.getRightOp(), smctx);
				// System.out.println("\t[Definition]" + lop + " " + rop);
				if (lop.isNull() || rop.isNull()) continue;
				if (lop.hasField()) /* get: a.f = x */
					Anderson.newGet(lop.var, rop.var, lop.field);
				else if (rop.hasField()) /* put: a = x.f */
					Anderson.newPut(lop.var, rop.var, rop.field);
				else /* a = x */
					Anderson.newEdge(rop.var, lop.var);
				continue ;
			}
			if (u instanceof ReturnVoidStmt)
				continue;
			if (u instanceof ReturnStmt) {
				ReturnStmt re = ((ReturnStmt) u);
				// System.out.println("\t[Return]" + re.getOp());
				if (re.getOp() instanceof Constant) continue; 
				Myassert.myassert(re.getOp() instanceof Local) ;
				TAI retvar = NameManager.getIndetifier(sm, re.getOp(), smctx);
				Anderson.newEdge(retvar.var, NameManager.getReturnIdentifier(sm, smctx));
			}
		}
	}

	protected String fancyTransform() throws Exception {
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
			// for (int i = 0; i < hashMod; i++)
			//	buildCFG(sm, i);
			buildCFG(sm, 0);
		}
		Anderson.run();
		return this.generateOutput();
	}
	protected String generateOutput() throws Exception {
		String answer = "";
		for (Entry<Integer, TreeSet<String> > q: queries.entrySet()) {
			answer += q.getKey() + ":";
			// System.out.println(q.getKey() + ":");
			// for (String var: q.getValue())
			// System.out.println("\t" + var);

			TreeSet<Integer> Alloc = new TreeSet<>();
			for (String var: q.getValue()) /* queries: queryId -> set{vars} */
			if (Anderson.Var2Heap.containsKey(var))
				for (Integer heap: Anderson.Var2Heap.get(var))
				if (Heap2Alloc.containsKey(heap)){ /* Var2Heap: var -> heapId */
					// Myassert.myassert (Heap2Alloc.containsKey(heap));
					Alloc.add(Heap2Alloc.get(heap));
				}
			
			for (Integer alloc: Alloc)
				answer += " " + alloc;
			answer += "\n";
		}
		System.out.println(answer);
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
				if (callee.equals("<benchmark.internal.BenchmarkN: void alloc(int)>") || callee.equals("<benchmark.internal.Benchmark: void alloc(int)>"))
					allocList.add((Integer) ((IntConstant) ie.getArgs().get(0)).value);
				if (callee.equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>") || callee.equals("<benchmark.internal.Benchmark: void test(int,java.lang.Object)>"))
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
		try {
			// Myassert.myassert (false);
			answer = this.fancyTransform();
		}
		catch (Exception e) {
			answer = this.naiveTransform();
			// answer = "";
			e.printStackTrace();
		}
		AnswerPrinter.printAnswer(answer);
	}
}
