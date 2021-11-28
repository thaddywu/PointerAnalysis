package generalJava;

import benchmark.internal.Benchmark;
import benchmark.objects.A;
import benchmark.objects.B;

/*
 * @testcase Null1
 * 
 * @version 1.0
 * 
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 * 
 * @description Direct alias to null
 */
public class Null1 {

  public static void main(String[] args) {

    // No allocation site
    A h = new A();
    B a = h.getH();
    B b = a;
    Benchmark.test(1, b); /* 1: */
    // Benchmark.use(b);
  }
}
