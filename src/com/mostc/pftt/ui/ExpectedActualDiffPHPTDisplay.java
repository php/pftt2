package com.mostc.pftt.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.table.DefaultTableModel;

import com.github.mattficken.io.RestartableInputStream;
import com.github.mattficken.io.ui.CharsetDebuggerPanel;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.StringUtil;

import se.datadosen.component.RiverLayout;

@SuppressWarnings("serial")
public class ExpectedActualDiffPHPTDisplay extends JScrollPane {
	protected JPanel horizontal_panel, horizontal_button_panel, vertical_panel;
	protected TextDisplayPanel expected_display, diff_display, actual_display, test_display;
	protected DefaultTableModel env_table_model;
	protected JTable env_table;
	protected JTextArea regex_compiler_dump_textarea, regex_output_textarea, http_request_textarea, http_response_textarea, ini_textarea, stdin_data_textarea, shell_script_textarea, expectf_textarea, pre_override_textarea, sapi_output_textarea;
	protected PhptTestResult test_result;
	protected JScrollPane regex_compiler_dump_jsp, regex_output_jsp, http_request_jsp, http_response_jsp, expectf_jsp, pre_override_jsp, sapi_output_jsp, ini_jsp, stdin_data_jsp, shell_script_jsp, env_table_jsp;
	protected ScrollbarSyncManager scrollbar_sync_mgr;
	
	public ExpectedActualDiffPHPTDisplay() {
		super(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		horizontal_panel = new JPanel(new RiverLayout(2, 2)) {
				@Override
				public Dimension getPreferredSize() {
					Dimension dim = super.getPreferredSize();
					return new Dimension(dim.width, 300);
				}
			};
		getHorizontalScrollBar().setUnitIncrement(100);
		setViewportView(horizontal_panel);
		vertical_panel = new JPanel(new InvisibleGridLayout(1, 12, 2, 2));
		horizontal_panel.add("p left hfill vfill", vertical_panel);//, BorderLayout.CENTER);
		horizontal_panel.add("p left hfill", horizontal_button_panel = new JPanel(new InvisibleGridLayout(1, 12, 2, 2)));//, BorderLayout.PAGE_END);
		
		scrollbar_sync_mgr = new ScrollbarSyncManager();
		
		actual_display = new TextDisplayPanel();
		actual_display.text_area.setToolTipText("Actual test output");
		actual_display.text_area.setBackground(new Color(220, 255, 150));
		vertical_panel.add(scrollbar_sync_mgr.add(new JScrollPane(actual_display.text_area)));
		
		expected_display = new TextDisplayPanel();
		expected_display.text_area.setToolTipText("Expected test output");
		expected_display.text_area.setBackground(new Color(220, 220, 150));
		vertical_panel.add(scrollbar_sync_mgr.add(new JScrollPane(expected_display.text_area)));
		
		diff_display = new TextDisplayPanel();
		diff_display.text_area.setToolTipText("Diff");
		diff_display.text_area.setBackground(new Color(220, 255, 255));
		vertical_panel.add(new JScrollPane(diff_display.text_area));
		
		expectf_textarea = new JTextArea();
		expectf_textarea.setToolTipText("EXPECTF section after regular expression patterns are added");
		expectf_textarea.setBackground(new Color(220, 150, 150));
		vertical_panel.add(scrollbar_sync_mgr.add(expectf_jsp = new JScrollPane(expectf_textarea)));
		
		regex_output_textarea = new JTextArea();
		regex_output_textarea.setToolTipText("Matches between actual output and EXPECTF section");
		regex_output_textarea.setBackground(new Color(255, 220, 150));
		vertical_panel.add(regex_output_jsp = new JScrollPane(regex_output_textarea));
		
		regex_compiler_dump_textarea = new JTextArea();
		regex_compiler_dump_textarea.setToolTipText("Compile dump from Regular Expression engine");
		regex_compiler_dump_textarea.setBackground(new Color(255, 255, 150));
		vertical_panel.add(regex_compiler_dump_jsp = new JScrollPane(regex_compiler_dump_textarea));
		
		http_request_textarea = new JTextArea();
		http_request_textarea.setToolTipText("HTTP Request(s)");
		http_request_textarea.setBackground(new Color(220, 150, 220));
		vertical_panel.add(http_request_jsp = new JScrollPane(http_request_textarea));
		
		http_response_textarea = new JTextArea();
		http_response_textarea.setToolTipText("HTTP Response(s)");
		http_response_textarea.setBackground(new Color(220, 150, 255));
		vertical_panel.add(scrollbar_sync_mgr.add(http_response_jsp = new JScrollPane(http_response_textarea)));
		
		sapi_output_textarea = new JTextArea();
		sapi_output_textarea.setToolTipText("Output from SAPI - did web server crash? etc...");
		sapi_output_textarea.setBackground(new Color(255, 255, 255));
		vertical_panel.add(sapi_output_jsp = new JScrollPane(sapi_output_textarea));
		
		pre_override_textarea = new JTextArea();
		pre_override_textarea.setToolTipText("Actual test output before any OS specific overrides applied");
		pre_override_textarea.setBackground(new Color(255, 150, 150));
		vertical_panel.add(scrollbar_sync_mgr.add(pre_override_jsp = new JScrollPane(pre_override_textarea)));
		
		ini_textarea = new JTextArea();
		ini_textarea.setToolTipText("entire INI used for this test case");
		ini_textarea.setBackground(new Color(255, 150, 220));
		vertical_panel.add(ini_jsp = new JScrollPane(ini_textarea));
		
		stdin_data_textarea = new JTextArea();
		stdin_data_textarea.setLineWrap(true);
		stdin_data_textarea.setToolTipText("STDIN");
		stdin_data_textarea.setBackground(new Color(255, 150, 255));
		vertical_panel.add(stdin_data_jsp = new JScrollPane(stdin_data_textarea));
		
		shell_script_textarea = new JTextArea();
		shell_script_textarea.setLineWrap(true);
		shell_script_textarea.setToolTipText("Shell Script");
		shell_script_textarea.setBackground(new Color(150, 255, 150));
		vertical_panel.add(shell_script_jsp = new JScrollPane(shell_script_textarea));
		
		env_table = new JTable(env_table_model = new DefaultTableModel());
		env_table.setToolTipText("ENV vars");
		env_table_model.addColumn("Name");
		env_table_model.addColumn("Value");
		env_table.setBackground(new Color(150, 220, 150));
		vertical_panel.add(env_table_jsp = new JScrollPane(env_table));
		
		test_display = new TextDisplayPanel();
		test_display.text_area.setToolTipText("PHPT");
		test_display.text_area.setBackground(new Color(150, 255, 255));
		vertical_panel.add(new JScrollPane(test_display.text_area));
		
		horizontal_button_panel.add(actual_display.button_panel);
		horizontal_button_panel.add(expected_display.button_panel);
		horizontal_button_panel.add(diff_display.button_panel);
		horizontal_button_panel.add(new JPanel()); // placeholder: expectf
		horizontal_button_panel.add(new JPanel()); // placeholder: http-request
		horizontal_button_panel.add(new JPanel()); // placeholder: http-response
		horizontal_button_panel.add(new JPanel()); // placeholder: sapi-output
		horizontal_button_panel.add(new JPanel()); // placeholder: pre-override
		horizontal_button_panel.add(new JPanel()); // placeholder: ini
		horizontal_button_panel.add(new JPanel()); // placeholder: stdin
		horizontal_button_panel.add(new JPanel()); // placeholder: shell-script
		horizontal_button_panel.add(new JPanel()); // placeholder: env
		horizontal_button_panel.add(test_display.button_panel);
	}
	
	public void showTest(PhptTestResult result) {
		if (result==null)
			return;
		
		this.test_result = result;
		
		while (env_table_model.getRowCount() > 0) {
			env_table_model.removeRow(0);
		}
		
		if (result.env!=null && result.env.size()>0) {
			env_table_jsp.setVisible(true);
			for (String name : result.env.keySet()) {
				env_table_model.addRow(new Object[]{name, result.env.get(name)});
			} 
		} else {
			env_table_jsp.setVisible(false);
		}
		if (result.ini==null) {
			ini_jsp.setVisible(false);
		} else {
			ini_jsp.setVisible(true);
			ini_textarea.setText(result.ini+"");
		}
		if (result.stdin_data!=null) {
			stdin_data_jsp.setVisible(true);
			stdin_data_textarea.setText(new String(result.stdin_data));
		} else {
			stdin_data_jsp.setVisible(false);
		}
		if (StringUtil.isEmpty(result.shell_script)) {
			shell_script_jsp.setVisible(false);
		} else {
			shell_script_jsp.setVisible(true);
			shell_script_textarea.setText(result.shell_script);
		}
		if (StringUtil.isEmpty(result.regex_compiler_dump)) {
			regex_compiler_dump_jsp.setVisible(false);
		} else {
			regex_compiler_dump_jsp.setVisible(true);
			regex_compiler_dump_textarea.setText(result.regex_compiler_dump);
		}
		if (StringUtil.isEmpty(result.regex_output)) {
			regex_output_jsp.setVisible(false);
		} else {
			regex_output_jsp.setVisible(true);
			regex_output_textarea.setText(result.regex_output);
		}
		if (StringUtil.isEmpty(result.http_request)) {
			http_request_jsp.setVisible(false);
		} else {
			http_request_jsp.setVisible(true);
			http_request_textarea.setText(result.http_request);
		}
		if (StringUtil.isEmpty(result.http_response)) {
			http_response_jsp.setVisible(false);
		} else {
			http_response_jsp.setVisible(true);
			http_response_textarea.setText(result.http_response);
		}
		
		// TODO temp
		expected_display.text_area.setText(result.test_case.getCommonCharset()+"\n"+result.test_case.getExpected());
		
		// TODO temp
		actual_display.showFile(result.test_case, result.actual_cs+"\n"+result.actual);
		try {
			test_display.showFile(result.test_case, result.test_case.getContents(result.host));
		} catch ( IOException ex ) {
			ErrorUtil.display_error(this, ex);
		}
		
		diff_display.showFile(result.test_case, result.diff_str==null?"":result.diff_str);
		if (StringUtil.isEmpty(result.expectf_output)) {
			expectf_jsp.setVisible(false);
		} else {
			expectf_jsp.setVisible(true);
			expectf_textarea.setText(result.expectf_output);
		}
		if (StringUtil.isEmpty(result.preoverride_actual)) {
			pre_override_jsp.setVisible(false);
		} else {
			pre_override_jsp.setVisible(true);
			pre_override_textarea.setText(result.preoverride_actual);
		}
		//
		String sapi_output = result.getSAPIOutput();
		if (StringUtil.isEmpty(sapi_output)) {
			sapi_output_jsp.setVisible(false);
		} else {
			sapi_output_jsp.setVisible(true);
			sapi_output_textarea.setText(sapi_output);
		}
		//
		
		vertical_panel.revalidate();
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
						setPosition(Integer.parseInt(row_field.getText())-1, Integer.parseInt(column_field.getText())-1); 
					}
				});
			
			button_panel.add("tab", column_field = new JTextField(3));
			column_field.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setPosition(Integer.parseInt(row_field.getText())-1, Integer.parseInt(column_field.getText())-1); 
					}
				});
			
			button_panel.add("left", charset_debugger_button = new JButton("Charset Debugger"));
			charset_debugger_button.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						CharsetDebuggerPanel panel = new CharsetDebuggerPanel();
						JFrame jf = new JFrame("Charset Debugger - "+test_result.test_case.getName());
						jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
						jf.setContentPane(panel);
						jf.pack();
						jf.setExtendedState(JFrame.MAXIMIZED_BOTH);
						jf.setVisible(true);
	
						// load file
						panel.setInputStream(new RestartableInputStream() {
								@Override
								public InputStream openInputStream() throws FileNotFoundException {
									return new ByteArrayInputStream(test_result.actual_cs==null?text_area.getText().getBytes():text_area.getText().getBytes(test_result.actual_cs));
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
	
				column_field.setText(Integer.toString(columnnum+1));
				row_field.setText(Integer.toString(linenum+1));
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
		} // end public void caretUpdate
	}
	
	protected static class ScrollbarSyncManager implements AdjustmentListener {
		protected ArrayList<JScrollPane> jsps;
		
		public ScrollbarSyncManager() {
			jsps = new ArrayList<JScrollPane>(5);
		}
		
		public JScrollPane add(JScrollPane jsp) {
			jsp.getVerticalScrollBar().addAdjustmentListener(this);
			jsps.add(jsp);
			return jsp;
		}

		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {
			for (JScrollPane jsp:jsps) {
				if (jsp.getVerticalScrollBar()==e.getAdjustable())
					continue;
				
				jsp.getVerticalScrollBar().setValue(e.getValue());
			}
		}
	} // end protected static class ScrollbarSyncManager

} // end public class ExpectedActualDiffPHPTDisplay
