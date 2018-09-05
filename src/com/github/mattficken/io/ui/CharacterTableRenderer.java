package com.github.mattficken.io.ui;

import java.awt.Color;
import java.awt.Component;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

import com.github.mattficken.io.CharsetRec;


public class CharacterTableRenderer extends DefaultTableCellRenderer {
	static final Color MISMATCH_COLOR = Color.RED;
	static final Color MATCH_COLOR = new Color(40, 220, 150);
	static final Color[] CHARSET_COLORS = new Color[] {
			Color.GRAY, Color.GREEN,
			Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
			Color.PINK, Color.WHITE, Color.YELLOW,
			new Color(255, 127, 0),
			new Color(255, 200, 0),
			new Color(255, 224, 0),
			new Color(150, 255, 0),
			new Color(0, 255, 127),
			new Color(0, 255, 200),
			new Color(0, 200, 255),
			new Color(0, 64, 255),
			new Color(64, 0, 255),
			new Color(150, 0, 255),
			new Color(255, 0, 255),
			new Color(255, 0, 150),
			new Color(255, 0, 0),			
			new Color(255, 100, 0),
			new Color(255, 200, 50),
			new Color(255, 224, 50),
			new Color(150, 255, 50),
			new Color(50, 255, 127),
			new Color(50, 255, 200),
			new Color(50, 200, 255),
			new Color(50, 64, 255),
			new Color(64, 50, 255),
			new Color(150, 50, 255),
			new Color(255, 50, 255),
			new Color(255, 50, 150),
			new Color(255, 50, 50),
			new Color(255, 50, 0),			
			new Color(224, 127, 0),
			new Color(224, 200, 0),
			new Color(224, 224, 0),
			new Color(224, 255, 0),
			new Color(0, 224, 127),
			new Color(0, 224, 200),
			new Color(0, 200, 224),
			new Color(0, 64, 224),
			new Color(64, 0, 224),
			new Color(150, 0, 224),
			new Color(224, 0, 224),
			new Color(224, 0, 150),
			new Color(224, 0, 0),			
			new Color(224, 100, 0),
			new Color(224, 200, 50),
			new Color(224, 224, 50),
			new Color(150, 224, 50),
			new Color(50, 224, 127),
			new Color(50, 224, 200),
			new Color(50, 200, 224),
			new Color(50, 64, 224),
			new Color(64, 50, 224),
			new Color(150, 50, 224),
			new Color(224, 50, 224),
			new Color(224, 50, 150),
			new Color(224, 50, 50),
			new Color(224, 50, 0)
		};
	static final SecureRandom rand = new SecureRandom(); 
	
	//
	static HashMap<Charset,Color> color_map = new HashMap<Charset,Color>();
	public static Color getColorForCharset(Charset cs) {
		synchronized(color_map) {
			Color color = color_map.get(cs);
			if (color!=null)
				return color;
			
			// choose color
			for ( Color new_color : CHARSET_COLORS) {
				if (!color_map.containsValue(new_color)) {
					color_map.put(cs, new_color);
					return new_color;
				}
			}
			
			color = CHARSET_COLORS[rand.nextInt(CHARSET_COLORS.length)];
			color_map.put(cs, color);
			return color;
		}
	}
	static {
		// always load these charsets in same order to ensure color is always the same
		for ( int i=0 ; i < CharsetRec.RECOGNIZABLE_CHARSETS.length ; i++ )
			getColorForCharset(CharsetRec.RECOGNIZABLE_CHARSETS[i]);
	}
	
	private EmptyBorder border = new EmptyBorder(30, 15, 30, 15);
	
	protected static String toIntString(byte[] bytes, int off, int len) {
		StringBuilder sb = new StringBuilder(len<<2);
		int b;
		for ( int i=0 ; i < len ; i++, off++ ) {
			if (i>0)
				sb.append(' ');
			b = bytes[off];
//			if (b>=0) {
//				if (b<100)
//					sb.append('0');
//				if (b<10)
//					sb.append('0');
//			} else if ( b > -10 ) {
//				sb.append('0');
//			}
			sb.append(b & 0xFF);
		}
		return sb.toString();
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
		if (value instanceof byte[]) {
			byte[] value_bytes = (byte[]) value;
			value = toIntString(value_bytes, 0, value_bytes.length);
		}
			
			
		JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
		
		if (selected) {
			// leave it the default selected color
		} else if (value instanceof Charset) {
			c.setBackground(getColorForCharset((Charset)value));
		} else if (( (CharacterTableModel) table.getModel() ).isMismatchedRow(row)) {
			c.setBackground(MISMATCH_COLOR);
		} else {
			c.setBackground(MATCH_COLOR);
		}
		System.out.println("getTableCellRendererComponent "+value);
		FontManager.ensureFont(c, value.toString(), 20);
		c.setVerticalTextPosition(SwingConstants.CENTER);
		c.setHorizontalTextPosition(column == 1 ? SwingConstants.CENTER : SwingConstants.LEFT);
		c.setBorder(border);
				
		return c;
	}
	
}
