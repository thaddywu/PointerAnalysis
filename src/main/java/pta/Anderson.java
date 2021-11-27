package pta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;
import java.lang.Comparable;

import soot.Local;

class CallRelation implements Comparable {
	public String retvar;
	public String object;
	public String method;
	public List<String> args;
	public String key;
	CallRelation(String retvar, String object, String method, List<String> args) {
		this.retvar = retvar; this.object = object; this.method = method; this.args = args ;
		this.key = this.retvar + this.object + this.method + this.args;
	}
	@Override
	public int compareTo(Object o) {
		return this.key.compareTo(((CallRelation) o).key);
	}

	/* When h added into Var2Heap(object),
		nabla(h, method) = m'
		args[0] -> $param0@m'
		$ret@m' -> retvar, if retvar
	*/
}
class PutRelation implements Comparable {
	public String a;
	public String field;
	public String key;
	PutRelation(String a, String field)
		{this.a = a; this.field = field; this.key = this.a + this.field; }
	@Override
	public int compareTo(Object o) {
		return this.key.compareTo(((PutRelation) o).key);
	}
	/* When h added into Var2Heap(x),
		h.f -> a
		a = x.field
	*/
}
class GetRelation implements Comparable {
	public String x;
	public String field;
	public String key;
	GetRelation(String x, String field)
		{this.x = x; this.field = field; this.key = this.x + this.field; }
	@Override
	public int compareTo(Object o) {
		return this.key.compareTo(((GetRelation) o).key);
	}
	/* When h added into Var2Heap(a),
		x -> h.f
		a.field = x
	*/
}
class QueueNode {
	public String node;
	public Integer heap;
	QueueNode(String node, Integer heap)
		{ this.node = node; this.heap = heap; }
}

class Anderson {
	static Queue<QueueNode> queue = new LinkedList<>();
	static Map< String, TreeSet<String>> edgeMap = new TreeMap<>();
	static Map< String, TreeSet<PutRelation>> putMap = new TreeMap<>();
	static Map< String, TreeSet<GetRelation>> getMap = new TreeMap<>();
	static Map< String, TreeSet<CallRelation>> callMap = new TreeMap<>();
	
	static Map< String, TreeSet<Integer>> Var2Heap = new TreeMap<>();
	
	static boolean passSingleton(Integer heap, String to) {
		if (!Var2Heap.containsKey(to))
			Var2Heap.put(to, new TreeSet<Integer>());
		if (Var2Heap.get(to).contains(heap)) return false;
		Var2Heap.get(to).add(heap);
		queue.add(new QueueNode(to, heap));
		return true;
	}
	static boolean passSet(String from, String to) {
		if (!Var2Heap.containsKey(from)) return false;
		if (!Var2Heap.containsKey(to))
			Var2Heap.put(to, new TreeSet<Integer>());

		boolean updated = false;
		for (Integer heap: Var2Heap.get(from))
		if (!Var2Heap.get(to).contains(heap)) {
			Var2Heap.get(from).add(heap);
			queue.add(new QueueNode(to, heap));
			updated = true;
		}
		return updated;
	}

	/* add new construct */
	static boolean newEdge(String u, String v) {
		if (!edgeMap.containsKey(u))
			edgeMap.put(u, new TreeSet<String>());
		if (edgeMap.get(u).contains(v)) return false;
		edgeMap.get(u).add(v);
		return true;
	}
	static boolean newPut(String a, String x, String f) {
		PutRelation pr = new PutRelation(a, f);
		if (!putMap.containsKey(x))
			putMap.put(x, new TreeSet<PutRelation>());
		if (putMap.get(x).contains(pr)) return false;
		putMap.get(x).add(pr);
		return true;
	}
	static boolean newGet(String a, String x, String f) {
		GetRelation gr = new GetRelation(x, f);
		if (!getMap.containsKey(a))
			getMap.put(a, new TreeSet<GetRelation>());
		if (getMap.get(a).contains(gr)) return false;
		getMap.get(a).add(gr);
		return true;
	}
	static boolean newCall(String a, String o, String f, List<String> args) {
		CallRelation cr = new CallRelation(a, o, f, args);
		if (!callMap.containsKey(o))
			callMap.put(o, new TreeSet<CallRelation>());
		if (callMap.get(o).contains(cr)) return false;
		callMap.get(o).add(cr);
		return true;
	}

	static void updateNewEdge(String u, String v) {
		if (newEdge(u, v)) passSet(u, v);
	}
	static void updateAllEdge(String from, Integer heap) {
		if (!edgeMap.containsKey(from)) return;
		for (String to: edgeMap.get(from))
			passSingleton(heap, to);
	}
	static void updateAllPut(String x, Integer heap) {
		if (!putMap.containsKey(x)) return;
		for (PutRelation pr: putMap.get(x))
			updateNewEdge(NameManager.getHeapIdentifier(heap, pr.field), pr.a);
	}
	static void updateAllGet(String a, Integer heap) {
		if (!getMap.containsKey(a)) return;
		for (GetRelation gr: getMap.get(a))
			updateNewEdge(gr.x, NameManager.getHeapIdentifier(heap, gr.field));
	}
	static void updateAllCall(String from, Integer heap) {
		if (!callMap.containsKey(from)) return;
		for (CallRelation cr: callMap.get(from)) {
			String sig = "1";
			assert (false);
			passSingleton(heap, NameManager.getThisIdentifier(sig));

			int index = 0;
			for (String arg: cr.args) {
				if (arg != null)
					updateNewEdge(arg, NameManager.getParamIdentifier(sig, index));
				index += 1;
			}
			if (cr.retvar != null)
				updateNewEdge(NameManager.getReturnIdentifier(sig), cr.retvar);
		}
	}
	static void run() {
		while (!queue.isEmpty()) {
			QueueNode qn = queue.remove();
			updateAllCall(qn.node, qn.heap);
			updateAllPut(qn.node, qn.heap);
			updateAllGet(qn.node, qn.heap);
			updateAllEdge(qn.node, qn.heap);
		}
	}
}