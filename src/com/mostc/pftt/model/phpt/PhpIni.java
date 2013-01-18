package com.mostc.pftt.model.phpt;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.util.StringUtil;

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
	public static final String ON = "On";
	public static final String OFF = "Off";
	public static final String UTF_8 = "UTF-8";
	public static final String ISO_8859_1 = "ISO-8859-1";
	public static final String U_INVALID_SUBSTITUTE = "U_INVALID_SUBSTITUTE";
	public static final String DOT_HTML = ".html";
	public static final String E_ALL_OR_E_STRICT = "E_ALL|E_STRICT";// TODO |E_WARNING|E_PARSE|E_NOTICE|E_COMPILE_ERROR|E_RECOVERABLE_ERROR|E_ERROR|E_CORE_ERROR"; // TODO E_WARN
	//
	private static String dllName(String name) {
		// FUTURE macos x and solaris support
		return LocalHost.isLocalhostWindows() ? "php_" + name + ".dll" : name + ".so";
	}
	// names for DLL or SO for dynamically loaded extensions
	public static final String EXT_BZ2 = dllName("bz2");
	public static final String EXT_COM_DOTNET = dllName("com_dotnet");
	public static final String EXT_CURL = dllName("curl");
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
	public static PhpIni createDefaultIniCopy(Host host, PhpBuild build) {
		PhpIni ini = new PhpIni();
		ini.putSingle("default_mimetype", "text/plain");
		ini.putMulti(OUTPUT_HANDLER, StringUtil.EMPTY);
		ini.putMulti(OPEN_BASEDIR, StringUtil.EMPTY);
		ini.putMulti(SAFE_MODE, 0);
		ini.putMulti(DISABLE_DEFS, OFF); // TODO StringUtil.EMPTY);
		ini.putMulti(OUTPUT_BUFFERING, ON); // TODO OFF);
		ini.putMulti(ERROR_REPORTING, E_ALL_OR_E_STRICT);
		ini.putMulti(DISPLAY_ERRORS, 1);
		ini.putMulti(DISPLAY_STARTUP_ERRORS, 0);
		ini.putMulti(LOG_ERRORS, 0);
		ini.putMulti(HTML_ERRORS, 0);// TODO 1);
		ini.putMulti(TRACK_ERRORS, 1);
		ini.putMulti(REPORT_MEMLEAKS, 1);
		ini.putMulti(REPORT_ZEND_DEBUG, 0);
		ini.putMulti(DOCREF_ROOT, StringUtil.EMPTY);
		ini.putMulti(DOCREF_EXT, DOT_HTML);
		ini.putMulti(ERROR_PREPEND_STRING, StringUtil.EMPTY);
		ini.putMulti(ERROR_APPEND_STRING, StringUtil.EMPTY);
		ini.putMulti(AUTO_PREPEND_FILE, StringUtil.EMPTY);
		ini.putMulti(AUTO_APPEND_FILE, StringUtil.EMPTY);
		ini.putMulti(MAGIC_QUOTES_RUNTIME, 0);
		ini.putMulti(IGNORE_REPEATED_ERRORS, 0);
		ini.putMulti(PRECISION, 14);
		ini.putMulti(UNICODE_RUNTIME_ENCODING, ISO_8859_1);
		ini.putMulti(UNICODE_SCRIPT_ENCODING, UTF_8);
		ini.putMulti(UNICODE_OUTPUT_ENCODING, UTF_8);
		ini.putMulti(UNICODE_FROM_ERROR_MODE, U_INVALID_SUBSTITUTE);
		ini.putMulti(SESSION_AUTO_START, 0);
		
		// default php.ini has these extensions on Windows
		// NOTE: this is validated by RequiredExtensionsSmokeTest. similar/same info is both there and here
		//       b/c that needs it for validation and its here because its in the default php.ini
		if (host.isWindows()) {
			ini.setExtensionDir(build.getDefaultExtensionDir());
			ini.addExtensions(
					EXT_BZ2,
					EXT_COM_DOTNET,
					EXT_CURL,
					EXT_FILEINFO,
					EXT_GD2,
					EXT_GETTEXT,
					EXT_GMP,
					EXT_INTL,
					EXT_IMAP,
					EXT_LDAP,
					EXT_MBSTRING,
					EXT_EXIF,
					EXT_MYSQL,
					EXT_MYSQLI,
					EXT_OPENSSL,
					EXT_PDO_MYSQL,
					EXT_PDO_PGSQL,
					EXT_PDO_SQLITE,
					EXT_PDO_ODBC,
					EXT_PGSQL,
					EXT_SHMOP,
					EXT_SOAP,
					EXT_SOCKETS,
					EXT_SQLITE3,
					EXT_TIDY,
					EXT_XMLRPC,
					EXT_XSL
				);
		}
		
		// TIMING: do this after all calls to #putMulti, etc... b/c that sets is_default = false
		ini.is_default = true;
		return ini;
	} // end public static PhpIni createDefaultIniCopy
	//
	//
	private final HashMap<String, ArrayList<String>> ini_map;
	private WeakReference<PhpIni> ext_ini;
	private WeakReference<String> ini_str, cli_arg;
	private boolean is_default = false;
	
	public PhpIni() {
		ini_map = new HashMap<String, ArrayList<String>>();
	}
	
	public PhpIni(String ini_str) {
		this(ini_str, "");
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
	static final Pattern PAT_BS = Pattern.compile("\\\\");
	static final Pattern PAT_FS = Pattern.compile("/");
	public PhpIni(String ini_str, String pwd) {
		this();
		if (pwd!=null&&ini_str.contains("{PWD}")) {
			// TODO important to use \\\\
			if (pwd.contains("cache_list")) {
				pwd="C:/php-sdk/php-test-pack-5.4-ts-windows-vc9-x86-r811cd76/ext/phar/tests/cache_list";
			}
			//pwd = "C:\\\\php-sdk\\\\php-test-pack-5.4-ts-windows-vc9-x86-r811cd76\\\\"; // TODO temp
			ini_str = StringUtil.replaceAll(PAT_PWD, pwd, ini_str);
			
			// BN: ensure that correct \\s are used for paths on Windows
			// TODO ini_str = StringUtil.replaceAll(PAT_FS, "\\\\", ini_str);
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
		this.ini_str = new WeakReference<String>(ini_str);
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
			c += host.pathsSeparator() + path;
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
		return host.exists(getExtensionDir(build) + "/"+dll_name);
	}
	
	/** adds the extension to this PhpIni.
	 * 
	 * automatically uses the correct name format for the given host (adds php_ and .dll for Windows)
	 * 
	 * @param host
	 * @param build
	 * @param dll_name
	 */
	public void addExtension(Host host, PhpBuild build, String dll_name) {
		if (!_hasExtension(host, build, dll_name))
			dll_name = dllName(dll_name);
		addExtension(dll_name);
	}
	
	public void addExtension(String dll_name) {
		putMulti(EXTENSION, dll_name);
	}
	
	public void addExtensions(String...dll_names) {
		for (String dll_name:dll_names)
			addExtension(dll_name);
	}
	
	/** replaces all directives in this PhpIni that match the given PhpIni with the values from 
	 * the given PhpIni (all current values in those directives in this, are overwritten).
	 * 
	 * @param ini
	 * @see #appendAll
	 */
	public void replaceAll(PhpIni ini) {
		this.ini_map.putAll(ini.ini_map);
		if (ini.countDirectives() > 0)
			this.cli_arg = this.ini_str = null;
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
			values = new ArrayList<String>(1);
			values.add(value);
			ini_map.put(directive, values);
		} else if (!values.contains(value)) {
			values.add(value);
		}
		is_default = false;
	}
	
	public void remove(String directive) {
		ini_map.remove(directive);
		cli_arg = ini_str = null;
		is_default = false;
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
	public String[] getExtensions() {
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
		for ( String directive : ini_map.keySet() ) {
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
		this.ini_str = new WeakReference<String>(ini_str);
		return ini_str;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		return o == this || ( o instanceof PhpIni && equals((PhpIni)o) );
	}
	
	public boolean equals(PhpIni ini) {
		return this.toString().equals(ini.toString());
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
		this.ext_ini = ext_ini.ext_ini = new WeakReference<PhpIni>(ext_ini);
		String ext_dir = get(EXTENSION_DIR);
		if (ext_dir!=null)
			ext_ini.putSingle(EXTENSION_DIR, ext_dir);
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
		//
		if (cli_arg!=null) {
			String cli_arg_str = cli_arg.get();
			if (cli_arg_str!=null)
				return cli_arg_str;
		}
		//
		StringBuilder sb = new StringBuilder(256);
		for ( String directive : getDirectives()) {
			String value = get(directive);
			if (value==null)
				continue; // allow "" empty values though
			
			// CRITICAL: escape these characters in the INI
			value = StringUtil.replaceAll(PAT_bs, "\\\\\"", StringUtil.replaceAll(PAT_amp, "\\\\&", StringUtil.replaceAll(PAT_pipe, "\\\\|", value)));
			
			// CRITICAL: in a windows batch script % is replaced with the command to execute.
			//           need to escape this value.
			if (host.isWindows())
				value = StringUtil.replaceAll(PAT_per, "\\%\\%", value);
			
			sb.append(" -d \"");
			sb.append(directive);
			sb.append("=");
			sb.append(value);
			sb.append("\"");
		}
		String cli_arg_str = sb.toString();
		cli_arg = new WeakReference<String>(cli_arg_str);
		return cli_arg_str;
	} // end public String toCliArgString
	static final Pattern PAT_bs = Pattern.compile("\"");
	static final Pattern PAT_amp = Pattern.compile("\\&");
	static final Pattern PAT_pipe = Pattern.compile(
			"\\|");
	static final Pattern PAT_per = Pattern.compile("\\%");
	
} // end public class PhpIni
