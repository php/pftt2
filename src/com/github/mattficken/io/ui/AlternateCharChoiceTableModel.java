package com.github.mattficken.io.ui;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.github.mattficken.io.ArrayUtil;
import com.github.mattficken.io.DebuggerCharsetDeciderDecoder.AllInfo;

public class AlternateCharChoiceTableModel extends AbstractTableModel {
	public static final String COL_NAME_CHARSET = "Charset";
	public static final String COL_NAME_BYTES = "Bytes";
	public static final String COL_NAME_CHAR = "Char";
	public static final String COL_NAME_DEC_CHAR = "";
	public static final String COL_NAME_CONFIDENCE = "Confidence";	
	final List<AllInfo> infos;	
	static Comparator<AllInfo> CONFIDENCE_COMPARATOR = new Comparator<AllInfo>() {

		@Override
		public int compare(AllInfo o1, AllInfo o2) {
			return o2.confidence - o1.confidence;
		}
		
	};
	
	public AlternateCharChoiceTableModel() {
		this.infos = new ArrayList<AllInfo>(0);
	}
	
	public AlternateCharChoiceTableModel(Collection<AllInfo> infos) {
		this.infos = ArrayUtil.toList(infos);
		
		Collections.sort(this.infos, CONFIDENCE_COMPARATOR);
	}

	@Override
	public int getRowCount() {
		return infos.size();
	}

	@Override
	public int getColumnCount() {
		return 5;
	}
	
	@Override
	public Class<?> getColumnClass(int col) {
		switch(col) {
		case 0:
			return Charset.class;
		case 1:
			return String.class;
		case 2:
			return Character.class;
		case 3:
		case 4:
			return Integer.class;
		}
		return null;
	}
	
	@Override
	public String getColumnName(int col) {
		switch(col) {
		case 0:
			return COL_NAME_CHARSET;
		case 1:
			return COL_NAME_BYTES;
		case 2:
			return COL_NAME_CHAR;
		case 3:
			return COL_NAME_DEC_CHAR;
		case 4:
			return COL_NAME_CONFIDENCE;
		}
		return null;
	}

	@Override
	public Object getValueAt(int row, int col) {
		switch(col) {
		case 0:
			return infos.get(row).cs;
		case 1:
			return infos.get(row).in_bytes;
		case 2:
			return infos.get(row).c;
		case 3:
			return (int) infos.get(row).c;
		case 4:
			return infos.get(row).confidence;
		}
		return null;
	}

}
