package com.mostc.pftt.ui;

import java.awt.GridLayout;
import java.awt.Point;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.results.PhpUnitResultWriter;

@SuppressWarnings("serial")
public class PhpUnitByTest extends JPanel {
	protected PhpUnitResultWriter writer;
	protected DefaultListModel test_list_model;
	protected JList test_list;
	protected JTextArea output_display;
	protected JScrollPane output_jsp;
	
	public PhpUnitByTest() {
		super(new GridLayout(1, 2));
		add(new JScrollPane(test_list = new JList(test_list_model = new DefaultListModel())));
		
		add(output_jsp = new JScrollPane(output_display = new JTextArea()));
		output_display.setColumns(40);
		output_display.setWrapStyleWord(false);
		
		test_list.addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(ListSelectionEvent e) {
					String test_name = (String) test_list.getSelectedValue();
					if (test_name==null)
						return;
					String output = writer.getTestOutput(test_name);
					if (output==null)
						output = "<No Output>";
					output_display.setText(output);	
					output_jsp.getViewport().setViewPosition(new Point(0, 0));
				}
			
			});
	}

	public void showResults(EPhpUnitTestStatus status, PhpUnitResultWriter writer) {
		this.writer = writer;
		test_list_model.clear();
		for ( String test_name : writer.getTestNames(status) ) {
			test_list_model.addElement(test_name);
		}
	}
	
}
