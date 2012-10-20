package com.mostc.pftt.util

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EBuildType;
import com.mostc.pftt.model.phpt.EBuildBranch;

/** Util for parsing the Snapshot download pages on windows.php.net to find
 * newest snapshot to download
 * 
 * Though there may be separate NTS and TS test-packs listed(they are identical), this will use whichever test-pack file is present,
 * as there are not different types of test-packs (there is no NTS or TS test-pack).
 * 
 * @see #findNewestPair
 * @see #downloadPair
 * 
 */

final class WindowsSnapshotDownloadUtil {
	static final URL PHP_5_3_DOWNLOAD = new URL("http://windows.php.net/downloads/snaps/php-5.3/")
	static final URL PHP_5_4_DOWNLOAD = new URL("http://windows.php.net/downloads/snaps/php-5.4/")
	static final URL PHP_5_5_DOWNLOAD = new URL("http://windows.php.net/downloads/snaps/php-5.5/")
	static final URL PHP_MASTER_DOWNLOAD = new URL("http://windows.php.net/downloads/snaps/master/")

	static URL getDownloadURL(EBuildBranch branch) {
		switch(branch) {
		case EBuildBranch.PHP_5_3:
			return PHP_5_3_DOWNLOAD;
		case EBuildBranch.PHP_5_4:
			return PHP_5_4_DOWNLOAD;
		case EBuildBranch.PHP_5_5:
			return PHP_5_5_DOWNLOAD;
		case EBuildBranch.MASTER:
			return PHP_MASTER_DOWNLOAD;
		}
		return null;
	}
	
	static class DownloadBuildTestPackPair {
		String build_path, test_pack_path;
		FindBuildTestPackPair found_pair;
	}
	
	/** downloads a found build and test-pack pair to the host and returns the file paths where both are stored
	 * 
	 * @param host
	 * @param found_pair
	 * @return
	 */
	static DownloadBuildTestPackPair downloadPair(Host host, FindBuildTestPackPair found_pair) {
		return new DownloadBuildTestPackPair();
	}
	
	static class FindBuildTestPackPair {
		URL build, test_pack;
		EBuildType build_type;
		EBuildBranch branch;
	}	
	
	/** checks if the snapshot release at the given URL has a test-pack and a build of the given type.
	 * 
	 * Though there may be separate NTS and TS test-packs listed(they are identical), this will use whichever test-pack file is present,
	 * as there are not different types of test-packs (there is no NTS or TS test-pack).
	 * 
	 * @param url
	 * @param build_type
	 * @return
	 */
	static boolean hasBuildTypeAndTestPack(URL url, EBuildType build_type) {
		return false;
	}
	
	static FindBuildTestPackPair findPair(EBuildType build_type, URL snap_url) {
		def root = new XmlSlurper().parse(snap_url);
		def testcases = root.depthFirst().findAll { it.name() == 'testcase' }
		
		return null;
	}
	
	/** finds the newest snapshot build and test-pack pair for the specific build type
	 * 
	 * some snapshot releases may have some build types but not all.
	 * 
	 * for example, searching for both TS and NTS builds may return different releases (revision numbers)
	 * 
	 * @param build_type
	 * @param download_url
	 * @return
	 */
	static FindBuildTestPackPair findNewestPair(EBuildType build_type, URL download_url) {
		FindBuildTestPackPair pair;
		
		// keep searching until specific build type is found. some build types might be missing from a release,
		// while others are there. also, some releases may just be empty.
		for ( URL snap_url : getSnapshotURLSNewestFirst(download_url) ) {
			pair = findPair(build_type, snap_url)
			if (pair != null)
				return pair;
		}
		
		return null;
	}

	/** parses windows.php.net's index page to find all the snapshot build revisions and sorts them
	 * with the newest snapshot URL (by creation time) first
	 * 
	 * @param download_url
	 * @return
	 */
	static List<URL> getSnapshotURLSNewestFirst(URL download_url) {
		def root = new XmlSlurper().parse(download_url);
		def testcases = root.depthFirst().findAll { it.name() == 'testcase' }
		// TODO
	}
	
	private WindowsSnapshotDownloadUtil() {}
} // end final class WindowsSnapshotDownloadUtil
