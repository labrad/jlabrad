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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.labrad.types.Bool;
import org.labrad.types.Cluster;
import org.labrad.types.Empty;
import org.labrad.types.Int;
import org.labrad.types.Str;
import org.labrad.types.Time;
import org.labrad.types.Type;
import org.labrad.types.Value;
import org.labrad.types.Word;

public class Hydrant {

  static Random random;

  static {
    random = new Random();
  }

  public static Type getRandomType() {
    return getRandomType(true, true, 0);
  }

  public static Type getRandomType(boolean noneOkay, boolean listOkay, int nStructs) {
    int min = 0, max = 9;
    if (!noneOkay) {
      min = 1;
    }
    if (nStructs >= 3) {
      max = 7;
    } else if (!listOkay) {
      max = 8;
    }
    int choice = random.nextInt(max - min + 1) + min;
    switch (choice) {
      case 0: return Empty.getInstance();
      case 1: return Bool.getInstance();
      case 2: return Int.getInstance();
      case 3: return Word.getInstance();
      case 4: return Str.getInstance();
      case 5: return Time.getInstance();
      case 6: return Value.of(getRandomUnits());
      case 7: return org.labrad.types.Complex.of(getRandomUnits());
      case 8:
        int length = random.nextInt(5) + 1;
        List<Type> elementTypes = new ArrayList<Type>();
        for (int i = 0; i < length; i++) {
          elementTypes.add(getRandomType(false, true, nStructs + 1));
        }
        return Cluster.of(elementTypes);
      case 9:
        Type elementType = getRandomType(false, false, random.nextInt(3) + 1);
        return org.labrad.types.List.of(elementType, nStructs + 1);
    }
    throw new RuntimeException("Invalid type choice.");
  }

  public static String getRandomUnits() {
    String[] choices = {null, "", "s", "ms", "us", "m", "m/s", "V^2/Hz", "V/Hz^1/2"};
    return choices[random.nextInt(choices.length)];
  }

  public static Data getRandomData() {
    Type t = getRandomType();
    return getRandomData(t);
  }

  public static Data getRandomData(String s) {
    Type t = Type.fromTag(s);
    return getRandomData(t);
  }

  public static Data getRandomData(Type t) {
    if (t instanceof Empty) return getRandomEmpty();
    if (t instanceof Bool) return getRandomBool();
    if (t instanceof Int) return getRandomInt();
    if (t instanceof Word) return getRandomWord();
    if (t instanceof Str) return getRandomStr();
    if (t instanceof Time) return getRandomTime();
    if (t instanceof Value) return getRandomValue((Value)t);
    if (t instanceof org.labrad.types.Complex) return getRandomComplex((org.labrad.types.Complex)t);
    if (t instanceof Cluster) return getRandomCluster((Cluster)t);
    if (t instanceof org.labrad.types.List) return getRandomList((org.labrad.types.List)t);
    throw new RuntimeException("Invalid type.");
  }

  // random none
  public static Data getRandomEmpty() {
    return Data.EMPTY;
  }

  // random boolean
  public static Data getRandomBool() {
    return Data.valueOf(random.nextBoolean());
  }

  // random integer
  public static Data getRandomInt() {
    return Data.valueOf(random.nextInt());
  }

  // random word
  public static Data getRandomWord() {
    return Data.valueOf((long)random.nextInt(2000000000));
  }

  // random string
  public static Data getRandomStr() {
    byte[] bytes = new byte[random.nextInt(100)];
    random.nextBytes(bytes);
    return Data.valueOf(bytes);
  }

  // random date
  public static Data getRandomTime() {
    long time = System.currentTimeMillis() + random.nextInt();
    return Data.valueOf(new Date(time));
  }

  // random value
  public static Data getRandomValue(Value t) {
    return Data.ofType(t).setValue(random.nextDouble());
  }

  // random complex
  public static Data getRandomComplex(org.labrad.types.Complex t) {
    return Data.ofType(t).setComplex(random.nextDouble(), random.nextDouble());
  }

  public static Data getRandomCluster(Cluster t) {
    List<Data> elements = new ArrayList<Data>();
    for (int i = 0; i < t.size(); i++) {
      elements.add(getRandomData(t.getSubtype(i)));
    }
    return Data.clusterOf(elements);
  }

  public static Data getRandomList(org.labrad.types.List t) {
    Data data = Data.ofType(t);
    Type elemType = t.getSubtype(0);
    int depth = t.getDepth();
    int[] shape = new int[depth];
    int[] indices = new int[depth];
    for (int i = 0; i < depth; i++) {
      shape[i] = random.nextInt((int)Math.pow(2, 5 - depth));
      indices[i] = 0;
    }
    data.setArrayShape(shape);
    fillList(data, elemType, indices, shape, 0);
    return data;
  }

  private static void fillList(Data data, Type elem, int[] indices, int[] shape, int depth) {
    if (depth == shape.length) {
      data.get(indices).set(getRandomData(elem));
    } else {
      for (int i = 0; i < shape[depth]; i++) {
        indices[depth] = i;
        fillList(data, elem, indices, shape, depth + 1);
      }
    }
  }
  /*
	def genList(elem, depth=1):
	    lengths = [randint(1, 2**(5-depth)) for _ in xrange(depth)]
	    def genNDList(ls):
	        if len(ls) == 1:
	            return [randValue(elem) for _ in xrange(ls[0])]
	        else:
	            return [genNDList(ls[1:]) for _ in xrange(ls[0])]
	    return genNDList(lengths)
   */

  /*
	def hoseDown(setting, n=1000, silent=True):
	    for _ in range(n):
	        t = randType()
	        v = randValue(t)
	        if not silent:
	            print t
	        try:
	            resp = setting(v)
	            assert v == resp
	        except:
	            print 'problem:', str(t), repr(t)
	            print str(T.flatten(v)[1]), str(T.flatten(resp)[1])
	            raise

	def hoseDataVault(dv, n=1000, silent=True):
	    for i in range(n):
	        t = randType(noneOkay=False)
	        v = randValue(t)
	        if not silent:
	            print t
	        try:
	            pname = 'p%03s' % i
	            dv.add_parameter(pname, v)
	            resp = dv.get_parameter(pname)
	            assert v == resp
	        except:
	            print 'problem:', str(t), repr(t)
	            print str(T.flatten(v)[1]), str(T.flatten(resp)[1])
	            raise
   */

}
