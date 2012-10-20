package com.github.mattficken.io.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.github.mattficken.io.AbstractDetectingCharsetReader;
import com.github.mattficken.io.ByCharReader;
import com.github.mattficken.io.ByChunkReader;
import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharacterReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.CharsetRec;
import com.github.mattficken.io.DebuggerCharsetDeciderDecoder;
import com.github.mattficken.io.ECharsetDetectionStyle;
import com.github.mattficken.io.EReadStyle;
import com.github.mattficken.io.ERecognizedLanguage;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.RestartableInputStream;
import com.github.mattficken.io.CharsetDeciderDecoder.ERecognizerGroup;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.StringUtil;

import se.datadosen.component.RiverLayout;

// TODO - character table
// TODO - have abstractdetectingcharsetreader use assigned charsetrecognizers
// TODO - alternate charset table
// TODO - override detected charset
// TODO - step through mismatches
// TODO - display detection window

public class CharsetDebuggerPanel extends JPanel {
	protected JTable char_byte_table, alternate_table;
	protected JButton prev_charset_button, next_charset_button, prev_mismatch_button, next_mismatch_button;
	protected BitEditField in_byte_field, out_byte_field;
	protected CharValueField char_field;
	protected JTextField detection_window_field;
	protected JTextArea char_sequence_area;
	protected JLabel detected_cs_label, detection_style_description_label;
	protected JComboBox test_cs_combo, detected_lang_combo, detection_style_combo, read_style_combo, chunk_size_combo, recognizer_group_combo;
	protected CharacterTableModel table;
	protected DebuggerCharsetDeciderDecoder decoder;
	protected InputStream in;
	protected RestartableInputStream rin;
	
	public CharsetDebuggerPanel() {
		super(new BorderLayout());
		this.table = new CharacterTableModel(decoder = new DebuggerCharsetDeciderDecoder(CharsetDeciderDecoder.EXPRESS_RECOGNIZERS)); 
		
		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		add(jsp);
		
		TextAreaTableCoordinator coord = new TextAreaTableCoordinator();
			
		char_byte_table = new JTable(table);
		char_byte_table.setRowHeight(70);
		CharacterTableRenderer renderer = new CharacterTableRenderer();
		char_byte_table.getColumn(CharacterTableModel.COL_NAME_IN_BYTES).setCellRenderer(renderer);
		char_byte_table.getColumn(CharacterTableModel.COL_NAME_CHAR).setCellRenderer(renderer);
		char_byte_table.getColumn(CharacterTableModel.COL_NAME_CHAR).setCellEditor(new DefaultCellEditor(new CharValueField()));
		char_byte_table.getColumn(CharacterTableModel.COL_NAME_DEC_CHAR).setCellRenderer(renderer);
		char_byte_table.getColumn(CharacterTableModel.COL_NAME_OUT_BYTES).setCellRenderer(renderer);		
		char_byte_table.getColumn(CharacterTableModel.COL_NAME_CHARSET).setCellRenderer(renderer);
		char_byte_table.getColumn(CharacterTableModel.COL_NAME_CHARSET).setCellEditor(new CharsetEditingCharsetTableEditor());
		char_byte_table.getSelectionModel().addListSelectionListener(coord);
		jsp.setLeftComponent(new JScrollPane(char_byte_table));
		
		JPanel right_panel = new JPanel(new RiverLayout());
		jsp.setRightComponent(right_panel);
		
		JPanel form_panel = new JPanel(new RiverLayout());
		
		alternate_table = new JTable(new AlternateCharChoiceTableModel());
		renderer = new CharacterTableRenderer();
		alternate_table.getColumn(AlternateCharChoiceTableModel.COL_NAME_BYTES).setCellRenderer(renderer);
		alternate_table.getColumn(AlternateCharChoiceTableModel.COL_NAME_CHAR).setCellRenderer(renderer);
		alternate_table.getColumn(AlternateCharChoiceTableModel.COL_NAME_DEC_CHAR).setCellRenderer(renderer);
		alternate_table.getColumn(AlternateCharChoiceTableModel.COL_NAME_CONFIDENCE).setCellRenderer(renderer);		
		alternate_table.getColumn(AlternateCharChoiceTableModel.COL_NAME_CHARSET).setCellRenderer(renderer);
		
				
		JPanel top_panel = new JPanel(new BorderLayout());
		top_panel.add(form_panel, BorderLayout.CENTER);
		top_panel.add(new JScrollPane(alternate_table), BorderLayout.LINE_END);
		
		alternate_table.setPreferredScrollableViewportSize(alternate_table.getPreferredSize());
				
		form_panel.add("p left", new JLabel("In Bytes:"));
		form_panel.add("tab hfill", in_byte_field = new BitEditField());
		in_byte_field.setFont(new Font("Times", Font.PLAIN, 18));
		form_panel.add("p left", new JLabel("Char:"));
		form_panel.add("tab hfill", char_field = new CharValueField());
		char_field.setFont(new Font("Times", Font.PLAIN, 18));
		form_panel.add("p left", new JLabel("Out Bytes:"));
		form_panel.add("tab hfill", out_byte_field = new BitEditField());
		out_byte_field.setFont(new Font("Times", Font.PLAIN, 18));
		form_panel.add("p left", new JLabel("Detection Window:"));
		form_panel.add("tab hfill", detection_window_field = new JTextField());
		
		right_panel.add("p left hfill", top_panel);
				
		right_panel.add("p left", prev_charset_button = new JButton("Prev Charset"));
		right_panel.add("left", next_charset_button = new JButton("Next Charset"));
		right_panel.add("left", prev_mismatch_button = new JButton("Prev Mis-Match"));
		right_panel.add("left", next_mismatch_button = new JButton("Next Mis-Match"));
		right_panel.add("left", new JLabel("Detected CS:"));
		right_panel.add("left", detected_cs_label = new JLabel("<detected>"));
		right_panel.add("left", new JButton("Test CS:"));
		right_panel.add("left", test_cs_combo = new JComboBox(CharsetRec.RECOGNIZABLE_CHARSETS));
		right_panel.add("left", new JLabel("Detected Lang:"));
		right_panel.add("left", detected_lang_combo = new JComboBox(ERecognizedLanguage.values()));
		
		TextAreaUpdater update = new TextAreaUpdater();
		
		right_panel.add("p left", new JLabel("Detection Style:"));
		right_panel.add("left", detection_style_combo = new JComboBox(ECharsetDetectionStyle.values()));
		detection_style_combo.addActionListener(update);
		right_panel.add("left", new JLabel("Read Style:"));
		right_panel.add("left", read_style_combo = new JComboBox(EReadStyle.values()));
		read_style_combo.addActionListener(update);
		right_panel.add("left", detection_style_description_label = new JLabel());
		
		right_panel.add("left", new JLabel("Recognizer Group:"));
		right_panel.add("left", recognizer_group_combo = new JComboBox(ERecognizerGroup.values()));
		recognizer_group_combo.addActionListener(update);
		right_panel.add("left", new JLabel("Chunk Size:"));
		right_panel.add("left", chunk_size_combo = new JComboBox(new Integer[]{1024, 2048, 4096, 8192, 16384, 32768, 65535, 512, 1536}));
		chunk_size_combo.addActionListener(update);
		
		right_panel.add("p left hfill vfill", new JScrollPane(char_sequence_area = new JTextArea()));
		char_sequence_area.setSelectedTextColor(Color.GREEN);
		char_sequence_area.setSelectionColor(Color.RED);
		char_sequence_area.addCaretListener(coord);
	}
	
	protected class TextAreaUpdater implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			ECharsetDetectionStyle cs = (ECharsetDetectionStyle) detection_style_combo.getSelectedItem();
			EReadStyle rs = (EReadStyle) read_style_combo.getSelectedItem();
			int chunk_size = (Integer) chunk_size_combo.getSelectedItem();
			ERecognizerGroup rg = (ERecognizerGroup) recognizer_group_combo.getSelectedItem();
			
			setCharsetDetectionAndReadStyle(cs, rs, rg, chunk_size);
		}
	}
	
	protected class TextAreaTableCoordinator implements CaretListener, ListSelectionListener {
		boolean selecting;
		@Override
		public void caretUpdate(CaretEvent e) {
			if(selecting)
				return;
			selecting = true;
			int dot = e.getDot();
			
			char_sequence_area.select(dot, dot+1);
			
			// select char in table
			char_byte_table.getSelectionModel().setSelectionInterval(dot, dot);
			// scroll to char
			char_byte_table.scrollRectToVisible(new Rectangle(char_byte_table.getCellRect(dot, 0, true)));
			
			showSelectedRow(dot);
			
			selecting = false;
		}
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if(selecting)
				return;
			selecting = true;
			
			int row = char_byte_table.getSelectedRow();
			
			showSelectedRow(row);
			char_sequence_area.requestFocus(true);
			char_sequence_area.select(row, row+1);
			
			selecting = false;
		}
		
		protected void showSelectedRow(int row) {
			if (row >= table.ca.getCharCount())
				return;
			
			detection_window_field.setText(StringUtil.toString(table.getDetectionWindow(row))); // XXX
			in_byte_field.setBytes(table.getInBytes(row));
			out_byte_field.setBytes(table.getOutBytes(row));
			char_field.setChar(table.getChar(row));
			Charset cs = table.getCharset(row);
			detected_cs_label.setText(StringUtil.toString(cs));
			test_cs_combo.setSelectedItem(cs);
			detected_lang_combo.setSelectedItem(table.getLanguage(row));			
			
			alternate_table.setModel(new AlternateCharChoiceTableModel(table.ca.getAllInfoForChar(row)));
		}			
		
	} // end protected class TextAreaTableCoordinator

	public void setInputStream(RestartableInputStream rin) {
		this.rin = rin;
				
		ECharsetDetectionStyle style = (ECharsetDetectionStyle) detection_style_combo.getSelectedItem();
		EReadStyle rs = (EReadStyle) read_style_combo.getSelectedItem();
		int chunk_size = (Integer) chunk_size_combo.getSelectedItem();
		ERecognizerGroup rg = (ERecognizerGroup) recognizer_group_combo.getSelectedItem();
		
		applyCharsetDetectionAndReadStyle(style, rs, rg, chunk_size);
	}
	
	public void setCharsetDetectionAndReadStyle(final ECharsetDetectionStyle cs, final EReadStyle rs, final ERecognizerGroup rg, final int chunk_size) {
		synchronized(this.in) {
			new Thread() {
				public void run() {
					applyCharsetDetectionAndReadStyle(cs, rs, rg, chunk_size);			
				}
			}.start();
		};
	}
	
	protected void applyCharsetDetectionAndReadStyle(ECharsetDetectionStyle cs, EReadStyle rs, ERecognizerGroup rg, int chunk_size) {
		try {
			in = IOUtil.ensureMarkSupported(rin.openInputStream());
		} catch ( Exception ex ) {
			ErrorUtil.display_error(this, ex);
			return;
		}
		
		detection_style_description_label.setText(cs.getStyleDescription(rs));
		
		table.ca.clear();
		
		final CharacterReader reader = cs.newCharReader(rs, this.in, new DebuggerCharsetDeciderDecoder(rg.getRecognizers()));
		
		if (reader instanceof AbstractDetectingCharsetReader)
			((AbstractDetectingCharsetReader)reader).recogs = rg.getRecognizers(); // TODO
		
		StringBuilder sb = new StringBuilder(16384);
		
		try {
			//
			if (reader instanceof ByLineReader) {
				ByLineReader lr = (ByLineReader) reader;
				String line;
				
				while (true) {
					line = lr.readLine();
					if (line!=null&&lr.hasMoreLines()) {
						sb.append(line);
						sb.append('\n');
					} else {
						break;
					}
				}
				
			} else if (reader instanceof ByCharReader) {
				ByCharReader cr = (ByCharReader) reader;
				char c;
				
				do {
					c = cr.readChar();
					if (c!=ByCharReader.END_OF_STREAM_CHAR)
						sb.append(c);
				} while ( cr.hasMoreChars() );
				
			} else if (reader instanceof ByChunkReader) {
				ByChunkReader cr = (ByChunkReader) reader;
				char[] chunk = new char[chunk_size];
				int len;
				
				do {
					len = cr.readChunk(chunk, 0, chunk.length);
					if (len>0)
						sb.append(chunk, 0, len);
					else
						break;
				} while ( cr.hasMoreChunks() );
				
			} else {
				ErrorUtil.display_error(this, "Unsupported CharacterReader subclass!");
			}
			//
		} catch ( Exception ex ) {
			ErrorUtil.display_error(this, ex);
		}
		
		try {
			reader.close();
		} catch ( Exception ex ) {
			ErrorUtil.display_error(this, ex);
		}
		
		String str = sb.toString();
		
		FontManager.ensureFont(char_sequence_area, str, 20);		
		char_sequence_area.setText(str);
	}
	
} // end public class CharsetDebuggerPanel
