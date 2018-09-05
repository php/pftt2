package com.github.mattficken.io.ui;

import java.nio.charset.Charset;
import javax.swing.table.AbstractTableModel;

import com.github.mattficken.io.DebuggerCharsetDeciderDecoder;
import com.github.mattficken.io.ERecognizedLanguage;


public class CharacterTableModel extends AbstractTableModel {
	public static final String COL_NAME_IN_BYTES = "In Bytes";
	public static final String COL_NAME_CHAR = "Char";
	public static final String COL_NAME_DEC_CHAR = "";
	public static final String COL_NAME_OUT_BYTES = "Out Bytes";
	public static final String COL_NAME_CHARSET = "Charset";
	protected final DebuggerCharsetDeciderDecoder ca;
	
	public CharacterTableModel(DebuggerCharsetDeciderDecoder ca) {
		this.ca = ca;
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		return col == 4;
	}
	
	public boolean isMismatchedRow(int row) {
		// XXX used by CharsetTableRenderer - called for each cell on each row - should be able to do 1 comparison per row only (PERF)
		return !getValueAt(row, 0).equals(getValueAt(row, 3));
	}
	
	public byte[] getDetectionWindow(int row) {
		return ca.getSelectedInfoForChar(row).detection_window_bytes;
	}
	
	public byte[] getInBytes(int row) {
		return ca.getSelectedInfoForChar(row).in_bytes;
	}

	public byte[] getOutBytes(int row) {
		return ca.getSelectedInfoForChar(row).out_bytes;
	}

	public char getChar(int row) {
		return ca.getSelectedInfoForChar(row).c;
	}
	
	public Charset getCharset(int row) {
		return ca.getSelectedInfoForChar(row).cs;
	}
	
	public ERecognizedLanguage getLanguage(int row) {
		return ca.getSelectedInfoForChar(row).lang;
	}

	@Override
	public int getRowCount() {
		return ca.getCharCount();
	}

	@Override
	public int getColumnCount() {
		return 5;
	}

	@Override
	public Object getValueAt(int row, int col) {
		switch(col) {
		case 0:
			return getInBytes(row);
		case 1:
			return getChar(row);
		case 2:
			return (int) getChar(row);
		case 3:
			return getOutBytes(row);
		case 4:
			return getCharset(row);
		}
		return null;
	}
	
	@Override
	public Class<?> getColumnClass(int col) {
		switch(col) {
		case 0:
		case 3:
			return String.class;
		case 1:
			return Character.class;
		case 2:
			return Integer.class;
		case 4:
			return Charset.class;
		}
		return null;
	}
	
	@Override
	public String getColumnName(int col) {
		switch(col) {
		case 0:
			return COL_NAME_IN_BYTES;
		case 1:
			return COL_NAME_CHAR;
		case 2:
			return COL_NAME_DEC_CHAR;
		case 3:
			return COL_NAME_OUT_BYTES;
		case 4:
			return COL_NAME_CHARSET;
		}
		return null;
	}
	
}
