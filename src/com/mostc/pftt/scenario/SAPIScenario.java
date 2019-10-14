package com.mostc.pftt.scenario;

import java.util.List;
import java.util.Map;

import com.github.mattficken.Overridable;
import com.github.mattficken.io.Trie;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.main.IENVINIFilter;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.SourceTestPack;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.app.SimpleTestCase;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.AbstractSimpleTestCaseRunner;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;
import com.mostc.pftt.runner.LocalSimpleTestPackRunner.SimpleTestThread;
import com.mostc.pftt.runner.PhptTestPreparer.PreparedPhptTestCase;
import com.mostc.pftt.scenario.SAPIScenario.BuildInstall;

/** Different scenarios for how PHP can be run
 * 
 * CLI - command line, all that has traditionally been tested
 * Builtin-WWW
 * IIS-Express-FastCGI - using IIS Express on Windows Clients
 * IIS-FastCGI - IIS on Windows Servers
 * mod_php - using Apache's mod_php
 * 
 * @author Matt Ficken
 *
*/

public abstract class SAPIScenario extends AbstractSerialScenario {

	public static SAPIScenario getSAPIScenario(ScenarioSet scenario_set) {
		return scenario_set.getScenario(SAPIScenario.class, DEFAULT_SAPI_SCENARIO);
	}
	
	/** returns if this test is expected to take more than 40 seconds to execute on this Scenario.
	 * 
	 * fe, some PHPT tests are slow on builtin_web scenario but not slow on apache.
	 * 
	 * most tests take only a few seconds or less, so 40 is pretty slow. 60 seconds is the
	 * maximum amount of time a test is allowed to execute, beyond that, its killed.
	 * 
	 * @param test_case
	 * @return
	 */
	@Overridable
	public boolean isSlowTest(PhptTestCase test_case) {
		return test_case.isSlowTest();
	}
	
	@Overridable
	public boolean isExpectedCrash(PhptTestCase test_case) {
		return false;
	}
	
	public class BuildInstall {
		public String install_path;
		
	}
	
	@Overridable
	public BuildInstall installBuild(ConsoleManager cm, ScenarioSetSetup scenario_set_setup, AHost host, String build_str) throws Exception {
		return new BuildInstall();
	}
	
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return SAPIScenario.class;
	}
	
	/** creates a runner to run a single PhptTestCase under this SAPI scenario
	 * 
	 * @param thread
	 * @param group_key
	 * @param prep
	 * @param cm
	 * @param twriter
	 * @param fs TODO
	 * @param host
	 * @param scenario_set_setup
	 * @param build
	 * @param src_test_pack
	 * @param active_test_pack
	 * @param xdebug TODO
	 * @param debugger_attached TODO
	 * @return
	 */
	public abstract AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey group_key, PreparedPhptTestCase prep, ConsoleManager cm, ITestResultReceiver twriter, FileSystemScenario fs, AHost host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack, boolean xdebug, boolean debugger_attached);
	
	public void close(ConsoleManager cm, boolean debug) {
		
	}

	@Override
	public abstract int getApprovedInitialThreadPoolSize(AHost host, int threads);
	@Override
	public abstract int getApprovedMaximumThreadPoolSize(AHost host, int threads);

	public abstract ESAPIType getSAPIType();

	/** creates a key to group test cases under
	 * 
	 * each key has a unique phpIni and/or ENV vars
	 * 
	 * Web Server SAPIs require grouping test cases by keys because a new WebServerInstance for each PhpIni, but
	 * a WebServerInstance can be used to run multiple test cases. this will boost performance.
	 * 
	 * @param cm
	 * @param fs TODO
	 * @param host
	 * @param build
	 * @param scenario_set_setup
	 * @param active_test_pack
	 * @param test_case
	 * @param filter
	 * @param group_key
	 * @return
	 * @throws Exception
	 */
	public abstract TestCaseGroupKey createTestGroupKey(ConsoleManager cm, FileSystemScenario fs, AHost host, PhpBuild build, ScenarioSetSetup scenario_set_setup, PhptActiveTestPack active_test_pack, PhptTestCase test_case, IENVINIFilter filter, TestCaseGroupKey group_key) throws Exception;
	
	public abstract PhpIni createIniForTest(ConsoleManager cm, FileSystemScenario fs, AHost host, PhpBuild build, PhptActiveTestPack active_test_pack, ScenarioSetSetup scenario_set_setup);

	public abstract AbstractPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(PhpUnitThread thread, TestCaseGroupKey group_key, ConsoleManager cm, ITestResultReceiver twriter, Map<String,String> globals, Map<String,String> env, FileSystemScenario fs, AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only);
	
	public abstract AbstractSimpleTestCaseRunner createSimpleTestCaseRunner(SimpleTestThread thread, ITestResultReceiver tmgr, ConsoleManager cm, FileSystemScenario fs, AHost host, ScenarioSetSetup scenario_set, PhpBuild build, PhpIni ini, SimpleTestCase test_case);
	
	public static Trie RANDOMLY_FAIL = PhptTestCase.createNamed(
			"ext/com_dotnet/tests/bug66431_1.phpt",
			"ext/curl/tests/curl_setopt_array_basic.phpt",
			"ext/curl/tests/curl_setopt_basic002.phpt",
			"ext/dom/tests/domdocument_loadhtmlfile_error1.phpt",
			"ext/dom/tests/domdocument_loadhtmlfile_error2.phpt",
			"ext/date/tests/bug41523.phpt",
			"ext/date/tests/bug65371.phpt",
			"ext/gd/tests/png2gd.phpt",
			"ext/gettext/tests/gettext_dgettext.phpt",
			"ext/imap/tests/bug31142_2.phpt",
			"ext/imap/tests/bug44098.phpt",
			"ext/imap/tests/bug46918.phpt",
			"ext/ldap/tests/ldap_errno_basic.phpt",
			"ext/openssl/tests/001.phpt",
			"ext/openssl/tests/026.phpt",
			"ext/openssl/tests/bug46127.phpt",
			"ext/openssl/tests/bug65538_001.phpt",
			"ext/openssl/tests/openssl_spki_export.phpt",
			"ext/openssl/tests/openssl_spki_verify.phpt",
			"ext/pdo/tests/bug_34630.phpt",
			"ext/pdo/tests/bug_44861.phpt",
			"ext/pdo/tests/pdo_026.phpt",
			"ext/pgsql/tests/bug64609.phpt",
			"ext/simplexml/tests/simplexmlelement_xpath.phpt",
			"ext/soap/tests/bugs/bug27722.phpt",
			"ext/soap/tests/bugs/bug27742.phpt",
			"ext/soap/tests/bugs/bug28969.phpt",
			"ext/soap/tests/bugs/bug30045.phpt",
			"ext/soap/tests/bugs/bug31695.phpt",
			"ext/soap/tests/bugs/bug38005.phpt",
			"ext/soap/tests/bugs/bug38067.phpt",
			"ext/soap/tests/bugs/bug39832.phpt",
			"ext/soap/tests/bugs/bug42214.phpt",
			"ext/soap/tests/bugs/bug42488.phpt",
			"ext/soap/tests/bugs/bug47273.phpt",
			"ext/soap/tests/bugs/bug50698_2.phpt",
			"ext/soap/tests/bugs/bug50698_3.phpt",
			"ext/soap/tests/interop/round2/base/r2_base_005p.phpt",
			"ext/soap/tests/interop/round2/base/r2_base_005s.phpt",
			"ext/soap/tests/interop/round2/base/r2_base_005w.phpt",
			"ext/soap/tests/interop/round4/groupi/r4_groupi_xsd_006w.phpt",
			"ext/soap/tests/server011.phpt",
			"ext/soap/tests/server012.phpt",
			"ext/soap/tests/server014.phpt",
			"ext/soap/tests/soap12/t27.phpt",
			"ext/soap/tests/soap12/t56.phpt",
			"ext/soap/tests/soap12/t58.phpt",
			"ext/soap/tests/soap12/t59.phpt",
			"ext/soap/tests/soap12/t61.phpt",
			"ext/sqlite/tests/bug26911.phpt",
			"ext/sqlite/tests/bug35248.phpt",
			"ext/sqlite/tests/bug48679.phpt",
			"ext/sqlite/tests/sqlite_002.phpt",
			"ext/sqlite/tests/sqlite_003.phpt",
			"ext/sqlite/tests/sqlite_004.phpt",
			"ext/sqlite/tests/sqlite_005.phpt",
			"ext/sqlite/tests/sqlite_006.phpt",
			"ext/sqlite/tests/sqlite_007.phpt",
			"ext/sqlite/tests/sqlite_008.phpt",
			"ext/sqlite/tests/sqlite_009.phpt",
			"ext/sqlite/tests/sqlite_010.phpt",
			"ext/sqlite/tests/sqlite_011.phpt",
			"ext/sqlite/tests/sqlite_012.phpt",
			"ext/sqlite/tests/sqlite_013.phpt",
			"ext/sqlite/tests/sqlite_016.phpt",
			"ext/sqlite/tests/sqlite_017.phpt",
			"ext/sqlite/tests/sqlite_026.phpt",
			"ext/sqlite/tests/sqlite_027.phpt",
			"ext/sqlite/tests/sqlite_closures_001.phpt",
			"ext/sqlite/tests/sqlite_closures_002.phpt",
			"ext/sqlite/tests/sqlite_oo_003.phpt",
			"ext/sqlite/tests/sqlite_oo_008.phpt",
			"ext/sqlite/tests/sqlite_oo_009.phpt",
			"ext/sqlite/tests/sqlite_oo_010.phpt",
			"ext/sqlite/tests/sqlite_oo_011.phpt",
			"ext/sqlite/tests/sqlite_oo_012.phpt",
			"ext/sqlite/tests/sqlite_oo_013.phpt",
			"ext/sqlite/tests/sqlite_oo_016.phpt",
			"ext/sqlite/tests/sqlite_oo_025.phpt",
			"ext/sqlite/tests/sqlite_oo_026.phpt",
			"ext/sqlite/tests/sqlite_oo_027.phpt",
			"ext/sqlite/tests/sqlite_oo_028.phpt",
			"ext/sqlite/tests/sqlite_oo_029.phpt",
			"ext/sqlite/tests/sqlite_spl_001.phpt",
			"ext/sqlite/tests/sqlite_spl_002.phpt",
			"ext/sqlite/tests/sqlite_spl_003.phpt",
			"ext/standard/tests/mail/mail_basic4.phpt",
			"ext/sockets/tests/socket_import_stream-5.phpt",
			"ext/standard/tests/file/pathinfo_basic2-win32.phpt",
			"tests/basic/enable_post_data_reading_05.phpt",
			"tests/basic/enable_post_data_reading_06.phpt",
			"tests/basic/002.phpt",
			"tests/basic/004.phpt",
			"tests/basic/005.phpt",
			"tests/basic/013.phpt",
			"tests/basic/014.phpt",
			"tests/basic/015.phpt",
			"tests/basic/016.phpt",
			"tests/basic/017.phpt",
			"tests/basic/018.phpt",
			"tests/basic/019.phpt",
			"tests/basic/020.phpt",
			"tests/basic/bug46759.phpt",
			"tests/basic/bug53180.phpt",
			"tests/strings/001.phpt",
			"sapi/tests/test007.phpt",
			"ext/pdo_mysql/tests/pdo_mysql_class_constants.php",
			"ext/standard/tests/versioning/php_sapi_name_variation001.phpt",
			"tests/run-test/test006.phpt",
			"ext/oci8/tests/password_old.phpt",
			"ext/oci8/tests/password_new.phpt",
			"ext/gd/tests/imageloadfont_invalid.phpt",
			"ext/mbstring/tests/zend_multibyte-01.phpt",
			"ext/soap/tests/bugs/bug66112.phpt",
			"ext/gd/tests/createfromwbmp2.phpt",
			"ext/soap/tests/server029.phpt",
			"ext/pdo_mysql/tests/pdo_mysql_class_constants.phpt",
			"ext/curl/tests/curl_basic_001.phpt",
			"ext/curl/tests/curl_basic_004.phpt",
			"ext/intl/tests/breakiter_getlocale_basic.phpt",
			"ext/intl/tests/bug58756_messageformatter.phpt",
			"ext/intl/tests/bug59597_64.phpt",
			"ext/intl/tests/bug62070.phpt",
			"ext/intl/tests/bug67052.phpt",
			"ext/intl/tests/calendar_getdayofweektype_basic.phpt",
			"ext/intl/tests/collator_asort.phpt",
			"ext/intl/tests/collator_compare.phpt",
			"ext/intl/tests/collator_get_locale.phpt",
			"ext/intl/tests/collator_get_sort_key.phpt",
			"ext/intl/tests/collator_get_sort_key_variant3.phpt",
			"ext/intl/tests/collator_sort.phpt",
			"ext/intl/tests/collator_sort_with_sort_keys.phpt",
			"ext/intl/tests/dateformat_format.phpt",
			"ext/intl/tests/dateformat_format_parse.phpt",
			"ext/intl/tests/dateformat_format_variant2.phpt",
			"ext/intl/tests/dateformat_get_set_calendar.phpt",
			"ext/intl/tests/dateformat_get_set_calendar_variant2.phpt",
			"ext/intl/tests/dateformat_get_set_pattern.phpt",
			"ext/intl/tests/dateformat_parse.phpt",
			"ext/intl/tests/dateformat_parse_timestamp_parsepos.phpt",
			"ext/intl/tests/dateformat_set_timezone_id.phpt",
			"ext/intl/tests/formatter_format_currency.phpt",
			"ext/intl/tests/formatter_get_locale.phpt",
			"ext/intl/tests/formatter_get_set_attribute.phpt",
			"ext/intl/tests/formatter_get_set_symbol.phpt",
			"ext/intl/tests/locale_filter_matches2.phpt",
			"ext/intl/tests/locale_get_display_script3.phpt",
			"ext/intl/tests/locale_get_keywords.phpt",
			"ext/intl/tests/locale_lookup.phpt",
			"ext/intl/tests/msgfmt_format_intlcalendar.phpt",
			"ext/intl/tests/msgfmt_format_intlcalendar_variant2.phpt",
			"ext/intl/tests/resourcebundle_null_mandatory_args.phpt",
			"ext/intl/tests/timezone_getcanonicalid_variant1.phpt",
			"ext/intl/tests/timezone_getdisplayname_variant2-49+.phpt",
			"ext/intl/tests/timezone_getdisplayname_variant2.phpt",
			"ext/openssl/tests/streams_crypto_method.phpt",
			"ext/pdo_firebird/tests/connect.phpt",
			"ext/pdo_firebird/tests/execute.phpt",
			"ext/pdo/tests/pdo_002.phpt",
			"ext/pdo/tests/pdo_020.phpt",
			"ext/pdo/tests/pdo_029.phpt",
			"ext/reflection/tests/bug36308.phpt",
			"zend/tests/multibyte/multibyte_encoding_006.phpt",
			"ext/calendar/tests/jdtojewish.phpt",
			"ext/enchant/tests/broker_free.phpt",
			"ext/enchant/tests/broker_init.phpt",
			"ext/enchant/tests/broker_request_dict.phpt",
			"ext/gettext/tests/gettext_phpinfo.phpt",
			"ext/imap/tests/bug53377.phpt",
			"ext/mysql/tests/bug53649.phpt",
			"ext/sockets/tests/socket_create_pair.phpt",
			"ext/standard/tests/strings/bug20934.phpt",
			"ext/standard/tests/strings/bug37244.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic1.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic10.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic2.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic6.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic7.phpt",
			"ext/standard/tests/strings/htmlentities19.phpt",
			"ext/standard/tests/strings/htmlentities_html5.phpt",
			"ext/xsl/tests/xsl-phpinfo.phpt",
			"tests/basic/003.phpt",
			"tests/lang/short_tags.003.phpt",
			"tests/output/ob_006.phpt",
			"zend/tests/anonymous_func_001.phpt",
			"ext/ldap/tests/ldap_sort_variation.phpt",
			"ext/mysqli/tests/mysqli_send_query.phpt",
			"ext/pdo/tests/bug_42917.phpt",
			"ext/pdo/tests/pdo_022.phpt",
			"ext/pdo/tests/pdo_027.phpt",
			"ext/standard/tests/file/glob_variation5.phpt",
			"zend/tests/multibyte/multibyte_encoding_006.phpt",
			"ext/calendar/tests/jdtojewish.phpt",
			"ext/enchant/tests/broker_free.phpt",
			"ext/enchant/tests/broker_init.phpt",
			"ext/enchant/tests/broker_request_dict.phpt",
			"ext/gettext/tests/gettext_phpinfo.phpt",
			"ext/imap/tests/bug53377.phpt",
			"ext/mbstring/tests/zend_multibyte-03.phpt",
			"ext/mbstring/tests/zend_multibyte-04.phpt",
			"ext/mbstring/tests/zend_multibyte-05.phpt",
			"ext/sockets/tests/socket_create_pair.phpt",
			"ext/sockets/tests/socket_getpeername_ipv4loop.phpt",
			"ext/standard/tests/strings/bug37244.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic1.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic10.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic2.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic6.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic7.phpt",
			"ext/standard/tests/strings/htmlentities19.phpt",
			"ext/standard/tests/strings/htmlentities_html5.phpt",
			"ext/tidy/tests/020.phpt",
			"ext/xsl/tests/xsl-phpinfo.phpt",
			// uses both POST and GET
			"tests/basic/003.phpt",
			"tests/output/ob_006.phpt",

			
			"ext/date/tests/bug41523-64bit.phpt",
			"ext/pdo/tests/bug_40285.phpt",
			"ext/date/tests/gmstrftime_variation22.phpt",
			"ext/intl/tests/locale_filter_matches.phpt",
			"ext/mbstring/tests/mb_strcut.phpt",
			"ext/mbstring/tests/mb_stripos.phpt",
			"ext/pdo/tests/bug_36798.phpt",
			"ext/pdo/tests/bug_39398.phpt",
			"ext/pdo/tests/pdo_032.phpt",
			"ext/standard/tests/file/001.phpt",
			"ext/dom/tests/domdocument_loadhtmlfile_variation1.phpt",
			"ext/mbstring/tests/mb_stripos.phpt",
			"tests/run-test/test004.phpt",
			"zend/tests/bug51822.phpt",
			"ext/pdo/tests/bug_36798.phpt",
			"ext/pdo/tests/bug_43663.phpt",
			"ext/date/tests/datetime_add-fall-type2-type3.phpt",
			"ext/date/tests/datetime_add-fall-type3-type2.phpt",
			"ext/date/tests/datetime_add-fall-type3-type3.phpt",
			"ext/date/tests/datetime_add-spring-type2-type3.phpt",
			"ext/date/tests/datetime_add-spring-type3-type2.phpt",
			"ext/date/tests/datetime_add-spring-type3-type3.phpt",
			"ext/date/tests/datetime_diff-fall-type2-type3.phpt",
			"ext/date/tests/datetime_diff-fall-type3-type2.phpt",
			"ext/date/tests/datetime_diff-fall-type3-type3.phpt",
			"ext/date/tests/datetime_diff-spring-type2-type3.phpt",
			"ext/date/tests/datetime_diff-spring-type3-type2.phpt",
			"ext/date/tests/datetime_diff-spring-type3-type3.phpt",
			"ext/date/tests/datetime_sub-fall-type2-type3.phpt",
			"ext/date/tests/datetime_sub-fall-type3-type2.phpt",
			"ext/date/tests/datetime_sub-fall-type3-type3.phpt",
			"ext/date/tests/datetime_sub-spring-type2-type3.phpt",
			"ext/date/tests/datetime_sub-spring-type3-type2.phpt",
			"ext/date/tests/datetime_sub-spring-type3-type3.phpt",
			"ext/date/tests/rfc-datetime_and_daylight_saving_time-type3-bd2.phpt",
			"ext/date/tests/rfc-datetime_and_daylight_saving_time-type3-fs.phpt",
			"ext/filter/tests/bug42718.phpt",
			"ext/standard/tests/math/bug45712.phpt",
			"zend/tests/bug63336.phpt",
			"zend/tests/bug65784.phpt",
			"zend/tests/method_static_var.phpt",
			"ext/date/tests/bug62896.phpt",
			"ext/date/tests/date-time-modify-times.phpt",
			"ext/filter/tests/041.phpt",
			"ext/json/tests/pass001.phpt",
			"ext/mbstring/tests/mb_detect_order.phpt",
			"ext/pdo_sqlite/tests/bug52487.phpt",
			"ext/standard/tests/file/bug22414.phpt",
			"ext/zlib/tests/gzgetc_basic.phpt",
			"tests/basic/027.phpt",
			"tests/output/ob_020.phpt",
			"ext/pdo/tests/pdo_028.phpt",
			"ext/session/tests/027.phpt",
			"ext/date/tests/bug62896.phpt",
			"ext/date/tests/date-time-modify-times.phpt",
			"ext/filter/tests/041.phpt",
			"ext/json/tests/pass001.phpt",
			"ext/mbstring/tests/mb_detect_order.phpt",
			"ext/opcache/tests/001_cli.phpt",
			"ext/opcache/tests/bug64353.phpt",
			"ext/pdo_sqlite/tests/bug52487.phpt",
			"ext/standard/tests/strings/htmlentities06.phpt",
			"ext/standard/tests/strings/htmlentities11.phpt",
			"ext/standard/tests/strings/vfprintf_variation10.phpt",
			"ext/zlib/tests/gzgetc_basic.phpt",
			"ext/zlib/tests/zlib_filter_inflate.phpt",
			"tests/basic/027.phpt",
			"zend/tests/magic_by_ref_004.phpt",
			"ext/pdo/tests/pdo_017.phpt",
			"ext/spl/tests/splfileobject_setcsvcontrol_variation001.phpt",
			"ext/xml/tests/bug26614.phpt",
			"ext/bz2/tests/bz2_filter_compress.phpt",
			"ext/bz2/tests/bz2_filter_decompress.phpt",
			"ext/calendar/tests/unixtojd.phpt",
			"ext/date/tests/bug20382-1.phpt",
			"ext/date/tests/bug21966.phpt",
			"ext/date/tests/bug26090.phpt",
			"ext/date/tests/bug30532.phpt",
			"ext/date/tests/bug32270.phpt",
			"ext/date/tests/bug33414-1.phpt",
			"ext/date/tests/bug49700.phpt",
			"ext/date/tests/bug52062.phpt",
			"ext/date/tests/bug52113.phpt",
			"ext/date/tests/bug52668.phpt",
			"ext/date/tests/bug54340.phpt",
			"ext/date/tests/bug60236.phpt",
			"ext/date/tests/bug65548.phpt",
			"ext/date/tests/date_diff.phpt",
			"ext/date/tests/date_diff1.phpt",
			"ext/date/tests/date_time_immutable-inherited.phpt",
			"ext/date/tests/date_time_immutable.phpt",
			"ext/date/tests/dateinterval_format_a.phpt",
			"ext/dom/tests/bug46335.phpt",
			"ext/filter/tests/033.phpt",
			"ext/iconv/tests/iconv_strrpos.phpt",
			"ext/intl/tests/bug55562.phpt",
			"ext/intl/tests/collator_asort_variant2.phpt",
			"ext/intl/tests/formatter_get_set_symbol2.phpt",
			"ext/intl/tests/idn_uts46_basic.phpt",
			"ext/intl/tests/locale_get_default.phpt",
			"ext/mbstring/tests/bug63447_003.phpt",
			"ext/mbstring/tests/mb_convert_encoding.phpt",
			"ext/mbstring/tests/mb_convert_variables.phpt",
			"ext/mbstring/tests/mb_detect_encoding.phpt",
			"ext/mbstring/tests/mb_get_info.phpt",
			"ext/mbstring/tests/mb_internal_encoding_ini_basic2.phpt",
			"ext/mbstring/tests/mb_output_handler_euc_jp.phpt",
			"ext/mbstring/tests/mb_strimwidth.phpt",
			"ext/mbstring/tests/mb_strlen.phpt",
			"ext/mbstring/tests/mb_strpos.phpt",
			"ext/mbstring/tests/mb_strwidth.phpt",
			"ext/mbstring/tests/mb_substr.phpt",
			"ext/mbstring/tests/overload01.phpt",
			"ext/mbstring/tests/simpletest.phpt",
			"ext/openssl/tests/bug55646.phpt",
			"ext/pcre/tests/007.phpt",
			"ext/pcre/tests/bug42298.phpt",
			"ext/pdo_sqlite/tests/bug33841.phpt",
			"ext/pdo_sqlite/tests/bug46139.phpt",
			"ext/session/tests/015.phpt",
			"ext/session/tests/018.phpt",
			"ext/session/tests/020.phpt",
			"ext/session/tests/021.phpt",
			"ext/session/tests/bug41600.phpt",
			"ext/session/tests/bug51338.phpt",
			"ext/standard/tests/file/bug26615.phpt",
			"ext/standard/tests/file/bug26938.phpt",
			"ext/standard/tests/file/php_fd_wrapper_01.phpt",
			"ext/standard/tests/file/php_fd_wrapper_02.phpt",
			"ext/standard/tests/general_functions/bug49056.phpt",
			"ext/standard/tests/strings/vprintf_variation10.phpt",
			"ext/zlib/tests/zlib_filter_deflate2.phpt",
			"tests/basic/022.phpt",
			"tests/basic/024.phpt",
			"ext/opcache/tests/blacklist.phpt",
			"ext/opcache/tests/revalidate_path_01.phpt",
			"ext/pdo/tests/bug_39656.phpt",
			"ext/pdo/tests/pdo_003.phpt",
			"sapi/cli/tests/bug65633.phpt",
			"tests/lang/034.phpt",
			"ext/pdo/tests/bug_43139.phpt",
			"ext/standard/tests/strings/vprintf_variation16_64bit.phpt",
			"sapi/cli/tests/bug65066_422.phpt",
			"ext/date/tests/bug27780.phpt",
			"ext/filter/tests/001.phpt",
			"ext/filter/tests/005.phpt",
			"ext/filter/tests/028.phpt",
			"ext/filter/tests/037.phpt",
			"ext/iconv/tests/iconv_mime_decode.phpt",
			"ext/iconv/tests/iconv_substr.phpt",
			"ext/intl/tests/collator_compare_variant2.phpt",
			"ext/intl/tests/collator_get_sort_key_variant2.phpt",
			"ext/intl/tests/cpbi_parts_iterator.phpt",
			"ext/intl/tests/formatter_format_currency2.phpt",
			"ext/intl/tests/msgfmt_format.phpt",
			"ext/intl/tests/msgfmt_parse.phpt",
			"ext/intl/tests/rbbiter_getrulestatus_basic.phpt",
			"ext/intl/tests/transliterator_clone.phpt",
			"ext/intl/tests/transliterator_create_from_rule_basic.phpt",
			"ext/intl/tests/transliterator_create_inverse_basic.phpt",
			"ext/intl/tests/transliterator_transliterate_basic.phpt",
			"ext/mbstring/tests/bug26639.phpt",
			"ext/mbstring/tests/casefold.phpt",
			"ext/mbstring/tests/mb_convert_kana.phpt",
			"ext/mbstring/tests/mb_ereg.phpt",
			"ext/mbstring/tests/mb_ereg_search.phpt",
			"ext/mbstring/tests/mb_ereg_search_regs.phpt",
			"ext/mbstring/tests/mb_ereg_search_xxx.phpt",
			"ext/mbstring/tests/mb_output_handler_shift_jis.phpt",
			"ext/mbstring/tests/mb_strstr.phpt",
			"ext/pdo/tests/pecl_bug_5772.phpt",
			"ext/standard/tests/file/fscanf_variation52.phpt",
			"ext/standard/tests/file/fscanf_variation53.phpt",
			"ext/standard/tests/general_functions/bug43293_1.phpt",
			"ext/standard/tests/math/bug30695.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic5.phpt",
			"ext/standard/tests/strings/html_entity_decode_html5.phpt",
			"ext/standard/tests/strings/htmlentities23.phpt",
			"tests/basic/011.phpt",
			"tests/basic/026.phpt",
			"tests/lang/bug25145.phpt",
			"tests/lang/bug32924.phpt",
			"zend/tests/multibyte/multibyte_encoding_004.phpt",

			
			// this test (at least the CLI scenario on Windows) opens a text editor (blocks until user manually closes it)
			"sapi/cli/tests/021.phpt",
			// these tests randomly fail (ignore them)
			"ext/standard/tests/network/gethostbyname_error006.phpt",
			"ext/standard/tests/network/shutdown.phpt",
			"ext/standard/tests/php_ini_loaded_file.phpt", 
			"tests/run-test/test010.phpt", 
			"ext/standard/tests/misc/time_sleep_until_basic.phpt", 
			"ext/standard/tests/misc/time_nanosleep_basic.phpt",
			"ext/mbstring/tests/bug45239.phpt",
			"ext/mbstring/tests/bug63447_001.phpt",
			"ext/mbstring/tests/bug63447_002.phpt",
			"ext/mbstring/tests/htmlent.phpt",
			"ext/intl/tests/formatter_format2.phpt",
			"ext/intl/tests/intl_get_error_message.phpt",
			"ext/intl/tests/rbbiter_getbinaryrules_basic.phpt",
			"ext/intl/tests/rbbiter_getrules_basic.phpt",
			"ext/mbstring/tests/mb_ereg_replace-compat-03.phpt",
			"ext/iconv/tests/ob_iconv_handler.phpt",
			"sapi/cli/tests/cli_process_title_windows.phpt",
			"ext/mbstring/tests/ini_language.phpt",
			"ext/mbstring/tests/mb_parse_str02.phpt",
			"ext/mbstring/tests/overload02.phpt",
			"ext/mbstring/tests/php_gr_jp_16242.phpt",
			"tests/basic/req60524-win.phpt",
			"tests/func/011.phpt",
			"zend/tests/unset_cv10.phpt",
			//
			"ext/pdo_mysql/tests/pdo_mysql___construct_ini.phpt",
			"ext/pcre/tests/backtrack_limit.phpt",
			"ext/pcre/tests/recursion_limit.phpt",
			"ext/phar/tests/bug45218_slowtest.phpt",
			"ext/phar/tests/phar_buildfromdirectory6.phpt",
			"ext/reflection/tests/015.phpt",
			"ext/standard/tests/file/bug24482.phpt",
			"ext/standard/tests/file/bug41655_1.phpt",
			"ext/standard/tests/strings/htmlentities10.phpt",
			"tests/basic/bug29971.phpt",
			"tests/lang/short_tags.002.phpt",
			"tests/security/open_basedir_glob_variation.phpt",
			"ext/standard/tests/network/tcp4loop.phpt",
			"ext/standard/tests/network/tcp6loop.phpt",
			"ext/standard/tests/network/udp4loop.phpt",
			"ext/standard/tests/network/udp6loop.phpt",
			"zend/tests/bug52041.phpt",
			"tests/func/bug64523.phpt",
			"zend/tests/halt_compiler4.phpt",
			"ext/filter/tests/004.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-01.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-02.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-03.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-05.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-06.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-07.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-08.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-09.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-11.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-12.phpt",
			"ext/mbstring/tests/mb_output_handler_runtime_ini_alteration-01.phpt",
			"ext/session/tests/bug60860.phpt",
			"ext/standard/tests/strings/htmlentities05.phpt",
			"ext/wddx/tests/004.phpt",
			"ext/wddx/tests/005.phpt",
			"ext/zlib/tests/bug65391.phpt",
			"ext/standard/tests/array/compact.phpt",
			"ext/standard/tests/file/bug45181.phpt",
			"ext/standard/tests/file/file_get_contents_error002.phpt",
			"ext/standard/tests/file/glob_variation2.phpt",
			"ext/standard/tests/file/readfile_variation3.phpt",
			"ext/standard/tests/file/rename_variation9.phpt",
			"ext/standard/tests/network/gethostbyname_error005.phpt",
			"ext/standard/tests/serialize/bug64146.phpt",
			"ext/standard/tests/strings/crypt_chars.phpt",
			"ext/standard/tests/strings/quoted_printable_encode_002.phpt",
			"ext/xsl/tests/bug26384.phpt",
			"ext/xsl/tests/xslt009.phpt",
			"ext/xsl/tests/xslt010.phpt",
			"tests/classes/factory_and_singleton_002.phpt",
			"tests/func/005a.phpt",
			"tests/output/bug60321.phpt",
			"tests/output/ob_get_status.phpt",
			"zend/tests/bug39542.phpt",
			"zend/tests/exception_009.phpt",
			"zend/tests/multibyte/multibyte_encoding_001.phpt",
			"zend/tests/multibyte/multibyte_encoding_005.phpt",
			"zend/tests/ns_086.phpt",
			"ext/curl/tests/curl_copy_handle_basic_008.phpt",
			"ext/curl/tests/curl_curlopt_readdata.phpt",
			"ext/curl/tests/curl_writeheader_callback.phpt",
			"ext/date/tests/bug28024.phpt",
			"ext/date/tests/bug32086.phpt",
			"ext/date/tests/bug35425.phpt",
			"ext/date/tests/date_default_timezone_get-3.phpt",
			"ext/dom/tests/domdocument_load_variation4.phpt",
			"ext/dom/tests/domdocument_loadxml_variation4.phpt",
			"ext/filter/tests/bug52209.phpt",
			"ext/gettext/tests/gettext_basic.phpt",
			"ext/intl/tests/timezone_getdisplayname_variant3.phpt",
			"ext/mbstring/tests/mb_http_input.phpt",
			"ext/pdo_sqlite/tests/bug_63916.phpt",
			"ext/sqlite3/tests/bug63921-64bit.phpt",
			"ext/standard/tests/general_functions/002.phpt",
			"ext/standard/tests/general_functions/006.phpt",
			"ext/standard/tests/strings/htmlentities.phpt",
			"ext/zlib/tests/bug55544.phpt",
			"ext/zlib/tests/bug_52944-darwin.phpt",
			"ext/zlib/tests/ob_001.phpt",
			"sapi/cli/tests/bug65066_100.phpt",
			"ext/date/tests/bug32555.phpt",
			"ext/standard/tests/strings/fprintf_variation_007.phpt",
			"ext/reflection/tests/005.phpt",
			"zend/tests/bug64720.phpt",
			"ext/zip/tests/bug40228.phpt",
			"ext/zip/tests/bug7214.phpt",
			"zend/tests/bug40236.phpt"
		);
	public static Trie NON_WINDOWS_EXTS = PhptTestCase.createExtensions("sysvsem", "sysvmsg", "sysvshm", "gettext", "exif", "readline", "posix", "shmop");
	public static Trie SCENARIO_EXTS = PhptTestCase.createExtensions("dba", "interbase", "intl", "ldap", "imap", "oci8", "pcntl", "pdo_firebird", "pdo_pgsql", "pgsql", "snmp", "sybase_ct", "sybase");

	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup setup, ESAPIType type, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		if (host.isWindows()) {
			if (test_case.isExtension(NON_WINDOWS_EXTS)) {
				// extensions not supported on Windows
				twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "OS not supported", null, null, null, null, null, null, null, null, null, null, null));
				
				return true;
			}
		} else if (test_case.isWin32Test()) {
			// TODO skip windows only extensions (mssql, pdo_mssql, com_dotnet)
			// skip windows specific tests if host is not windows
			// do an early quick check... also fixes problem with sapi/cli/tests/021.phpt
			
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "OS not supported", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		}
		// TODO || ?
		if ((test_case.getExtensionName()!=null&&(test_case.getExtensionName().equals("intl")||test_case.getExtensionName().equals("oci8")))||test_case.containsSection(EPhptSection.REQUEST)||test_case.isNamed(RANDOMLY_FAIL)) {
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
			// TODO temp
		} else if (test_case.isExtension(SCENARIO_EXTS)) {
			// TODO don't bother run these SKIPIFs without the scenario loaded
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, "test would've been skipped", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		}
		return false;
	} // end public boolean willSkip
	
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, FileSystemScenario fs, AHost host, ScenarioSetSetup scenario_set_setup, ESAPIType type, PhpIni ini, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		if (test_case.isExtensionTest() && !build.isExtensionEnabled(cm, fs, host, type, ini, test_case.getExtensionName())) {
			// if extension-under-test is not loaded, don't bother running test since it'll just be skipped (or false fail)
			
			twriter.addResult(host, scenario_set_setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, "Extension not loaded", null, null, null, ini, null, null, null, null, null, null, null));
			
			return true;
		}
		return false;
	} // end public boolean willSkip

	public abstract int getSlowTestTimeSeconds();
	public abstract long getFastTestTimeSeconds();

	public abstract void sortTestCases(List<PhptTestCase> test_cases);

	public boolean isParallelOk() {
		return true;
	}

	public void prep(ConsoleManager cm, AHost storage_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, FileSystemScenario fs, AHost runner_host, PhpIni ini, Map<String,String> env, ActiveTestPack act_test_pack, SourceTestPack<?,?> src_test_pack) {
		
	}
	
} // end public abstract class AbstractSAPIScenario
