package com.github.mattficken.io.ui;

import java.awt.Font;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

/** Manages fonts, primarily internationalized fonts to make sure that fonts exist to support
 * displaying chars from charsets the debugger supports.
 * 
 * @author matt
 *
 */

public final class FontManager {
	
	private static ArrayList<Font> fonts;
	static {
		fonts = new ArrayList<Font>(7);
		
		registerStaticFontFile("DroidSansArabic.ttf");
		registerStaticFontFile("DroidSansHebrew.ttf");
		registerStaticFontFile("DroidSansJapanese.ttf");
		// include this font after japanese font - will mess w/ Hiranga chars (possibly others)
		registerStaticFontFile("BabelStoneHan.ttf");
		registerStaticFontFile("DroidSansThai.ttf");
		registerStaticFontFile("DroidSerif-Regular.ttf");
		// last font is the fallback that will be used if nothing else matches!
		registerStaticFontFile("DroidSansFallback.ttf");
	}
	
	private static void registerStaticFontFile(String name) {
		InputStream in = FontManager.class.getResourceAsStream(name);
		registerFont(in);
	}
		
	public static void ensureFont(JTextComponent comp, char c) {
		ensureFont(comp, c, 0); // any size
	}
	public static void ensureFont(JTextComponent comp, char c, int size) {
		Font font = comp.getFont();
		if (font.canDisplay(c)) {
			if ( font.getSize() < size ) {
				comp.setFont(ensureSize(font, size));
			}
		} else {
			comp.setFont(getFont(c, size));
		}
	}
	public static void ensureFont(JTextComponent comp, String text) {
		ensureFont(comp, text, 0); // any size
	}
	public static void ensureFont(JTextComponent comp, String text, int size) {
		Font font = comp.getFont();
		if (font.canDisplayUpTo(text) == -1) {
			if ( font.getSize() < size ) {
				comp.setFont(ensureSize(font, size));
			}
		} else {
			comp.setFont(getFont(text, size));
		}
	}
	public static void ensureFont(JLabel comp, String text) {
		ensureFont(comp, text, 0); // any size
	}
	public static void ensureFont(JLabel comp, String text, int size) {
		Font font = comp.getFont();
		if (font.canDisplayUpTo(text) == -1) {
			if ( font.getSize() < size ) {
				comp.setFont(ensureSize(font, size));
			}
		} else {
			comp.setFont(getFont(text, size));
		}
	}

	public static Font ensureSize(Font font, int size) {
		if (font.getSize() == size)
			return font;
		return new Font(font.getName(), font.getStyle(), size);
	}
	public static Font getFont(String text, Charset cs, int size) {
		return ensureSize(getFont(text, cs), size);
	}
	public static Font getFont(String text, int size) {
		return getFont(text, null, size);
	}
	public static Font getFont(String text, Charset cs) {
		for(Font font:fonts) {
			if (font.canDisplayUpTo(text)==-1)
				return font;
		}
		return fonts.size() > 0 ? fonts.get(fonts.size()-1) : null;
	}
	public static Font getFont(String text) {
		return getFont(text, null);
	}
	public static Font getFont(char text, Charset cs, int size) {
		return ensureSize(getFont(text, cs), size);
	}
	public static Font getFont(char text, int size) {
		return getFont(text, null, size);
	}
	public static Font getFont(char text, Charset cs) {
		for(Font font:fonts) {
			if (font.canDisplay((int)text))
				return font;
		}
		return fonts.get(fonts.size()-1);
	}
	public static Font getFont(char text) {
		return getFont(text, null);
	}
	public static void registerFont(InputStream in) {
		registerFont(in, null);
	}
	public static void registerFont(InputStream in, Charset cs) {
		try {
			Font font = ensureSize(Font.createFont(Font.TRUETYPE_FONT, in), 16);
			fonts.add(font);
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}	
	}
	
	private FontManager() {}
} // end public class FontManager
