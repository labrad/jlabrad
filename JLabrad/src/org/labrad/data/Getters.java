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

public class Getters {
	public static Getter<Boolean> boolGetter = new Getter<Boolean>() {
		public Type getType() { return Bool.getInstance(); }
		public Boolean get(Data data) { return data.getBool(); }
	};
	public static Getter<Integer> intGetter = new Getter<Integer>() {
		public Type getType() { return Int.getInstance(); }
		public Integer get(Data data) { return data.getInt(); }
	};
	public static Getter<Long> wordGetter = new Getter<Long>() {
		public Type getType() { return Word.getInstance(); }
		public Long get(Data data) { return data.getWord(); }
	};
	public static Getter<String> stringGetter = new Getter<String>() {
		public Type getType() { return Str.getInstance(); }
		public String get(Data data) { return data.getString(); }
	};
	public static Getter<Date> dateGetter = new Getter<Date>() {
		public Type getType() { return Time.getInstance(); }
		public Date get(Data data) { return data.getTime(); }
	};
	public static Getter<Double> valueGetter = new Getter<Double>() {
		private final Type type = org.labrad.types.Value.of(null);
		public Type getType() { return type; }
		public Double get(Data data) { return data.getValue(); }
	};
	public static Getter<Complex> complexGetter = new Getter<Complex>() {
		private final Type type = org.labrad.types.Value.of(null);
		public Type getType() { return type; }
		public Complex get(Data data) { return data.getComplex(); }
	};
}
