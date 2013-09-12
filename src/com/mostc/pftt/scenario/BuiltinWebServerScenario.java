package com.mostc.pftt.scenario;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.github.mattficken.io.Trie;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.BuiltinWebServerManager;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.BuiltinWebHttpPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.BuiltinWebHttpPhptTestCaseRunner;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;

/** Tests PHP using PHP's builtin web server.
 * 
 * This is the web server that's run when a user runs: php -S
 * 
 * This feature is only available (this scenario can only be run against) PHP 5.4+ (not PHP 5.3)
 * 
 * @author Matt Ficken
 *
 */

public class BuiltinWebServerScenario extends WebServerScenario {

	protected BuiltinWebServerScenario() {
		super(new BuiltinWebServerManager());
	}
	
	/** don't run this scenario on PHP 5.3
	 * 
	 */
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return !build.is53(cm, host);
	}

	@Override
	public String getName() {
		return "Builtin-Web";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}
	
	@Override
	public int getApprovedInitialThreadPoolSize(AHost host, int threads) {
		// XXX update this calculation from time to time as this web server's performance improves (probably decrease number)
		return host.getCPUCount() * 6;//8;
	}
	
	@Override
	public int getApprovedMaximumThreadPoolSize(AHost host, int threads) {
		return host.getCPUCount() * 8;// TODO 10;
	}
	
	@Override
	public boolean isExpectedCrash(PhptTestCase test_case) {
		return test_case.isExtension("pdo") 
				|| test_case.isNamed(BUILTIN_WEB_EXPECTED_CRASHES) 
				|| isSlowTest(test_case)
				|| super.isExpectedCrash(test_case);
	}
	
	public static final Trie BUILTIN_WEB_EXPECTED_CRASHES = PhptTestCase.createNamed(
			"ext/standard/tests/network/udp6loop.phpt",
			"ext/standard/tests/serialize/serialization_objects_003.phpt",
			"tests/basic/rfc1867_anonymous_upload.phpt",
			"ext/standard/tests/mail/mail_variation_alt3-win32.phpt",
			"ext/standard/tests/mail/mail_basic_alt4-win32.phpt",
			"ext/calendar/tests/jdtounix.phpt",
			"ext/bcmath/tests/bcdiv.phpt",
			"ext/bcmath/tests/bcpow.phpt",
			"ext/standard/tests/network/long2ip_variation1.phpt",
			"ext/bcmath/tests/bcpow.phpt",
			"ext/fileinfo/tests/finfo_file_001.phpt",
			"ext/standard/tests/network/gethostbyname_error003.phpt",
			"ext/standard/tests/network/gethostbyname_error002.phpt",
			"tests/func/002.phpt",
			"ext/zip/tests/bug7214.phpt",
			"sapi/cgi/tests/011.phpt",
			"ext/tokenizer/tests/token_get_all_variation13.phpt",
			"ext/tidy/tests/025.phpt",
			"ext/zip/tests/zip_entry_filesize.phpt",
			"ext/tidy/tests/004.phpt",
			"ext/tidy/tests/006.phpt",
			"ext/tidy/tests/003.phpt",
			"ext/xml/tests/xml_set_element_handler_error.phpt",
			"ext/standard/tests/url/parse_url_basic_004.phpt",
			"ext/tidy/tests/027.phpt",
			"ext/tidy/tests/014.phpt",
			"ext/standard/tests/url/rawurlencode_variation_001.phpt",
			"ext/standard/tests/url/parse_url_relative_scheme.phpt",
			"ext/xml/tests/xml_parse_into_struct_variation1.phpt",
			"ext/zip/tests/bug7658.phpt",
			"ext/standard/tests/url/get_headers_error_001.phpt",
			"ext/zlib/tests/zlib_scheme_copy_variation2.phpt",
			"ext/standard/tests/url/urldecode_error_001.phpt",
			"ext/zlib/tests/readgzfile_variation14.phpt",
			"ext/xml/tests/bug32001b.phpt",
			"ext/standard/tests/url/bug55273.phpt",
			"ext/zlib/tests/readgzfile_variation13.phpt",
			"ext/zlib/tests/gzwrite_variation1.phpt",
			"ext/standard/tests/url/bug55399.phpt",
			"ext/standard/tests/url/base64_decode_basic_001.phpt",
			"ext/xml/tests/xml_parser_free_variation1.phpt",
			"ext/tidy/tests/001.phpt",
			"ext/standard/tests/url/parse_url_basic_003.phpt",
			"ext/zip/tests/bug7214.phpt",
			"ext/xml/tests/xml_set_character_data_handler_variation1.phpt",
			"ext/tidy/tests/016.phpt",
			"ext/standard/tests/url/base64_encode_basic_002.phpt",
			"ext/standard/tests/url/base64_decode_variation_002.phpt",
			"ext/standard/tests/url/base64_decode_variation_001.phpt",
			"ext/xml/tests/xml_parse_variation1.phpt",
			"ext/standard/tests/network/gethostbyname_error004.phpt",
			"ext/tidy/tests/016.phpt",
			"tests/basic/bug51709_2.phpt",
			"tests/basic/009.phpt",
			"ext/xml/tests/xml_set_character_data_handler_variation1.phpt",
			"ext/zlib/tests/gzwrite_error.phpt",
			"ext/xml/tests/xml_parser_set_option_variation1.phpt",
			"ext/xml/tests/xml_error_string_variation1.phpt",
			"ext/standard/tests/streams/bug61115-1.phpt",
			"ext/standard/tests/streams/bug46024.phpt",
			"ext/standard/tests/streams/bug49936.phpt",
			"ext/standard/tests/serialize/serialization_resources_001.phpt",
			"ext/standard/tests/serialize/precision.phpt",
			"ext/standard/tests/streams/bug61115.phpt",
			"ext/standard/tests/serialize/bug62836_1.phpt",
			"ext/standard/tests/serialize/serialization_objects_009.phpt",
			"ext/standard/tests/streams/bug46024.phpt",
			"ext/standard/tests/misc/time_nanosleep_error5.phpt",
			"ext/standard/tests/mail/ezmlm_hash_basic_64bit.phpt",
			"ext/standard/tests/serialize/serialization_objects_005.phpt",
			"ext/standard/tests/streams/bug61115-2.phpt",
			"ext/standard/tests/class_object/get_object_vars_error_001.phpt",
			"ext/standard/tests/class_object/get_parent_class_variation_002.phpt",
			"ext/standard/tests/class_object/get_parent_class_error_001.phpt",
			"ext/standard/tests/class_object/get_parent_class_variation_001.phpt",
			"ext/standard/tests/class_object/get_object_vars_variation_003.phpt",
			"ext/standard/tests/class_object/get_class_methods_variation_002.phpt",
			"ext/standard/tests/class_object/get_declared_classes_variation1.phpt",
			"ext/standard/tests/class_object/get_class_variation_001.phpt",
			"ext/standard/tests/class_object/interface_exists_variation1.phpt",
			"ext/standard/tests/class_object/interface_exists_variation4.phpt",
			"ext/reflection/tests/reflectionclass_issubclassof_error1.phpt",
			"ext/mbstring/tests/mb_ereg1.phpt",
			"ext/mcrypt/tests/mcrypt_filters.phpt",
			"ext/openssl/tests/014.phpt",
			"ext/openssl/tests/014.phpt",
			"ext/libxml/tests/bug54440.phpt",
			"ext/openssl/tests/bug54992.phpt",
			"ext/openssl/tests/bug48182.phpt",
			"ext/mcrypt/tests/mcrypt_enc_self_test.phpt",
			"ext/mcrypt/tests/mcrypt_ecb_variation5.phpt",
			"ext/ereg/tests/015.phpt",
			"ext/filter/tests/003.phpt",
			"ext/enchant/tests/broker_describe.phpt",
			"ext/filter/tests/037.phpt",
			"ext/filter/tests/bug46973.phpt",
			"ext/ereg/tests/eregi_variation_001.phpt",
			"ext/ereg/tests/spliti_error_002.phpt",
			"ext/ereg/tests/split_error_002.phpt",
			"ext/ereg/tests/spliti_variation_001.phpt",
			"ext/filter/tests/bug46973.phpt",
			"ext/filter/tests/bug8315.phpt",
			"ext/filter/tests/028.phpt",
			"ext/ereg/tests/ereg_variation_004.phpt",
			"ext/ereg/tests/eregi_replace_error_002.phpt",
			"ext/filter/tests/010.phpt",
			"ext/exif/tests/exif_imagetype_variation1.phpt",
			"ext/ereg/tests/spliti_basic_001.phpt",
			"ext/ereg/tests/ereg_replace_error_002.phpt",
			"ext/filter/tests/017.phpt",
			"ext/ereg/tests/eregi_error_002.phpt",
			"ext/ereg/tests/ereg_replace_basic_001.phpt",
			"ext/ereg/tests/ereg_error_002.phpt",
			"ext/filter/tests/bug49510.phpt",
			"ext/calendar/tests/jdtomonthname.phpt",
			"ext/com_dotnet/tests/bug33386.phpt",
			"ext/com_dotnet/tests/bug45280.phpt",
			"ext/calendar/tests/easter_days.phpt",
			"ext/calendar/tests/cal_to_jd.phpt",
			"ext/com_dotnet/tests/bug34272.phpt",
			"ext/calendar/tests/jdtofrench.phpt",
			"ext/calendar/tests/jdtomonthname.phpt",
			"ext/calendar/tests/jddayofweek.phpt",
			"ext/bcmath/tests/bcdiv_error2.phpt",
			"ext/bcmath/tests/bcdiv_error1.phpt",
			"ext/calendar/tests/easter_days.phpt",
			"ext/reflection/tests/bug45571.phpt",
			"ext/bcmath/tests/bcpowmod_error3.phpt",
			"ext/bcmath/tests/bcmod_error1.phpt",
			"ext/readline/tests/readline_read_history_001.phpt",
			"ext/readline/tests/readline_list_history_001.phpt",
			"ext/readline/tests/readline_info_001.phpt",
			"ext/reflection/tests/016.phpt",
			"ext/readline/tests/readline_completion_function_001.phpt",
			"ext/bz2/tests/002.phpt",
			"ext/bcmath/tests/bcpowmod_error2.phpt",
			"ext/bz2/tests/001.phpt",
			"ext/calendar/tests/jdtounix.phpt",
			"ext/calendar/tests/easter_date.phpt",
			"ext/standard/tests/network/udp4loop.phpt",
			"ext/sockets/tests/socket_select-wrongparams-2.phpt",
			"ext/xmlwriter/tests/oo_009.phpt",
			"ext/reflection/tests/traits005.phpt",
			"ext/reflection/tests/reflectionobject_export_basic2.phpt",
			"ext/reflection/tests/traits003.phpt"
		); // end BUILTIN_WEB_EXPECTED_CRASHES

	
	@Override
	public boolean isSlowTest(PhptTestCase test_case) {
		return test_case.isExtension(BUILTIN_WEB_SLOW_TESTS) 
				|| super.isSlowTest(test_case);
	}
	
	public static final Trie BUILTIN_WEB_SLOW_TESTS = PhptTestCase.createExtensions("mbstring",
			"intl", "gd", "session", "reflection", "dom", "date", "spl",
			"standard/tests/strings", "standard/tests/math", "standard/tests/image", "standard/tests/general_functions/",
			"standard/tests/file", "gettext", "xml", "zlib", "iconv", "ctype", "gmp", "hash");
	static {
		BUILTIN_WEB_SLOW_TESTS.addString("tests/security/");
		BUILTIN_WEB_SLOW_TESTS.addString("tests/lang/");
		BUILTIN_WEB_SLOW_TESTS.addString("tests/classes/");
		BUILTIN_WEB_SLOW_TESTS.addString("zend/tests/");
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.CLI_WWW;
	}
	
	@Override
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey group_key, PhptTestCase test_case, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack, boolean xdebug) {
		return new BuiltinWebHttpPhptTestCaseRunner(xdebug, this, group_key.getPhpIni(), group_key.getEnv(), params, httpproc, httpexecutor, smgr, thread.getThreadWebServerInstance(), thread, test_case, cm, twriter, host, scenario_set_setup, build, src_test_pack, active_test_pack);
	}
	
	
	@Override
	public AbstractPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(PhpUnitThread thread, TestCaseGroupKey group_key, ConsoleManager cm, ITestResultReceiver twriter, Map<String,String> globals, Map<String,String> env, AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only) {
		return new BuiltinWebHttpPhpUnitTestCaseRunner(this, thread, twriter, params, httpproc, httpexecutor, smgr, thread.getThreadWebServerInstance(), globals, env, cm, runner_host, scenario_set_setup, build, test_case, my_temp_dir, constants, include_path, include_files, ini, reflection_only);
	}
	
	@Override
	public void sortTestCases(List<PhptTestCase> test_cases) {
		// fast tests first
		Collections.sort(test_cases, new Comparator<PhptTestCase>() {
				@Override
				public int compare(PhptTestCase a, PhptTestCase b) {
					final boolean as = !isSlowTest(a);
					final boolean bs = !isSlowTest(b);
					return ( as ^ bs ) ? ( as ^ true  ? -1 : 1 ) : 0;
				}
			});
	}
	
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup setup, ESAPIType type, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		if (super.willSkip(cm, twriter, host, setup, type, build, src_test_pack, test_case)) {
			return true;
		} else if (test_case.isNamed(NOT_ON_BUILTIN_WEB_SERVER)) {

			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test is not valid on builtin web server", null, null, null, null, null, null, null, null, null, null, null));
			return true;
		} else {
			return false;
		}
	}
	
	// TODO try re-enabling these and see which timeout
	public static Trie NOT_ON_BUILTIN_WEB_SERVER = PhptTestCase.createNamed(
				// XXX these don't work right on the builtin web server 
				"ext/mbstring/tests/mb_ereg_search.phpt",
				"ext/json/tests/pass001.phpt",
				"ext/intl/tests/transliterator_transliterate_basic.phpt",
				"ext/date/tests/bug17988.phpt",
				"ext/date/tests/bug30532.phpt",
				"ext/date/tests/bug32086.phpt",
				"ext/date/tests/bug35425.phpt",
				"ext/date/tests/bug51393.phpt",
				"ext/date/tests/bug53502.phpt",
				"ext/date/tests/bug63435.phpt",
				"ext/date/tests/date_time_immutable.phpt",
				"ext/filter/tests/041.phpt",
				"ext/iconv/tests/bug48289.phpt",
				"ext/iconv/tests/iconv_mime_decode.phpt",
				"ext/intl/tests/breakiter_clone_basic.phpt",
				"ext/intl/tests/breakiter_factories_basic.phpt",
				"ext/intl/tests/breakiter_first_basic.phpt",
				"ext/intl/tests/breakiter_settext_basic.phpt",
				"ext/intl/tests/bug61487.phpt",
				"ext/intl/tests/bug62070.phpt",
				"ext/intl/tests/bug62082.phpt",
				"ext/intl/tests/bug62083.phpt",
				"ext/intl/tests/bug62915-2.phpt",
				"ext/intl/tests/bug62915.phpt",
				"ext/intl/tests/calendar_before_after_error.phpt",
				"ext/intl/tests/calendar_equals_error.phpt",
				"ext/intl/tests/calendar_get_getactualmaximum_minumum_error2.phpt",
				"ext/intl/tests/calendar_isequivalentto_error.phpt",
				"ext/intl/tests/calendar_settimezone_error.phpt",
				"ext/intl/tests/idn_uts46_basic.phpt",
				"ext/intl/tests/timezone_hassamerules_error.phpt",
				"ext/intl/tests/transliterator_clone.phpt",
				"ext/intl/tests/transliterator_create_basic.phpt",
				"ext/intl/tests/transliterator_create_from_rule_basic.phpt",
				"ext/intl/tests/transliterator_create_inverse_basic.phpt",
				"ext/intl/tests/transliterator_list_ids_basic.phpt",
				"ext/intl/tests/uconverter_enum.phpt",
				"ext/intl/tests/uconverter_func_basic.phpt",
				"ext/intl/tests/uconverter_oop_algo.phpt",
				"ext/intl/tests/uconverter_oop_basic.phpt",
				"ext/intl/tests/uconverter_oop_callback.phpt",
				"ext/intl/tests/uconverter_oop_callback_return.phpt",
				"ext/intl/tests/uconverter_oop_subst.phpt",
				"ext/mbstring/tests/bug54494.phpt",
				"ext/mbstring/tests/mb_split_empty_match.phpt",
				"ext/mbstring/tests/mb_strcut_missing_boundary_check.phpt",
				"ext/mbstring/tests/overload01.phpt",
				"ext/phar/tests/bug45218_slowtest.phpt",
				"ext/phar/tests/tar/tar_003.phpt",
				"ext/standard/tests/strings/htmlentities05.phpt",
				/*"ext/xsl/tests/bug26384.phpt",
				"ext/xsl/tests/xslt001.phpt",
				"ext/xsl/tests/xslt002.phpt",
				"ext/xsl/tests/xslt003.phpt",
				"ext/xsl/tests/xslt004.phpt",
				"ext/xsl/tests/xslt005.phpt",
				"ext/xsl/tests/xslt006.phpt",
				"ext/xsl/tests/xslt007.phpt",
				"ext/xsl/tests/xslt008.phpt",
				"ext/xsl/tests/xslt009.phpt",
				"ext/xsl/tests/xslt010.phpt",
				"ext/xsl/tests/xslt012.phpt",
				"ext/xsl/tests/xsltprocessor_removeparameter-invalidparam.phpt",
				"ext/xsl/tests/xsltprocessor_removeparameter.phpt",
				//"tests/basic/022.phpt",
				"tests/basic/023.phpt",
				"tests/basic/bug29971.phpt",
				"tests/lang/short_tags.002.phpt",*/
				"zend/tests/errmsg_021.phpt"
			);

	@Override
	public int getSlowTestTimeSeconds() {
		return 40;
	}
	
	@Override
	public long getFastTestTimeSeconds() {
		return 30;
	}

} // end public class BuiltinWebServerScenario
