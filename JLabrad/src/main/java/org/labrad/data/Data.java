/*
 * Copyright 2008 Matthew Neeley
 * 
 * This file is part of JLabrad.
 *
 * JLabrad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JLabrad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JLabrad.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.labrad.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.labrad.errors.NonIndexableTypeException;
import org.labrad.types.Cluster;
import org.labrad.types.Empty;
import org.labrad.types.Type;

/**
 * The Data class encapsulates the data format used to communicate between
 * LabRAD servers and clients.  This data format is based on the
 * capabilities of LabVIEW, from National Instruments.  Each piece of LabRAD
 * data has a Type object which is specified by a String type tag.
 */
public class Data implements Serializable, Cloneable {
  private static final long serialVersionUID = 1L;

  public static final String STRING_ENCODING = "ISO-8859-1";
  public static final Data EMPTY = new Data("");

  // time
  // TODO check timezones in time translation
  // LabRAD measures time as seconds and fractions of a second since Jan 1, 1904 GMT.
  // The Java Date class measures time as milliseconds since Jan 1, 1970 GMT.
  // The difference between these two is 24107 days.
  // 
  private static long DELTA_SECONDS = 24107 * 24 * 60 * 60;

  private Type type;
  private byte[] data;
  private int ofs;
  private List<byte[]> heap;

  /**
   * Make a copy of this Data object.
   */
  @Override
  public Data clone() {
    Data clone = new Data(this.getType());
    copy(this, clone);
    return clone;
  }

  /**
   * Copy Data object src to dest.
   * @param src
   * @param dest
   * @return
   */
  private static Data copy(Data src, Data dest) {
    switch (src.getType().getCode()) {
      case BOOL: dest.setBool(src.getBool()); break;
      case INT: dest.setInt(src.getInt()); break;
      case WORD: dest.setWord(src.getWord()); break;
      case VALUE: dest.setValue(src.getValue()); break;
      case COMPLEX: dest.setComplex(src.getComplex()); break;
      case TIME: dest.setTime(src.getTime()); break;
      case STR: dest.setBytes(src.getBytes()); break;
      case LIST:
        int[] shape = src.getArrayShape();
        int[] indices = new int[shape.length];
        dest.setArrayShape(shape);
        copyList(src, dest, shape, indices, 0);
        break;

      case CLUSTER:
        for (int i = 0; i < src.getClusterSize(); i++) {
          copy(src.get(i), dest.get(i));
        }
        break;

      case ERROR:
        dest.setError(src.getErrorCode(), src.getErrorMessage());
        // TODO add error payloads
        //clone.setPayload(src.getErrorPayload());

      default:
        throw new RuntimeException("Not implemented!");
    }
    return dest;
  }

  /**
   * Copy a (possibly multidimensional) list from another Data object to this one.
   * @param other
   * @param shape
   * @param indices
   * @param level
   */
  private static void copyList(Data src, Data dest, int[] shape, int[] indices, int level) {
    for (int i = 0; i < shape[level]; i++) {
      indices[level] = i;
      if (level == shape.length - 1) {
        copy(src.get(indices), dest.get(indices));
      } else {
        copyList(src, dest, shape, indices, level + 1);
      }
    }
  }


  // static constructors for building clusters from groups of data objects
  /**
   * Build a cluster from an array of other data objects.
   * @param elements
   * @return
   */
  public static Data clusterOf(Data...elements) {
    return Data.clusterOf(Arrays.asList(elements));
  }

  /**
   * Build a cluster from a list of other data objects.
   * @param elements
   * @return
   */
  public static Data clusterOf(List<Data> elements) {
    List<Type> elementTypes = new ArrayList<Type>();
    for (Data elem : elements) {
      elementTypes.add(elem.getType());
    }
    Data cluster = new Data(Cluster.of(elementTypes));
    for (int i = 0; i < elementTypes.size(); i++) {
      cluster.get(i).set(elements.get(i));
    }
    return cluster;
  }

  /**
   * Build a LabRAD list from a java array of data objects.
   * @param elements
   * @return
   */
  public static Data listOf(Data...elements) {
    return Data.listOf(Arrays.asList(elements));
  }

  /**
   * Build a LabRAD list from a java List of data objects.
   * @param elements
   * @return
   */
  public static Data listOf(List<Data> elements) {
    Type elementType;
    if (elements.size() == 0) {
      elementType = Empty.getInstance();
    } else {
      elementType = elements.get(0).getType();
    }
    Data data = new Data(org.labrad.types.List.of(elementType));
    data.setArraySize(elements.size());
    int i = 0;
    for (Data elem : elements) {
      data.get(i++).set(elem);
    }
    return data;
  }

  public static <T> Data listOf(List<T> elements, Setter<T> setter) {
    Type elementType;
    if (elements.size() == 0) {
      elementType = Empty.getInstance();
    } else {
      elementType = setter.getType();
    }
    Data data = new Data(org.labrad.types.List.of(elementType));
    data.setList(elements, setter);
    return data;
  }

  // static constructors for basic types
  public static Data valueOf(boolean b) { return new Data("b").setBool(b); }
  public static Data valueOf(int i) { return new Data("i").setInt(i); }
  public static Data valueOf(long w) { return new Data("w").setWord(w); }
  public static Data valueOf(byte[] b) { return new Data("s").setBytes(b); }
  public static Data valueOf(String s) { return new Data("s").setString(s); }
  public static Data valueOf(Date t) { return new Data("t").setTime(t); }

  public static Data valueOf(double v) {
    return new Data("v").setValue(v);
  }
  public static Data valueOf(double v, String units) {
    return new Data("v[" + units + "]").setValue(v);
  }

  public static Data valueOf(double re, double im) {
    return new Data("c").setComplex(re, im);
  }
  public static Data valueOf(double re, double im, String units) {
    return new Data("c[" + units + "]").setComplex(re, im);
  }

  // static constructors for arrays of basic types
  public static Data valueOf(boolean[] a) {
    Data data = Data.ofType("*b");
    data.setArraySize(a.length);
    for (int i = 0; i < a.length; i++) {
      data.setBool(a[i], i);
    }
    return data;
  }

  public static Data valueOf(int[] a) {
    Data data = Data.ofType("*i");
    data.setArraySize(a.length);
    for (int i = 0; i < a.length; i++) {
      data.setInt(a[i], i);
    }
    return data;
  }

  public static Data valueOf(long[] a) {
    Data data = Data.ofType("*w");
    data.setArraySize(a.length);
    for (int i = 0; i < a.length; i++) {
      data.setWord(a[i], i);
    }
    return data;
  }

  public static Data valueOf(double[] a) {
    Data data = Data.ofType("*v");
    data.setArraySize(a.length);
    for (int i = 0; i < a.length; i++) {
      data.setValue(a[i], i);
    }
    return data;
  }

  public static Data valueOf(double[] a, String units) {
    Data data = Data.ofType("*v[" + units + "]");
    data.setArraySize(a.length);
    for (int i = 0; i < a.length; i++) {
      data.setValue(a[i], i);
    }
    return data;
  }

  public static Data valueOf(String[] a) {
    Data data = Data.ofType("*s");
    data.setArraySize(a.length);
    for (int i = 0; i < a.length; i++) {
      data.setString(a[i], i);
    }
    return data;
  }
  
  /*
  public static Data valueOf(Date[] t) {
    return new Data("t").setTime(t);
  }

  public static Data valueOf(double[] re, double[] im) {
  	return new Data("c").setComplex(re, im);
  }
  public static Data valueOf(double[] re, double[] im, String units) {
  	return new Data("c[" + units + "]").setComplex(re, im);
  }
  */

  //static constructors for 2D arrays of basic types
  // TODO ensure that 2D and 3D arrays are rectangular
  public static Data valueOf(boolean[][] a) {
    Data data = Data.ofType("*2b");
    data.setArrayShape(a.length, a.length > 0 ? a[0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        data.setBool(a[i][j], i, j);
      }
    }
    return data;
  }

  public static Data valueOf(int[][] a) {
    Data data = Data.ofType("*2i");
    data.setArrayShape(a.length, a.length > 0 ? a[0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        data.setInt(a[i][j], i, j);
      }
    }
    return data;
  }

  public static Data valueOf(long[][] a) {
    Data data = Data.ofType("*2w");
    data.setArrayShape(a.length, a.length > 0 ? a[0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        data.setWord(a[i][j], i, j);
      }
    }
    return data;
  }

  public static Data valueOf(double[][] a) {
    Data data = Data.ofType("*2v");
    data.setArrayShape(a.length, a.length > 0 ? a[0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        data.setValue(a[i][j], i, j);
      }
    }
    return data;
  }

  public static Data valueOf(double[][] a, String units) {
    Data data = Data.ofType("*2v[" + units + "]");
    data.setArrayShape(a.length, a.length > 0 ? a[0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        data.setValue(a[i][j], i, j);
      }
    }
    return data;
  }

  public static Data valueOf(String[][] a) {
    Data data = Data.ofType("*2s");
    data.setArrayShape(a.length, a.length > 0 ? a[0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        data.setString(a[i][j], i, j);
      }
    }
    return data;
  }
  
  
  //static constructors for 3D arrays of basic types
  public static Data valueOf(boolean[][][] a) {
    Data data = Data.ofType("*3b");
    data.setArrayShape(a.length,
                       a.length > 0 ? a[0].length : 0,
                       a.length > 0 && a[0].length > 0 ? a[0][0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        for (int k = 0; k < a[0][0].length; k++) {
          data.setBool(a[i][j][k], i, j, k);
        }
      }
    }
    return data;
  }

  public static Data valueOf(int[][][] a) {
    Data data = Data.ofType("*3i");
    data.setArrayShape(a.length,
        a.length > 0 ? a[0].length : 0,
        a.length > 0 && a[0].length > 0 ? a[0][0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        for (int k = 0; k < a[0][0].length; k++) {
          data.setInt(a[i][j][k], i, j, k);
        }
      }
    }
    return data;
  }

  public static Data valueOf(long[][][] a) {
    Data data = Data.ofType("*3w");
    data.setArrayShape(a.length,
        a.length > 0 ? a[0].length : 0,
        a.length > 0 && a[0].length > 0 ? a[0][0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        for (int k = 0; k < a[0][0].length; k++) {
          data.setWord(a[i][j][k], i, j, k);
        }
      }
    }
    return data;
  }

  public static Data valueOf(double[][][] a) {
    Data data = Data.ofType("*3v");
    data.setArrayShape(a.length,
        a.length > 0 ? a[0].length : 0,
        a.length > 0 && a[0].length > 0 ? a[0][0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        for (int k = 0; k < a[0][0].length; k++) {
          data.setValue(a[i][j][k], i, j, k);
        }
      }
    }
    return data;
  }

  public static Data valueOf(double[][][] a, String units) {
    Data data = Data.ofType("*3v[" + units + "]");
    data.setArrayShape(a.length,
        a.length > 0 ? a[0].length : 0,
        a.length > 0 && a[0].length > 0 ? a[0][0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        for (int k = 0; k < a[0][0].length; k++) {
          data.setValue(a[i][j][k], i, j, k);
        }
      }
    }
    return data;
  }

  public static Data valueOf(String[][][] a) {
    Data data = Data.ofType("*3s");
    data.setArrayShape(a.length,
        a.length > 0 ? a[0].length : 0,
        a.length > 0 && a[0].length > 0 ? a[0][0].length : 0);
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        for (int k = 0; k < a[0][0].length; k++) {
          data.setString(a[i][j][k], i, j, k);
        }
      }
    }
    return data;
  }
  
  
  // static constructors for specific types
  public static Data ofType(String tag) {
    return new Data(tag);
  }

  public static Data ofType(Type type) {
    return new Data(type);
  }

  /**
   * Construct a Data object for a given LabRAD type tag.
   * 
   * @param tag
   *            the LabRAD type tag of this Data object
   */
  public Data(String tag) {
    this(Type.fromTag(tag));
  }

  /**
   * Construct a Data object for a given Type object.
   * 
   * @param tag
   *            the LabRAD Type of this Data object
   */
  public Data(Type type) {
    this.type = type;
    data = createFilledByteArray(type.dataWidth());
    ofs = 0;
    heap = createHeap(type);
  }

  /**
   * Construct a Data object from a Type and raw data.
   * 
   * This constructor is used internally in unflattening,
   * and also to construct "views" into a pre-existing
   * Data object.
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
  private Data(Type type, byte[] data, int ofs, List<byte[]> heap) {
    this.type = type;
    this.data = data;
    this.ofs = ofs;
    this.heap = heap;
  }

  /**
   * Creates a byte array of the specified length filled with 0xff.
   * This is used to mark pointers into the heap so we know when we can
   * reuse heap addresses.  By initializing the byte array with 0xff,
   * all heap addresses will initially be set to -1, which is never
   * a valid heap index.
   * @param length of byte array to create
   * @return array of bytes initialized with 0xff
   */
  private static byte[] createFilledByteArray(int length) {
    byte[] data = new byte[length];
    Arrays.fill(data, (byte) 0xff);
    return data;
  }

  /**
   * Create a new heap object for data of the given type.  If the type in
   * question is fixed width, then no heap is needed, so we use an empty list.
   * @param type
   * @return
   */
  private static List<byte[]> createHeap(Type type) {
    List<byte[]> heap;
    if (type.isFixedWidth()) {
      heap = Collections.emptyList();
    } else {
      heap = new ArrayList<byte[]>();
    }
    return heap;
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
   * Test whether this data object matches the given type.
   */
  public boolean matchesType(Type type) {
    return getType().matches(type);
  }

  public boolean matchesType(String tag) {
    return getType().matches(tag);
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
  public byte[] toBytes() throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    toBytes(os, type, data, ofs, heap);
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
   * @throws IOException if writing to the output stream fails
   */
  private static void toBytes(ByteArrayOutputStream os, Type type,
      byte[] buf, int ofs, List<byte[]> heap) throws IOException {
    if (type.isFixedWidth()) {
      os.write(buf, ofs, type.dataWidth());
    } else {
      switch (type.getCode()) {
        case STR:
          byte[] sbuf = heap.get(Bytes.getInt(buf, ofs));
          Bytes.writeInt(os, sbuf.length);
          os.write(sbuf);
          break;

        case LIST:
          int depth = type.getDepth();
          Type elementType = type.getSubtype(0);
          // compute total number of elements in the list
          int size = 1;
          for (int i = 0; i < depth; i++) {
            size *= Bytes.getInt(buf, ofs + 4 * i);
          }
          // write the list shape
          os.write(buf, ofs, 4 * depth);
          // write the list data
          byte[] lbuf = heap.get(Bytes.getInt(buf, ofs + 4 * depth));
          if (elementType.isFixedWidth()) {
            // for fixed-width data, just copy in one big chunk
            os.write(lbuf, 0, elementType.dataWidth() * size);
          } else {
            // for variable-width data, flatten recursively
            int width = elementType.dataWidth();
            for (int i = 0; i < size; i++) {
              toBytes(os, elementType, lbuf, width * i, heap);
            }
          }
          break;

        case CLUSTER:
          for (int i = 0; i < type.size(); i++) {
            toBytes(os, type.getSubtype(i), buf, ofs + type.getOffset(i), heap);
          }
          break;

        case ERROR:
          String tag = "is" + type.getSubtype(0).toString();
          toBytes(os, Type.fromTag(tag), buf, ofs, heap);
          break;

        default:
          throw new RuntimeException("Unknown type.");
      }
    }
  }

  /**
   * Unflatten bytes from the specified buffer into Data, according to the Type.
   * 
   * @param buf
   * @param type
   * @return
   * @throws IOException
   */
  public static Data fromBytes(byte[] buf, Type type) throws IOException {
    return fromBytes(new ByteArrayInputStream(buf), type);
  }

  /**
   * Unflatten a Data object from the given input stream of bytes.
   * 
   * @param is
   * @param type
   * @return
   * @throws IOException
   */
  public static Data fromBytes(ByteArrayInputStream is, Type type) throws IOException {
    byte[] data = new byte[type.dataWidth()];
    List<byte[]> heap = createHeap(type);
    fromBytes(is, type, data, 0, heap);
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
  private static void fromBytes(ByteArrayInputStream is,
      Type type, byte[] buf, int ofs, List<byte[]> heap) throws IOException {
    if (type.isFixedWidth()) {
      is.read(buf, ofs, type.dataWidth());
    } else {
      switch (type.getCode()) {
        case STR:
          int len = Bytes.readInt(is);
          byte[] sbuf = new byte[len];
          Bytes.setInt(buf, ofs, heap.size());
          heap.add(sbuf);
          is.read(sbuf, 0, len);
          break;

        case LIST:
          int depth = type.getDepth();
          Type elementType = type.getSubtype(0);
          int elementWidth = elementType.dataWidth();
          is.read(buf, ofs, 4 * depth);
          int size = 1;
          for (int i = 0; i < depth; i++) {
            size *= Bytes.getInt(buf, ofs + 4 * i);
          }
          byte[] lbuf = new byte[elementWidth * size];
          Bytes.setInt(buf, ofs + 4 * depth, heap.size());
          heap.add(lbuf);
          if (elementType.isFixedWidth()) {
            is.read(lbuf, 0, elementWidth * size);
          } else {
            for (int i = 0; i < size; i++) {
              fromBytes(is, type.getSubtype(0), lbuf, elementWidth * i,
                  heap);
            }
          }
          break;

        case CLUSTER:
          for (int i = 0; i < type.size(); i++) {
            fromBytes(is, type.getSubtype(i), buf,
                ofs + type.getOffset(i), heap);
          }
          break;

        case ERROR:
          String tag = "is" + type.getSubtype(0).toString();
          fromBytes(is, Type.fromTag(tag), buf, ofs, heap);
          break;

        default:
          throw new RuntimeException("Unknown type.");
      }
    }
  }

  public String toString() {
    return "Data(\"" + type.toString() + "\")";
  }

  /**
   * Returns a pretty-printed version of this LabRAD data.
   * 
   * @return
   */
  public String pretty() {
    String s = "", u;
    switch (type.getCode()) {
      case EMPTY: return "";
      case BOOL: return Boolean.toString(getBool());
      case INT: return Integer.toString(getInt());
      case WORD: return Long.toString(getWord());

      case VALUE:
        u = type.getUnits();
        return Double.toString(getValue()) + (u != null ? " [" + u + "]" : "");

      case COMPLEX:
        Complex c = getComplex();
        u = type.getUnits();
        return Double.toString(c.getReal()) + (c.getImag() >= 0 ? "+" : "") + 
        Double.toString(c.getImag()) + "i" + (u != null ? " [" + u + "]" : "");

      case TIME: return getTime().toString();
      case STR: return '"' + getString() + '"';

      case LIST:
        int[] shape = getArrayShape();
        int[] indices = new int[type.getDepth()];
        return prettyList(shape, indices, 0);

      case CLUSTER:
        StringBuffer buf = new StringBuffer(s);
        for (int i = 0; i < getClusterSize(); i++) {
          buf.append(", ");
          buf.append(get(i).pretty());
        }
        return "(" + buf.toString().substring(2) + ")";

      case ERROR:
        return "Error(" + Integer.toString(getErrorCode()) + ", "
        + getErrorMessage() + ")";

      default:
        throw new RuntimeException("Unknown type: " + type.pretty() + ".");
    }
  }

  /**
   * Returns a pretty-printed version of a list object.
   * @param shape
   * @param indices
   * @param level
   * @return
   */
  private String prettyList(int[] shape, int[] indices, int level) {
    String s = "";
    if (shape[level] > 0) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < shape[level]; i++) {
        indices[level] = i;
        if (level == shape.length - 1) {
          if (i > 0) sb.append(", ");
          sb.append(get(indices).pretty());
          //s += ", " + get(indices).pretty();
        } else {
          if (i > 0) sb.append(", ");
          sb.append(prettyList(shape, indices, level + 1));
          //s += ", " + prettyList(shape, indices, level + 1);
        }
      }
      s = sb.toString();
    }
    return "[" + s + "]";
  }

  /**
   * Checks that the type of this data object is compatible with the specified type.
   * @param code
   */
  private void getSubtype(Type.Code code) {
    if (type.getCode() != code) {
      throw new RuntimeException(
          "Type mismatch: expecting " + code +
          " but found " + type.getCode() + " instead.");
    }
  }

  /**
   * Extracts the subtype from this data object at the specified location.
   * Also checks that the type at this location is a subtype of the specified type.
   * 
   * @param code
   * @param indices
   * @return
   */
  private Type getSubtype(Type.Code code, int...indices) {
    Type type = getSubtype(indices);
    if (type.getCode() != code) {
      throw new RuntimeException(
          "Type mismatch: expecting " + code +
          " at " + Arrays.toString(indices) +
          " but found " + type.getCode() + " instead.");
    }
    return type;
  }

  /**
   * Extracts a subtype without typechecking.
   * @param indices
   * @return
   */
  private Type getSubtype(int...indices) {
    Type type = this.type;
    int dimsLeft = 0;
    for (int i : indices) {
      switch (type.getCode()) {
        case LIST:
          if (dimsLeft == 0) {
            dimsLeft = type.getDepth();
          }
          dimsLeft--;
          if (dimsLeft == 0) {
            type = type.getSubtype(i);
          }
          break;

        case CLUSTER:
          type = type.getSubtype(i);
          break;

        default:
          throw new NonIndexableTypeException(type);
      }
    }
    if (dimsLeft != 0) {
      throw new RuntimeException("Not enough indices for array.");
    }
    return type;
  }

  /**
   * Gets a view into the data array at the position specified by indices.
   * @param indices
   * @return
   */
  private ByteArrayView getOffset(int...indices) {
    Type type = this.type;
    byte[] data = this.data;
    int depth = 0, dimsLeft = 0;
    int[] shape = {}, listIndices = {};
    int ofs = this.ofs;
    for (int i : indices) {
      switch (type.getCode()) {
        case LIST:
          if (dimsLeft == 0) {
            // read list shape
            depth = type.getDepth();
            shape = new int[depth];
            listIndices = new int[depth];
            for (int j = 0; j < depth; j++) {
              shape[j] = Bytes.getInt(data, ofs + 4 * j);
            }
            dimsLeft = depth;
            data = heap.get(Bytes.getInt(data, ofs + 4 * depth));
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
              ofs += type.dataWidth() * listIndices[dim] * product;
              product *= shape[dim];
            }
          }
          break;

        case CLUSTER:
          ofs += type.getOffset(i);
          type = type.getSubtype(i);
          break;

        default:
          throw new NonIndexableTypeException(type);
      }
    }
    if (dimsLeft != 0) {
      throw new RuntimeException("Not enough indices for array.");
    }
    return new ByteArrayView(data, ofs);
  }

  /**
   * Get a Data subobject at the specified list of indices.  Note that
   * this returns a view rather than a copy, so any modifications to
   * the subobject will be reflected in the original data.
   * @param indices
   * @return
   */
  public Data get(List<Integer> indices) {
    int[] indexArray = new int[indices.size()];
    int i = 0;
    for (int e : indices) indexArray[i++] = e;
    return get(indexArray);
  }

  /**
   * Get a Data subobject at the specified array of indices.  Note that
   * this returns a view rather than a copy, so any modifications to
   * the subobject will be reflected in the original data.
   * @param indices
   * @return
   */
  public Data get(int...indices) {
    Type type = getSubtype(indices);
    ByteArrayView pos = getOffset(indices);
    return new Data(type, pos.getBytes(), pos.getOffset(), heap);
  }

  /**
   * Set this data object based on the value of the other object.  In this case,
   * to prevent strangeness with shared heaps, the other object is copied into
   * this data object.
   * @param other
   * @return
   */
  public Data set(Data other) {
    copy(other, this);
    return this;
  }


  // type checks
  public boolean isBool() { return type instanceof org.labrad.types.Bool; }
  public boolean isInt() { return type instanceof org.labrad.types.Int; }
  public boolean isWord() { return type instanceof org.labrad.types.Word; }
  public boolean isBytes() { return type instanceof org.labrad.types.Str; }
  public boolean isString() { return type instanceof org.labrad.types.Str; }
  public boolean isValue() { return type instanceof org.labrad.types.Value; }
  public boolean isComplex() { return type instanceof org.labrad.types.Complex; }
  public boolean isTime() { return type instanceof org.labrad.types.Time; }
  public boolean isArray() { return type instanceof org.labrad.types.List; }
  public boolean isCluster() { return type instanceof org.labrad.types.Cluster; }
  public boolean isEmpty() { return type instanceof org.labrad.types.Empty; }
  public boolean isError() { return type instanceof org.labrad.types.Error; }
  public boolean hasUnits() {
    return ((type instanceof org.labrad.types.Value) ||
        (type instanceof org.labrad.types.Complex))
        && (type.getUnits() != null);
  }

  // indexed type checks
  //    public boolean isBool(int...indices) { return get(indices).isBool(); }
  //    public boolean isInt(int...indices) { return get(indices).isInt(); }
  //    public boolean isWord(int...indices) { return get(indices).isWord(); }
  //    public boolean isBytes(int...indices) { return get(indices).isBool(); }
  //    public boolean isString(int...indices) { return get(indices).isString(); }
  //    public boolean isValue(int...indices) { return get(indices).isValue(); }
  //    public boolean isComplex(int...indices) { return get(indices).isComplex(); }
  //    public boolean isTime(int...indices) { return get(indices).isTime(); }
  //    public boolean isArray(int...indices) { return get(indices).isArray(); }
  //    public boolean isCluster(int...indices) { return get(indices).isCluster(); }
  //    public boolean hasUnits(int...indices) { return get(indices).hasUnits(); }

  // getters
  public boolean getBool() {
    getSubtype(Type.Code.BOOL);
    return Bytes.getBool(getOffset());
  }

  public int getInt() {
    getSubtype(Type.Code.INT);
    return Bytes.getInt(getOffset());
  }

  public long getWord() {
    getSubtype(Type.Code.WORD);
    return Bytes.getWord(getOffset());
  }

  public byte[] getBytes() {
    getSubtype(Type.Code.STR);
    return heap.get(Bytes.getInt(getOffset()));
  }

  public String getString() {
    try {
      return new String(getBytes(), STRING_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unsupported string encoding.");
    }
  }

  public String getString(String encoding) throws UnsupportedEncodingException {
    return new String(getBytes(), encoding);
  }

  public double getValue() {
    getSubtype(Type.Code.VALUE);
    return Bytes.getDouble(getOffset());
  }

  public Complex getComplex() {
    getSubtype(Type.Code.COMPLEX);
    return Bytes.getComplex(getOffset());
  }

  public String getUnits() {
    return type.getUnits();
  }

  public Date getTime() {
    getSubtype(Type.Code.TIME);
    ByteArrayView ofs = getOffset();
    long seconds = Bytes.getLong(ofs.getBytes(), ofs.getOffset());
    long fraction = Bytes.getLong(ofs.getBytes(), ofs.getOffset() + 8);
    seconds -= DELTA_SECONDS;
    fraction = (long)(((double) fraction) / Long.MAX_VALUE * 1000);
    return new Date(seconds * 1000 + fraction);
  }

  public int getArraySize() {
    int[] shape = getArrayShape();
    if (shape.length > 1) {
      throw new RuntimeException("Can't get size of multi-dimensional array.  Use getArrayShape.");
    }
    return shape[0];
  }

  public int[] getArrayShape() {
    getSubtype(Type.Code.LIST);
    int depth = type.getDepth();
    int[] shape = new int[depth];
    ByteArrayView pos = getOffset();
    for (int i = 0; i < depth; i++) {
      shape[i] = Bytes.getInt(pos.getBytes(), pos.getOffset() + 4*i);
    }
    return shape;
  }

  public int getClusterSize() {
    getSubtype(Type.Code.CLUSTER);
    return type.size();
  }

  public int getErrorCode() {
    getSubtype(Type.Code.ERROR);
    return Bytes.getInt(getOffset());
  }

  public String getErrorMessage() {
    getSubtype(Type.Code.ERROR);
    ByteArrayView pos = getOffset();
    int index = Bytes.getInt(pos.getBytes(), pos.getOffset() + 4);
    try {
      return new String(heap.get(index), STRING_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unsupported string encoding.");
    }
  }

  public Data getErrorPayload() {
    getSubtype(Type.Code.ERROR);
    ByteArrayView pos = getOffset();
    return new Data(type.getSubtype(0),
        pos.getBytes(), pos.getOffset() + 8, heap);
  }


  // indexed getters
  //    public boolean getBool(int...indices) { return get(indices).getBool(); }
  //    public int getInt(int...indices) { return get(indices).getInt(); }
  //    public long getWord(int...indices) { return get(indices).getWord(); }
  //    public byte[] getBytes(int...indices) { return get(indices).getBytes(); }
  //    public String getString(int...indices) { return get(indices).getString(); }
  //    public String getString(String encoding, int...indices)
  //            throws UnsupportedEncodingException {
  //        return get(indices).getString(encoding);
  //    }
  //    public double getValue(int...indices) { return get(indices).getValue(); }
  //    public Complex getComplex(int...indices) { return get(indices).getComplex(); }
  //    public String getUnits(int...indices) { return getSubtype(indices).getUnits(); }
  //    public Date getTime(int...indices) { return get(indices).getTime(); }
  //    public int getArraySize(int...indices) { return get(indices).getArraySize(); }
  //    public int[] getArrayShape(int...indices) { return get(indices).getArrayShape(); }
  //    public int getClusterSize(int...indices) {
  //        return getSubtype(Type.Code.CLUSTER, indices).size();
  //    }

  // setters
  public Data setBool(boolean data) {
    getSubtype(Type.Code.BOOL);
    Bytes.setBool(getOffset(), data);
    return this;
  }

  public Data setInt(int data) {
    getSubtype(Type.Code.INT);
    Bytes.setInt(getOffset(), data);
    return this;
  }

  public Data setWord(long data) {
    getSubtype(Type.Code.WORD);
    Bytes.setWord(getOffset(), data);
    return this;
  }

  public Data setBytes(byte[] data) {
    getSubtype(Type.Code.STR);
    ByteArrayView ofs = getOffset();
    int heapLocation = Bytes.getInt(ofs);
    if (heapLocation == -1) {
      // not yet set in the heap
      Bytes.setInt(ofs, heap.size());
      heap.add(data);
    } else {
      // already set in the heap, reuse old spot
      heap.set(heapLocation, data);
    }
    return this;
  }

  public Data setString(String data) {
    try {
      setBytes(data.getBytes(STRING_ENCODING));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unsupported string encoding.");
    }
    return this;
  }

  public Data setString(String data, String encoding)
  throws UnsupportedEncodingException {
    return setBytes(data.getBytes(encoding));
  }

  public Data setValue(double data) {
    getSubtype(Type.Code.VALUE);
    Bytes.setDouble(getOffset(), data);
    return this;
  }

  public Data setComplex(Complex data) {
    getSubtype(Type.Code.COMPLEX);
    Bytes.setComplex(getOffset(), data);
    return this;
  }

  public Data setComplex(double re, double im) {
    return setComplex(new Complex(re, im));
  }

  public Data setTime(Date date) {
    getSubtype(Type.Code.TIME);
    long millis = date.getTime();
    long seconds = millis / 1000 + DELTA_SECONDS;
    long fraction = millis % 1000;
    fraction = (long)(((double) fraction) / 1000 * Long.MAX_VALUE);
    ByteArrayView ofs = getOffset();
    Bytes.setLong(ofs.getBytes(), ofs.getOffset(), seconds);
    Bytes.setLong(ofs.getBytes(), ofs.getOffset() + 8, fraction);
    return this;
  }

  public Data setArraySize(int size) {
    setArrayShape(new int[] {size});
    return this;
  }

  public Data setArrayShape(List<Integer> shape) {
    int[] shapeArray = new int[shape.size()];
    for (int i = 0; i < shape.size(); i++) {
      shapeArray[i] = shape.get(i);
    }
    return setArrayShape(shapeArray);
  }

  public Data setArrayShape(int...shape) {
    getSubtype(Type.Code.LIST);
    Type elementType = type.getSubtype(0);
    int depth = type.getDepth();
    if (shape.length != depth) {
      throw new RuntimeException("Array depth mismatch!");
    }
    ByteArrayView pos = getOffset();
    int size = 1;
    for (int i = 0; i < depth; i++) {
      Bytes.setInt(pos.getBytes(), pos.getOffset() + 4*i, shape[i]);
      size *= shape[i];
    }
    byte[] buf = createFilledByteArray(elementType.dataWidth() * size);
    int heapIndex = Bytes.getInt(pos.getBytes(), pos.getOffset() + 4*depth);
    if (heapIndex == -1) {
      Bytes.setInt(pos.getBytes(), pos.getOffset() + 4*depth, heap.size());
      heap.add(buf);
    } else {
      heap.set(heapIndex, buf);
    }
    return this;
  }

  public Data setError(int code, String message) {
    getSubtype(Type.Code.ERROR);
    ByteArrayView pos = getOffset();
    Bytes.setInt(pos.getBytes(), pos.getOffset(), code);
    try {
      byte[] buf = message.getBytes(STRING_ENCODING);
      int heapIndex = Bytes.getInt(pos.getBytes(), pos.getOffset() + 4);
      if (heapIndex == -1) {
        Bytes.setInt(pos.getBytes(), pos.getOffset()+4, heap.size());
        heap.add(buf);
      } else {
        heap.set(heapIndex, buf);
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unicode encoding exception.");
    }
    return this;
  }


  // indexed setters
  public Data setBool(boolean data, int...indices) {
    get(indices).setBool(data);
    return this;
  }

  public Data setInt(int data, int...indices) {
    get(indices).setInt(data);
    return this;
  }

  public Data setWord(long data, int...indices) {
    get(indices).setWord(data);
    return this;
  }

  public Data setBytes(byte[] data, int...indices) {
    get(indices).setBytes(data);
    return this;
  }

  public Data setString(String data, int...indices) {
    get(indices).setString(data);
    return this;
  }

  public Data setString(String data, String encoding, int...indices)
  throws UnsupportedEncodingException {
    get(indices).setString(data, encoding);
    return this;
  }

  public Data setValue(double data, int...indices) {
    get(indices).setValue(data);
    return this;
  }

  public Data setComplex(Complex data, int...indices) {
    get(indices).setComplex(data);
    return this;
  }

  public Data setComplex(double re, double im, int...indices) {
    return setComplex(new Complex(re, im), indices);
  }

  public Data setTime(Date date, int...indices) {
    get(indices).setTime(date);
    return this;
  }

  public Data setArraySize(int size, int...indices) {
    get(indices).setArraySize(size);
    return this;
  }

  public Data setArrayShape(int[] shape, int...indices) {
    get(indices).setArrayShape(shape);
    return this;
  }

  public Data setArrayShape(List<Integer> shape, int...indices) {
    get(indices).setArrayShape(shape);
    return this;
  }

  // array getters
  public boolean[] getBoolArray() {
    getSubtype(Type.Code.LIST);
    getSubtype(Type.Code.BOOL, 0);
    int len = getArraySize();
    boolean[] result = new boolean[len];
    for (int i = 0; i < len; i++) {
      result[i] = get(i).getBool();
    }
    return result;
  }

  public int[] getIntArray() {
    getSubtype(Type.Code.LIST);
    getSubtype(Type.Code.INT, 0);
    int len = getArraySize();
    int[] result = new int[len];
    for (int i = 0; i < len; i++) {
      result[i] = get(i).getInt();
    }
    return result;
  }

  public long[] getWordArray() {
    getSubtype(Type.Code.LIST);
    getSubtype(Type.Code.WORD, 0);
    int len = getArraySize();
    long[] result = new long[len];
    for (int i = 0; i < len; i++) {
      result[i] = get(i).getWord();
    }
    return result;
  }

  public double[] getValueArray() {
    getSubtype(Type.Code.LIST);
    getSubtype(Type.Code.VALUE, 0);
    int len = getArraySize();
    double[] result = new double[len];
    for (int i = 0; i < len; i++) {
      result[i] = get(i).getValue();
    }
    return result;
  }
  
  public double[][] getValueArray2D() {
    getSubtype(Type.Code.LIST);
    getSubtype(Type.Code.VALUE, 0, 0);
    int[] shape = getArrayShape();
    double[][] result = new double[shape[0]][];
    for (int i = 0; i < shape[0]; i++) {
      result[i] = new double[shape[1]];
      for (int j = 0; j < shape[1]; j++) {
        result[i][j] = get(i, j).getValue();
      }
    }
    return result;
  }

  public String[] getStringArray() {
    getSubtype(Type.Code.LIST);
    getSubtype(Type.Code.STR, 0);
    int len = getArraySize();
    String[] result = new String[len];
    for (int i = 0; i < len; i++) {
      result[i] = get(i).getString();
    }
    return result;
  }

  public Data[] getDataArray() {
    getSubtype(Type.Code.LIST);
    int len = getArraySize();
    Data[] result = new Data[len];
    for (int i = 0; i < len; i++) {
      result[i] = get(i);
    }
    return result;
  }

  // vectorized getters
  public List<Data> getDataList() {
    getSubtype(Type.Code.LIST);
    int len = getArraySize();
    List<Data> result = new ArrayList<Data>();
    for (int i = 0; i < len; i++) {
      result.add(get(i));
    }
    return result;
  }

  public List<Data> getClusterAsList() {
    getSubtype(Type.Code.CLUSTER);
    int len = getClusterSize();
    List<Data> result = new ArrayList<Data>();
    for (int i = 0; i < len; i++) {
      result.add(get(i));
    }
    return result;
  }
  
  public <T> List<T> getList(Getter<T> getter) {
    getSubtype(Type.Code.LIST);
    int len = getArraySize();
    List<T> result = new ArrayList<T>();
    if (len == 0) return result;
    getSubtype(getter.getType().getCode(), 0);
    for (int i = 0; i < len; i++) {
      result.add(getter.get(get(i)));
    }
    return result;
  }
  public List<Boolean> getBoolList() { return getList(Getters.boolGetter); }
  public List<Integer> getIntList() { return getList(Getters.intGetter); }
  public List<Long> getWordList() { return getList(Getters.wordGetter); }
  public List<String> getStringList() { return getList(Getters.stringGetter); }
  public List<Date> getDateList() { return getList(Getters.dateGetter); }
  public List<Double> getDoubleList() { return getList(Getters.valueGetter); }
  public List<Complex> getComplexList() { return getList(Getters.complexGetter); }

  // vectorized indexed getters
  //	public List<Boolean> getBoolList(int...indices) { return get(indices).getBoolList(); }
  //	public List<Integer> getIntList(int...indices) { return get(indices).getIntList(); }
  //	public List<Long> getWordList(int...indices) { return get(indices).getWordList(); }
  //	public List<String> getStringList(int...indices) { return get(indices).getStringList(); }


  // vectorized setters
  public <T> Data setList(List<T> data, Setter<T> setter) {
    getSubtype(Type.Code.LIST); // make sure this is a list
    if (data.size() > 0) {
    	getSubtype(setter.getType().getCode(), 0); // make sure the element type is correct
    }
    setArraySize(data.size());
    int i = 0;
    for (T elem : data) {
      setter.set(get(i++), elem);
    }
    return this;
  }
  public Data setBoolList(List<Boolean> data) { return setList(data, Setters.boolSetter); }
  public Data setIntList(List<Integer> data) { return setList(data, Setters.intSetter); }
  public Data setWordList(List<Long> data) { return setList(data, Setters.wordSetter); }
  public Data setStringList(List<String> data) { return setList(data, Setters.stringSetter); }
  public Data setDateList(List<Date> data) { return setList(data, Setters.dateSetter); }
  public Data setDoubleList(List<Double> data) { return setList(data, Setters.valueSetter); }
  public Data setComplexList(List<Complex> data) { return setList(data, Setters.complexSetter); }


  // vectorized indexed setters
  public Data setBoolList(List<Boolean> data, int...indices) {
    get(indices).setBoolList(data);
    return this;
  }

  public Data setIntList(List<Integer> data, int...indices) {
    get(indices).setIntList(data);
    return this;
  }

  public Data setWordList(List<Long> data, int...indices) {
    get(indices).setWordList(data);
    return this;
  }

  public Data setStringList(List<String> data, int...indices) {
    get(indices).setStringList(data);
    return this;
  }

  public Data setDateList(List<Date> data, int...indices) {
    get(indices).setDateList(data);
    return this;
  }

  public Data setDoubleList(List<Double> data, int...indices) {
    get(indices).setDoubleList(data);
    return this;
  }

  public Data setComplexList(List<Complex> data, int...indices) {
    get(indices).setComplexList(data);
    return this;
  }



  // some basic tests of the data object
  public static void main(String[] args) throws IOException {
    Random rand = new Random();
    boolean b;
    int i, count;
    long l;
    double d, re, im;

    Data d1, d2;
    byte[] flat;

    d1 = new Data("i");
    d1.setInt(100);
    assert d1.getInt() == 100;

    d1 = new Data("s");
    d1.setString("This is a test.");
    System.out.println(d1.getString());

    d1 = new Data("t");
    for (count = 0; count < 100000; count++) {
      Date date1 = new Date(rand.nextLong());
      d1.setTime(date1);
      Date date2 = d1.getTime();
      assert date1.equals(date2);
    }
    System.out.println("Date okay.");

    d1 = new Data("*s");
    d1.setArraySize(20);
    for (count = 0; count < 20; count++) {
      d1.setString("This is string " + Integer.toString(count), count);
    }
    for (count = 0; count < 20; count++) {
      System.out.println(d1.get(count).getString());
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
    d1.setString(s, 3);
    d1.setValue(d, 4);
    d1.setComplex(re, im, 5);

    assert b == d1.get(0).getBool();
    assert i == d1.get(1).getInt();
    assert l == d1.get(2).getWord();
    assert s.equals(d1.get(3).getString());
    assert d == d1.get(4).getValue();
    Complex c = d1.get(5).getComplex();
    assert re == c.getReal();
    assert im == c.getImag();
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
      d1.setString(s, count, 3);
      d1.setValue(d, count, 4);
      d1.setComplex(re, im, count, 5);

      assert b == d1.get(count, 0).getBool();
      assert i == d1.get(count, 1).getInt();
      assert l == d1.get(count, 2).getWord();
      assert s.equals(d1.get(count, 3).getString());
      assert d == d1.get(count, 4).getValue();
      c = d1.get(count, 5).getComplex();
      assert re == c.getReal();
      assert im == c.getImag();
    }
    System.out.println("List of Cluster okay.");
    System.out.println(d1.pretty());

    flat = d1.toBytes();
    d2 = fromBytes(flat, Type.fromTag("*(biwsv[m]c[m/s])"));
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
    flat = d1.toBytes();
    d2 = fromBytes(flat, Type.fromTag("*2i"));
    System.out.println(d2.pretty());

    d1 = new Data("*3s");
    d1.setArrayShape(2, 2, 2);
    for (int m = 0; m < 2; m++) {
      for (int n = 0; n < 2; n++) {
        for (int p = 0; p < 2; p++) {
          d1.setString("TestString(" + m + n + p + ")", m, n, p);
        }
      }
    }
    System.out.println(d1.pretty());
    flat = d1.toBytes();
    d2 = fromBytes(flat, Type.fromTag("*3s"));
    System.out.println(d2.pretty());

    System.out.println("done.");
  }
}
