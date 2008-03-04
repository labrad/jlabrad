package org.labrad.clusters;

public class Cluster {
    Object[] items;

    Cluster(Object... items) {
        this.items = items;
    }

    public Object get(int index) {
        return items[index];
    }
}
