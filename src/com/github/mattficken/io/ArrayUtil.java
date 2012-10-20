package com.github.mattficken.io;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayUtil {
	
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
