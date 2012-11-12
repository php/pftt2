package com.mostc.pftt.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.table.DefaultTableModel;

import com.github.mattficken.io.RestartableInputStream;
import com.github.mattficken.io.ui.CharsetDebuggerPanel;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.telemetry.PhptTestResult;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.StringUtil;

import se.datadosen.component.RiverLayout;

@SuppressWarnings("serial")
public class ExpectedActualDiffPHPTDisplay extends JScrollPane {
	protected JScrollPane horizontal_jsp, vertical_jsp;
	protected JPanel horizontal_panel, horizontal_button_panel, vertical_panel;
	protected TextDisplayPanel expected_display, diff_display, actual_display, test_display;
	protected DefaultTableModel env_table_model;
	protected JTable env_table;
	protected JTextArea stdin_data_textarea, shell_script_textarea, expectf_textarea, pre_override_textarea, sapi_output_textarea;
	protected PhptTestResult test;
			
	public ExpectedActualDiffPHPTDisplay() {
		horizontal_jsp = this;
					
		horizontal_panel = new JPanel(new BorderLayout());
		horizontal_jsp.getViewport().setView(horizontal_panel);
		horizontal_button_panel  = new JPanel(new GridLayout(1, 7, 2, 2));		
		vertical_panel = new JPanel(new GridLayout(1, 7, 2, 2));
			
		horizontal_panel.add(vertical_jsp = new JScrollPane(vertical_panel), BorderLayout.CENTER);
		vertical_jsp = this;
		vertical_jsp.getViewport().setView(vertical_panel);
		horizontal_panel.add(horizontal_button_panel, BorderLayout.PAGE_END);
		
		horizontal_jsp.getHorizontalScrollBar().setUnitIncrement(20);
		vertical_jsp.getVerticalScrollBar().setUnitIncrement(20);
		
		actual_display = new TextDisplayPanel();
		actual_display.text_area.setToolTipText("Actual test output");
		expected_display = new TextDisplayPanel();
		expected_display.text_area.setToolTipText("Expected test output");
		diff_display = new TextDisplayPanel();
		diff_display.text_area.setToolTipText("Diff");
		test_display = new TextDisplayPanel();
		expectf_textarea = new JTextArea();
		expectf_textarea.setToolTipText("EXPECTF section after regular expression patterns are added");
		pre_override_textarea = new JTextArea();
		pre_override_textarea.setToolTipText("Actual test output before any OS specific overrides applied");
		sapi_output_textarea = new JTextArea();
		sapi_output_textarea.setToolTipText("Output from SAPI - did web server crash? etc...");
		
		horizontal_button_panel.add(actual_display.button_panel);
		horizontal_button_panel.add(expected_display.button_panel);
		horizontal_button_panel.add(diff_display.button_panel);
		horizontal_button_panel.add(new JPanel()); // placeholder
		horizontal_button_panel.add(test_display.button_panel);
		horizontal_button_panel.add(new JPanel()); // placeholder
		horizontal_button_panel.add(new JPanel()); // placeholder
		
		vertical_panel.add(actual_display.text_area);
		vertical_panel.add(expected_display.text_area);
		vertical_panel.add(diff_display.text_area);
		vertical_panel.add(expectf_textarea);
		vertical_panel.add(pre_override_textarea);
		
		
		JPanel prepared_panel = new JPanel(new GridLayout(5, 1, 2, 2));
		prepared_panel.add(new JScrollPane(sapi_output_textarea));
		prepared_panel.add(new JScrollPane(stdin_data_textarea = new JTextArea()));
		stdin_data_textarea.setLineWrap(true);
		prepared_panel.add(new JScrollPane(shell_script_textarea = new JTextArea()));
		shell_script_textarea.setLineWrap(true);
		prepared_panel.add(new JScrollPane(env_table = new JTable(env_table_model = new DefaultTableModel())));
		env_table_model.addColumn("Name");
		env_table_model.addColumn("Value");
		
		vertical_panel.add(test_display.text_area);
	}
	
	public void showTest(PhptTestResult test) {
		if (test==null)
			return;
		
		this.test = test;
		
		while (env_table_model.getRowCount() > 0) {
			env_table_model.removeRow(0);
		}
		
		if (test.env!=null) {
			for (String name : test.env.keySet()) {
				env_table_model.addRow(new Object[]{name, test.env.get(name)});
			} 
		}
		if (test.stdin_data!=null)
			stdin_data_textarea.setText(new String(test.stdin_data));
		else
			stdin_data_textarea.setText("");
		shell_script_textarea.setText(test.shell_script);
		
		expected_display.text_area.setText(test.test_case.getCommonCharset()+"\n"+test.test_case.getExpected());
		
		actual_display.showFile(test.test_case, test.actual);
		try {
			test_display.showFile(test.test_case, test.test_case.getContents(test.host));
		} catch ( IOException ex ) {
			ErrorUtil.display_error(this, ex);
		}
		 
		diff_display.showFile(test.test_case, test.diff_str==null?"":test.diff_str);
		expectf_textarea.setText(test.expectf_output==null?"":test.expectf_output);
		pre_override_textarea.setText(test.preoverride_actual==null?"":test.preoverride_actual);
		//
		String sapi_output = test.getSAPIOutput();
		sapi_output_textarea.setText(sapi_output==null?"":sapi_output);
		//
		
		SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					getViewport().setViewPosition(new Point(0, 0)); // scroll to top (ensure top is visible)		
				}
			});		
	}
	
	protected class TextDisplayPanel implements CaretListener {
		protected JTextArea text_area;
		protected JTextField row_field, column_field;
		protected JLabel filename_field;
		protected JPanel button_panel;
		protected JButton charset_debugger_button, copy_button;
		
		public TextDisplayPanel() {
			button_panel = new JPanel(new RiverLayout(2, 2));
			
			text_area = new JTextArea();
			text_area.addCaretListener(this);
			text_area.setColumns(60);
			text_area.setLineWrap(true);
			// important: monospaced font
			text_area.setFont(new Font("Monospaced", Font.PLAIN, 20));
			
			button_panel.add("p left", row_field = new JTextField(3));
			row_field.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setPosition(Integer.parseInt(row_field.getText()), Integer.parseInt(column_field.getText())); 
					}
				});
			
			button_panel.add("tab", column_field = new JTextField(3));
			column_field.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setPosition(Integer.parseInt(row_field.getText()), Integer.parseInt(column_field.getText())); 
					}
				});
			
			button_panel.add("left", charset_debugger_button = new JButton("Charset Debugger"));
			charset_debugger_button.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						CharsetDebuggerPanel panel = new CharsetDebuggerPanel();
						JFrame jf = new JFrame("Charset Debugger - "+test.test_case.getName());
						jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
						jf.setContentPane(panel);
						jf.pack();
						jf.setExtendedState(JFrame.MAXIMIZED_BOTH);
						jf.setVisible(true);
	
						// load file
						panel.setInputStream(new RestartableInputStream() {
							@Override
							public InputStream openInputStream() throws FileNotFoundException {
								// TODO provide charset to bytearrayinputstream
								return new ByteArrayInputStream(text_area.getText().getBytes());
							}
						});
					}
				});
			
			button_panel.add("tab", copy_button = new JButton("Copy"));
			copy_button.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						JOptionPane.showMessageDialog(ExpectedActualDiffPHPTDisplay.this, "Copy to clipboard not implemented");
					}
				});
		
			button_panel.add("tab hfill", filename_field = new JLabel());
		}
		
		public void showFile(PhptTestCase test_case, String text) {
			filename_field.setText(test_case.getName());
			
			text_area.setText(text);
			
			row_field.setText("1");
			column_field.setText("1");
			setPosition(1, 1);
		}
		
		void setPosition(int row, int col) {
			String[] lines = StringUtil.splitLines(text_area.getText());
			int len = 0;
			for ( int i=0 ; i < row && i < lines.length ; i++ )
				len += lines[i].length();
			len += col;
						
			text_area.setCaretPosition(Math.min(text_area.getDocument().getLength(), len));
		}
		
		@Override
		public void caretUpdate(CaretEvent ce) {
			try {
				int caretpos = text_area.getCaretPosition();
				int linenum = text_area.getLineOfOffset(caretpos);
	
				int columnnum = caretpos - text_area.getLineStartOffset(linenum);
	
				column_field.setText(Integer.toString(columnnum));
				row_field.setText(Integer.toString(linenum));
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
		} // end public void caretUpdate
	}

} // end public class ExpectedActualDiffPHPTDisplay
