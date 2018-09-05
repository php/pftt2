package com.github.mattficken.io;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayUtil {
	
	public static <E extends Object> List<E> clone(List<E> in) {
		ArrayList<E> out = new ArrayList<E>(in.size());
		out.addAll(in);
		return out;
	}
	
	public static <E extends Object> E[] toArray(List<E> unknown_options) {
		return unknown_options.toArray(newArray(unknown_options, unknown_options.size()));
	}
	
	public static <E extends Object> E[] rshift(E[] array, int off) {
		return rshift(array, off, array.length-off);
	}
	
	public static <E extends Object> E[] rshift(E[] array, int off, int len) {
		E[] out = newArray(array, len);
		
		System.arraycopy(array, off, out, 0, len);
		
		return out;
	}
	
	public static <E extends Object> E[] newArray(E[] sample, int size) throws NullPointerException, ArrayIndexOutOfBoundsException {
		return newArray(sample[0], size);
	}
	
	public static <E extends Object> E[] newArray(List<E> sample, int size) throws NullPointerException, ArrayIndexOutOfBoundsException {
		return newArray(sample.get(0), size);
	}
	
	public static <E extends Object> E[] newArray(E sample, int size) throws NullPointerException {
		return (E[]) Array.newInstance(sample.getClass(), size);
	}
	
	public static final int NOT_FOUND = -1;
	public static <E extends Object> int indexOf(E[] in, E a, int off, int len) {
		for ( ; off < len ; off++ ) {
			if (in[off].equals(a))
				return off;
		}
		return NOT_FOUND;
	}
	
	public static <E extends Object> boolean contains(E[] in, E a, int off, int len) {
		return indexOf(in, a, off, len) != NOT_FOUND;
	}
	
	public static <E extends Object> List<E> copyNoDuplicates(List<E> list) {
		ArrayList<E> out = new ArrayList<E>(list.size());
		copyNoDuplicates(list, out);
		return out;
	}
	
	public static <E extends Object> void copyNoDuplicates(List<E> in, List<E> out) {
		for ( E e : in ) {
			if (!out.contains(e))
				out.add(e);
		}
	}
	
	public static <E extends Object> E[] copyNoDuplicates(E[] in) {
		if (in==null)
			return null;
		else if (in.length<2)
			return in;
		E[] out = newArray(in, in.length);
		int i=0;
		for ( E e : in ) {
			if (!contains(out, e, 0, i)) {
				out[i++] = e;
			}
		}
		return out;
	}
	
	public static <E extends Object> E[] mergeNoDuplicates(Class<E> clazz, E[] ... lists) {
		if (lists==null) {
			return null;
		} else if (lists.length==0) {
			return lists[0];
		} else if (lists.length==1) {
			return copyNoDuplicates(lists[0]);
		} else {
			ArrayList<E> out = new ArrayList<E>(lists[0]==null?lists[0].length+16:16);
			
			for (E[] list : lists) {
				if (list==null)
					continue;
				
				for (E e : list) {
					if (e==null)
						continue;
					else if (!out.contains(e))
						out.add(e);				
				}
			}
			
			return clazz == null ? null : (E[]) out.toArray((E[])Array.newInstance(clazz, out.size()));
		}
	}
	
	public static <E extends Object> E[] mergeAllowDuplicates(E[] ... lists) {
		if (lists==null)
			return null;
		
		ArrayList<E> out = new ArrayList<E>(lists[0]==null?lists[0].length+16:16);
		
		Class<?> clazz = null;
		for (E[] list : lists) {
			if (list==null)
				continue;
			
			for (E e : list) {
				if (e==null)
					continue;
				else if (clazz==null)
					clazz = e.getClass();
				else if (e.getClass()!=clazz)
					continue;
				else
					out.add(e);				
			}
		}
		
		return clazz == null ? null : (E[]) out.toArray((E[])Array.newInstance(clazz, out.size()));
	}
	
	public static <E extends Object> ArrayList<E> toList(E[] e) {
		if (e==null)
			return new ArrayList<E>(0); // TODO temp
		ArrayList<E> o = new ArrayList<E>(e.length);
		for ( int i=0 ; i < e.length ; i++ )
			o.add(e[i]);
		return o;
	}
	
	public static <E extends Object> List<E> toList(E e) {
		ArrayList<E> o = new ArrayList<E>(1);
		o.add(e);
		return o;
	}

	public static <E extends Object> List<E> toList(Collection<E> c) {
		if (c instanceof List)
			return (List<E>) c;
		ArrayList<E> o = new ArrayList<E>(c.size());
		for ( E e : c )
			o.add(e);
		return o;
	}
	
}
