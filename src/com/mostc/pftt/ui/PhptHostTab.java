package com.mostc.pftt.ui;

import groovy.ui.ConsoleTextEditor;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.results.PhptResultPack;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.AbstractTestPackRunner.ETestPackRunnerState;
import com.mostc.pftt.runner.PhptTestPackRunner;

import se.datadosen.component.RiverLayout;

@SuppressWarnings("serial")
public class PhptHostTab extends JSplitPane {
	protected JPanel panel, button_panel;
	protected JProgressBar progress_bar, pass_bar;
	protected JButton stop_button, prev_file_button, prev_button, next_file_button, next_button, to_actual_button, to_expect_button, ignore_button, skip_button, pass_button;
	protected JLabel pass_label, total_label, fail_label, crash_label, xfail_label, xfail_works_label, skip_label, xskip_label, bork_label, unsupported_label, test_exceptions_label;
	protected JMenuBar jmb;
	protected JMenu options_menu, status_list_menu;
	protected ExpectedActualDiffPHPTDisplay eat_display;
	protected JCheckBoxMenuItem host_console_cb;
	protected JSplitPane jsp;
	protected final DefaultListModel fail_list_model, crash_list_model, xfail_list_model, xfail_works_list_model, xskip_list_model, skip_list_model, pass_list_model, bork_list_model, unsupported_list_model, test_exceptions_list_model;
	protected JList test_list;
	protected JScrollPane test_list_jsp;
	protected ConsoleTextEditor host_console;
	protected Host host;
	protected final JRadioButtonMenuItem list_fail_rb, list_xfail_rb, list_crash_rb, list_xfail_works_rb, list_skip_rb, list_xskip_rb, list_pass_rb, list_bork_rb, list_unsupported_rb, list_test_exceptions_rb;
	
	public PhptHostTab(Host host, final PhptTestPackRunner phpt_test_pack_runner) {
		super(JSplitPane.VERTICAL_SPLIT);
		this.host = host;
		setOneTouchExpandable(true);
		
		//
		setBottomComponent(host_console = new ConsoleTextEditor()); // TODO provide context to console
		host_console.setVisible(false);
		
		fail_list_model = new DefaultListModel();
		crash_list_model = new DefaultListModel();
		xfail_list_model = new DefaultListModel();
		xfail_works_list_model = new DefaultListModel();
		xskip_list_model = new DefaultListModel();
		skip_list_model = new DefaultListModel();
		pass_list_model = new DefaultListModel();
		bork_list_model = new DefaultListModel();
		unsupported_list_model = new DefaultListModel();
		test_exceptions_list_model = new DefaultListModel();
		
		//
		setTopComponent(panel = new JPanel(new RiverLayout()));
		
		panel.add("p left", new JLabel("Progress"));		
		panel.add("left", progress_bar = new JProgressBar(0, 12000)); // @see #showResult 
		progress_bar.setStringPainted(true);
		panel.add("left", stop_button = new JButton("Stop"));
		stop_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					phpt_test_pack_runner.setState(ETestPackRunnerState.NOT_RUNNING);
					stop_button.setEnabled(false);
				}
			});
		panel.add("left", new JLabel("Pass"));
		panel.add("left", pass_label = new JLabel("0"));
		panel.add("left", pass_bar = new JProgressBar(0, 12000)); // #showResult updates pass_bar maximum
		pass_bar.setStringPainted(true);
		panel.add("left", new JLabel("Total"));
		panel.add("left", total_label = new JLabel("0"));
		panel.add("left", new JLabel("Fail"));
		panel.add("left", fail_label = new JLabel("0"));
		panel.add(new JLabel("CRASH"));
		panel.add(crash_label = new JLabel("0"));
		panel.add("left", new JLabel("XFail"));
		panel.add("left", xfail_label = new JLabel("0"));
		panel.add("left", new JLabel("XSkip"));
		panel.add(xskip_label = new JLabel("0"));
		panel.add("left", new JLabel("Skip"));
		panel.add("left", skip_label = new JLabel("0"));
		panel.add(new JLabel("Bork"));
		panel.add(bork_label = new JLabel("0"));
		panel.add("left", new JLabel("XFail Works"));
		panel.add("left", xfail_works_label = new JLabel("0"));
		panel.add(new JLabel("Unsupported"));
		panel.add(unsupported_label = new JLabel("0"));
		panel.add(new JLabel("Test Exceptions"));
		panel.add(test_exceptions_label = new JLabel("0"));
		
		///////////////
		
		panel.add("p left hfill", button_panel = new JPanel(new GridLayout(1, 5, 10, 10)));
		
		button_panel.add(to_actual_button = new JButton("To Actual"));
		to_actual_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptHostTab.this, "Changing test EXPECT* section to match Actual - Not implemented");
				}
			});
		button_panel.add(to_expect_button = new JButton("To Expect"));
		to_expect_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptHostTab.this, "Changing Actual output to match Expected  - Not implemented");
				}
			});
		button_panel.add(ignore_button = new JButton("Ignore"));
		ignore_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptHostTab.this, "Ignore test (remove from telemetry entirely, no skip, no xskip, no pass) - Not implemented");
				}
			});
		button_panel.add(skip_button = new JButton("Skip"));
		skip_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptHostTab.this, "Skip test - Not implemented");
				}
			});
		button_panel.add(pass_button = new JButton("Pass"));
		pass_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptHostTab.this, "Marking test as PASS - Not implemented");
				}
			});
		

		button_panel.add(jmb = new JMenuBar());
		jmb.add(options_menu = new JMenu("Options"));
		options_menu.add(status_list_menu = new JMenu("Status List"));
		
		ButtonGroup list_button_group = new ButtonGroup();
		status_list_menu.add(list_fail_rb = new JRadioButtonMenuItem("Fail"));
		list_fail_rb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { 
				if (list_fail_rb.isSelected())
					showList(fail_list_model); 
			} });
		list_button_group.add(list_fail_rb);
		status_list_menu.add(list_crash_rb = new JRadioButtonMenuItem("CRASH"));
		list_crash_rb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { 
				if (list_crash_rb.isSelected())
					showList(crash_list_model); 
			} });
		list_button_group.add(list_crash_rb);
		status_list_menu.add(list_xfail_rb = new JRadioButtonMenuItem("XFail"));
		list_xfail_rb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { 
				if (list_xfail_rb.isSelected())
					showList(xfail_list_model); 
			} });
		list_button_group.add(list_xfail_rb);
		status_list_menu.add(list_xfail_works_rb = new JRadioButtonMenuItem("XFail_Works"));
		list_xfail_works_rb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { 
				if (list_xfail_works_rb.isSelected())
					showList(xfail_works_list_model); 
			} });
		list_button_group.add(list_xfail_works_rb);
		status_list_menu.add(list_skip_rb = new JRadioButtonMenuItem("Skip"));
		list_skip_rb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { 
				if (list_skip_rb.isSelected())
					showList(skip_list_model); 
			} });
		list_button_group.add(list_skip_rb);
		status_list_menu.add(list_xskip_rb = new JRadioButtonMenuItem("XSkip"));
		list_xskip_rb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { 
				if (list_xskip_rb.isSelected())
					showList(xskip_list_model); 
			} });
		list_button_group.add(list_xskip_rb);
		status_list_menu.add(list_pass_rb = new JRadioButtonMenuItem("Pass"));
		list_pass_rb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { 
				if (list_pass_rb.isSelected())
					showList(pass_list_model); 
			} });
		list_button_group.add(list_pass_rb);
		status_list_menu.add(list_bork_rb = new JRadioButtonMenuItem("Bork"));
		list_bork_rb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { 
				if (list_bork_rb.isSelected())
					showList(bork_list_model); 
			} });
		list_button_group.add(list_bork_rb);
		status_list_menu.add(list_unsupported_rb = new JRadioButtonMenuItem("Unsupported"));
		list_unsupported_rb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { 
				if (list_unsupported_rb.isSelected())
					showList(unsupported_list_model); 
			} });
		list_button_group.add(list_unsupported_rb);
		status_list_menu.add(list_test_exceptions_rb = new JRadioButtonMenuItem("Test Exceptions"));
		list_test_exceptions_rb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { 
				if (list_test_exceptions_rb.isSelected())
					showList(test_exceptions_list_model); 
			} });
		list_button_group.add(list_test_exceptions_rb);
		
		options_menu.add(host_console_cb = new JCheckBoxMenuItem("Host Console"));
		host_console_cb.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					host_console.setVisible(host_console_cb.isSelected());
					setDividerLocation(0.75d);
				}			
			});
		
		panel.add("p left hfill vfill", jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT));

		//
		JPanel scroll_button_panel = new JPanel(new GridLayout(4, 1, 10, 10));
		scroll_button_panel.add(prev_file_button = new JButton("Prev File", new ImageIcon(Toolkit.getDefaultToolkit().createImage(PhptHostTab.class.getResource("go-top.png")))));
		prev_file_button.setVerticalTextPosition(SwingUtilities.BOTTOM); prev_file_button.setHorizontalTextPosition(SwingUtilities.CENTER);
		prev_file_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptHostTab.this, "Showing Previous File - not implemented");
				}
			});
		scroll_button_panel.add(prev_button = new JButton("Prev", new ImageIcon(Toolkit.getDefaultToolkit().createImage(PhptHostTab.class.getResource("go-up.png")))));
		prev_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptHostTab.this, "Showing Previous Difference - not implemented");
				}
			});
		prev_button.setVerticalTextPosition(SwingUtilities.BOTTOM); prev_button.setHorizontalTextPosition(SwingUtilities.CENTER);
		scroll_button_panel.add(next_button = new JButton("Next", new ImageIcon(Toolkit.getDefaultToolkit().createImage(PhptHostTab.class.getResource("go-down.png")))));
		next_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptHostTab.this, "Showing Next Difference - not implemented");
				}
			});
		next_button.setVerticalTextPosition(SwingUtilities.BOTTOM); next_button.setHorizontalTextPosition(SwingUtilities.CENTER);
		scroll_button_panel.add(next_file_button = new JButton("Next File", new ImageIcon(Toolkit.getDefaultToolkit().createImage(PhptHostTab.class.getResource("go-bottom.png")))));
		next_file_button.setVerticalTextPosition(SwingUtilities.BOTTOM); next_file_button.setHorizontalTextPosition(SwingUtilities.CENTER);
		next_file_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptHostTab.this, "Showing Next File - not implemented");
				}
			});		
		//
		
		JPanel eat_display_and_scroll_button_panel = new JPanel(new BorderLayout());
		eat_display_and_scroll_button_panel.add(eat_display = new ExpectedActualDiffPHPTDisplay(), BorderLayout.CENTER);
		eat_display_and_scroll_button_panel.add(scroll_button_panel, BorderLayout.LINE_END);
		jsp.setLeftComponent(eat_display_and_scroll_button_panel);
		
		jsp.setRightComponent(test_list_jsp = new JScrollPane(test_list = new JList()));
		test_list.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					eat_display.showTest((PhptTestResult) test_list.getSelectedValue());
				}			
			});
		test_list_jsp.setVisible(false);
	}
	
	protected void showList(DefaultListModel model) {
		if (model!=null)
			test_list.setModel(model);
		test_list_jsp.setVisible(true);
		jsp.setDividerLocation(0.75d);
	}
	
	protected int crash, pass, fail, xfail, xfail_works, skip, xskip, bork, unsupported, test_exceptions; // XXX duplicates functionality from PhptTelemetry
	
	public void showResult(final int total, final int completed, final PhptTestResult result) {
		SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progress_bar.setMaximum(total);
					
					switch(result.status) {
					case CRASH:
						crash++;
						crash_label.setText(Integer.toString(crash));
						crash_list_model.addElement(result);
						
						break;
					case FAIL:
						fail++;
						fail_label.setText(Integer.toString(fail));
						
						pass_bar.setString(Float.toString(PhptResultPack.round1( (float)( ((float)pass) / ((float)( pass + fail + crash )) )))+"%"); // 1 decimal places nn.y
						pass_bar.setMaximum(fail+pass);
						total_label.setText(""+(fail+pass));
						
						// show in list
						fail_list_model.addElement(result);
						break;
					case XFAIL:
						xfail++;
						xfail_label.setText(Integer.toString(xfail));
						
						xfail_list_model.addElement(result);
						break;
					case XFAIL_WORKS:
						xfail_works++;
						xfail_works_label.setText(Integer.toString(xfail_works));
						
						xfail_works_list_model.addElement(result);
						break;
					case SKIP:
						skip++;
						skip_label.setText(Integer.toString(skip));
						
						skip_list_model.addElement(result);
						break;
					case XSKIP:
						xskip++;
						xskip_label.setText(Integer.toString(xskip));
						
						xskip_list_model.addElement(result);
						break;
					case BORK:
						bork++;
						bork_label.setText(Integer.toString(bork));
						
						bork_list_model.addElement(result);
						break;
					case TEST_EXCEPTION:
						test_exceptions++;
						test_exceptions_label.setText(Integer.toString(test_exceptions));
						
						test_exceptions_list_model.addElement(result);
						break;
					case PASS:
						pass++;
						pass_bar.setString(Float.toString(PhptResultPack.round1( (float)( (double)pass / ((double)( pass + fail )) )))+"%"); // 1 decimal places nn.y
						
						
						pass_bar.setValue(pass);
						pass_bar.setMaximum(fail+pass);
						pass_label.setText(Integer.toString(pass));
						total_label.setText(""+(fail+pass));
						
						pass_list_model.addElement(result);
						break;
					default:
						unsupported++;
						unsupported_label.setText(Integer.toString(unsupported));
						
						unsupported_list_model.addElement(result);
					}
					progress_bar.setValue(fail+xfail+skip+xskip+bork+unsupported+pass);
				} // end public void run
			});
	} // end public void show_result

}  // end public class HostTab
