package org.labrad.clusters;

public class Cluster3<A,B,C> {
	private A a;
	private B b;
	private C c;
	
	
	Cluster3(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	public A get0() { return a; }
	public B get1() { return b; }
	public C get2() { return c; }
}
