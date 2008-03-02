package org.labrad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.RuntimeException;
import java.util.Random;
import java.util.Vector;
import org.labrad.types.Type;
import org.labrad.clusters.Cluster2;

public class Data {
	private static final String encoding = "ISO-8859-1";
	public static final Data EMPTY = new Data("");
	
	Type type;
	byte[] data;
	int ofs;
	Vector<byte[]> heap;
	
	/**
	 * Construct a Data object for a given LabRAD type tag.
	 * 
	 * @param tag	the LabRAD type tag of this Data object.
	 */
	Data(String tag) {
		this(Type.parse(tag));
	}
	
	/**
	 * Construct a Data object for a given Type object.
	 * 
	 * @param tag	the LabRAD Type of this Data object.
	 */
	Data(Type type) {
		this.type = type;
		data = new byte[type.dataWidth()];
		ofs = 0;
		heap = new Vector<byte[]>();
	}
	
	/**
	 * Construct a Data object from a Type and raw data.
	 * This constructor is used internally in unflattening, and
	 * also to construct "views" into a pre-existing Data object.
	 * 
	 * @param type	a Type object, a parsed version of the LabRAD type tag
	 * @param data	byte array of data for this object
	 * @param ofs	offset into the data byte array.  allows "views" into data
	 * @param heap	storage for pieces of variable-length data
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
	 * Get a boolean from a byte array.
	 * 
	 * @param buf	byte array of data.
	 * @param ofs	offset into byte array.
	 * @return
	 */
	private static boolean getBool(byte[] buf, int ofs) {
		return buf[ofs] != 0;
	}
	
	/**
	 * Store a boolean into a byte array.
	 * 
	 * @param buf	byte array of data.
	 * @param ofs	offset into byte array.
	 * @param data	boolean value to set.
	 */
	private static void setBool(byte[] buf, int ofs, boolean data) {
		buf[ofs] = data ? (byte)1 : (byte)0;
	}
//	public static boolean readBool(ByteArrayInputStream is) {
//		return is.read() != 0;
//	}
//	public static void writeBool(ByteArrayOutputStream os, boolean data) {
//		os.write(data ? 1 : 0);
//	}
	
	/**
	 * Get an int from a byte array.
	 * 
	 * @param buf	byte array of data.
	 * @param ofs	offset into byte array.
	 * @return
	 */
	private static int getInt(byte[] buf, int ofs) {
		return (int)((0xFF & (int)buf[ofs+0]) << 24
	               | (0xFF & (int)buf[ofs+1]) << 16
	               | (0xFF & (int)buf[ofs+2]) <<  8
	               | (0xFF & (int)buf[ofs+3]) <<  0);
	}
	
	/**
	 * Store an int into a byte array.
	 * 
	 * @param buf	byte array of data.
	 * @param ofs	offset into byte array.
	 * @param data	integer value to set.
	 */
	private static void setInt(byte[] buf, int ofs, int data) {
		buf[ofs+0] = (byte)((data & 0xFF000000) >> 24);
		buf[ofs+1] = (byte)((data & 0x00FF0000) >> 16);
		buf[ofs+2] = (byte)((data & 0x0000FF00) >>  8);
		buf[ofs+3] = (byte)((data & 0x000000FF) >>  0);
	}
	
	/**
	 * Read an int from a byte stream.
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	private static int readInt(ByteArrayInputStream is) throws IOException {
		byte[] bytes = new byte[4];
		is.read(bytes, 0, 4);
		return getInt(bytes, 0);
	}
	
	/**
	 * Write an int to a byte stream.
	 * 
	 * @param os
	 * @param data
	 * @throws IOException
	 */
	private static void writeInt(ByteArrayOutputStream os, int data) throws IOException {
		byte[] bytes = new byte[4];
		setInt(bytes, 0, data);
		os.write(bytes);
	}
	
	
	private static long getWord(byte[] buf, int ofs) {
		return (long)(0xFF & (int)buf[ofs+0]) << 24
             | (long)(0xFF & (int)buf[ofs+1]) << 16
             | (long)(0xFF & (int)buf[ofs+2]) <<  8
             | (long)(0xFF & (int)buf[ofs+3]) <<  0;
	}
	private static void setWord(byte[] buf, int ofs, long data) {
		buf[ofs+0] = (byte)((data & 0xFF000000) >> 24);
		buf[ofs+1] = (byte)((data & 0x00FF0000) >> 16);
		buf[ofs+2] = (byte)((data & 0x0000FF00) >>  8);
		buf[ofs+3] = (byte)((data & 0x000000FF) >>  0);
	}
//	public static long readWord(ByteArrayInputStream is) throws IOException {
//		byte[] bytes = new byte[4];
//		is.read(bytes, 0, 4);
//		return getWord(bytes, 0);
//	}
//	public static void writeWord(ByteArrayOutputStream os, long data) throws IOException {
//		byte[] bytes = new byte[4];
//		setWord(bytes, 0, data);
//		os.write(bytes);
//	}
	
	
	private static long getLong(byte[] buf, int ofs) {
	    return (long)(0xFF & (int)buf[ofs+0]) << 56
	         | (long)(0xFF & (int)buf[ofs+1]) << 48
             | (long)(0xFF & (int)buf[ofs+2]) << 40
             | (long)(0xFF & (int)buf[ofs+3]) << 32
             | (long)(0xFF & (int)buf[ofs+4]) << 24
             | (long)(0xFF & (int)buf[ofs+5]) << 16
             | (long)(0xFF & (int)buf[ofs+6]) <<  8
             | (long)(0xFF & (int)buf[ofs+7]) <<  0;
	}
	private static void setLong(byte[] buf, int ofs, long data) {
		buf[ofs+0] = (byte)((data & 0xFF00000000000000L) >> 56);
		buf[ofs+1] = (byte)((data & 0x00FF000000000000L) >> 48);
		buf[ofs+2] = (byte)((data & 0x0000FF0000000000L) >> 40);
		buf[ofs+3] = (byte)((data & 0x000000FF00000000L) >> 32);
		buf[ofs+4] = (byte)((data & 0x00000000FF000000L) >> 24);
		buf[ofs+5] = (byte)((data & 0x0000000000FF0000L) >> 16);
		buf[ofs+6] = (byte)((data & 0x000000000000FF00L) >>  8);
		buf[ofs+7] = (byte)((data & 0x00000000000000FFL) >>  0);
	}
//	public static long readLong(ByteArrayInputStream is) throws IOException {
//		byte[] bytes = new byte[8];
//		is.read(bytes, 0, 8);
//		return getLong(bytes, 0);
//	}
//	public static void writeLong(ByteArrayOutputStream os, long data) throws IOException {
//		byte[] bytes = new byte[8];
//		setLong(bytes, 0, data);
//		os.write(bytes);
//	}
	
	
	private static byte[] getTime(byte[] buf, int ofs) {
		byte[] t = new byte[16];
		System.arraycopy(buf, ofs, t, 0, 16);
		return t;
	}	
	private static void setTime(byte[] buf, int ofs, byte[] data) {
		System.arraycopy(data, 0, buf, ofs, 16);
	}
//	public static byte[] readTime(ByteArrayInputStream is) throws IOException {
//		byte[] bytes = new byte[16];
//		is.read(bytes, 0, 16);
//		return getTime(bytes, 0);
//	}
//	public static void writeTime(ByteArrayOutputStream os, byte[] data) throws IOException {
//		byte[] bytes = new byte[16];
//		setTime(bytes, 0, data);
//		os.write(bytes);
//	}
	
	
	private static double getDouble(byte[] buf, int ofs) {
		return Double.longBitsToDouble(getLong(buf, ofs));
	}
	private static void setDouble(byte[] buf, int ofs, double data) {
		setLong(buf, ofs, Double.doubleToRawLongBits(data));
	}
//	public static double readDouble(ByteArrayInputStream is) throws IOException {
//		byte[] bytes = new byte[8];
//		is.read(bytes, 0, 8);
//		return getDouble(bytes, 0);
//	}
//	public static void writeDouble(ByteArrayOutputStream os, double data) throws IOException {
//		byte[] bytes = new byte[8];
//		setDouble(bytes, 0, data);
//		os.write(bytes);
//	}
	
	
	private static Complex getComplex(byte[] buf, int ofs) {
		return new Complex(getDouble(buf, ofs), getDouble(buf, ofs+8));
	}
	private static void setComplex(byte[] buf, int ofs, Complex data) {
		setDouble(buf, ofs, data.real);
		setDouble(buf, ofs+8, data.imag);
	}
//	public static Complex readComplex(ByteArrayInputStream is) throws IOException {
//		byte[] bytes = new byte[16];
//		is.read(bytes, 0, 8);
//		return getComplex(bytes, 0);
//	}
//	public static void writeComplex(ByteArrayOutputStream os, Complex data) throws IOException {
//		byte[] bytes = new byte[16];
//		setComplex(bytes, 0, data);
//		os.write(bytes);
//	}
	
	
	public byte[] flatten() throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		flatten(os, type, data, ofs, heap);
		return os.toByteArray();
	}
	private void flatten(ByteArrayOutputStream os, Type type, byte[] buf, int ofs, Vector<byte[]> heap) throws IOException {
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
				byte[] sbuf = heap.get(getInt(buf, ofs));
				writeInt(os, sbuf.length);
				os.write(sbuf);
				break;
				
			case '*':
				int depth = type.getDepth();
				Type elementType = type.getSubtype(0);
				int size = 1;
				for (i = 0; i < depth; i++) {
					size *= getInt(buf, ofs + 4*i);;
				}
				os.write(buf, ofs, 4*depth);
				// write data from array
				byte[] lbuf = heap.get(getInt(buf, ofs + 4*depth));
				for (i = 0; i < size; i++) {
					flatten(os, elementType, lbuf, elementType.dataWidth() * i, heap);
				}
				break;
				
			case '(':
				for (i = 0; i < type.size(); i++) {
					flatten(os, type.getSubtype(i), buf, ofs + type.getOffset(i), heap);
				}
				break;
				
			default:
				throw new RuntimeException("Unknown type.");
		}
	}
	
	
	public static Data unflatten(byte[] buf, String tag) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(buf);
		Type type = Type.parse(tag);
		return unflatten(is, type);
	}
	public static Data unflatten(byte[] buf, Type type) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(buf);
		return unflatten(is, type);
	}
	public static Data unflatten(ByteArrayInputStream is, String tag) throws IOException {
		Type type = Type.parse(tag);
		return unflatten(is, type);
	}
	public static Data unflatten(ByteArrayInputStream is, Type type) throws IOException {
		byte[] data = new byte[type.dataWidth()];
		Vector<byte[]> heap = new Vector<byte[]>();
		unflatten(is, type, data, 0, heap);
		return new Data(type, data, 0, heap);
	}
	private static void unflatten(ByteArrayInputStream is, Type type, byte[] buf, int ofs, Vector<byte[]> heap) throws IOException {
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
				int len = readInt(is);
				byte[] sbuf = new byte[len];
				setInt(buf, ofs, heap.size());
				heap.add(sbuf);
				is.read(sbuf, 0, len);
				break;
				
			case '*':
				int depth = type.getDepth();
				Type elementType = type.getSubtype(0);
				int elementWidth = elementType.dataWidth();
				is.read(buf, ofs, 4*depth);
				int size = 1;
				for (i = 0; i < depth; i++) {
					size *= getInt(buf, ofs + 4*i);
				}
				byte[] lbuf = new byte[elementWidth * size];
				setInt(buf, ofs + 4*depth, heap.size());
				heap.add(lbuf);
				for (i = 0; i < size; i++) {
					unflatten(is, type.getSubtype(0), lbuf, elementWidth * i, heap);
				}
				break;
				
			case '(':
				for (i = 0; i < type.size(); i++) {
					unflatten(is, type.getSubtype(i), buf, ofs + type.getOffset(i), heap);
				}
				break;
				
			case 'E':
				break;
				
			default:
				throw new RuntimeException("Unknown type.");
		}
	}
	
	
	public String pretty(int...indices) {
		Data data = getData(indices);
		return data.pretty();
	}
		
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
					s = Double.toString(c.real) + "+" + Double.toString(c.imag) + "i";
				}
				u = type.getUnits();
				if (u != null) {
					s += " [" + u + "]";
				}
				return s;
				
			case 't': return getTime().toString();
			case 's': return "\"" + getStr() + "\"";
			
			case '*':
				int[] shape = getArraySize();
				int[] indices = new int[type.getDepth()];
				return prettyList(shape, indices, 0);
				
			case '(':
				for (int i = 0; i < getClusterSize(); i++) {
					s += ", " + pretty(i);
				}
				return "(" + s.substring(2) + ")";
				
			case 'E':
				return "Error!";
				
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
				s += ", " + prettyList(shape, indices, level+1);
			}
		}
		return "[" + s.substring(2) + "]";
	}
	
	
    public boolean isEmpty() { return type instanceof org.labrad.types.Empty; }
    public boolean isError() { return type instanceof org.labrad.types.Error; }
    
    private Type getSubtype(int...indices) {
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
    
    private Cluster2<byte[], Integer> getOffset(int...indices) {
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
	    					shape[j] = getInt(data, ofs + 4*j);
	    				}
	    				dimsLeft = depth;
	    				data = heap.get(getInt(data, ofs + 4*depth));
	    			}
	    			// read one listIndex
	    			listIndices[depth-dimsLeft] = i;
	    			dimsLeft--;
	    			if (dimsLeft == 0) {
	    				// set type to be element type of array
	    				type = type.getSubtype(0);
	    				ofs = 0;
	    				// calculate offset into array
	    				int product = 1;
	    				for (int dim = depth-1; dim >= 0; dim--) {
	    					ofs += type.dataWidth() * listIndices[dim] * product;
	    					product *= shape[dim];
	    				}
	    			}
	    			break;
	    			
	    		case '(':
	    			ofs += type.getOffset(i);
	    			type = type.getSubtype(i);
	    			break;
	    			
	    		default:
	    			throw new RuntimeException("Non-indexable type: " + type.getCode() + ".");
    		}
    	}
    	if (dimsLeft != 0) {
    		throw new RuntimeException("Not enough indices for array.");
    	}
    	return new Cluster2<byte[], Integer>(data, ofs);
    }
    
    
    public Data getData(int...indices) {
    	Type type = getSubtype(indices);
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	return new Data(type, pos.get0(), pos.get1(), heap);
    }
    
    
    public boolean isBool(int...indices) {
    	return getSubtype(indices) instanceof org.labrad.types.Bool;
    }
    public boolean getBool(int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	return getBool(pos.get0(), pos.get1());
    }
    public Data setBool(boolean data, int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	setBool(pos.get0(), pos.get1(), data);
    	return this;
    }
    
    
    public boolean isInt(int...indices) {
    	return getSubtype(indices) instanceof org.labrad.types.Int;
    }
    public int getInt(int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	return getInt(pos.get0(), pos.get1());
    }
    public Data setInt(int data, int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	setInt(pos.get0(), pos.get1(), data);
    	return this;
    }
    
    
    public boolean isWord(int...indices) {
    	return getSubtype(indices) instanceof org.labrad.types.Word;
    }
    public long getWord(int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	return getWord(pos.get0(), pos.get1());
    }
    public Data setWord(long data, int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	setWord(pos.get0(), pos.get1(), data);
    	return this;
    }
    
    
    public boolean isStr(int...indices) {
    	return getSubtype(indices) instanceof org.labrad.types.Str;
    }
    public String getStr(int...indices) {
    	try {
    		return new String(getBytes(indices), encoding);
    	} catch (UnsupportedEncodingException e) {
    		throw new RuntimeException("Unsupported string encoding.");
    	}
    }
    public Data setStr(String data, int...indices) {
    	try {
    		setBytes(data.getBytes(encoding), indices);
    	} catch (UnsupportedEncodingException e) {
    		throw new RuntimeException("Unsupported string encoding.");
    	}
    	return this;
    }
    public boolean isBytes(int...indices) {
    	return getSubtype(indices) instanceof org.labrad.types.Str;
    }
    public byte[] getBytes(int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	return heap.get(getInt(pos.get0(), pos.get1()));
    }
    public Data setBytes(byte[] data, int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	setInt(pos.get0(), pos.get1(), heap.size());
    	heap.add(data);
    	return this;
    }
    

    public boolean isValue(int...indices) {
    	return getSubtype(indices) instanceof org.labrad.types.Value;
    }
    public double getValue(int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	return getDouble(pos.get0(), pos.get1());
    }
    public Data setValue(double data, int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	setDouble(pos.get0(), pos.get1(), data);
    	return this;
    }
    

    public boolean isComplex(int...indices) {
    	return getSubtype(indices) instanceof org.labrad.types.Complex;
    }
    public Complex getComplex(int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	return getComplex(pos.get0(), pos.get1());
    }
    public Data setComplex(Complex data, int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	setComplex(pos.get0(), pos.get1(), data);
    	return this;
    }
    public Data setComplex(double re, double im, int...indices) {
    	setComplex(new Complex(re, im), indices);
    	return this;
    }
    
    
    public boolean hasUnits(int...indices) {
    	Type type = getSubtype(indices);
    	return ((type instanceof org.labrad.types.Value)
    	         || (type instanceof org.labrad.types.Complex))
    	       && (type.getUnits() != null);
    }
    public String getUnits(int...indices) {
    	return getSubtype(indices).getUnits();
    }
    
    
    //TODO: correct time translation
    public boolean isTime(int...indices) {
    	return getSubtype(indices) instanceof org.labrad.types.Time;
    }
    public byte[] getTime(int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	return getTime(pos.get0(), pos.get1());
    }
    public Data setTime(byte[] data, int...indices) {
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	setTime(pos.get0(), pos.get1(), data);
    	return this;
    }

    
    public boolean isArray(int...indices) {
    	return getSubtype(indices) instanceof org.labrad.types.List;
    }
    public int[] getArraySize(int...indices) {
    	Type type = getSubtype(indices);
    	int depth = type.getDepth();
    	int[] shape = new int[depth];
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	for (int i = 0; i < depth; i++) {
    		shape[i] = getInt(pos.get0(), pos.get1() + 4*i);
    	}
    	return shape;
    }
    public Data setArraySize(int size, int...indices) {
    	int[] shape = {size};
    	setArrayShape(shape, indices);
    	return this;
    }
    public Data setArrayShape(int...shape) {
    	int[] indices = {};
    	setArrayShape(shape, indices);
    	return this;
    }
    public Data setArrayShape(int[] shape, int...indices) {
    	Type type = getSubtype(indices);
    	Type elementType = type.getSubtype(0);
    	int depth = type.getDepth();
        if (shape.length != depth) {
        	throw new RuntimeException("Array depth mismatch!");
        }
    	Cluster2<byte[], Integer> pos = getOffset(indices);
    	int size = 1;
    	for (int i = 0; i < depth; i++) {
    		setInt(pos.get0(), pos.get1() + 4*i, shape[i]);
    		size *= shape[i];
    	}
    	byte[] buf = new byte[elementType.dataWidth() * size];
    	setInt(pos.get0(), pos.get1() + 4*depth, heap.size());
    	heap.add(buf);
    	return this;
    }
    
    
    public boolean isCluster(int...indices) {
    	return getSubtype(indices) instanceof org.labrad.types.Cluster;
    }
    public int getClusterSize(int...indices) {
    	return getSubtype(indices).size();
    }
    
    
    public static void main(String[] args) throws IOException {
    	byte[] bs = new byte[100];
    	Random rand = new Random();
    	int count;
    	
    	boolean b;
    	for (count = 0; count < 1000; count++) {
    		b = rand.nextBoolean();
    		setBool(bs, 0, b);
    		assert b == getBool(bs, 0);
    	}
    	System.out.println("Bool okay.");
    	
    	int i;
    	for (count = 0; count < 1000000; count++) {
    		i = rand.nextInt();
    		setInt(bs, 0, i);
    		assert i == getInt(bs, 0);
    	}
    	System.out.println("Int okay.");
    	
    	long l;
    	for (count = 0; count < 1000000; count++) {
    		l = Math.abs(rand.nextLong()) % 4294967296L;
    		setWord(bs, 0, l);
    		assert l == getWord(bs, 0);
    	}
    	System.out.println("Word okay.");
    	
    	for (count = 0; count < 1000000; count++) {
    		l = rand.nextLong();
    		setLong(bs, 0, l);
    		assert l == getLong(bs, 0);
    	}
    	System.out.println("Long okay.");
    	
    	double d;
    	for (count = 0; count < 100000; count++) {
    		d = rand.nextGaussian();
    		setDouble(bs, 0, d);
    		assert d == getDouble(bs, 0);
    	}
    	System.out.println("Double okay.");
    	
    	double re, im;
    	for (count = 0; count < 100000; count++) {
    		re = rand.nextGaussian();
    		im = rand.nextGaussian();
    		Complex c1 = new Complex(re, im);
    		setComplex(bs, 0, c1);
    		Complex c2 = getComplex(bs, 0);
    		assert (c1.real == c2.real) && (c1.imag == c2.imag);
    	}
    	System.out.println("Complex okay.");
    	
    	
    	Data data, data2;
    	byte[] flat;
    	
    	data = new Data("i");
    	data.setInt(100);
    	assert data.getInt() == 100;
    	
    	data = new Data("s");
    	data.setStr("This is a test.");
    	System.out.println(data.getStr());
    	
    	data = new Data("*s");
    	data.setArraySize(20);
    	for (count = 0; count < 20; count++) {
    		data.setStr("This is string " + Integer.toString(count), count);
    	}
    	for (count = 0; count < 20; count++) {
    		System.out.println(data.getStr(count));
    	}
    	
    	data = new Data("biwsvc");
    	b = rand.nextBoolean();
    	i = rand.nextInt();
    	l = Math.abs(rand.nextLong()) % 4294967296L;
    	String s = Long.toString(rand.nextLong());
    	d = rand.nextGaussian();
    	re = rand.nextGaussian();
    	im = rand.nextGaussian();
    	
    	data.setBool(b, 0);
    	data.setInt(i, 1);
    	data.setWord(l, 2);
    	data.setStr(s, 3);
    	data.setValue(d, 4);
    	data.setComplex(re, im, 5);
    	
    	assert b == data.getBool(0);
    	assert i == data.getInt(1);
    	assert l == data.getWord(2);
    	assert s.equals(data.getStr(3));
    	assert d == data.getValue(4);
    	Complex c = data.getComplex(5);
    	assert re == c.real;
    	assert im == c.imag;
    	System.out.println("Cluster okay.");
    	System.out.println(data.pretty());
    	
    	data = new Data("*(biwsv[m]c[m/s])");
    	data.setArraySize(20);
    	for (count = 0; count < 20; count++) {
	    	b = rand.nextBoolean();
	    	i = rand.nextInt();
	    	l = Math.abs(rand.nextLong()) % 4294967296L;
	    	s = Long.toString(rand.nextLong());
	    	d = rand.nextGaussian();
	    	re = rand.nextGaussian();
	    	im = rand.nextGaussian();
	    	
	    	data.setBool(b, count, 0);
	    	data.setInt(i, count, 1);
	    	data.setWord(l, count, 2);
	    	data.setStr(s, count, 3);
	    	data.setValue(d, count, 4);
	    	data.setComplex(re, im, count, 5);
	    	
	    	assert b == data.getBool(count, 0);
	    	assert i == data.getInt(count, 1);
	    	assert l == data.getWord(count, 2);
	    	assert s.equals(data.getStr(count, 3));
	    	assert d == data.getValue(count, 4);
	    	c = data.getComplex(count, 5);
	    	assert re == c.real;
	    	assert im == c.imag;
    	}
    	System.out.println("List of Cluster okay.");
    	System.out.println(data.pretty());
    	
    	flat = data.flatten();
    	data2 = unflatten(flat, "*(biwsv[m]c[m/s])");
    	System.out.println(data2.pretty());
    	
    	// test multi-dimensional list
    	data = new Data("*2i");
    	data.setArrayShape(4, 3);
    	for (int m = 0; m < 4; m++) {
    		for (int n = 0; n < 3; n++) {
    			data.setInt(rand.nextInt(), m, n);
    		}
    	}
    	System.out.println(data.pretty());
    	flat = data.flatten();
    	data2 = unflatten(flat, "*2i");
    	System.out.println(data2.pretty());
    	
    	
    	data = new Data("*3s");
    	data.setArrayShape(2, 2, 2);
    	for (int m = 0; m < 2; m++) {
    		for (int n = 0; n < 2; n++) {
    			for (int p = 0; p < 2; p++) {
    				data.setStr("TestString(" + m + n + p + ")", m, n, p);
    			}
    		}
    	}
    	System.out.println(data.pretty());
    	flat = data.flatten();
    	data2 = unflatten(flat, "*3s");
    	System.out.println(data2.pretty());
    	
    	
    	System.out.println("done.");
    }
}
