package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

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

class Pack {
  Pack(int i) {item = i;}
  int item;
}

class father {
  int a = 0;
  static int b = 9;
  public int multi(int a) { return a; }
  public int multi(int a, int b) { return a + b; }
  public int get() { return this.multi(a) + this.multi(a,b); }
}
class son extends father {
  Pack pack;
  public int get() { return b; }
}

public class FieldSensitivity {

  public FieldSensitivity() {}

  private void assign(A x, A y) {
    y.f = x.f;
  }

  private void test() {
    BenchmarkN.alloc(1);
    B b = new B();
    BenchmarkN.alloc(2);
    A a = new A(b);
    BenchmarkN.alloc(3);
    A c = new A();
    BenchmarkN.alloc(4);
    B e = new B();
    assign(a, c);
    B d = c.f;

    BenchmarkN.test(1, d);
  }
  
  public void test2() {
    BenchmarkN.alloc(5);
    B b = new B();
    BenchmarkN.alloc(6);
    B c = new B();
    BenchmarkN.alloc(7);
    A a1 = new A();
    BenchmarkN.alloc(8);
    A a2 = new A();
    a1.f = b;
    a2.f = c;
    BenchmarkN.test(2, a1.f);
    BenchmarkN.test(3, a2.f);
  }
  

  public void test3(int a) {
    father x = new son();
    int r = x.get();
  }
  

  public static void main(String[] args) {
    // int a[] = new int[10];
    // int i =7;
    // a[i-1]=a[i+2];
    // BenchmarkN.alloc(9);
    // son x = new son();
    // x.pack = new Pack(5);
    FieldSensitivity fs2 = new FieldSensitivity();
    // fs2.test();
    // fs2.test2();
    fs2.test3(77);
  }

}
