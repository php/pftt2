package com.mostc.pftt.main

/** Script executed through PfttMain that downloads snapshot builds, tests them (with PHPTs), archives and uploads
 * telemetry and generates an FBC report to upload and email
 * 
 * @see `pftt exec` command
 * 
 */

import com.mostc.pftt.model.phpt.EBuildBranch;
import com.mostc.pftt.model.phpt.EBuildType;
import com.mostc.pftt.model.smoke.RequiredExtensionsSmokeTest;
import com.mostc.pftt.model.smoke.RequiredFeaturesSmokeTest;


PfttMain pftt;

for (EBuildBranch branch : [EBuildBranch.PHP_5_3, EBuildBranch.PHP_5_4]) {
	for (EBuildType build_type: EBuildType.values()) {
		pair = pftt.get_latest_snapshot_build_test_pack(branch, build_type);
		
		email('No snapshot build found')
		
		download()
		test()
		archive_telem()
		fbc_report()
		upload_telem()
		upload_report()
		email_report()
	}
}
