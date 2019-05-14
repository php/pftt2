package com.mostc.pftt.model.core;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.lang.SystemUtils;

import com.github.mattficken.io.ArrayUtil;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.scenario.FileSystemScenario;

/** A PHP INI is the configuration for PHP.
 * 
 * It is composed of one or more directives which may have one or more values.
 * 
 * Important directives include:
 * -INCLUDE_PATH - directories to use when looking for a php file to include (module, etc...)
 * -EXTENSION_DIR - directory to look for extensions
 * -EXTENSION - specifies the DLLs or SOs (by name) to load as dynamic extensions from EXTENSION_DIR
 * 
 * @author Matt Ficken
 *
 */

public class PhpIni {
	// directives
	public static final String INCLUDE_PATH = "include_path";
	public static final String EXTENSION = "extension";
	public static final String ZEND_EXTENSION = "zend_extension";
	public static final String EXTENSION_DIR = "extension_dir";
	public static final String OUTPUT_HANDLER = "output_handler";
	public static final String OPEN_BASEDIR = "open_basedir";
	public static final String SAFE_MODE = "safe_mode";
	public static final String DISABLE_DEFS = "disable_defs";
	public static final String OUTPUT_BUFFERING = "output_buffering";
	public static final String ERROR_REPORTING = "error_reporting";
	public static final String DISPLAY_ERRORS = "display_errors";
	public static final String DISPLAY_STARTUP_ERRORS = "display_startup_errors";
	public static final String LOG_ERRORS = "log_errors";
	public static final String HTML_ERRORS = "html_errors";
	public static final String TRACK_ERRORS = "track_errors";
	public static final String REPORT_MEMLEAKS = "report_memleaks";
	public static final String REPORT_ZEND_DEBUG = "report_zend_debug";
	public static final String DOCREF_ROOT = "docref_root";
	public static final String DOCREF_EXT = "docref_ext";
	public static final String ERROR_PREPEND_STRING = "error_prepend_string";
	public static final String ERROR_APPEND_STRING = "error_append_string";
	public static final String AUTO_PREPEND_FILE = "auto_prepend_file";
	public static final String AUTO_APPEND_FILE = "auto_append_file";
	public static final String MAGIC_QUOTES_RUNTIME = "magic_quotes_runtime";
	public static final String IGNORE_REPEATED_ERRORS = "ignore_repeated_errors";
	public static final String PRECISION = "precision";
	public static final String UNICODE_RUNTIME_ENCODING = "unicode.runtime_encoding";
	public static final String UNICODE_SCRIPT_ENCODING = "unicode.script_encoding";
	public static final String UNICODE_OUTPUT_ENCODING = "unicode.output_encoding";
	public static final String UNICODE_FROM_ERROR_MODE = "unicode.from_error_mode";
	public static final String SESSION_AUTO_START = "session.auto_start";
	public static final String SYS_TEMP_DIR = "sys_temp_dir";
	public static final String ON = "On";
	public static final String OFF = "Off";
	public static final String UTF_8 = "UTF-8";
	public static final String ISO_8859_1 = "ISO-8859-1";
	public static final String U_INVALID_SUBSTITUTE = "U_INVALID_SUBSTITUTE";
	public static final String DOT_HTML = ".html";
	public static final String E_ALL_NOTICE_WARNING = "E_ALL | E_NOTICE | E_WARNING";
	public static final String E_ALL_STRICT_DEPRECATED = "E_ALL | E_STRICT | E_DEPRECATED";
	//
	private static String dllName(String name) {
		// FUTURE macos x and solaris support
		return LocalHost.isLocalhostWindows() ? "php_" + name + ".dll" : name + ".so";
	}
	// names for DLL or SO for dynamically loaded extensions
	public static final String EXT_BZ2 = dllName("bz2");
	public static final String EXT_COM_DOTNET = dllName("com_dotnet");
	public static final String EXT_CURL = dllName("curl");
	public static final String EXT_ENCHANT = dllName("enchant");
	public static final String EXT_FILEINFO = dllName("fileinfo");
	public static final String EXT_GD2 = dllName("gd2");
	public static final String EXT_GETTEXT = dllName("gettext");
	public static final String EXT_GMP = dllName("gmp");
	public static final String EXT_INTL = dllName("intl");
	public static final String EXT_IMAP = dllName("imap");
	public static final String EXT_LDAP = dllName("ldap");
	public static final String EXT_MBSTRING = dllName("mbstring");
	public static final String EXT_EXIF = dllName("exif");
	public static final String EXT_MYSQL = dllName("mysql");
	public static final String EXT_MYSQLI = dllName("mysqli");
	public static final String EXT_OPENSSL = dllName("openssl");
	public static final String EXT_PDO_ODBC = dllName("pdo_odbc");
	public static final String EXT_PDO_MYSQL = dllName("pdo_mysql");
	public static final String EXT_PDO_PGSQL = dllName("pdo_pgsql");
	public static final String EXT_PDO_SQLITE = dllName("pdo_sqlite");
	public static final String EXT_PGSQL = dllName("pgsql");
	public static final String EXT_SHMOP = dllName("shmop");
	public static final String EXT_SOAP = dllName("soap");
	public static final String EXT_SOCKETS = dllName("sockets");
	public static final String EXT_SQLITE3 = dllName("sqlite3");
	public static final String EXT_TIDY = dllName("tidy");
	public static final String EXT_XMLRPC = dllName("xmlrpc");
	public static final String EXT_XSL = dllName("xsl");
	public static final String EXT_SODIUM = dllName("sodium");
	public static final String EXT_ZEND_TEST = dllName("zend_test");
	//
	//
	private final HashMap<String, ArrayList<String>> ini_map;
	private SoftReference<PhpIni> ext_ini;
	private SoftReference<String> ini_str, cli_arg;
	public boolean is_default = false;
	
	public PhpIni() {
		ini_map = new HashMap<String, ArrayList<String>>();
	}
	
	public PhpIni(String ini_str) {
		this(ini_str, "", "");
		// "" => replace {PWD} with "" 
	}
	
	private PhpIni(HashMap<String, ArrayList<String>> ini_map) {
		this.ini_map = ini_map;
	}
	
	@Override
	public PhpIni clone() {
		@SuppressWarnings("unchecked")
		PhpIni o = new PhpIni((HashMap<String,ArrayList<String>>)this.ini_map.clone());
		o.is_default = this.is_default;
		o.ext_ini = this.ext_ini;
		o.ini_str = this.ini_str;
		o.cli_arg = this.cli_arg;
		return o;
	}
	
	static final Pattern PAT_PWD = Pattern.compile("\\{PWD\\}");
	static final Pattern PAT_TMP = Pattern.compile("\\{TMP\\}");
	static final Pattern PAT_BS = Pattern.compile("\\\\");
	static final Pattern PAT_FS = Pattern.compile("/");
	public PhpIni(String ini_str, String pwd, String tmp) {
		this();
		if (pwd!=null&&ini_str.contains("{PWD}")) {
			if(SystemUtils.IS_OS_WINDOWS)
			{
				// CRITICAL: ensure that correct \\s are used for paths on Windows
				String escapedPwd = StringUtil.replaceAll(PAT_BS, "\\\\\\\\", pwd);
				ini_str = StringUtil.replaceAll(PAT_PWD, escapedPwd, ini_str);
			}
			else
			{
				ini_str = StringUtil.replaceAll(PAT_PWD, pwd, ini_str);
			}
		}
		if (tmp!=null&&ini_str.contains("{TMP}")) {
			if(SystemUtils.IS_OS_WINDOWS)
			{
				// CRITICAL: ensure that correct \\s are used for paths on Windows
				String escapedTmp = StringUtil.replaceAll(PAT_BS, "\\\\\\\\", tmp);
				ini_str = StringUtil.replaceAll(PAT_TMP, escapedTmp, ini_str);
			}
			else
			{
				ini_str = StringUtil.replaceAll(PAT_TMP, tmp, ini_str);
			}
		}
		// read ini string, line by line
		for (String line : StringUtil.splitLines(ini_str)) {
			if (line.length()==0||line.startsWith(";"))
				// comment line, ignore it
				continue;
			
			int ini_i = line.indexOf("=");
			if (ini_i!=-1) {
				String ini_name = line.substring(0, ini_i).trim();
				String ini_value = ini_i+1>=line.length() ? StringUtil.EMPTY : line.substring(ini_i+1).trim();
				putMulti(ini_name, ini_value);
			}
		}
		this.ini_str = new SoftReference<String>(ini_str);
	}
		
	/** add the path to the include path (if not already present)
	 * 
	 * @param host
	 * @param path
	 */
	public void addToIncludePath(Host host, String path) {
		String c;
		if (ini_map.containsKey(INCLUDE_PATH)) {
			c = get(INCLUDE_PATH);
			c += host.mPathsSeparator() + path;
		} else {
			c = path;
		}
		putSingle(INCLUDE_PATH, c);
	}
	
	/** checks if the extension is enabled in this PhpIni.
	 * 
	 * automatically uses the correct name format for the given host (adds php_ and .dll for Windows)
	 *  
	 *  Note: this does NOT check static/builtin extensions for this build. For that, check PhpBuild#isExtensionEnabled
	 *  
	 *  @see PhpBuild#isExtensionEnabled
	 * @param host
	 * @param build
	 * @param dll_name
	 * @return
	 */
	public boolean hasExtension(Host host, PhpBuild build, String dll_name) {
		return _hasExtension(host, build, dll_name) || ( !dll_name.startsWith("php_") && _hasExtension(host, build, dllName(host, dll_name)));
	}
	
	protected String dllName(Host host, String name) {
		// FUTURE macos x and solaris support
		return host.isWindows() ? "php_" + name + ".dll" : name + ".so";
	}
	
	protected boolean _hasExtension(Host host, PhpBuild build, String dll_name) {
		return host.mExists(getExtensionDir(build) + "/"+dll_name);
	}
	
	/** adds the extension to this PhpIni.
	 * 
	 * automatically uses the correct name format for the given host (adds php_ and .dll for Windows)
	 * 
	 * @param host
	 * @param build
	 * @param dll_name
	 * @return TRUE if extension is added to the INI, false if DLL not found
	 */
	public boolean addExtension(Host host, PhpBuild build, String dll_name) {
		if (!_hasExtension(host, build, dll_name))
			dll_name = dllName(dll_name);
		if (_hasExtension(host, build, dll_name)) {
			addExtension(dll_name);
			return true;
		} else {
			return false;
		}
	}
	
	/** adds extension to INI for this build and returns true if the build was able to load it
	 * 
	 * @param cm
	 * @param fs
	 * @param host
	 * @param type
	 * @param build
	 * @param dll_name
	 * @return
	 */
	public boolean addExtensionAndCheck(ConsoleManager cm, FileSystemScenario fs, AHost host, ESAPIType type, PhpBuild build, String dll_name) {
		if (addExtension(host, build, dll_name)) {
			try {
				if (build.isExtensionEnabled(cm, fs, host, type, this, dll_name))
					return true;
				else if (cm!=null)
					cm.println(EPrintType.CLUE, getClass(), "Extension DLL/SO could not be loaded: "+dll_name);
			} catch ( Exception ex ) {
				if (cm!=null)
					cm.println(EPrintType.CLUE, getClass(), "Unable to tell if DLL/SO was loaded: "+dll_name);
			}
		} else if (cm!=null) {
			cm.println(EPrintType.CLUE, getClass(), "Extension DLL/SO not found: "+dll_name);
		}
		return false;
	}
	
	public void addExtension(String dll_name) {
		putMulti(EXTENSION, dll_name);
	}
	
	public void addExtensions(Host host, PhpBuild build, String...dll_names) {
		for (String dll_name:dll_names)
			addExtension(host, build, dll_name);
	}
	
	/** replaces all directives in this PhpIni that match the given PhpIni with the values from 
	 * the given PhpIni (all current values in those directives in this, are overwritten).
	 * 
	 * @param ini
	 * @see #appendAll
	 */
	public void replaceAll(PhpIni ini) {
		// TODO temp this.ini_map.putAll(ini.ini_map);
		System.out.println("270 "+this);
		for ( String dir : ini.getDirectives() ) {
			System.out.println("272 "+dir);
			this.setMulti(dir, ini.getMulti(dir));
		}
		System.out.println("275 "+this);
		/*for ( String dir : ini.getDirectives() ) {
			if (dir.equals("date.timezone")) {
				System.exit(0);
			}
		}
		if (ini.ini_str!=null) {
			String a = ini.ini_str.get();
			if (a.contains("date.timezone"))
				System.exit(0);
		}*/
		is_default = false;
	}
	
	/** appends all values from all directives from the given PhpIni to this PhpIni
	 * 
	 * @param ini
	 */
	public void appendAll(PhpIni ini) {
		for (String directive:ini.getDirectives()) {
			for (String value:ini.getMulti(directive))
				this.putMulti(directive, value);
		}
		is_default = false;
	}
	
	public int countDirectives() {
		return this.ini_map.size();
	}
	
	public int countValues(String directive) {
		ArrayList<String> values = ini_map.get(directive);
		return values == null ? 0 : values.size();
	}
	
	public int countAllValues() {
		int count = 0;
		for ( ArrayList<String> values : ini_map.values())
			count += values.size();
		return count;
	}
	
	public boolean isEmpty() {
		return this.ini_map.isEmpty();
	}
	
	public void putSingle(String directive, int value) {
		putSingle(directive, Integer.toString(value));
	}
	
	/** sets (replacing) the value for the given directive.
	 * 
	 * all current values are removed
	 * 
	 * @param directive
	 * @param value
	 * @see #setExtensionDir
	 */
	public void putSingle(String directive, String value) {
		doPutSingle(directive, value);
	}
	
	protected void doPutSingle(String directive, String value) {
		if (value==null)
			value = "";
		ArrayList<String> values = new ArrayList<String>(1);
		values.add(value);
		ini_map.put(directive, values);
		cli_arg = ini_str = null;
		is_default = false;
	}

	public void putMulti(String directive, int value) {
		putMulti(directive, Integer.toString(value));
	}
	
	/** adds the given value for the given directive (ignoring duplicate values)
	 * 
	 * @param directive
	 * @param value
	 * @see #putSingle
	 * @see #addExtension - should use this for extensions instead
	 * @see #addToIncludePath - should use this for include path parts instead
	 * 
	 */
	public void putMulti(String directive, String value) {
		ArrayList<String> values = ini_map.get(directive);
		if (values==null) {
			if (value==null)
				value = "";
			values = new ArrayList<String>(1);
			values.add(value);
			ini_map.put(directive, values);
		} else if (value==null) {
			// allow at most 1 blank value
			return;
		} else if (!values.contains(value)) {
			values.add(value);
		}
		is_default = false;
	}
	
	/** replaces directive with given values
	 * 
	 * @param directive
	 * @param values
	 */
	public void setMulti(String directive, String[] values) {
		if (values==null) {
			remove(directive);
		} else {
			ArrayList<String> new_value = ArrayUtil.toList(values);
			ini_map.put(directive, new_value);
			this.cli_arg = this.ini_str = null;
		}
		is_default = false;
	}

	/** removes all values of directive from this ini.
	 * 
	 * @param directive
	 */
	public void remove(String directive) {
		ini_map.remove(directive);
		cli_arg = ini_str = null;
		is_default = false;
	}
	
	/** removes value from directive if present (other values for directive are not affected)
	 * 
	 * @param directive
	 * @param value
	 */
	public void removeValue(String directive, String value) {
		ArrayList<String> values = ini_map.get(directive);
		if (values!=null) {
			cli_arg = ini_str = null;
			is_default = false;
			
			while (values.remove(value)) {};
		}
	}
	
	@Nullable
	public String getIncludePath() {
		return get(INCLUDE_PATH);
	}
	
	@Nullable
	public String getExtensionDir() {
		return get(EXTENSION_DIR);
	}
	
	public String getExtensionDir(PhpBuild build) {
		String ext_dir = getExtensionDir();
		if (build != null && StringUtil.isEmpty(ext_dir))
			ext_dir = build.getDefaultExtensionDir();
		return ext_dir;
	}
	
	public void removeIncludePath() {
		remove(INCLUDE_PATH);
	}
	
	public void setExtensionDir(String ext_dir) {
		putSingle(EXTENSION_DIR, ext_dir);
	}
	
	/** returns the DLL or SO name for all dynamically loaded extensions in this PhpIni or NULL
	 * 
	 * @return
	 */
	@Nullable
	public String[] getEnabledExtensions() {
		return getMulti(EXTENSION);
	}
	
	/** returns all values for this directive.
	 * 
	 * most directives only have 1 value, but a few will have multiple values (ex: EXTENSION)
	 * 
	 * @param directive
	 * @return
	 */
	@Nullable
	public String[] getMulti(String directive) {
		ArrayList<String> values = ini_map.get(directive);
		return values == null ? null : values.toArray(new String[values.size()]);
	}
	
	@Nullable
	public String get(String directive) {
		ArrayList<String> values = ini_map.get(directive);
		return values == null || values.isEmpty() ? null : values.get(0);
	}

	public Set<String> getDirectives() {
		return ini_map.keySet();
	}
	
	/** checks if the given directive is explicitly set to ON.
	 * 
	 * if value is blank or directive missing, etc... this will always return false. never assumes ON for any directive.
	 * 
	 * @param directive
	 * @return
	 */
	public boolean isOn(String directive) {
		return StringUtil.equalsIC(get(directive), ON);
	}
	
	/** checks if the given directive is explicitly set to OFF.
	 * 
	 * if value is blank or directive missing, etc... this will always return false.
	 * 
	 * @param directive
	 * @return
	 */
	public boolean isOff(String directive) {
		return StringUtil.equalsIC(get(directive), OFF);
	}
	
	public boolean containsKey(String directive) {
		return ini_map.containsKey(directive);
	}
	
	/** checks if the value of the given directive exactly matches the given value (case sensitive)
	 * 
	 * @param directive
	 * @param value
	 * @return
	 */
	public boolean containsExact(String directive, String value) {
		ArrayList<String> values = ini_map.get(directive);
		if (values==null||values.isEmpty())
			return false;
		for (String a : values) {
			if (a.equals(value))
				return true;
		}
		return false;
	}
	
	/** checks if the value of the given directive contains the given value (ignoring case)
	 * 
	 * @param directive
	 * @param value
	 * @return
	 */
	public boolean containsPartial(String directive, String value) {
		ArrayList<String> values = ini_map.get(directive);
		if (values==null||values.isEmpty())
			return false;
		value = value.toLowerCase();
		for (String a : values) {
			if (a.toLowerCase().contains(value))
				return true;
		}
		return false;
	}
	
	public boolean hasExtension(String ext_name) {
		return containsPartial(EXTENSION, ext_name);
	}
	
	public boolean isDefault() {
		return is_default;
	}
	
	@Override
	public String toString() {
		String ini_str;
		if (this.ini_str!=null) {
			ini_str = this.ini_str.get();
			if (ini_str!=null)
				return ini_str;
		}
		
		StringBuilder sb = new StringBuilder(1024);
		
		// alphabetize directives (to make it more human readable. php doesn't care)
		ArrayList<String> directives = new ArrayList<String>(ini_map.size());
		directives.addAll(ini_map.keySet());
		Collections.sort(directives);
		
		for ( String directive : directives ) {
			for ( String value : ini_map.get(directive) ) {
				sb.append(directive);
				sb.append('=');
				if (value.contains("=")||value.contains("&")||value.contains("~"))
					value = StringUtil.ensureQuoted(value);
				sb.append(value);
				sb.append('\n');
			}
		}
		ini_str = sb.toString();
		this.ini_str = new SoftReference<String>(ini_str);
		return ini_str;
	}
	
	@Override
	public int hashCode() {
		return ini_map.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		return o == this || ( o instanceof PhpIni && equals((PhpIni)o) );
	}
	
	public boolean equals(PhpIni ini) {
		return this.ini_map.equals(ini.ini_map);
	}
	
	/** returns a PhpIni that only has the EXTENSION and EXTENSION_DIR directives from this PhpIni.
	 * 
	 * No other directives are copied.
	 * 
	 * Useful if you want to setup php to use the same extensions but without changing other directives.
	 * 
	 * @see #replaceAll 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public PhpIni getExtensionsOnly() {
		PhpIni ext_ini;
		if (this.ext_ini!=null) {
			ext_ini = this.ext_ini.get();
			if (ext_ini!=null)
				return ext_ini;
		}
		
		ext_ini = new ReadOnlyPhpIni();
		this.ext_ini = ext_ini.ext_ini = new SoftReference<PhpIni>(ext_ini);
		String ext_dir = get(EXTENSION_DIR);
		if (ext_dir!=null)
			ext_ini.doPutSingle(EXTENSION_DIR, ext_dir);
		ArrayList<String> values = ini_map.get(EXTENSION);
		if (values!=null)
			ext_ini.ini_map.put(EXTENSION, (ArrayList<String>)values.clone());
		return ext_ini;
	}

	/** generates -d console arguments to pass these INI directives to php.exe or php-cgi.exe
	 * 
	 * NOTE: use -n console option to tell php.exe to ignore an .INI file, otherwise it will load the .INI file
	 * and override only the directives in this string (other directives from file will remain!)
	 * 
	 * @param host
	 * @return
	 */
	public String toCliArgString(Host host) {
		if (cli_arg!=null) {
			String cli_arg_str = cli_arg.get();
			if (cli_arg_str!=null)
				return cli_arg_str;
		}
		//
		StringBuilder sb = new StringBuilder(256);
		for ( String directive : getDirectives()) {
			for ( String value : getMulti(directive) ) {
				if (value==null)
					continue; // allow "" empty values though
				
				// CRITICAL: escape these characters in the INI
				value = StringUtil.replaceAll(PAT_bs, "\\\\\"", StringUtil.replaceAll(PAT_amp, "\\\\&", StringUtil.replaceAll(PAT_pipe, "\\\\|", value)));
				 
				// CRITICAL: in a windows batch script % is replaced with the command to execute.
				//           need to escape this value.
				if (host.isWindows())
					value = StringUtil.replaceAll(PAT_per, "\\%\\%", value);
				
				// without quoting only when needed, will get parse errors from php (blocking winpopups on Windows)
				if (value.contains(" ")) {
					sb.append(" -d \"");
					sb.append(directive);
					sb.append("=");
					sb.append(value);
					sb.append("\"");	
				} else {
					sb.append(" -d ");
					sb.append(directive);
					sb.append("=");
					sb.append(value);
				}
			}
		}
		String cli_arg_str = sb.toString();
		cli_arg = new SoftReference<String>(cli_arg_str);
		return cli_arg_str;
	} // end public String toCliArgString
	static final Pattern PAT_bs = Pattern.compile("\"");
	static final Pattern PAT_amp = Pattern.compile("\\&");
	static final Pattern PAT_pipe = Pattern.compile("\\|");
	static final Pattern PAT_per = Pattern.compile("\\%");
	
	public boolean containsAny(String ...directives) {
		for ( String d : directives ) {
			if (containsKey(d))
				return true;
		}
		return false;
	}

	/** returns TRUE if this PhpIni includes all the same directives and values from o_ini. Unlike #equals, will still return
	 * TRUE if this PhpIni has additional directives. 
	 * 
	 * For multi-value directives, the values do not have to be in the same position or order.
	 * 
	 * @param o_ini
	 * @return
	 */
	public boolean includes(PhpIni o_ini) {
		for (String o_directive : o_ini.getDirectives()) {
			for (String o_value : o_ini.getMulti(o_directive)) {
				if (!containsExact(o_directive, o_value)) {
					return false;
				}
			}
		}
		return true;
	}
	
} // end public class PhpIni
