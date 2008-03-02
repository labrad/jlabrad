package org.labrad.clusters;

public class Cluster4<A,B,C,D> {
	private A a;
	private B b;
	private C c;
	private D d;
	
	Cluster4(A a, B b, C c, D d) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}
	
	public A get0() { return a; }
	public B get1() { return b; }
	public C get2() { return c; }
	public D get3() { return d; }
}
