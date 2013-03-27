package com.github.mattficken.io;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayUtil {
	
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
	
	public static <E extends Object> E[] mergeNoDuplicates(Class<E> clazz, E[] ... lists) {
		if (lists==null)
			return null;
		else if (true)
			return lists[0];
		
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

	public static <E extends Object> List<E> toList(Collection<E> c) {
		if (c instanceof List)
			return (List<E>) c;
		ArrayList<E> o = new ArrayList<E>(c.size());
		for ( E e : c )
			o.add(e);
		return o;
	}
	
}
