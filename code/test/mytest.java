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
	int b = 0;
	static int c = 0;
	static int d = 0;
	int f() { return a ;}
	int g() { return b ;}
	int h() { return c ;}
	int k() { return d ;}
	static int l() { return 0; }
	static int r() { return 0; }
}
class son extends father {
	int b = 1;
	static int d = 1;
	int f() { return a ;}
	int g() { return b ;}
	int h() { return c ;}
	int k() { return d ;}
	static int l() { return 1; }
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
		System.out.println(x.a);
		System.out.println(x.b);
		System.out.println(x.c);
		System.out.println(x.d);
		System.out.println(x.f());
		System.out.println(x.g());
		System.out.println(x.h());
		System.out.println(x.k());
		System.out.println(x.l());
		System.out.println(x.r());
    
		son y = new son();
		System.out.println(y.a);
		System.out.println(y.b);
		System.out.println(y.c);
		System.out.println(y.d);
		System.out.println(y.f());
		System.out.println(y.g());
		System.out.println(y.h());
		System.out.println(y.k());
		System.out.println(y.l());
		System.out.println(y.r());
  }

  public void test4(int a) {
		father x = new son();
    int z = 0;
    z = x.c;
    
		son y = new son();
		z = y.c;
  }
  

  public static void main(String[] args) {
    // int a[] = new int[10];
    // int i =7;
    // a[i-1]=a[i+2];
    // BenchmarkN.alloc(9);
    // x.pack = new Pack(5);
    FieldSensitivity fs2 = new FieldSensitivity();
    fs2.test();
    fs2.test2();
    fs2.test3(77);
    fs2.test4(77);
  }

}
