package com.mostc.pftt.util;

import java.awt.Component;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

public final class ErrorUtil {
	
	public static void display_error(Component c, String msg) {
		JOptionPane.showMessageDialog(c, msg, "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	public static void display_error(Component c, Throwable ex) {
		ex.printStackTrace();
	
		display_error(c, toString(ex));
	}
	
	public static String toString(Throwable ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);		
		pw.close();
		return sw.toString();
	}

	private ErrorUtil() {}
}
