package com.mostc.pftt.util;

import java.util.ArrayList;

import com.github.mattficken.io.StringUtil;

public class AlignedTable {
	protected final int max_width;
	protected final ArrayList<String[]> table;
	protected final int[] column_widths;
	
	public AlignedTable(int columns, int max_width) {
		this.max_width = max_width;
		this.table = new ArrayList<String[]>(10);
		
		column_widths = new int[columns];
	}
	
	public AlignedTable addRow(String ...cells) {
		if (cells.length!=column_widths.length)
			return null;
		for ( int i=0 ; i < cells.length ; i++ ) {
			if (cells[i].length()>column_widths[i])
				column_widths[i] = cells[i].length();
		}
		
		table.add(cells);
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(200);
		String cell;
		int i, j, k, line_len, chunk_size, left_dist;
		for ( String[] cells : table ) {
			line_len = 0;
			for ( i=0 ; i < cells.length ; i++ ) {
				cell = cells[i];
				cell = cell.replace("\n", "").replace("\r", "").replace("\t", "");
				if (i>0) {
					sb.append(' ');
					line_len += 1;
				}
				if (i+1<cells.length && cell.length() < column_widths[i]) {
					cell = StringUtil.padLast(cell, column_widths[i], ' ');
				}
				chunk_size = max_width-line_len;
				if (chunk_size>0&&chunk_size<cell.length()){
					chunk_size = max_width-line_len;
					left_dist = line_len;
					sb.append(cell.substring(0, chunk_size));
					if (cell.length()>chunk_size) {
						for ( j=chunk_size ; j < cell.length() ; j+=chunk_size) {
							sb.append('\n');
							for ( k=0 ; k < left_dist ; k++ )
								sb.append(' ');
							if (j+chunk_size<cell.length())
								sb.append(cell.substring(j, j+chunk_size));
							else
								sb.append(cell.substring(j));
						}
					}
					
					// add extra blank line to separate multi-line rows
					sb.append("\n");
				} else {
					line_len += cell.length();
					sb.append(cell);
				}
			} // end for
			if (i>0) {
				sb.append("\n");
			}
		} // end for
		
		return sb.toString();
	}
}
