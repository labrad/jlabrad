package org.labrad.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Vector;

import org.labrad.types.Type;

/**
 * The Data class encapsulates the data format used to communicate between
 * LabRAD servers and clients.  This data format is strongly based on the
 * capabilities of LabVIEW, from National Instruments.  Each piece of LabRAD
 * data has a Type object which is specified by a String type tag.
 */
public class Data {
    private static final String ENCODING = "ISO-8859-1";
    public static final Data EMPTY = new Data("");

    Type type;
    byte[] data;
    int ofs;
    Vector<byte[]> heap;

    /**
     * Construct a Data object for a given LabRAD type tag.
     * 
     * @param tag
     *            the LabRAD type tag of this Data object.
     */
    public Data(String tag) {
        this(Type.parse(tag));
    }

    /**
     * Construct a Data object for a given Type object.
     * 
     * @param tag
     *            the LabRAD Type of this Data object.
     */
    public Data(Type type) {
        this.type = type;
        data = new byte[type.dataWidth()];
        ofs = 0;
        heap = new Vector<byte[]>();
    }

    /**
     * Construct a Data object from a Type and raw data. This constructor is
     * used internally in unflattening, and also to construct "views" into a
     * pre-existing Data object.
     * 
     * @param type
     *            a Type object, a parsed version of the LabRAD type tag
     * @param data
     *            byte array of data for this object
     * @param ofs
     *            offset into the data byte array. allows "views" into data
     * @param heap
     *            storage for pieces of variable-length data
     */
    private Data(Type type, byte[] data, int ofs, Vector<byte[]> heap) {
        this.type = type;
        this.data = data;
        this.ofs = ofs;
        this.heap = heap;
    }

    /**
     * Get the LabRAD type of this data object, as a Type object.
     * 
     * @return
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the LabRAD type tag string of this data object.
     * 
     * @return
     */
    public String getTag() {
        return type.toString();
    }

    /**
     * Flatten LabRAD data into an array of bytes, suitable for sending over the wire.
     */
    public byte[] flatten() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        flatten(os, type, data, ofs, heap);
        return os.toByteArray();
    }

    /**
     * Flatten LabRAD data into the specified ByteArrayOutputStream.
     * 
     * Start flattening the specified buffer at some offset, using the given heap
     * for variable-length data chunks, and the Type object to specify how the
     * bytes are to be interpreted.
     * 
     * @param os
     * @param type
     * @param buf
     * @param ofs
     * @param heap
     * @throws IOException
     */
    private void flatten(ByteArrayOutputStream os, Type type,
    		             byte[] buf, int ofs, Vector<byte[]> heap) throws IOException {
        int i;
        switch (type.getCode()) {
            case '_': break; // do nothing for empty data
            case 'b': os.write(buf, ofs, 1); break;
            case 'i': os.write(buf, ofs, 4); break;
            case 'w': os.write(buf, ofs, 4); break;
            case 'v': os.write(buf, ofs, 8); break;
            case 'c': os.write(buf, ofs, 16); break;
            case 't': os.write(buf, ofs, 16); break;

            case 's':
                byte[] sbuf = heap.get(ByteManip.getInt(buf, ofs));
                ByteManip.writeInt(os, sbuf.length);
                os.write(sbuf);
                break;

            case '*':
                int depth = type.getDepth();
                Type elementType = type.getSubtype(0);
                int size = 1;
                for (i = 0; i < depth; i++) {
                    size *= ByteManip.getInt(buf, ofs + 4 * i);
                }
                os.write(buf, ofs, 4 * depth);
                // write data from array
                byte[] lbuf = heap.get(ByteManip.getInt(buf, ofs + 4 * depth));
                for (i = 0; i < size; i++) {
                    flatten(os, elementType, lbuf, elementType.dataWidth() * i,
                            heap);
                }
                break;

            case '(':
                for (i = 0; i < type.size(); i++) {
                    flatten(os, type.getSubtype(i), buf, ofs
                            + type.getOffset(i), heap);
                }
                break;

            case 'E':
                String tag = "is" + type.getSubtype(0).toString();
                flatten(os, Type.parse(tag), buf, ofs, heap);
                break;

            default:
                throw new RuntimeException("Unknown type.");
        }
    }

    /**
     * Unflatten bytes from the specified buffer into Data, according to the tag.
     * 
     * @param buf
     * @param tag
     * @return
     * @throws IOException
     */
    public static Data unflatten(byte[] buf, String tag) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(buf);
        Type type = Type.parse(tag);
        return unflatten(is, type);
    }

    /**
     * Unflatten bytes from the specified buffer into Data, according to the Type.
     * 
     * @param buf
     * @param type
     * @return
     * @throws IOException
     */
    public static Data unflatten(byte[] buf, Type type) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(buf);
        return unflatten(is, type);
    }

    /**
     * Unflatten from an input stream of bytes, according to the tag.
     * 
     * @param is
     * @param tag
     * @return
     * @throws IOException
     */
    public static Data unflatten(ByteArrayInputStream is, String tag)
            throws IOException {
        Type type = Type.parse(tag);
        return unflatten(is, type);
    }

    /**
     * Unflatten from an input stream of bytes, according to Type.
     * 
     * @param is
     * @param type
     * @return
     * @throws IOException
     */
    public static Data unflatten(ByteArrayInputStream is, Type type)
            throws IOException {
        byte[] data = new byte[type.dataWidth()];
        Vector<byte[]> heap = new Vector<byte[]>();
        unflatten(is, type, data, 0, heap);
        return new Data(type, data, 0, heap);
    }

    /**
     * Unflatten from a stream of bytes according to type, into the middle
     * of a Data object, as specified by the byte buffer, offset, and heap.
     * 
     * @param is
     * @param type
     * @param buf
     * @param ofs
     * @param heap
     * @throws IOException
     */
    private static void unflatten(ByteArrayInputStream is, Type type,
            byte[] buf, int ofs, Vector<byte[]> heap) throws IOException {
        int i;
        switch (type.getCode()) {
            case '_': break; // do nothing for empty data
            case 'b': is.read(buf, ofs, 1); break;
            case 'i': is.read(buf, ofs, 4); break;
            case 'w': is.read(buf, ofs, 4); break;
            case 'v': is.read(buf, ofs, 8); break;
            case 'c': is.read(buf, ofs, 16); break;
            case 't': is.read(buf, ofs, 16); break;

            case 's':
                int len = ByteManip.readInt(is);
                byte[] sbuf = new byte[len];
                ByteManip.setInt(buf, ofs, heap.size());
                heap.add(sbuf);
                is.read(sbuf, 0, len);
                break;

            case '*':
                int depth = type.getDepth();
                Type elementType = type.getSubtype(0);
                int elementWidth = elementType.dataWidth();
                is.read(buf, ofs, 4 * depth);
                int size = 1;
                for (i = 0; i < depth; i++) {
                    size *= ByteManip.getInt(buf, ofs + 4 * i);
                }
                byte[] lbuf = new byte[elementWidth * size];
                ByteManip.setInt(buf, ofs + 4 * depth, heap.size());
                heap.add(lbuf);
                for (i = 0; i < size; i++) {
                    unflatten(is, type.getSubtype(0), lbuf, elementWidth * i,
                            heap);
                }
                break;

            case '(':
                for (i = 0; i < type.size(); i++) {
                    unflatten(is, type.getSubtype(i), buf, ofs
                            + type.getOffset(i), heap);
                }
                break;

            case 'E':
                String tag = "is" + type.getSubtype(0).toString();
                unflatten(is, Type.parse(tag), buf, ofs, heap);
                break;

            default:
                throw new RuntimeException("Unknown type.");
        }
    }

    /**
     * Return a human-readable, pretty-printed version of this LabRAD data,
     * or some indexed subpart of it.
     * 
     * @param indices
     * @return
     */
    public String pretty(int... indices) {
        return getData(indices).pretty();
    }

    /**
     * Return a pretty-printed version of this LabRAD data.
     * 
     * @return
     */
    public String pretty() {
        String s = "", u = "";
        switch (type.getCode()) {
            case '_': return "";
            case 'b': return Boolean.toString(getBool());
            case 'i': return Integer.toString(getInt());
            case 'w': return Long.toString(getWord());

            case 'v':
                s = Double.toString(getValue());
                u = type.getUnits();
                if (u != null) {
                    s += " [" + u + "]";
                }
                return s;

            case 'c':
                Complex c = getComplex();
                if (c.imag < 0) {
                    s = Double.toString(c.real) + Double.toString(c.imag) + "i";
                } else {
                    s = Double.toString(c.real) + "+" + Double.toString(c.imag)
                            + "i";
                }
                u = type.getUnits();
                if (u != null) {
                    s += " [" + u + "]";
                }
                return s;

            case 't': return getTime().toString();
            case 's': return "\"" + getStr() + "\"";

            case '*':
                int[] shape = getArrayShape();
                int[] indices = new int[type.getDepth()];
                return prettyList(shape, indices, 0);

            case '(':
                for (int i = 0; i < getClusterSize(); i++) {
                    s += ", " + pretty(i);
                }
                return "(" + s.substring(2) + ")";

            case 'E':
                return "Error(" + Integer.toString(getErrorCode()) + ", "
                        + getErrorMessage() + ")";

            default:
                throw new RuntimeException("Unknown type: " + type.pretty() + ".");
        }
    }

    private String prettyList(int[] shape, int[] indices, int level) {
        String s = "";
        for (int i = 0; i < shape[level]; i++) {
            indices[level] = i;
            if (level == shape.length - 1) {
                s += ", " + pretty(indices);
            } else {
                s += ", " + prettyList(shape, indices, level + 1);
            }
        }
        return "[" + s.substring(2) + "]";
    }

    public boolean isEmpty() {
        return type instanceof org.labrad.types.Empty;
    }

    public boolean isError() {
        return type instanceof org.labrad.types.Error;
    }
    
    /**
     * getSubtype
     * 
     * Extract a subtype from this data object as specified
     * by the given indices.  Also check that the type at this
     * location is a subtype of the specified type.
     * 
     * @param code
     * @param indices
     * @return
     */
    private Type getSubtype(char code, int... indices) {
    	Type type = getSubtype(indices);
    	if (type.getCode() != code) {
    		String indicesList = "";
    		for (int i : indices) {
    			indicesList += ", " + Integer.toString(i);
    		}
    		indicesList = "[" + indicesList.substring(2) + "]";
    		throw new RuntimeException(
    				"Type mismatch: expecting " + code + " at " + indicesList +
    				" but found " + type.getCode() + " instead.");
    	}
    	return type;
    }
    
    /**
     * getSubtype, but without typechecking
     * @param indices
     * @return
     */
    private Type getSubtype(int... indices) {
        Type type = this.type;
        int dimsLeft = 0;
        for (int i : indices) {
            if (type instanceof org.labrad.types.List) {
                if (dimsLeft == 0) {
                    dimsLeft = type.getDepth();
                }
                dimsLeft--;
                if (dimsLeft == 0) {
                    type = type.getSubtype(i);
                }
            } else if (type instanceof org.labrad.types.Cluster) {
                type = type.getSubtype(i);
            } else {
                throw new RuntimeException("Non-indexable type.");
            }
        }
        if (dimsLeft != 0) {
            throw new RuntimeException("Not enough indices for array.");
        }
        return type;
    }
    
    /**
     * get a view into the data array at the position specified by indices.
     * @param indices
     * @return
     */
    private ByteArrayView getOffset(int... indices) {
        Type type = this.type;
        byte[] data = this.data;
        int depth = 0, dimsLeft = 0;
        int[] shape = {}, listIndices = {};
        int ofs = this.ofs;
        for (int i : indices) {
            switch (type.getCode()) {
                case '*':
                    if (dimsLeft == 0) {
                        // read list shape
                        depth = type.getDepth();
                        shape = new int[depth];
                        listIndices = new int[depth];
                        for (int j = 0; j < depth; j++) {
                            shape[j] = ByteManip.getInt(data, ofs + 4 * j);
                        }
                        dimsLeft = depth;
                        data = heap.get(ByteManip.getInt(data, ofs + 4 * depth));
                    }
                    // read one listIndex
                    listIndices[depth - dimsLeft] = i;
                    dimsLeft--;
                    if (dimsLeft == 0) {
                        // set type to be element type of array
                        type = type.getSubtype(0);
                        ofs = 0;
                        // calculate offset into array
                        int product = 1;
                        for (int dim = depth - 1; dim >= 0; dim--) {
                            ofs += type.dataWidth() * listIndices[dim]
                                    * product;
                            product *= shape[dim];
                        }
                    }
                    break;

                case '(':
                    ofs += type.getOffset(i);
                    type = type.getSubtype(i);
                    break;

                default:
                    throw new RuntimeException("Non-indexable type: "
                            + type.getCode() + ".");
            }
        }
        if (dimsLeft != 0) {
            throw new RuntimeException("Not enough indices for array.");
        }
        return new ByteArrayView(data, ofs);
    }

    /**
     * Get a Data view into a subobject given by indices.
     * @param indices
     * @return
     */
    public Data getData(int... indices) {
        Type type = getSubtype(indices);
        ByteArrayView pos = getOffset(indices);
        return new Data(type, pos.getBytes(), pos.getOffset(), heap);
    }

    
    // boolean
    public boolean isBool(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.Bool;
    }

    public boolean getBool(int... indices) {
    	getSubtype('b', indices);
        return ByteManip.getBool(getOffset(indices));
    }

    public Data setBool(boolean data, int... indices) {
    	getSubtype('b', indices);
        ByteManip.setBool(getOffset(indices), data);
        return this;
    }

    
    // int
    public boolean isInt(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.Int;
    }

    public int getInt(int... indices) {
    	getSubtype('i', indices);
        return ByteManip.getInt(getOffset(indices));
    }

    public Data setInt(int data, int... indices) {
    	getSubtype('i', indices);
        ByteManip.setInt(getOffset(indices), data);
        return this;
    }

    
    // word
    public boolean isWord(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.Word;
    }

    public long getWord(int... indices) {
    	getSubtype('w', indices);
        return ByteManip.getWord(getOffset(indices));
    }

    public Data setWord(long data, int... indices) {
    	getSubtype('w', indices);
        ByteManip.setWord(getOffset(indices), data);
        return this;
    }

    
    // str
    public boolean isStr(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.Str;
    }

    public String getStr(int... indices) {
        try {
            return new String(getBytes(indices), ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported string encoding.");
        }
    }

    public Data setStr(String data, int... indices) {
        try {
            setBytes(data.getBytes(ENCODING), indices);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported string encoding.");
        }
        return this;
    }

    
    // bytes
    public boolean isBytes(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.Str;
    }

    public byte[] getBytes(int... indices) {
    	getSubtype('s', indices);
        return heap.get(ByteManip.getInt(getOffset(indices)));
    }

    public Data setBytes(byte[] data, int... indices) {
    	getSubtype('s', indices);
        ByteManip.setInt(getOffset(indices), heap.size());
        heap.add(data);
        return this;
    }

    
    // value
    public boolean isValue(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.Value;
    }

    public double getValue(int... indices) {
    	getSubtype('v', indices);
        return ByteManip.getDouble(getOffset(indices));
    }

    public Data setValue(double data, int... indices) {
    	getSubtype('v', indices);
        ByteManip.setDouble(getOffset(indices), data);
        return this;
    }

    
    // complex
    public boolean isComplex(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.Complex;
    }

    public Complex getComplex(int... indices) {
    	getSubtype('c', indices);
        return ByteManip.getComplex(getOffset(indices));
    }

    public Data setComplex(Complex data, int... indices) {
    	getSubtype('c', indices);
        ByteManip.setComplex(getOffset(indices), data);
        return this;
    }

    public Data setComplex(double re, double im, int... indices) {
    	return setComplex(new Complex(re, im), indices);
    }

    public boolean hasUnits(int... indices) {
        Type type = getSubtype(indices);
        return ((type instanceof org.labrad.types.Value) || (type instanceof org.labrad.types.Complex))
                && (type.getUnits() != null);
    }

    public String getUnits(int... indices) {
    	return getSubtype(indices).getUnits();
    }

    
    // time
    // TODO: correct time translation
    public boolean isTime(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.Time;
    }

    public byte[] getTime(int... indices) {
    	getSubtype('t', indices);
        return ByteManip.getTime(getOffset(indices));
    }

    public Data setTime(byte[] data, int... indices) {
    	getSubtype('t', indices);
        ByteManip.setTime(getOffset(indices), data);
        return this;
    }

    
    // arrays
    public boolean isArray(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.List;
    }

    public int[] getArrayShape(int... indices) {
    	Type type = getSubtype('*', indices);
        int depth = type.getDepth();
        int[] shape = new int[depth];
        ByteArrayView pos = getOffset(indices);
        for (int i = 0; i < depth; i++) {
            shape[i] = ByteManip.getInt(pos.getBytes(), pos.getOffset() + 4*i);
        }
        return shape;
    }

    public int getArraySize(int... indices) {
    	int[] shape = getArrayShape(indices);
    	if (shape.length > 1) {
    		throw new RuntimeException("Can't get size of multi-dimensional array.  Use getArrayShape.");
    	}
        return shape[0];
    }
    
    public Data setArraySize(int size, int... indices) {
        setArrayShape(new int[] {size}, indices);
        return this;
    }

    public Data setArrayShape(int... shape) {
        setArrayShape(shape, new int[] {});
        return this;
    }

    public Data setArrayShape(int[] shape, int... indices) {
        Type type = getSubtype('*', indices);
        Type elementType = type.getSubtype(0);
        int depth = type.getDepth();
        if (shape.length != depth) {
            throw new RuntimeException("Array depth mismatch!");
        }
        ByteArrayView pos = getOffset(indices);
        int size = 1;
        for (int i = 0; i < depth; i++) {
            ByteManip.setInt(pos.getBytes(), pos.getOffset() + 4*i, shape[i]);
            size *= shape[i];
        }
        byte[] buf = new byte[elementType.dataWidth() * size];
        ByteManip.setInt(pos.getBytes(), pos.getOffset() + 4*depth, heap.size());
        heap.add(buf);
        return this;
    }

    
    // clusters
    public boolean isCluster(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.Cluster;
    }

    public int getClusterSize(int... indices) {
        return getSubtype('(', indices).size();
    }

    
    // errors
    public boolean isError(int... indices) {
        return getSubtype(indices) instanceof org.labrad.types.Error;
    }

    public int getErrorCode(int... indices) {
    	getSubtype('E', indices);
        return ByteManip.getInt(getOffset(indices));
    }

    public String getErrorMessage(int... indices) {
    	getSubtype('E', indices);
        ByteArrayView pos = getOffset(indices);
        int index = ByteManip.getInt(pos.getBytes(), pos.getOffset() + 4);
        try {
            return new String(heap.get(index), ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported string encoding.");
        }
    }

    public Data getErrorPayload(int... indices) {
        Type type = getSubtype('E', indices);
        ByteArrayView pos = getOffset(indices);
        return new Data(type.getSubtype(0),
        		        pos.getBytes(), pos.getOffset() + 8, heap);
    }

    public Data setError(int code, String message, int... indices) {
    	getSubtype('E', indices);
        ByteArrayView pos = getOffset(indices);
        ByteManip.setInt(pos.getBytes(), pos.getOffset(), code);
        ByteManip.setInt(pos.getBytes(), pos.getOffset()+4, heap.size());
        try {
        	heap.add(message.getBytes(ENCODING));
        } catch (UnsupportedEncodingException e) {
        	throw new RuntimeException("Unicode encoding exception.");
        }
        return this;
    }

    public static void main(String[] args) throws IOException {
        byte[] bs = new byte[100];
        Random rand = new Random();
        int count;

        boolean b;
        for (count = 0; count < 1000; count++) {
            b = rand.nextBoolean();
            ByteManip.setBool(bs, 0, b);
            assert b == ByteManip.getBool(bs, 0);
        }
        System.out.println("Bool okay.");

        int i;
        for (count = 0; count < 1000000; count++) {
            i = rand.nextInt();
            ByteManip.setInt(bs, 0, i);
            assert i == ByteManip.getInt(bs, 0);
        }
        System.out.println("Int okay.");

        long l;
        for (count = 0; count < 1000000; count++) {
            l = Math.abs(rand.nextLong()) % 4294967296L;
            ByteManip.setWord(bs, 0, l);
            assert l == ByteManip.getWord(bs, 0);
        }
        System.out.println("Word okay.");

        for (count = 0; count < 1000000; count++) {
            l = rand.nextLong();
            ByteManip.setLong(bs, 0, l);
            assert l == ByteManip.getLong(bs, 0);
        }
        System.out.println("Long okay.");

        double d;
        for (count = 0; count < 100000; count++) {
            d = rand.nextGaussian();
            ByteManip.setDouble(bs, 0, d);
            assert d == ByteManip.getDouble(bs, 0);
        }
        System.out.println("Double okay.");

        double re, im;
        for (count = 0; count < 100000; count++) {
            re = rand.nextGaussian();
            im = rand.nextGaussian();
            Complex c1 = new Complex(re, im);
            ByteManip.setComplex(bs, 0, c1);
            Complex c2 = ByteManip.getComplex(bs, 0);
            assert (c1.real == c2.real) && (c1.imag == c2.imag);
        }
        System.out.println("Complex okay.");

        Data d1, d2;
        byte[] flat;

        d1 = new Data("i");
        d1.setInt(100);
        assert d1.getInt() == 100;

        d1 = new Data("s");
        d1.setStr("This is a test.");
        System.out.println(d1.getStr());

        d1 = new Data("*s");
        d1.setArraySize(20);
        for (count = 0; count < 20; count++) {
            d1.setStr("This is string " + Integer.toString(count), count);
        }
        for (count = 0; count < 20; count++) {
            System.out.println(d1.getStr(count));
        }

        d1 = new Data("biwsvc");
        b = rand.nextBoolean();
        i = rand.nextInt();
        l = Math.abs(rand.nextLong()) % 4294967296L;
        String s = Long.toString(rand.nextLong());
        d = rand.nextGaussian();
        re = rand.nextGaussian();
        im = rand.nextGaussian();

        d1.setBool(b, 0);
        d1.setInt(i, 1);
        d1.setWord(l, 2);
        d1.setStr(s, 3);
        d1.setValue(d, 4);
        d1.setComplex(re, im, 5);

        assert b == d1.getBool(0);
        assert i == d1.getInt(1);
        assert l == d1.getWord(2);
        assert s.equals(d1.getStr(3));
        assert d == d1.getValue(4);
        Complex c = d1.getComplex(5);
        assert re == c.real;
        assert im == c.imag;
        System.out.println("Cluster okay.");
        System.out.println(d1.pretty());

        d1 = new Data("*(biwsv[m]c[m/s])");
        d1.setArraySize(20);
        for (count = 0; count < 20; count++) {
            b = rand.nextBoolean();
            i = rand.nextInt();
            l = Math.abs(rand.nextLong()) % 4294967296L;
            s = Long.toString(rand.nextLong());
            d = rand.nextGaussian();
            re = rand.nextGaussian();
            im = rand.nextGaussian();

            d1.setBool(b, count, 0);
            d1.setInt(i, count, 1);
            d1.setWord(l, count, 2);
            d1.setStr(s, count, 3);
            d1.setValue(d, count, 4);
            d1.setComplex(re, im, count, 5);

            assert b == d1.getBool(count, 0);
            assert i == d1.getInt(count, 1);
            assert l == d1.getWord(count, 2);
            assert s.equals(d1.getStr(count, 3));
            assert d == d1.getValue(count, 4);
            c = d1.getComplex(count, 5);
            assert re == c.real;
            assert im == c.imag;
        }
        System.out.println("List of Cluster okay.");
        System.out.println(d1.pretty());

        flat = d1.flatten();
        d2 = unflatten(flat, "*(biwsv[m]c[m/s])");
        System.out.println(d2.pretty());

        // test multi-dimensional list
        d1 = new Data("*2i");
        d1.setArrayShape(4, 3);
        for (int m = 0; m < 4; m++) {
            for (int n = 0; n < 3; n++) {
                d1.setInt(rand.nextInt(), m, n);
            }
        }
        System.out.println(d1.pretty());
        flat = d1.flatten();
        d2 = unflatten(flat, "*2i");
        System.out.println(d2.pretty());

        d1 = new Data("*3s");
        d1.setArrayShape(2, 2, 2);
        for (int m = 0; m < 2; m++) {
            for (int n = 0; n < 2; n++) {
                for (int p = 0; p < 2; p++) {
                    d1.setStr("TestString(" + m + n + p + ")", m, n, p);
                }
            }
        }
        System.out.println(d1.pretty());
        flat = d1.flatten();
        d2 = unflatten(flat, "*3s");
        System.out.println(d2.pretty());

        System.out.println("done.");
    }
}
