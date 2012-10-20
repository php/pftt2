package com.github.mattficken.io;

import java.util.Iterator;
import java.util.List;

public class ListByLineReader extends IteratorByLineReader implements Iterable<String> {
	protected final List<String> list;
	
	public ListByLineReader(List<String> list) {
		super(list);
		this.list = list;
	}

	@Override
	public Iterator<String> iterator() {
		return list.iterator();
	}

}
