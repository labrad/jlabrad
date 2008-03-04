package org.labrad.clusters;

public class Cluster2<A, B> {
    private A a;

    private B b;

    public Cluster2(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A get0() {
        return a;
    }

    public B get1() {
        return b;
    }
}
