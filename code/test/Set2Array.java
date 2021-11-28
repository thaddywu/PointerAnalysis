package test;

import benchmark.internal.Benchmark;
import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

import java.util.LinkedList;
import java.util.Queue;
import java.util.HashSet;

/*
 * @testcase FieldSensitivity2
 * 
 * @version 1.0
 * 
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 * 
 * @description Field Sensitivity without static method
 */

public class Set2Array {
  public static HashSet<A> newset() {
    HashSet<A> set = new HashSet<A>();
    Benchmark.alloc(1);
    A a = new A();
    Benchmark.alloc(2);
    A b = new A();
    Benchmark.alloc(3);
    A c = new A();
    Benchmark.alloc(4);
    A d = new A();
    Benchmark.alloc(5);
    A e = new A();
    Benchmark.alloc(6);
    A f = new A();
    Benchmark.alloc(7);
    A g = new A();
    Benchmark.alloc(8);
    A h = new A();
    Benchmark.alloc(9);
    A i = new A();
    Benchmark.alloc(10);
    A j = new A();
    set.add(e); /* alloc: 5 */
    return set;
  }
  static HashSet<A> transform(HashSet<A> in) {
    HashSet<A> out = new HashSet<>();
    // out.addAll(in);
    out = in;
    return out;
  }
  static Queue<A> transform2(HashSet<A> in) {
    Queue<A> out = new LinkedList<>();
    for (A x: in)
      out.add(x);
    return out;
  }
  static HashSet<A> transform3(Queue<A> in) {
    HashSet<A> out = new HashSet<>();
    while (!in.isEmpty()) {
      out.add(in.remove());
    }
    return out;
  }

  public static void main(String[] args) {
    
      HashSet<A> set = transform3(transform2(transform(newset())));

      A[] sample = new A[10];
      A[] x = set.toArray(sample);
      A y = null;
      for (A t: x)
        { y = t; break; }

      Benchmark.test(1, x);
  }

}
