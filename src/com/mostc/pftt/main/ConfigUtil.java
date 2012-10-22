package com.mostc.pftt.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.codehaus.groovy.control.CompilationFailedException;
import org.columba.ristretto.smtp.SMTPProtocol;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.scenario.ScenarioSet;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

/** Handles loading a configuration (hosts, scenarios, credentials, etc...) from a groovy script.
 * 
 * This allows for loading scenarios and hosts without creating a special configuration format, its just executable code.
 * 
 * @see ConfigUtil#getHosts
 * @see ConfigUtil#loadConfigFromFile
 * 
 * An example configuration file:
 * hosts() {
 * 		[
 * 			// connect to a host using ssh
 *         //
 *         // can test multiple operating systems, one host for each OS (so multiple hosts)
 * 			new SSHHost("192.168.1.1", "administrator", "password01!")
 * 		]
 * }
 * scenario_sets() {
 * 		[
 * 			// provide address of a MySQL server
 * 			new MySQLScenario()
 * 		]
 * }
 * configure_smtp(def smtp_client) {
 * 		// specify smtp server and credentials 
 * }
 * configure_ftp_client(def ftp_client) {
 * 		// specify ftp server and credentials
 * }
 * 
 * @author Matt Ficken
 * 
 */

public final class ConfigUtil {
	public static final String HOSTS_METHOD = "hosts";
	public static final String SCENARIO_SETS_METHOD = "scenario_sets";
	public static final String CONFIGURE_SMTP_METHOD = "configure_smtp";
	public static final String CONFIGURE_FTP_CLIENT_METHOD = "configure_ftp_client";
	
	@SuppressWarnings("unchecked")
	public static List<Host> getHosts(GroovyObject gobj) {
		return (List<Host>) gobj.invokeMethod(HOSTS_METHOD, null);
	}
	
	@SuppressWarnings("unchecked")
	public static List<ScenarioSet> getScenarioSets(GroovyObject gobj) {
		return (List<ScenarioSet>) gobj.invokeMethod(SCENARIO_SETS_METHOD, null);
	}
	
	public static void configureSMTPProtocol(GroovyObject gobj, SMTPProtocol smtp) {
		gobj.invokeMethod(CONFIGURE_SMTP_METHOD, smtp);
	}
	
	public static void configureFTPClient(GroovyObject gobj, FTPClient ftp) {
		gobj.invokeMethod(CONFIGURE_FTP_CLIENT_METHOD, ftp);
	}
	
	@SuppressWarnings("deprecation")
	public static GroovyObject loadConfigFromStream(InputStream in) throws CompilationFailedException, InstantiationException, IllegalAccessException {
		GroovyClassLoader gcl = new GroovyClassLoader(ConfigUtil.class.getClassLoader());
		return (GroovyObject) gcl.parseClass(in).newInstance();
	}
	
	public static GroovyObject loadConfigFromFile(File file) throws CompilationFailedException, InstantiationException, IllegalAccessException, IOException {
		GroovyClassLoader gcl = new GroovyClassLoader(ConfigUtil.class.getClassLoader());
		return (GroovyObject) gcl.parseClass(file).newInstance();		
	}
	
	private ConfigUtil() {}
	
} // end public final class ConfigUtil
