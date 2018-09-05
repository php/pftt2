package com.mostc.pftt.results;

import java.awt.Component;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

import com.github.mattficken.io.StringUtil;

public final class ConsoleManagerUtil {
	
	public static void printStackTrace(EPrintType type, Class<?> clazz, ConsoleManager cm, String method_name, Exception ex, String msg) {
		if (cm==null)
			printStackTrace(clazz, ex);
		else
			cm.addGlobalException(type, clazz, method_name, ex, msg);
	}
	public static void printStackTrace(EPrintType type, Class<?> clazz, ConsoleManager cm, String method_name, Exception ex, String msg, Object a) {
		if (cm==null)
			printStackTrace(clazz, ex);
		else
			cm.addGlobalException(type, clazz, method_name, ex, msg, a);
	}
	public static void printStackTrace(EPrintType type, Class<?> clazz, ConsoleManager cm, String method_name, Exception ex, String msg, Object a, Object b) {
		if (cm==null)
			printStackTrace(clazz, ex);
		else
			cm.addGlobalException(type, clazz, method_name, ex, msg, a, b);
	}
	public static void printStackTrace(EPrintType type, Class<?> clazz, ConsoleManager cm, String method_name, Exception ex, String msg, Object a, Object b, Object c) {
		if (cm==null)
			printStackTrace(clazz, ex);
		else
			cm.addGlobalException(type, clazz, method_name, ex, msg, a, b, c);
	}
	public static void printStackTraceDebug(Class<?> clazz, ConsoleManager cm, Throwable ex) {
		if (cm != null && cm.isPfttDebug()) {
			printStackTrace(clazz, cm, ex);
		}
	}
	
	public static void printStackTrace(Class<?> clazz, ConsoleManager cm, Throwable ex) {
		if (cm==null) {
			System.err.println(toString(ex));
		} else {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, clazz, "method_name", ex, "");
		}
	}
	
	public static void printStackTrace(Class<?> clazz, Throwable ex) {
		System.err.println(toString(ex));
	}
	
	public static void printStackTrace(Throwable ex) {
		System.err.println(toString(ex));
	}
	
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
		//sw.close(); 
		String str = sw.toString();
		if (StringUtil.isEmpty(str))
			// be sure something useful gets returned
			str = ex.getClass().toString();
		return str;
	}

	private ConsoleManagerUtil() {}
}
