package com.mostc.pftt.scenario;

import java.io.IOException;
import java.util.Collection;

import javax.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import com.github.mattficken.Overridable;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptSourceTestPack;
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
 * @see SAPIScenario - provides the SAPI that a PhpBuild is run under (Apache-ModPHP, CLI, etc...)
 * @see INIScenario - edits/adds to the INI used to run a PhptTestCase
 * @see FileSystemScenario - provides the filesystem a PhpBuild is run on (local, remote, etc...)
 * 
 * @author Matt Ficken
 *
 */

public abstract class Scenario {

	// TODO
	@Overridable
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup setup, ESAPIType type, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		return false;
	}
	
	@Overridable
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return getClass();
	}
	
	/** Provide DIRECTORIES containing debugging symbols to Symbolic Debugger.
	 * 
	 * Ex: this is used to provide Apache debug symbols to WinDebug(on Windows).
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param debug_path
	 */
	@Overridable
	public void addToDebugPath(ConsoleManager cm, AHost host, PhpBuild build, Collection<String> debug_path) {
		
	}
	
	/** does #setup need to be called before this Scenario can be used?
	 * 
	 * @param layer
	 * @return
	 */
	@Overridable
	public boolean setupRequired(EScenarioSetPermutationLayer layer) {
		return !isPlaceholder(layer);
	}
	
	@Overridable
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return SETUP_SUCCESS;
	}
	public static final IScenarioSetup SETUP_SUCCESS = new SimpleScenarioSetup() {
			@Override
			public void close(ConsoleManager cm) {
				
			}
			@Override
			public String getNameWithVersionInfo() {
				return getName();
			}
			@Override
			public String getName() {
				return "Success";
			}
		};
	public static final IScenarioSetup SETUP_FAILED = null;
	
	/** TODO
	 * 
	 * @return
	 */
	public abstract String getName();
	
	/** is scenario implemented? if not calling #setup should return SETUP_FAILED
	 * 
	 * @return
	 */
	public abstract boolean isImplemented();
		
	/** TODO
	 * 
	 * @param layer
	 * @return
	 */
	@Overridable 
	public boolean isPlaceholder(@Nullable EScenarioSetPermutationLayer layer) {
		return false;
	}
	
	@Overridable
	public boolean ignoreForShortName(EScenarioSetPermutationLayer layer) {
		return isPlaceholder(layer);
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	@Overridable
	protected String processNameAndVersionInfo(String name) {
		return name;
	}

	/** TRUE if UAC (Run As Administrator or Privilege Elevation) is required when
	 * starting scenario on Windows
	 * 
	 * #start
	 * @return
	 */
	@Overridable
	public boolean isUACRequiredForStart() {
		return false;
	}
	
	// TODO
	@Overridable
	public boolean isUACRequiredForSetup() {
		return false;
	}
	
	/** TODO
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param scenario_set
	 * @param layer TODO
	 * @return
	 */
	@Overridable
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return true;
	}
	
	public final boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return isSupported(cm, host, build, scenario_set, null);
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public static final CliScenario CLI_SCENARIO = new CliScenario();
	public static final LocalFileSystemScenario LOCALFILESYSTEM_SCENARIO = new LocalFileSystemScenario();
	public static final SAPIScenario DEFAULT_SAPI_SCENARIO = CLI_SCENARIO;
	public static final FileSystemScenario DEFAULT_FILESYSTEM_SCENARIO = LOCALFILESYSTEM_SCENARIO;
	
	public static Scenario[] getAllDefaultScenarios() {
		return new Scenario[]{
				new PlainSocketScenario(),
				new NoCodeCacheScenario(),
				CLI_SCENARIO,
				LOCALFILESYSTEM_SCENARIO,
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
		if (!scenario_set.contains(CodeCacheScenario.class))
			scenario_set.add(new NoCodeCacheScenario());
		if (!scenario_set.contains(PathsScenario.class))
			scenario_set.add(new NormalPathsScenario());
		if (!scenario_set.contains(SocketScenario.class))
			scenario_set.add(new PlainSocketScenario());
		if (!scenario_set.contains(FileSystemScenario.class))
			scenario_set.add(LOCALFILESYSTEM_SCENARIO);
		if (!scenario_set.contains(SAPIScenario.class))
			scenario_set.add(CLI_SCENARIO);
		if (!scenario_set.contains(EnchantScenario.class))
			scenario_set.add(new EnchantScenario());
	}
	
	public int getApprovedInitialThreadPoolSize(AHost host, int threads) {
		return threads;
	}
	
	public int getApprovedMaximumThreadPoolSize(AHost host, int threads) {
		return threads;
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
