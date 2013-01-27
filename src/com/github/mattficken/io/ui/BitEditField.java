package com.github.mattficken.io.ui;

import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import com.github.mattficken.io.StringUtil;

public class BitEditField extends JTextField implements DocumentListener {

	public BitEditField(byte[] bytes) {
		this();
		setBytes(bytes);
	}
	
	public void setBytes(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for ( int i=0 ; i < bytes.length; i++ ) {
			if (i>0)
				sb.append(' ');
			sb.append(StringUtil.padFirst(Integer.toString(bytes[i] & 0xFF, 2), 8, '0'));
		}
		setText(sb.toString());
	}
			
	final Color entryBg;
	final Highlighter hilit;
	final Highlighter.HighlightPainter painter;

	public BitEditField() {
		hilit = new DefaultHighlighter();
		painter = new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY);
		super.setHighlighter(hilit);
		entryBg = super.getBackground();
		super.getDocument().addDocumentListener(this);
	}

	protected void highlightOnes() {
		hilit.removeAllHighlights();
		String content = super.getText();
		int index = -1;
		while (true) {
			index = content.indexOf('1', index+1);
			if (index == -1)
				break;
			
			try {
				int end = index + 1;
				hilit.addHighlight(index, end, painter);
				super.setCaretPosition(end);
				super.setBackground(getBackground());
			} catch (BadLocationException e) {
				break;
			} 
			
		} // end while
	}

	@Override
	public void insertUpdate(DocumentEvent ev) {
		highlightOnes();
	}
	@Override
	public void removeUpdate(DocumentEvent ev) {
		highlightOnes();
	}
	@Override
	public void changedUpdate(DocumentEvent ev) {
	}

} // end public class BitEditField
