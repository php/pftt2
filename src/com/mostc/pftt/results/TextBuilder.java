package com.mostc.pftt.results;

import groovy.util.BuilderSupport;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Map;

import com.github.mattficken.io.StringUtil;

/** Drop-in replacement for MarkupBuilder that generates plain text instead
 * 
 * @author Matt Ficken
 *
 */

public class TextBuilder extends BuilderSupport {
	protected final StringWriter sw;
	protected final LinkedList<Table> tables;
	protected Table cur_table;
	protected Tr cur_tr;
	protected TdTable cur_td;
	
	public TextBuilder(StringWriter sw) {
		this.sw = sw;
		
		tables = new LinkedList<Table>();
	}
	
	protected abstract class TdTable {

		public abstract void append(StringWriter sw, int max_len);
		public abstract int getLength();
		
	}
	
	protected class Table extends TdTable {
		protected final LinkedList<Tr> trs;
		protected final boolean toplevel;
		
		protected Table(boolean toplevel) {
			this.toplevel = toplevel;
			trs = new LinkedList<Tr>();
		}

		public void add(Tr tr) {
			trs.add(tr);
		}
		
		@Override
		public int getLength() {
			int len = 0;
			for ( int max_len : calcMaxLengths() ) {
				len += max_len;
			}
			return len;
		}
		
		protected LinkedList<Integer> calcMaxLengths() {
			LinkedList<Integer> max_lengths = new LinkedList<Integer>();
			int col = 0, len, max_len;
			for ( Tr tr : trs ) {
				col = 0; len = 0; max_len = 0;
				for ( TdTable td : tr.tds ) {
					len = td.getLength();
					if (col < max_lengths.size()) {
						max_len = max_lengths.get(col);
						if (len>max_len) {
							max_len = len;
							max_lengths.set(col, max_len);
						}
					} else {
						max_len = len;
						//max_lengths.set(col, max_len);
						max_lengths.add(max_len);
					}
					col++;
				}
			}
			return max_lengths;
		}

		@Override
		public void append(StringWriter sw, int max_len) {
			LinkedList<Integer> max_lengths = calcMaxLengths();
			for ( Tr tr : trs ) {
				tr.append(sw, max_lengths);
				if (toplevel)
					sw.append("\r\n");
			}
		}
		
	}
	
	protected class Tr {
		protected final LinkedList<TdTable> tds;
		
		protected Tr() {
			tds = new LinkedList<TdTable>();
		}

		public void append(StringWriter sw, LinkedList<Integer> max_lengths) {
			int col = 0;
			for ( TdTable td : tds ) {
				sw.append("| ");
				td.append(sw, max_lengths.get(col));
				sw.append(" ");
				col++;
			}
			sw.append("\r\n");
			col = 0;
			int max_len, i;
			for ( TdTable td : tds ) {
				sw.append("+-");
				max_len = max_lengths.get(col);
				for ( i=0 ; i < max_len ; i++ )
					sw.append("-");
				sw.append("-");
				col++;
			}
		}

		public void add(TdTable td) {
			tds.add(td);
		}
		
	}
	
	protected class Td extends TdTable {
		protected String str;
		protected final int colspan;
		
		protected Td(int colspan, String str) {
			this.colspan = colspan;
			this.str = str;
		}

		@Override
		public void append(StringWriter sw, int max_len) {
			// TODO indent if there are newline characters
			sw.append(StringUtil.padLast(str, max_len, ' '));
		}
		
		@Override
		public int getLength() {
			int i = str.indexOf('\n');
			if (i==-1)
				i = str.length();
			return i / colspan;
		}
		
	}
	
	@Override
	protected void nodeCompleted(Object parent, Object child) {
		final String name_str = child.toString();
		if (name_str.equals("td")) {
			cur_tr.add(cur_td);
			cur_td = null;
		} else if (name_str.equals("tr")) {
			cur_table.add(cur_tr);
			cur_tr = null;
		} else if (name_str.equals("table")) {
			if (tables.size()<2)
				// important: append child table's contexts to parent
				cur_table.append(sw, 0); // 0 => not used
			if (tables.isEmpty()) {
				sw.append("\r\n");
				cur_table = null;
			} else {
				cur_table = tables.removeLast();
			}
		} else if (ignoreName(name_str)) {
			// ignore
		}
	}
	
	protected boolean ignoreName(String name_str) {
		return name_str.equals("script")||name_str.equals("head")||name_str.equals("title")||name_str.equals("body")||name_str.equals("html")||name_str.equals("meta");
	}
	
	@Override
	protected Object createNode(Object name) {
		return createNode(name, null);
	}

	@Override
	protected Object createNode(Object name, Object value) {
		return createNode(name, null, value);
	}

	@Override
	protected Object createNode(Object name, Map attributes) {
		return createNode(name, attributes, null);
	}
	
	protected void append(String value) {
		if (cur_td==null||!(cur_td instanceof Td))
			sw.append(value);
		else
			((Td)cur_td).str += value;
	}

	@Override
	protected Object createNode(Object name, Map attributes, Object value) {
		final String name_str = name.toString();
		if (name_str.equals("p")||name_str.equals("br")) {
			if (value==null)
				append("\r\n"); // <br> shouldn't have a value
			else
				append("\r\n"+StringUtil.toString(value));
		} else if (name_str.equals("td")) {
			final int colspan = attributes!=null && attributes.containsKey("colspan") ? Integer.parseInt(attributes.get("colspan").toString()) : 1;
			
			cur_td = new Td(colspan, value==null?"":StringUtil.toString(value));
		} else if (name_str.equals("tr")) {
			cur_tr = new Tr();
		} else if (name_str.equals("table")) {
			if (tables.isEmpty()) {
				tables.add(cur_table = new Table(true));
			} else {
				cur_td = cur_table = new Table(false);
				tables.add(cur_table);
			}
		} else if (name_str.equals("a")) {
			if (attributes!=null && attributes.containsKey("href"))
				append(attributes.get("href").toString());
			else
				append(StringUtil.toString(value));
		} else if (name_str.equals("img")) {
			if (attributes!=null && attributes.containsKey("alt"))
				append(attributes.get("alt").toString());
			else
				append(StringUtil.toString(value));
		} else if (ignoreName(name_str)) {
			// ignore
		} else if (value!=null) {
			append(StringUtil.toString(value));
		}
		return getName(name_str);
	}

	@Override
	protected void setParent(Object arg0, Object arg1) {
	}
	
} // end public class TextBuilder
