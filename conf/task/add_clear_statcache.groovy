
def describe() {
	"PHPT Editing: Add <?php clearstatcache(); ?> to the start of named tests"
}

def helpMsg() {
	"""

== add_clear_statcache example usage ==
ca -c add_clear_statcache -add_clear_statcache_name ext/standard/tests/file/bug24482.phpt php-5.5.0rc3-nts-win32-vc11-x86 php-test-pack-5.5.0rc3

ca -c add_clear_statcache -add_clear_statcache_name ext/standard/tests/file/bug php-5.5.0rc3-nts-win32-vc11-x86 php-test-pack-5.5.0rc3

"""
}

// @Field is required to make this field accessible to methods
@Field String test_name;

def processConsoleOptions(List options) {
	int a = StringUtil.indexOfCS(options, "-add_clear_statcache_name");
	if (a==-1) {
		System.out.println("add_clear_statcache: missing -add_clear_statcache_name console option");
		System.out.println(helpMsg());
		System.exit(-200);
	} else {
		test_name = options[a+1];options.remove(a);options.remove(a);
	}
	
	System.out.println("add_clear_statcache:");
	System.out.println("add_clear_statcache: test name $test_name");
	System.out.println("add_clear_statcache:");
}

import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.PhptTestCase;

def processPHPT(PhptTestCase test_case) {
	if (!test_case.getName().startsWith(test_name))
		return;
		
	String php_code = test_case.get(EPhptSection.FILE);
	php_code = "<?php clearstatcache(); ?>$php_code";
	test_case.put(EPhptSection.FILE, php_code);
}
