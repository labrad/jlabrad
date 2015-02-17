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

package org.labrad;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.labrad.annotations.Accepts;
import org.labrad.annotations.Returns;
import org.labrad.annotations.Setting;
import org.labrad.data.Data;
import org.labrad.data.Getter;
import org.labrad.data.Getters;
import org.labrad.handlers.MultiArgHandler;
import org.labrad.handlers.MultiArgVoidHandler;
import org.labrad.handlers.OverloadedSettingHandler;
import org.labrad.handlers.SingleArgHandler;
import org.labrad.handlers.SingleArgVoidHandler;
import org.labrad.handlers.ZeroArgHandler;
import org.labrad.handlers.ZeroArgVoidHandler;
import org.labrad.types.Empty;
import org.labrad.types.Type;
import org.labrad.types.TypeDescriptor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SettingHandlers {
  /**
   * Helper class that encapsulates a setting handler and all of its accepted types
   * @author maffoo
   *
   */
  private static class TypedHandler {
    private final List<TypeDescriptor> t;
    private final List<TypeDescriptor> r;
    private final SettingHandler h;
    public TypedHandler(List<TypeDescriptor> t, List<TypeDescriptor> r, SettingHandler h) {
      this.t = t;
      this.r = r;
      this.h = h;
    }
    public List<TypeDescriptor> getTypes() { return t; }
    public List<TypeDescriptor> getReturnTypes() { return r; }
    public SettingHandler getHandler() { return h; }
  }

  /**
   * Create a SettingHandler for a method or set of overridden methods
   * @param s
   * @param overloads
   * @return
   */
  public static SettingHandler forMethods(Setting s, List<Method> overloads) {
    // TODO add returned types to typed handlers since they may need to flatten objects before sending them

    // for registration with labrad, we need to keep track of the full list
    // of accepted and returned types across all overloads of this method
    List<TypeDescriptor> accepts = Lists.newArrayList(); // list of all accepted types
    List<TypeDescriptor> returns = Lists.newArrayList(); // list of all returned types
    
    // the overloaded handler has a map from types to single method handlers
    List<TypedHandler> handlers = Lists.newArrayList();

    for (Method m : overloads) {
      // get accepted types for this overload
      TypedHandler th = getHandler(m, s);
      
      // check each type for conflicts with types from other overloads
      for (TypeDescriptor typeTD : th.getTypes()) {
        Type type = typeTD.getType();
        for (TypeDescriptor otherTD : accepts) {
          Type other = otherTD.getType();
          if (type.matches(other) || other.matches(type)) {
            Failure.fail("Type conflict in overloads for method '%s'", m.getName());
          }
        }
        // add this type to the main list
        accepts.add(typeTD);
      }
      // add this handler to the handler map
      handlers.add(th);
      
      // get returned types for this overload
      // we check them for overlap with the previously-specified
      // return types, and only add them if they are new
      for (TypeDescriptor td : th.getReturnTypes()) {
        Type t = td.getType();
        // check whether a matching type has already been added to the 
        boolean match = false;
        for (TypeDescriptor otherTD : returns) {
          Type other = otherTD.getType();
          if (t.matches(other) || other.matches(t)) {
            match = true;
            break;
          }
        }
        if (!match) { returns.add(td); }
      }
    }

    // build the final handler, by making a composite or extracting out just one handler from the bunch
    if (handlers.size() == 1) {
      // just use the unique handler
      // need to specify return types here
      return handlers.get(0).getHandler();

    } else {
      // make a map from all types to the appropriate handlers
      Map<Type, SettingHandler> typeMap = Maps.newHashMap();
      for (TypedHandler th : handlers) {
        for (TypeDescriptor td : th.getTypes()) {
          typeMap.put(td.getType(), th.getHandler());
        }
      }
      // build an OverloadedSettingHandler
      List<String> acceptedTypes = Lists.newArrayList();
      for (TypeDescriptor td : accepts) { acceptedTypes.add(td.getTag()); }

      List<String> returnedTypes = Lists.newArrayList();
      for (TypeDescriptor td : returns) { returnedTypes.add(td.getTag()); }

      return new OverloadedSettingHandler(s, acceptedTypes, returnedTypes, typeMap);
    }
  }


  private static final boolean[] BOOL_ARRAY = new boolean[0];
  private static final int[] INT_ARRAY = new int[0];
  private static final long[] LONG_ARRAY = new long[0];
  private static final double[] DOUBLE_ARRAY = new double[0];
  private static final String[] STRING_ARRAY = new String[0];
  private static final Data[] DATA_ARRAY = new Data[0];
  private static final byte[] BYTE_ARRAY = new byte[0];

  /**
   * Get a handler for a particular method, along with a list of all
   * accepted types for the method (since each parameter may accept multiple types) 
   * @param m
   * @param s
   * @return
   */
  @SuppressWarnings("rawtypes")
  private static TypedHandler getHandler(Method m, Setting s) {
    // each parameter to the method has a type that we can infer,
    // as well as optional types from the @Accepts annotation

    // from the inferred type, we should be able to build a getter

    // we must also verify that all types given in the @Accepts annotation
    // match the inferred type, which will ensure that the getter is
    // compatible with what is coming in

    List<String> acceptedTypes = Lists.newArrayList();
    List<String> returnedTypes = Lists.newArrayList();
    List<Getter> getters = Lists.newArrayList();

    // build a list of translators for the arguments
    // build a list of accepted types
    java.lang.reflect.Type[] paramTypes = m.getGenericParameterTypes();
    Annotation[][] paramAnnotations = m.getParameterAnnotations();
    List<List<TypeDescriptor>> paramAcceptedTypes = Lists.newArrayList();

    for (int i = 0; i < paramTypes.length; i++) {
      java.lang.reflect.Type cls = paramTypes[i];

      // infer the of each argument and create a getter for it
      Getter getter = null;
      Type inferredType = null;

      // primitives
      if (cls.equals(Boolean.TYPE) || cls.equals(Boolean.class)) {
        getter = Getters.boolGetter; 
        inferredType = Type.fromTag("b");

      } else if (cls.equals(Integer.TYPE) || cls.equals(Integer.class)) {
        getter = Getters.intGetter;
        inferredType = Type.fromTag("i");

      } else if (cls.equals(Long.TYPE) || cls.equals(Long.class)) {
        getter = Getters.wordGetter;
        inferredType = Type.fromTag("w");

      } else if (cls.equals(String.class)) {
        getter = Getters.stringGetter;
        inferredType = Type.fromTag("s");

      } else if (cls.equals(Double.TYPE) || cls.equals(Double.class)) {
        getter = Getters.valueGetter;
        inferredType = Type.fromTag("v");

      // arbitrary LabRAD data
      } else if (cls.equals(Data.class)) {
        getter = null;
        inferredType = Type.fromTag("?");


      // Array of primitives or data
      } else if (cls.equals(BOOL_ARRAY.getClass())) {
        getter = Getters.boolArrayGetter;
        inferredType = Type.fromTag("*b");

      } else if (cls.equals(INT_ARRAY.getClass())) {
        getter = Getters.intArrayGetter;
        inferredType = Type.fromTag("*i");

      } else if (cls.equals(LONG_ARRAY.getClass())) {
        getter = Getters.wordArrayGetter;
        inferredType = Type.fromTag("*w");

      } else if (cls.equals(DOUBLE_ARRAY.getClass())) {
        getter = Getters.valueArrayGetter;
        inferredType = Type.fromTag("*v");

      } else if (cls.equals(STRING_ARRAY.getClass())) {
        getter = Getters.stringArrayGetter;
        inferredType = Type.fromTag("*s");

      } else if (cls.equals(DATA_ARRAY.getClass())) {
        getter = Getters.dataArrayGetter;
        inferredType = Type.fromTag("*?");

      } else if (cls.equals(BYTE_ARRAY.getClass())) {
        getter = Getters.byteArrayGetter;
        inferredType = Type.fromTag("s");


      // List of primitives or data
      } else if (cls instanceof ParameterizedType) {
        ParameterizedType paramCls = (ParameterizedType)cls;
        java.lang.reflect.Type raw = paramCls.getRawType();
        java.lang.reflect.Type[] elems = paramCls.getActualTypeArguments();

        if (raw.equals(List.class)) {
          java.lang.reflect.Type elem = elems[0];

          // primitives
          if (elem.equals(Boolean.TYPE) || cls.equals(Boolean.class)) {
            getter = Getters.boolListGetter; 
            inferredType = Type.fromTag("*b");

          } else if (elem.equals(Integer.TYPE) || cls.equals(Integer.class)) {
            getter = Getters.intListGetter;
            inferredType = Type.fromTag("*i");

          } else if (elem.equals(Long.TYPE) || cls.equals(Long.class)) {
            getter = Getters.wordListGetter;
            inferredType = Type.fromTag("*w");

          } else if (elem.equals(String.class)) {
            getter = Getters.stringListGetter;
            inferredType = Type.fromTag("*s");

          // LabRAD data
          } else if (elem.equals(Data.class)) {
            getter = Getters.dataListGetter;
            inferredType = Type.fromTag("*?");
          }
        }
      }
      if (inferredType == null) {
        Failure.fail("Unable to infer a LabRAD type for parameter %d of method '%s'",
            i, m.getName());
      }
      getters.add(getter);

      List<TypeDescriptor> acceptedTags = Lists.newArrayList();
      // if there is an @Accepts annotation present on this parameter,
      // check the compatibility of the accepted types with the inferred type
      for (Annotation a : paramAnnotations[i]) {
        if (Accepts.class.isInstance(a)) {
          Accepts types = (Accepts)a;
          for (String tag : types.value()) {
            TypeDescriptor td = new TypeDescriptor(tag);
            Type aType = td.getType();
            if (!aType.matches(inferredType)) {
              Failure.fail("Accepted type '%s' does not match inferred type '%s'",
                  tag, inferredType);
            }
            acceptedTags.add(td);
          }
        }
      }
      // if there was no @Accepts annotation, then just use the inferred type
      if (acceptedTags.size() == 0) {
        acceptedTags.add(new TypeDescriptor(inferredType));
      }
      paramAcceptedTypes.add(acceptedTags);
    }


    // create a handler of the appropriate type for this setting
    SettingHandler handler;
    List<TypeDescriptor> accepts;
    List<TypeDescriptor> returns;

    int numArgs = m.getParameterTypes().length;
    boolean isVoid = (m.getReturnType() == Void.TYPE);
    returns = getReturnedTypes(m);
    for (TypeDescriptor td : returns) {
      returnedTypes.add(td.getTag());
    }
    
    switch (numArgs) {
      case 0:
        accepts = Lists.newArrayList();
        accepts.add(new TypeDescriptor(Empty.getInstance()));
        if (isVoid) {
          handler = new ZeroArgVoidHandler(m, s);
        } else {
          handler = new ZeroArgHandler(m, s, returnedTypes);          
        }
        break;

      case 1:
        accepts = paramAcceptedTypes.get(0);
        for (TypeDescriptor td : accepts) {
          acceptedTypes.add(td.getTag());
        }
        if (isVoid) {
          handler = new SingleArgVoidHandler(m, s, acceptedTypes, getters.get(0));
        } else {
          handler = new SingleArgHandler(m, s, acceptedTypes, returnedTypes, getters.get(0));
        }
        break;

      default:
        accepts = getAcceptedTypeCombinations(paramAcceptedTypes);
        for (TypeDescriptor td : accepts) {
          acceptedTypes.add(td.getTag());
        }
        if (isVoid) {
          handler = new MultiArgVoidHandler(m, s, acceptedTypes, getters);
        } else {
          handler = new MultiArgHandler(m, s, acceptedTypes, returnedTypes, getters);
        }
    }

    return new TypedHandler(accepts, returns, handler);
  }

  /**
   * Get a list of accepted types given lists of types accepted for
   * each of a set of parameters.
   * @param acceptedLists
   * @return
   */
  private static List<TypeDescriptor> getAcceptedTypeCombinations(List<List<TypeDescriptor>> acceptedLists) {
    List<List<TypeDescriptor>> typeLists = combinations(acceptedLists);
    List<TypeDescriptor> combinedTypes = Lists.newArrayList();
    for (List<TypeDescriptor> types : typeLists) {
      // XXX need an intelligent way to combine tags here
      StringBuilder tag = new StringBuilder();
      StringBuilder description = new StringBuilder();
      for (int i = 0; i < types.size(); i++) {
        if (i > 0) {
          tag.append(", ");
          description.append(", ");
        }
        TypeDescriptor td = types.get(i);
        tag.append(td.getType().toString());
        description.append(td.getTag());
      }
      combinedTypes.add(new TypeDescriptor(tag + ": " + description));
    }
    return combinedTypes;
  }

  /**
   * Get a list of all combinations of elements, chosen one each from the given lists
   * @param <T>
   * @param lists
   * @return
   */
  private static <T> List<List<T>> combinations(List<List<T>> lists) {
    List<List<T>> ans = new ArrayList<List<T>>();
    if (lists.size() == 0) {
      ans.add(new ArrayList<T>());
      return ans;
    }
    List<List<T>> tails = combinations(lists.subList(1, lists.size()));
    for (T head : lists.get(0)) {
      for (List<T> tail : tails) {
        List<T> entry = new ArrayList<T>();
        entry.add(head);
        entry.addAll(tail);
        ans.add(entry);
      }
    }
    return ans;
  }


  /**
   * Get a list of all returned LabRAD types for a method
   * @param m
   * @return
   */
  private static List<TypeDescriptor> getReturnedTypes(Method m) {
    // TODO check Java return types and provide setters for them if possible
    // TODO if there is an annotation and inferred types, make sure they are compatible
    List<TypeDescriptor> ans = Lists.newArrayList();
    boolean hasAnnotation = false;
    for (Annotation annot : m.getAnnotations()) {
      if (Returns.class.isInstance(annot)) {
        hasAnnotation = true;
        Returns types = (Returns)annot;
        for (String tag : types.value()) {
          ans.add(new TypeDescriptor(tag));
        }
      }
    }
    if (!hasAnnotation) {
      if (m.getReturnType() == Data.class) {
        ans.add(new TypeDescriptor("?"));
      }
      
      else if (m.getReturnType() == Void.TYPE) {
        ans.add(new TypeDescriptor(""));
      }
    }
    return ans;
  }

  // TODO add Flattenable interface for automatic conversion to/from LabRAD types (ala Serializable)
  // TODO allow Lists and Arrays of Flattenable types
}
