REM runs all tests on an RC build to make sure the binary is ready for production use

REM rctest <ts build>;<nts build> <phpt test pack>

REM core_all,app_all: 30 scenario sets (+6 for IIS-Standard, +6 for IIS express)
REM ui_all: 12 scenario sets (+3 for IIS-Standard, +3 for IIS express    *2 for Internet Explorer)
pftt -c apache,cli,builtin_web,deduplication,dfs,localfs,opcache,no_code_cache,symfony,joomla,wordpress -no_result_file_for_pass_xskip_skip core_all,app_all,ui_all %*
