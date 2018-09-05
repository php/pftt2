package com.github.mattficken.io.ui;

import javax.swing.JTextField;

public class CharValueField extends JTextField {

	public CharValueField() {
		
	}
	public CharValueField(char c) {
		
	}
	public void setChar(char c) {
		super.setText(Integer.toString((int)c));
	}
	// NOTE: verify entry point - correct location to add new code ? (ex: add #getFont call to #setChar, then realized it needed to be in #setText ?)
	@Override
	public void setText(String text) {
		FontManager.ensureFont(this, text, 20);
		
		super.setText(text);
	}
}
