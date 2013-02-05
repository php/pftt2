package com.mostc.pftt.ui;

import groovy.ui.ConsoleTextEditor;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.LineBorder;

import se.datadosen.component.RiverLayout;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.LocalPhptTestPackRunner;
import com.mostc.pftt.scenario.ScenarioSet;

@SuppressWarnings("serial")
public class PhptDebuggerFrame extends JPanel {
	protected JTabbedPane tabs;
	protected PhptHostTab localhost_tab;
	protected ConsoleTextEditor console;
	protected JToggleButton filter_button_base, filter_button_test, filter_button_base_plus_test, filter_button_base_minus_test, filter_button_test_minus_base, filter_button_base_eq_test;
	protected JButton exit_button, report_button;
	protected JMenu scenario_menu;
	protected JTextField status_field;
	
	public PhptDebuggerFrame(LocalPhptTestPackRunner phpt_test_pack_runner) {
		super(new RiverLayout(5, 2));
		
		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsp.setOneTouchExpandable(true);
		jsp.setTopComponent(tabs = new JTabbedPane());
		jsp.setBottomComponent(console = new ConsoleTextEditor()); // TODO provide context to console
		console.setVisible(false);
		
		add("p left hfill vfill", jsp);
	
		JPanel menubar = new JPanel(new RiverLayout(2, 2));
		menubar.add("p left", new JLabel("Status:"));
		menubar.add("left hfill", status_field = new JTextField());
		status_field.setEditable(false);
		JMenuBar jmb = new JMenuBar();
		menubar.add("right", jmb);
		//
		// TODO base TP and build vs test TP and build
		jmb.add(new JMenu("Base:"));
		jmb.add(new JMenu("Test:"));
		jmb.add(scenario_menu = new JMenu("Scenarios"));
		ButtonGroup scenario_bg = new ButtonGroup();
		for ( ScenarioSet set : ScenarioSet.getDefaultScenarioSets() ) { // TODO
			JRadioButtonMenuItem rb = new JRadioButtonMenuItem(set.toString());
			scenario_bg.add(rb);
			scenario_menu.add(rb);
		}
		//
		
		menubar.add("right", report_button = new JButton("FBC Report"));
		
		menubar.add("right", exit_button = new JButton("Exit"));
		exit_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});
		
		JPanel telemetry_filter_panel = new JPanel(new RiverLayout());
		telemetry_filter_panel.setToolTipText("Telemetry Filter");
		telemetry_filter_panel.setBorder(new LineBorder(Color.BLACK));
		ButtonGroup filter_bg = new ButtonGroup();
		telemetry_filter_panel.add("left", filter_button_base = new JToggleButton("Base"));
		filter_button_base.setToolTipText("Telemetry from Base build/test-pack");
		filter_button_base.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptDebuggerFrame.this, "Showing only Telemetry from Base build/test-pack not implemented");
				}
			});
		filter_bg.add(filter_button_base);
		telemetry_filter_panel.add("left", filter_button_test = new JToggleButton("Test", true));
		filter_button_test.setToolTipText("Telemetry from Test build/test-pack");
		filter_button_test.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					//
				}
			});
		filter_bg.add(filter_button_test);
		telemetry_filter_panel.add("left", filter_button_base_plus_test = new JToggleButton("Base+Test"));
		filter_button_base_plus_test.setToolTipText("All Telemetry from Base and Test build/test-pack");
		filter_button_base_plus_test.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptDebuggerFrame.this, "Showing all Telemetry from Base and Test build/test-pack not implemented");
				}
			});
		filter_bg.add(filter_button_base_plus_test);
		telemetry_filter_panel.add("left", filter_button_base_minus_test = new JToggleButton("Base-Test"));
		filter_button_base_minus_test.setToolTipText("Telemetry in Test but not Base build/test-pack");
		filter_button_base_minus_test.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptDebuggerFrame.this, "Showing only Telemetry from Base not in the Test build/test-pack not implemented");
				}
			});
		filter_bg.add(filter_button_base_minus_test);
		telemetry_filter_panel.add("left", filter_button_test_minus_base = new JToggleButton("Test-Base"));
		filter_button_test_minus_base.setToolTipText("Telemetry in Base but not Test build/test-pack");
		filter_button_test_minus_base.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptDebuggerFrame.this, "Showing only Telemetry from Test not in the Base build/test-pack not implemented");
				}
			});
		filter_bg.add(filter_button_test_minus_base);
		telemetry_filter_panel.add("left", filter_button_base_eq_test = new JToggleButton("Base=Test"));
		filter_button_base_eq_test.setToolTipText("Only Matching Telemetry from Base and Test build/test-pack");
		filter_button_base_eq_test.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(PhptDebuggerFrame.this, "Showing only Telemetry that is in both Base and Test build/test-pack not implemented");
				}
			});
		filter_bg.add(filter_button_base_eq_test);
		menubar.add("right", telemetry_filter_panel); 
		add("p left hfill", menubar);
		
		tabs.addTab("Localhost", localhost_tab = new PhptHostTab(new LocalHost(), phpt_test_pack_runner));
	}
	
	public void showResult(AHost host, int total, int completed, PhptTestResult result) {
		localhost_tab.showResult(total, completed, result);
	}
	
	public void showGlobalConsole() {
		// TODO
	}
	
} // end public class PhptDebuggerFrame
