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

import java.util.Date;

import org.labrad.types.Bool;
import org.labrad.types.Int;
import org.labrad.types.Str;
import org.labrad.types.Time;
import org.labrad.types.Type;
import org.labrad.types.Word;

public class Setters {
  public static Setter<Boolean> boolSetter = new Setter<Boolean>() {
    public Type getType() { return Bool.getInstance(); }
    public void set(Data data, Boolean value) { data.setBool(value); }
  };
  public static Setter<Integer> intSetter = new Setter<Integer>() {
    public Type getType() { return Int.getInstance(); }
    public void set(Data data, Integer value) { data.setInt(value); }
  };
  public static Setter<Long> wordSetter = new Setter<Long>() {
    public Type getType() { return Word.getInstance(); }
    public void set(Data data, Long value) { data.setWord(value); }
  };
  public static Setter<String> stringSetter = new Setter<String>() {
    public Type getType() { return Str.getInstance(); }
    public void set(Data data, String value) { data.setString(value); }
  };
  public static Setter<Date> dateSetter = new Setter<Date>() {
    public Type getType() { return Time.getInstance(); }
    public void set(Data data, Date value) { data.setTime(value); }
  };
  public static Setter<Double> valueSetter = new Setter<Double>() {
    private final Type type = org.labrad.types.Value.of(null);
    public Type getType() { return type; }
    public void set(Data data, Double value) { data.setValue(value); }
  };
  public static Setter<Complex> complexSetter = new Setter<Complex>() {
    private final Type type = org.labrad.types.Complex.of(null);
    public Type getType() { return type; }
    public void set(Data data, Complex value) { data.setComplex(value); }
  };
}
