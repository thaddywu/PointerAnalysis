package test;
import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

import java.util.LinkedList;
import java.util.Queue;
import java.util.HashSet;

public class mytest1800012913 {
  public static HashSet<A> newset() {
    HashSet<A> set = new HashSet<A>();
    BenchmarkN.alloc(1);
    A a = new A();
    BenchmarkN.alloc(2);
    A b = new A();
    BenchmarkN.alloc(3);
    A c = new A();
    BenchmarkN.alloc(4);
    A d = new A();
    BenchmarkN.alloc(5);
    A e = new A();
    BenchmarkN.alloc(6);
    A f = new A();
    BenchmarkN.alloc(7);
    A g = new A();
    BenchmarkN.alloc(8);
    A h = new A();
    BenchmarkN.alloc(9);
    A i = new A();
    BenchmarkN.alloc(10);
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

      BenchmarkN.test(1, x);
  }

}
