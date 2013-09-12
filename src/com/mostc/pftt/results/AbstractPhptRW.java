package com.mostc.pftt.results;

import java.util.List;

import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EPhptTestStatus;

public abstract class AbstractPhptRW extends AbstractTestResultRW {
	public abstract EBuildBranch getTestPackBranch();
	public abstract String getTestPackVersion();
	@Override
	public float passRate() {
		return PhpResultPack.round1(100.0f * ((float)count(EPhptTestStatus.PASS)) / ((float)(count(EPhptTestStatus.PASS) + count(EPhptTestStatus.CRASH) + count(EPhptTestStatus.FAIL) + count(EPhptTestStatus.TIMEOUT))));
	}
	public abstract int count(EPhptTestStatus status);
	public abstract List<String> getTestNames(EPhptTestStatus status);
	
	public boolean isTooMuchChange(AbstractPhptRW base) {
		return 
				( 10 < Math.abs(count(EPhptTestStatus.FAIL) - base.count(EPhptTestStatus.FAIL)) )
				|| ( 20 < Math.abs(count(EPhptTestStatus.CRASH) - base.count(EPhptTestStatus.CRASH)))
				|| ( 100 < Math.abs(count(EPhptTestStatus.PASS) - base.count(EPhptTestStatus.PASS)))
			;
	}
	public boolean isTestStatus(String test_name, EPhptTestStatus status) {
		return getTestNames(status).contains(test_name);
	}
	public abstract String getPath();
	
	protected void check(EPhptTestStatus status, List<String> names) {
		if (status==EPhptTestStatus.FAIL) {
			// TODO temp

			names.remove("ext/date/tests/bug33957.phpt");
			names.remove("ext/date/tests/bug34087.phpt");
			names.remove("ext/gd/tests/bug39780.phpt");
			names.remove("ext/date/tests/bug52062.phpt");
			names.remove("ext/date/tests/bug54340.phpt");
			names.remove("ext/date/tests/date_add_basic2.phpt");
			names.remove("ext/date/tests/bug20382-1.phpt");
names.remove("ext/date/tests/bug32270.phpt");
names.remove("ext/date/tests/bug49700.phpt");
names.remove("ext/date/tests/bug50680.phpt");
names.remove("ext/date/tests/bug60236.phpt");
names.remove("ext/date/tests/date_create_from_format_basic.phpt");
names.remove("ext/date/tests/date_diff1.phpt");
names.remove("ext/date/tests/date_parse_from_format_basic.phpt");
names.remove("ext/date/tests/strtotime_basic2.phpt");
names.remove("ext/ereg/tests/004.phpt");
names.remove("ext/standard/tests/file/file_get_contents_error001.phpt");
names.remove("ext/standard/tests/file/windows_links/bug48746_2.phpt");
			names.remove("ext/reflection/tests/005.phpt");
			names.remove("sapi/cli/tests/bug65066_511.phpt");
			names.remove("ext/zlib/tests/ob_001.phpt");
			names.remove("sapi/cli/tests/bug61679.phpt");
			names.remove("ext/date/tests/bug28024.phpt");
			names.remove("ext/date/tests/bug32086.phpt");
			names.remove("ext/date/tests/bug35425.phpt");
			names.remove("ext/date/tests/date_default_timezone_get-3.phpt");
			names.remove("ext/gettext/tests/gettext_phpinfo.phpt");
			names.remove("ext/xsl/tests/xsl-phpinfo.phpt");
			names.remove("tests/basic/027.phpt");
			names.remove("tests/lang/bug35176.phpt");
			names.remove("ext/standard/tests/general_functions/002.phpt");
			names.remove("ext/standard/tests/general_functions/006.phpt");
			names.remove("ext/standard/tests/strings/htmlentities.phpt");
			names.remove("ext/dom/tests/domdocument_load_variation4.phpt");
			names.remove("ext/dom/tests/domdocument_loadxml_variation4.phpt");
			names.remove("ext/mysql/tests/bug53649.phpt");
			names.remove("ext/mysqli/tests/022.phpt");
			names.remove("ext/mysqli/tests/mysqli_insert_packet_overflow.phpt");
			names.remove("ext/mysqli/tests/mysqli_set_local_infile_default.phpt");
			names.remove("ext/mysqli/tests/mysqli_stmt_bind_param_many_columns.phpt");
			names.remove("ext/mysqli/tests/mysqli_fetch_field.phpt");
			names.remove("ext/mysqli/tests/mysqli_insert_id_variation.phpt");
			names.remove("ext/mysqli/tests/mysqli_prepare_no_object.phpt");
			names.remove("ext/tidy/tests/020.phpt");
			names.remove("tests/basic/022.phpt");
			names.remove("tests/output/ob_018.phpt");
			names.remove("ext/filter/tests/004.phpt");
			names.remove("ext/wddx/tests/004.phpt");
			names.remove("ext/wddx/tests/005.phpt");
			names.remove("tests/func/bug64523.phpt");
			names.remove("ext/standard/tests/strings/fprintf_variation_004.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_pattern-01.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_pattern-02.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_pattern-03.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_pattern-05.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_pattern-06.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_pattern-07.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_pattern-08.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_pattern-09.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_pattern-11.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_pattern-12.phpt");
			names.remove("ext/mbstring/tests/mb_output_handler_runtime_ini_alteration-01.phpt");
			names.remove("ext/mysqli/tests/bug45289.phpt");
			names.remove("ext/mysqli/tests/mysqli_fetch_field_direct_oo.phpt");
			names.remove("ext/mysqli/tests/mysqli_get_server_version.phpt");
			names.remove("ext/mysqli/tests/mysqli_prepare.phpt");
			names.remove("ext/mysqli/tests/mysqli_stmt_get_warnings.phpt");
			names.remove("sapi/cli/tests/bug65066_100.phpt");
			names.remove("sapi/cli/tests/bug65066_422.phpt");
			names.remove("ext/phar/tests/tar/phar_commitwrite.phpt");
			names.remove("ext/phar/tests/tar/phar_setsignaturealgo2.phpt");
			names.remove("ext/phar/tests/zip/phar_commitwrite.phpt");
		}
	}
}
