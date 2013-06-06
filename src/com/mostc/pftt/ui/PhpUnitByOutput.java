package com.mostc.pftt.ui;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.results.PhpUnitResultWriter;

@SuppressWarnings("serial")
public class PhpUnitByOutput extends JPanel {
	protected DefaultListModel output_list_model, test_list_model;
	protected HashMap<String,ByOutputResult> by_output_map;
	protected JList output_list, test_list;

	public PhpUnitByOutput() {
		super(new GridLayout(1, 2));
		by_output_map = new HashMap<String,ByOutputResult>();
		
		add(new JScrollPane(output_list = new JList(output_list_model = new DefaultListModel())));
		output_list.setCellRenderer(new TextAreaListCellRenderer());
		
		add(new JScrollPane(test_list = new JList(test_list_model = new DefaultListModel())));
		
		output_list.addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(ListSelectionEvent e) {
					ByOutputResult bor = (ByOutputResult) output_list.getSelectedValue();
					if (bor==null)
						return;
					
					test_list_model.clear();
					
					// alphabetize
					Collections.sort(bor.test_names); 
					for ( String test_name : bor.test_names ) {
						test_list_model.addElement(test_name);
					}
				}
			
			});
	}
	
	public void showResults(EPhpUnitTestStatus status, PhpUnitResultWriter writer) {
		by_output_map.clear();
		output_list_model.clear();
		
		ByOutputResult bor;
		String output;
		for ( String test_name : writer.getTestNames(status) ) {
			output = writer.getTestOutput(test_name);
			if (output==null)
				output = "";
			else
				output = StringUtil.max(output, 80);
			
			bor = by_output_map.get(output);
			if (bor==null) {
				bor = new ByOutputResult(output);
				by_output_map.put(output, bor);
				output_list_model.addElement(bor);
			}
			bor.test_names.add(test_name);
		}
	}
		
	protected class ByOutputResult {
		protected final LinkedList<String> test_names;
		protected final String output;
		
		public ByOutputResult(String output) {
			this.output = output;
			test_names = new LinkedList<String>();
		}
		
		public String toString() {
			return output;
		}
	}
	
	protected class TextAreaListCellRenderer extends JTextArea implements ListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			setColumns(40);
			setWrapStyleWord(false);
			setText(StringUtil.toString(value));
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			return this;
		}
	}
	
}
