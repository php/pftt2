package com.mostc.pftt.scenario;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import com.github.mattficken.Overridable;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;

/** Scenario to test PHP under.
 * 
 * Often a whole set of Scenarios (@see ScenarioSet) are used together.
 * 
 * May include custom INI configuration, extensions, environment variables, etc...
 * 
 * Can be used to setup remote services and configure PHP to use them for testing PHP core or extensions.
 *
 * @see ScenarioSet
 * 
 * Important Scenario Types
 * @see AbstractSAPIScenario - provides the SAPI that a PhpBuild is run under (Apache-ModPHP, CLI, etc...)
 * @see AbstractINIScenario - edits/adds to the INI used to run a PhptTestCase
 * @see AbstractFileSystemScenario - provides the filesystem a PhpBuild is run on (local, remote, etc...)
 * 
 * @author Matt Ficken
 *
 */

public abstract class Scenario {

	@Overridable
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		return false;
	}
	
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return getClass();
	}
	
	/** Provide directories and files containing debugging symbols to Symbolic Debugger.
	 * 
	 * Ex: this is used to provide Apache debug symbols to WinDebug(on Windows).
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param debug_path
	 */
	public void addToDebugPath(ConsoleManager cm, AHost host, PhpBuild build, Collection<String> debug_path) {
		
	}
	
	public boolean prepare(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhptActiveTestPack test_pack) {
		return true;
	}
	
	public boolean setupRequired() {
		return !isPlaceholder();
	}
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return true;
	}
	public static enum EScenarioStartState {
		STARTED,
		FAILED_TO_START,
		SKIP
	}
	/** not called if #isPlaceholder is true
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param scenario_set
	 * @param _ini
	 * @return
	 */
	public EScenarioStartState start(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, PhpIni _ini) {
		return EScenarioStartState.STARTED;
	}
	/** not called if #isPlaceholder is true
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param scenario_set
	 * @param _ini
	 * @return
	 */
	public boolean stop(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, PhpIni _ini) {
		return true;
	}
	public abstract String getName();
	public abstract boolean isImplemented();
	
	/**
	 * 
	 * @return
	 */
	public String getNameWithVersionInfo() {
		return getName();
	}
	
	/** @see ScenarioSet#getENV
	 * 
	 * @param env
	 */
	public void getENV(Map<String, String> env) {
		
	}
	
	public boolean isPlaceholder() {
		return false;
	}
	
	public boolean ignoreForShortName() {
		return isPlaceholder();
	}

	public boolean hasENV() {
		return false;
	}
	
	/** TRUE if UAC (Run As Administrator or Privilege Elevation) is required when
	 * starting scenario on Windows
	 * 
	 * #start
	 * @return
	 */
	public boolean isUACRequiredForStart() {
		return false;
	}
	
	public boolean isUACRequiredForSetup() {
		return false;
	}
	
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return true;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public static final CliScenario CLI_SCENARIO = new CliScenario();
	public static final LocalFileSystemScenario LOCALFILESYSTEM_SCENARIO = new LocalFileSystemScenario();
	public static final AbstractSAPIScenario DEFAULT_SAPI_SCENARIO = CLI_SCENARIO;
	public static final AbstractFileSystemScenario DEFAULT_FILESYSTEM_SCENARIO = LOCALFILESYSTEM_SCENARIO;
	
	public static Scenario[] getAllDefaultScenarios() {
		return new Scenario[]{
				new PlainSocketScenario(),
				new NoCodeCacheScenario(),
				CLI_SCENARIO,
				LOCALFILESYSTEM_SCENARIO,
				// these database scenarios can be here because they don't require any special configuration
				// (like connecting to a database server that the user would have to setup. those scenarios
				//  have to be in configuration files (in the 'conf' directory))
				new MSAccessScenario(), // ignored if MS-Access not installed
				new SQLite3Scenario(),
				new NormalPathsScenario(),
				new EnchantScenario()
			};
	} // end public static Scenario[] getAllDefaultScenarios
	
	/** ensures ScenarioSet contains important scenarios like a file system, SAPI
	 * and code-cache
	 * 
	 * @param scenario_set
	 */
	public static void ensureContainsCriticalScenarios(ScenarioSet scenario_set) {
		if (!scenario_set.contains(AbstractCodeCacheScenario.class))
			scenario_set.add(new NoCodeCacheScenario());
		if (!scenario_set.contains(PathsScenario.class))
			scenario_set.add(new NormalPathsScenario());
		if (!scenario_set.contains(AbstractSocketScenario.class))
			scenario_set.add(new PlainSocketScenario());
		if (!scenario_set.contains(AbstractFileSystemScenario.class))
			scenario_set.add(LOCALFILESYSTEM_SCENARIO);
		if (!scenario_set.contains(AbstractSAPIScenario.class))
			scenario_set.add(CLI_SCENARIO);
		if (!scenario_set.contains(EnchantScenario.class))
			scenario_set.add(new EnchantScenario());
	}
	
	/** writes Scenario to XML
	 * 
	 * @param serial
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void serialize(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
		serial.startTag(null, "scenario");
		serial.attribute(null, "name", Host.toContext(getClass()));
		serial.endTag(null, "scenario");
	}
	
	public void parseCustom(XmlPullParser parser) {
		
	}
	
	/** parses a Scenario or Scenario subclass from xml
	 * 
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	public static Scenario parse(XmlPullParser parser) throws XmlPullParserException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		main_loop:
		while(true) {
			parser.next();
			switch(parser.getEventType()) {
			case XmlPullParser.START_TAG:
				if (parser.getName().equals("scenario")) {
					String name = parser.getAttributeValue(null, "name");
					
					Class<?> clazz = Class.forName(Scenario.class.getPackage().getName()+"."+name);
					
					Scenario scenario = (Scenario) clazz.newInstance();
					scenario.parseCustom(parser);
					return scenario;
				}
				
				break;
			case XmlPullParser.END_TAG:
				break main_loop;
			case XmlPullParser.END_DOCUMENT:
				break main_loop;
			case XmlPullParser.TEXT:
				break;
			default:
			} // end switch
		} // end while
		return null;
	} // end public static Scenario parse
	
} // end public abstract class Scenario
