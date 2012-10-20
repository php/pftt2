package com.github.mattficken.io.ui;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

import com.github.mattficken.io.CharsetRec;


public class CharsetEditingCharsetTableEditor extends DefaultCellEditor {

	public CharsetEditingCharsetTableEditor() {
		super(new JComboBox(CharsetRec.RECOGNIZABLE_CHARSETS));
	}

}
