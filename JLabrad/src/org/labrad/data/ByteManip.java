package org.labrad.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ByteManip {
	/**
	 * 
	 * @param bv
	 * @return
	 */
	static boolean getBool(ByteArrayView bv) {
		return getBool(bv.getBytes(), bv.getOffset());
	}
	
	/**
	 * Get a boolean from a byte array.
	 * 
	 * @param buf
	 *            byte array of data.
	 * @param ofs
	 *            offset into byte array.
	 * @return
	 */
	static boolean getBool(byte[] buf, int ofs) {
	    return buf[ofs] != 0;
	}

	static void setBool(ByteArrayView bv, boolean data) {
		setBool(bv.getBytes(), bv.getOffset(), data);
	}
	
	/**
	 * Store a boolean into a byte array.
	 * 
	 * @param buf
	 *            byte array of data.
	 * @param ofs
	 *            offset into byte array.
	 * @param data
	 *            boolean value to set.
	 */
	static void setBool(byte[] buf, int ofs, boolean data) {
	    buf[ofs] = data ? (byte) 1 : (byte) 0;
	}

	static int getInt(ByteArrayView bv) {
		return getInt(bv.getBytes(), bv.getOffset());
	}
	
	/**
	 * Get an int from a byte array.
	 * 
	 * @param buf
	 *            byte array of data.
	 * @param ofs
	 *            offset into byte array.
	 * @return
	 */
	static int getInt(byte[] buf, int ofs) {
	    return (int) ((0xFF & (int) buf[ofs + 0]) << 24
	                | (0xFF & (int) buf[ofs + 1]) << 16
	                | (0xFF & (int) buf[ofs + 2]) << 8
	                | (0xFF & (int) buf[ofs + 3]) << 0);
	}

	static void setInt(ByteArrayView bv, int data) {
		setInt(bv.getBytes(), bv.getOffset(), data);
	}
	
	/**
	 * Store an int into a byte array.
	 * 
	 * @param buf
	 *            byte array of data.
	 * @param ofs
	 *            offset into byte array.
	 * @param data
	 *            integer value to set.
	 */
	static void setInt(byte[] buf, int ofs, int data) {
	    buf[ofs + 0] = (byte) ((data & 0xFF000000) >> 24);
	    buf[ofs + 1] = (byte) ((data & 0x00FF0000) >> 16);
	    buf[ofs + 2] = (byte) ((data & 0x0000FF00) >> 8);
	    buf[ofs + 3] = (byte) ((data & 0x000000FF) >> 0);
	}
	
	/**
	 * Read an int from a byte stream.
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	static int readInt(ByteArrayInputStream is) throws IOException {
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
	static void writeInt(ByteArrayOutputStream os, int data)
	        throws IOException {
	    byte[] bytes = new byte[4];
	    setInt(bytes, 0, data);
	    os.write(bytes);
	}

	static long getWord(ByteArrayView bv) {
		return getWord(bv.getBytes(), bv.getOffset());
	}
	
	/**
	 * Get a word (32 bit unsigned integer) from a byte array.
	 * 
	 * @param buf
	 * @param ofs
	 * @return
	 */
	static long getWord(byte[] buf, int ofs) {
	    return (long) (0xFF & (int) buf[ofs + 0]) << 24
	         | (long) (0xFF & (int) buf[ofs + 1]) << 16
	         | (long) (0xFF & (int) buf[ofs + 2]) << 8
	         | (long) (0xFF & (int) buf[ofs + 3]) << 0;
	}

	static void setWord(ByteArrayView bv, long data) {
		setWord(bv.getBytes(), bv.getOffset(), data);
	}
	
	/**
	 * Set a word in a byte array.
	 * 
	 * @param buf
	 * @param ofs
	 * @param data
	 */
	static void setWord(byte[] buf, int ofs, long data) {
	    buf[ofs + 0] = (byte) ((data & 0xFF000000) >> 24);
	    buf[ofs + 1] = (byte) ((data & 0x00FF0000) >> 16);
	    buf[ofs + 2] = (byte) ((data & 0x0000FF00) >> 8);
	    buf[ofs + 3] = (byte) ((data & 0x000000FF) >> 0);
	}

	static long getLong(ByteArrayView bv) {
		return getLong(bv.getBytes(), bv.getOffset());
	}
	
	/**
	 * Get a long (64bit signed integer) from a byte array.
	 * 
	 * @param buf
	 * @param ofs
	 */
	static long getLong(byte[] buf, int ofs) {
	    return (long) (0xFF & (int) buf[ofs + 0]) << 56
	         | (long) (0xFF & (int) buf[ofs + 1]) << 48
	         | (long) (0xFF & (int) buf[ofs + 2]) << 40
	         | (long) (0xFF & (int) buf[ofs + 3]) << 32
	         | (long) (0xFF & (int) buf[ofs + 4]) << 24
	         | (long) (0xFF & (int) buf[ofs + 5]) << 16
	         | (long) (0xFF & (int) buf[ofs + 6]) <<  8
	         | (long) (0xFF & (int) buf[ofs + 7]) <<  0;
	}

	static void setLong(ByteArrayView bv, long data) {
		setLong(bv.getBytes(), bv.getOffset(), data);
	}
	
	/**
	 * Set a long in a byte array.
	 * 
	 * @param buf
	 * @param ofs
	 * @param data
	 */
	static void setLong(byte[] buf, int ofs, long data) {
	    buf[ofs + 0] = (byte) ((data & 0xFF00000000000000L) >> 56);
	    buf[ofs + 1] = (byte) ((data & 0x00FF000000000000L) >> 48);
	    buf[ofs + 2] = (byte) ((data & 0x0000FF0000000000L) >> 40);
	    buf[ofs + 3] = (byte) ((data & 0x000000FF00000000L) >> 32);
	    buf[ofs + 4] = (byte) ((data & 0x00000000FF000000L) >> 24);
	    buf[ofs + 5] = (byte) ((data & 0x0000000000FF0000L) >> 16);
	    buf[ofs + 6] = (byte) ((data & 0x000000000000FF00L) >>  8);
	    buf[ofs + 7] = (byte) ((data & 0x00000000000000FFL) >>  0);
	}

	static byte[] getTime(ByteArrayView bv) {
		return getTime(bv.getBytes(), bv.getOffset());
	}
	
	/**
	 * Get a timestamp from a byte array.
	 *
	 * @param buf
	 * @param ofs
	 */
	static byte[] getTime(byte[] buf, int ofs) {
	    byte[] t = new byte[16];
	    System.arraycopy(buf, ofs, t, 0, 16);
	    return t;
	}

	static void setTime(ByteArrayView bv, byte[] data) {
		setTime(bv.getBytes(), bv.getOffset(), data);
	}
	
	/**
	 * Set a timestamp in a byte array.
	 * 
	 * @param buf
	 * @param ofs
	 * @param data
	 */
	static void setTime(byte[] buf, int ofs, byte[] data) {
	    System.arraycopy(data, 0, buf, ofs, 16);
	}

	static double getDouble(ByteArrayView bv) {
		return getDouble(bv.getBytes(), bv.getOffset());
	}
	
	/**
	 * Get a Double (64-bit floating point) from a byte array.
	 * 
	 * @param buf
	 * @param ofs
	 */
	static double getDouble(byte[] buf, int ofs) {
	    return Double.longBitsToDouble(getLong(buf, ofs));
	}

	static void setDouble(ByteArrayView bv, double data) {
		setDouble(bv.getBytes(), bv.getOffset(), data);
	}
	
	/**
	 * Set a Double in a byte array.
	 * 
	 * @param buf
	 * @param ofs
	 * @param data
	 */
	static void setDouble(byte[] buf, int ofs, double data) {
	    setLong(buf, ofs, Double.doubleToRawLongBits(data));
	}

	static Complex getComplex(ByteArrayView bv) {
		return getComplex(bv.getBytes(), bv.getOffset());
	}
	
	/**
	 * Get a complex (64-bit real, 64-bit imag) from a byte array.
	 * 
	 * @param buf
	 * @param ofs
	 */
	static Complex getComplex(byte[] buf, int ofs) {
	    return new Complex(getDouble(buf, ofs), getDouble(buf, ofs + 8));
	}

	static void setComplex(ByteArrayView bv, Complex data) {
		setComplex(bv.getBytes(), bv.getOffset(), data);
	}
	
	/**
	 * Set a complex in a byte array.
	 * 
	 * @param buf
	 * @param ofs
	 * @param data
	 */
	static void setComplex(byte[] buf, int ofs, Complex data) {
	    setDouble(buf, ofs, data.real);
	    setDouble(buf, ofs + 8, data.imag);
	}

}
