package com.github.mattficken.io.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JFrame;

import com.github.mattficken.io.RestartableInputStream;



// TODO EBCDIC MacOS-Roman TRON? KOI8-U KOI7 MIK ISCII GSM-03.38 HZ/GB2312 TIS-620 VISCII BOCU1?

public class CharsetDebuggerMain {
	
	public static void main(String[] args) throws IOException {
		//File file = new File("C:\\php-sdk\\php-test-pack-5.4-nts-windows-vc9-x86-r041dd77\\ext\\mbstring\\tests\\bug26639.phpt");
		final File file = new File("/tmp/1.txt");//"/home/matt/php-sdk/php5.4-201209171930/tests/lang/operators/operator_lt_basic.phpt");
		
		CharsetDebuggerPanel panel = new CharsetDebuggerPanel();
		JFrame jf = new JFrame("Charset Debugger - "+file.getName());
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.setContentPane(panel);
		jf.pack();
		jf.setExtendedState(JFrame.MAXIMIZED_BOTH);
		jf.setVisible(true);

		// load file
		panel.setInputStream(new RestartableInputStream() {
			@Override
			public InputStream openInputStream() throws FileNotFoundException {
				return new BufferedInputStream(new FileInputStream(file));
			}
		});
	}

}
