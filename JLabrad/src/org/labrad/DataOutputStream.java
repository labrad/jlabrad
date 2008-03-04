package org.labrad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.lang.RuntimeException;
import java.nio.charset.Charset;

public class DataOutputStream extends ByteArrayOutputStream {

    void flatten(boolean data) {
        if (data) {
            this.write(1);
        } else {
            this.write(0);
        }
    }

    void flatten(int data) {
        this.write((data & 0xFF000000) >> 24);
        this.write((data & 0x00FF0000) >> 16);
        this.write((data & 0x0000FF00) >> 8);
        this.write((data & 0x000000FF) >> 0);
    }

    void flatten(long data) {
        this.write((int) ((data & 0xFF000000) >> 24));
        this.write((int) ((data & 0x00FF0000) >> 16));
        this.write((int) ((data & 0x0000FF00) >> 8));
        this.write((int) ((data & 0x000000FF) >> 0));
    }

    void flatten(Date data) {
        // TODO: this implementation is bogus!
        flatten(data.getTime());
        flatten(0L);
    }

    static final Charset charset = Charset.forName("ISO-8859-1");

    void flatten(String data) {
        flatten(data.length());
        try {
            this.write(data.getBytes(charset));
        } catch (IOException e) {
            throw new RuntimeException("Error while flattening string.");
        }
    }

    void flatten(double data) {
        flatten(Double.doubleToRawLongBits(data));
    }

    void flatten(Complex data) {
        flatten(data.real);
        flatten(data.imag);
    }

    void flatten(Object[] data) {
        for (Object item : data) {
            flatten(item);
        }
    }

    void flatten(Object data) {
        if (data instanceof Boolean) {
            flatten((Boolean) data);
        } else if (data instanceof Integer) {
            flatten((Integer) data);
        } else if (data instanceof Long) {
            flatten((Long) data);
        } else if (data instanceof Date) {
            flatten((Data) data);
        } else if (data instanceof String) {
            flatten((String) data);
        } else if (data instanceof Double) {
            flatten((Double) data);
        } else if (data instanceof Complex) {
            flatten((Complex) data);
        } else if (data instanceof Object[]) {
            flatten((Object[]) data);
        }
        throw new RuntimeException("Cannot flatten class "
                + data.getClass().toString() + ".");
    }
}
