package com.mostc.pftt.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.codehaus.groovy.control.CompilationFailedException;
import org.columba.ristretto.smtp.SMTPProtocol;

import com.github.mattficken.io.IOUtil;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

/** Handles loading a configuration (hosts, scenarios, credentials, etc...) from a groovy script.
 * 
 * This allows for loading scenarios and hosts without creating a special configuration format, its just executable code.
 * 
 * @see Config#getHosts
 * @see Config#loadConfigFromFile
 * 
 * An example configuration file:
 * def hosts() {
 * 		[
 * 			// connect to a host using ssh
 *         //
 *         // can test multiple operating systems, one host for each OS (so multiple hosts)
 * 			new SSHHost("192.168.1.1", "administrator", "password01!")
 * 		]
 * }
 * def scenario_sets() {
 * 		[
 * 			// provide address of a MySQL server
 * 			new MySQLScenario()
 * 		]
 * }
 * def configure_smtp(def smtp_client) {
 * 		// specify smtp server and credentials 
 * }
 * def configure_ftp_client(def ftp_client) {
 * 		// specify ftp server and credentials
 * }
 * 
 * @author Matt Ficken
 * 
 */

public final class Config {
	public static final String HOSTS_METHOD = "hosts";
	public static final String SCENARIO_SETS_METHOD = "scenario_sets";
	public static final String SCENARIOS_METHOD = "scenarios";
	public static final String CONFIGURE_SMTP_METHOD = "configure_smtp";
	public static final String CONFIGURE_FTP_CLIENT_METHOD = "configure_ftp_client";
	//
	protected final LinkedList<Host> hosts;
	protected final LinkedList<ScenarioSet> scenario_sets;
	protected GroovyObject configure_smtp_method, configure_ftp_client_method;
	
	protected Config() {
		hosts = new LinkedList<Host>();
		scenario_sets = new LinkedList<ScenarioSet>();
	}
	
	public List<Host> getHosts() {
		return hosts;
	}
	
	/** returns the ScenarioSets from this configuration
	 * 
	 * this is the ScenarioSets defined in the configuration file(s)'s 
	 * scenario_sets() functions AND all valid permutations of Scenarios
	 * provided by the scenarios() functions.
	 * 
	 * @return
	 */
	public List<ScenarioSet> getScenarioSets() {
		return scenario_sets;
	}
	
	public boolean configureSMTP(SMTPProtocol smtp) {
		return configureSMTP(null, smtp);
	}
	
	public boolean configureSMTP(ConsoleManager cm, SMTPProtocol smtp) {
		if (configure_smtp_method==null)
			return false;
		try {
			configure_smtp_method.invokeMethod(CONFIGURE_SMTP_METHOD, smtp);
			
			return true;
		} catch ( Exception ex ) {
			if (cm==null)
				ex.printStackTrace();
			else
				cm.printStackTrace(ex);
		}
		return false;
	}
	
	public boolean configureFTPClient(FTPClient ftp) {
		return configureFTPClient(null, ftp);
	}
	
	public boolean configureFTPClient(ConsoleManager cm, FTPClient ftp) {
		if (configure_ftp_client_method==null)
			return false;
		try {
			configure_ftp_client_method.invokeMethod(CONFIGURE_FTP_CLIENT_METHOD, ftp);
			
			return true;
		} catch ( Exception ex ) {
			if (cm==null)
				ex.printStackTrace();
			else
				cm.printStackTrace(ex);
		}
		return false;
	}
	
	public static Config loadConfigFromStreams(ConsoleManager cm, InputStream... ins) throws CompilationFailedException, InstantiationException, IllegalAccessException, IOException {
		GroovyClassLoader loader = new GroovyClassLoader(Config.class.getClassLoader());
		
		Config config = new Config();
		
		// don't load default scenarios. configuration file(s) completely replace them (not merged)
		LinkedList<Scenario> scenarios = new LinkedList<Scenario>();
		
		// load each configuration streams
		GroovyObject go;
		Class<?> clazz;
		int i=1;
		for (InputStream in : ins) {
			clazz = loader.parseClass(importString(IOUtil.toString(in, IOUtil.QUARTER_MEGABYTE)));
			
			go = (GroovyObject) clazz.newInstance();
			
			// call methods in file to get configuration (hosts, etc...)
			loadObjectToConfig(cm, config, go, scenarios, "InputStream #"+(i++)+" ("+in+")");
		}
		
		return loadConfigCommon(cm, scenarios, config);
	} // end public static Config loadConfigFromStreams
	
	public static Config loadConfigFromFiles(ConsoleManager cm, File... files) throws CompilationFailedException, InstantiationException, IllegalAccessException, IOException {
		GroovyClassLoader loader = new GroovyClassLoader(Config.class.getClassLoader());
		
		Config config = new Config();
		
		// don't load default scenarios. configuration file(s) completely replace them (not merged)
		LinkedList<Scenario> scenarios = new LinkedList<Scenario>();
		
		// load each configuration file
		GroovyObject go;
		Class<?> clazz;
		for (File file : files) {
			clazz = loader.parseClass(importString(IOUtil.toString(new FileInputStream(file), IOUtil.QUARTER_MEGABYTE)));
			
			go = (GroovyObject) clazz.newInstance();
			
			// call methods in file to get configuration (hosts, etc...)
			loadObjectToConfig(cm, config, go, scenarios, file.getAbsolutePath());
		}
		
		return loadConfigCommon(cm, scenarios, config);
	} // end public static Config loadConfigFromFiles
	
	protected static String importString(String code) {
		// a hack to import common classes for configuration files (XXX do this a better way)
		StringBuilder sb = new StringBuilder(128+code.length());
		// import all standard Scenarios and Host types
		sb.append("import ");sb.append(Scenario.class.getPackage().getName());sb.append(".*;\n");
		sb.append("import ");sb.append(Host.class.getPackage().getName());sb.append(".*;\n");
		sb.append("import ");sb.append(SMTPProtocol.class.getName());sb.append(";\n");
		sb.append("import ");sb.append(FTPClient.class.getName());sb.append(";\n");
		sb.append(code);
		return sb.toString();
	}
	
	protected static Config loadConfigCommon(ConsoleManager cm, LinkedList<Scenario> scenarios, Config config) {
		// configs may specify individual scenarios, in addition or in place of whole sets
		//
		// permute the given individual scenarios and add them to the list of scenario sets
		for (ScenarioSet scenario_set : ScenarioSet.permuteScenarioSets(scenarios) ) {
			if (!config.scenario_sets.contains(scenario_set))
				config.scenario_sets.add(scenario_set);
		}
		// make sure all scenario sets have a filesystem and SAPI
		for (ScenarioSet scenario_set : config.scenario_sets )
			ScenarioSet.ensureSetHasFileSystemAndSAPI(scenario_set);
		//
		
		if (cm!=null) {
			// report progress
			if (config.hosts.size()>0) {
				cm.println(Config.class, "Loaded "+config.hosts.size()+" hosts");
			}
			if (config.scenario_sets.size()>0) {
				cm.println(Config.class, "Loaded "+config.scenario_sets.size()+" Scenario-Sets");
			}
		}
		
		
		return config;
	} // end protected static Config loadConfigCommon
	
	protected static void loadObjectToConfig(ConsoleManager cm, Config config, GroovyObject go, List<Scenario> scenarios, String file_name) {
		Object ret;
		try {
			ret = go.invokeMethod(HOSTS_METHOD, null);
			if (ret instanceof List) {
				for (Object o : (List<?>)ret) {
					if (o instanceof Host) {
						if (!config.hosts.contains(o))
							config.hosts.add((Host)o);
					} else {
						cm.println("Config", "List returned by hosts() must only contain Host objects, not: "+o.getClass()+" see: "+file_name);
					}
				}
			} else {
				cm.println("Config", "hosts() must return List of Hosts, not: "+(ret==null?"null":ret.getClass())+" see: "+file_name);
			}
		} catch ( Exception ex ) {
		}
		try {
			ret = go.invokeMethod(SCENARIO_SETS_METHOD, null);
			if (ret instanceof List) {
				for (Object o : (List<?>)ret) {
					if (o instanceof ScenarioSet) {
						if (!config.scenario_sets.contains(o))
							config.scenario_sets.add((ScenarioSet)o);
					} else {
						cm.println("Config", "List returned by scenario_sets() must only contain ScenarioSet objects, not: "+o.getClass()+" see: "+file_name);
					}
				}
			} else {
				cm.println("Config", "scenario_sets() must return List of ScenarioSets, not: "+(ret==null?"null":ret.getClass())+" see: "+file_name);
			}
		} catch ( Exception ex ) {
		}
		try {
			ret = go.invokeMethod(SCENARIOS_METHOD, null);
			if (ret instanceof List) {
				for (Object o : (List<?>)ret) {
					if (o instanceof Scenario) {
						if (!scenarios.contains(o))
							scenarios.add((Scenario)o);
					} else {
						cm.println("Config", "List returned by scenarios() must only contain Scenario objects, not: "+o.getClass()+" see: "+file_name);
					}
				}
			} else {
				cm.println("Config", "scenarios() must return List of Scenarios, not: "+(ret==null?"null":ret.getClass())+" see: "+file_name);
			}
		} catch ( Exception ex ) {
		}
		try {
			if (go.getProperty(CONFIGURE_SMTP_METHOD)!=null) {
				if (config.configure_smtp_method!=null)
					cm.println("Config", CONFIGURE_SMTP_METHOD+"() overriden by : "+file_name);
				config.configure_smtp_method = go;
			}
		} catch ( Exception ex ) {
		}
		try {
			if (go.getProperty(CONFIGURE_FTP_CLIENT_METHOD)!=null) {
				if (config.configure_ftp_client_method!=null)
					cm.println("Config", CONFIGURE_FTP_CLIENT_METHOD+"() overriden by : "+file_name);
				config.configure_ftp_client_method = go;
			}
		} catch ( Exception ex ) {
		}
	} // end protected static void loadObjectToConfig
	
} // end public final class ConfigUtil
