package com.mostc.pftt.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.results.PhpUnitResultWriter;
import com.mostc.pftt.runner.AbstractTestPackRunner.ETestPackRunnerState;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner;
import com.mostc.pftt.scenario.ScenarioSetSetup;

import se.datadosen.component.RiverLayout;

@SuppressWarnings("serial")
public class PhpUnitHostTab extends JPanel {
	protected final AHost host;
	protected final PhpUnitSourceTestPack test_pack;
	protected final ScenarioSetSetup scenario_set_setup;
	protected PhpUnitResultWriter writer;
	//
	protected JLabel pass_label, failure_label, error_label, warning_label, notice_label,
		skip_label, deprecated_label, not_implemented_label, unsupported_label,
		test_exception_label, crash_label, bork_label, xskip_label;
	protected ArrayList<StatusRadioButton> status_rb_group;
	protected JRadioButton by_output_rb, by_test_rb;
	protected JButton stop_button;
	protected PhpUnitByOutput by_output;
	protected PhpUnitByTest by_test;
	protected JPanel workarea;

	public PhpUnitHostTab(final LocalPhpUnitTestPackRunner test_pack_runner, AHost host, PhpUnitSourceTestPack test_pack, ScenarioSetSetup scenario_set) {
		super(new RiverLayout());
		this.host = host;
		this.test_pack = test_pack;
		this.scenario_set_setup = scenario_set;
		add("p left", stop_button = new JButton("Stop"));
		stop_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					test_pack_runner.setState(ETestPackRunnerState.NOT_RUNNING);
					stop_button.setEnabled(false);
				}
			});
		
		add("left", by_output_rb = new JRadioButton("Show By Output", true));
		by_output_rb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (by_output_rb.isSelected()) {
						workarea.remove(by_test);
						by_test_rb.setSelected(false);
						workarea.add(by_output, BorderLayout.CENTER);
					} else {
						workarea.remove(by_output);
						by_test_rb.setSelected(true);
						workarea.add(by_test, BorderLayout.CENTER);
					}
					workarea.revalidate();
				}
			});
		add("left", by_test_rb = new JRadioButton("Show By Test", false));
		by_test_rb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (by_test_rb.isSelected()) {
						workarea.remove(by_output);
						workarea.add(by_test, BorderLayout.CENTER);
						by_output_rb.setSelected(false);
					} else {
						by_output_rb.setSelected(true);
						workarea.remove(by_test);
						workarea.add(by_output, BorderLayout.CENTER);
					}
					workarea.revalidate();
				}
			});
		
		status_rb_group = new ArrayList<StatusRadioButton>(13);
		add("left", new StatusRadioButton(EPhpUnitTestStatus.PASS));
		add("left", pass_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.FAILURE));
		add("left", failure_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.ERROR));
		add("left", error_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.WARNING));
		add("left", warning_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.NOTICE));
		add("left", notice_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.SKIP));
		add("left", skip_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.DEPRECATED));
		add("left", deprecated_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.NOT_IMPLEMENTED));
		add("left", not_implemented_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.UNSUPPORTED));
		add("left", unsupported_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.TEST_EXCEPTION));
		add("left", test_exception_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.CRASH));
		add("left", crash_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.BORK));
		add("left", bork_label = new JLabel("0"));
		add("left", new StatusRadioButton(EPhpUnitTestStatus.XSKIP));
		add("left", xskip_label = new JLabel("0"));
		
		workarea = new JPanel(new BorderLayout());
		add("p left hfill vfill", workarea);
		workarea.add(by_output = new PhpUnitByOutput(), BorderLayout.CENTER);
		by_test = new PhpUnitByTest();
	}
	
	public void setPhpResultPackWriter(PhpResultPackWriter result_pack) {
		writer = (PhpUnitResultWriter) result_pack.getPhpUnit(host, test_pack, scenario_set_setup);
	}
	
	public void setStatus(EPhpUnitTestStatus status) {
		by_output.showResults(status, writer);
		by_test.showResults(status, writer);
	}
	
	protected class StatusRadioButton extends JRadioButton implements ActionListener {
		protected final EPhpUnitTestStatus status;
		
		public StatusRadioButton(EPhpUnitTestStatus status) {
			super(status.toString(), false);
			this.status = status;
			addActionListener(this);
			status_rb_group.add(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			for ( StatusRadioButton rb : status_rb_group ) {
				if (rb!=this)
					rb.setSelected(false);
			}
			if (isSelected()) {
				setStatus(status);
			}
		}
	}
	
	public void updateCount() {
		pass_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.PASS)));
		failure_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.FAILURE)));
		error_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.ERROR)));
		warning_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.WARNING)));
		notice_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.NOTICE)));
		skip_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.SKIP)));
		deprecated_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.DEPRECATED)));
		not_implemented_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.NOT_IMPLEMENTED)));
		unsupported_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.UNSUPPORTED)));
		test_exception_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.TEST_EXCEPTION)));
		crash_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.CRASH)));
		bork_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.BORK)));
		xskip_label.setText(Integer.toString(writer.count(EPhpUnitTestStatus.XSKIP)));
	}
	
}
