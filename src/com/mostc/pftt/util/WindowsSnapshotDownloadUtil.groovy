package com.mostc.pftt.util

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EBuildType;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.ECPUArch;

/** Util for parsing the Snapshot download pages on windows.php.net to find
 * newest snapshot to download
 * 
 * Though there may be separate NTS and TS test-packs listed(they are identical), this will use whichever test-pack file is present,
 * as there are not different types of test-packs (there is no NTS or TS test-pack).
 * 
 * @see http://windows.php.net/downloads/snaps/
 * @see #findNewestPair
 * @see #downloadPair
 * @author Matt Ficken
 * 
 */

final class WindowsSnapshotDownloadUtil {
	static final URL PHP_5_3_DOWNLOAD = new URL("http://windows.php.net/downloads/snaps/php-5.3/")
	static final URL PHP_5_4_DOWNLOAD = new URL("http://windows.php.net/downloads/snaps/php-5.4/")
	static final URL PHP_5_5_DOWNLOAD = new URL("http://windows.php.net/downloads/snaps/php-5.5/")
	static final URL PHP_5_6_DOWNLOAD = new URL("http://windows.php.net/downloads/snaps/php-5.6/")
	static final URL PHP_MASTER_DOWNLOAD = new URL("http://windows.php.net/downloads/snaps/master/")

	static File snapshotURLtoLocalFile(AHost host, URL url) {
		String local_path = null;
		if (url.getHost().equals("windows.php.net")) {
			if (url.getPath().contains("release")||url.getPath().contains("qa")||url.getPath().contains("/snaps/php-5.3/")||url.getPath().contains("/snaps/php-5.4/")||url.getPath().contains("/snaps/php-5.5/")||url.getPath().contains("/snaps/master/")) {
				local_path = AHost.basename(url.getPath());
			} else if (url.getPath().startsWith("/downloads/")) {
				// some special build being shared on windows.php.net (probably unstable, expiremental, etc...)
				local_path = url.getPath().replaceAll("/downloads/", "");
				if (local_path.startsWith("/snaps/"))
					local_path = local_path.replaceAll("/snaps/", "");
			}
		}
		if (local_path==null) {
			// fallback: store in directory named after URL: php-sdk/<url>/<build>
			local_path = url.getHost()+"_"+url.getPath().replaceAll("/", "_");
		} else if (local_path.toLowerCase().endsWith(".zip")) {
			local_path = local_path.substring(0, local_path.length()-".zip".length());
		}
		return new File(host.getPhpSdkDir()+"/"+local_path);
	}
	
	static URL getDownloadURL(EBuildBranch branch) {
		switch(branch) {
		case EBuildBranch.PHP_5_3:
			return PHP_5_3_DOWNLOAD;
		case EBuildBranch.PHP_5_4:
			return PHP_5_4_DOWNLOAD;
		case EBuildBranch.PHP_5_5:
			return PHP_5_5_DOWNLOAD;
		case EBuildBranch.PHP_5_6:
			return PHP_5_6_DOWNLOAD;
		case EBuildBranch.MASTER:
			return PHP_MASTER_DOWNLOAD;
		}
		return null;
	}
	
	static URL toSnapURL(EBuildBranch branch, String revision) {
		return new URL(getDownloadURL(branch).toString()+"/"+revision);
	}
		
	static FindBuildTestPackPair getDownloadURL(EBuildBranch branch, EBuildType build_type, ECPUArch cpu_arch, String revision) {
		URL snap_url = toSnapURL(branch, revision);
		FindBuildTestPackPair pair = findPair(build_type, cpu_arch, snap_url);
		pair.branch = branch;
		return pair;
	}
	
	static FindBuildTestPackPair findPair(EBuildType build_type, ECPUArch cpu_arch, URL snap_url) {
		HtmlCleaner cleaner = new HtmlCleaner();
		def node = cleaner.clean(snap_url);
		String xml_str = new SimpleXmlSerializer(cleaner.getProperties()).getXmlAsString(node);
		
		def root = new XmlSlurper(false, false).parseText(xml_str);
		def build_url = null, test_pack_url = null, debug_pack_url = null;
		root.depthFirst().findAll { 
				if (it.name() == 'a') {
					if (it.text().contains(cpu_arch.toString().toLowerCase())) {
					
						if (it.text().endsWith(".zip")&&it.text().toLowerCase().contains("-test-")) {
							test_pack_url = it['@href']
						} else if (it.text().endsWith(".zip")&&it.text().toLowerCase().contains("-"+build_type.toString().toLowerCase()+"-")&&it.text().toLowerCase().contains("-debug-")) {
							debug_pack_url = it['@href']
						} else if (it.text().toLowerCase().contains("-devel-")) {
							// ignore
						} else if (it.text().endsWith(".zip")&&it.text().toLowerCase().contains("-"+build_type.toString().toLowerCase()+"-")) {
							build_url = it['@href'];
						}
						
						
					
					}
				}
			} // end findAll
		if (build_url==null||test_pack_url==null)
			return null;
		FindBuildTestPackPair pair = new FindBuildTestPackPair();
		pair.cpu_arch = cpu_arch;
		pair.build_type = build_type;
		if (build_url!=null)
			pair.build = new URL("http://"+snap_url.getHost()+"/"+build_url);
		if (test_pack_url!=null)
			pair.test_pack = new URL("http://"+snap_url.getHost()+"/"+test_pack_url);
		if (test_pack_url!=null)
			pair.debug_pack = new URL("http://"+snap_url.getHost()+"/"+debug_pack_url);
		return pair;
	}
		
	static class FindBuildTestPackPair {
		URL build, test_pack, debug_pack;
		ECPUArch cpu_arch;
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
	 * @param cpu_arch
	 * @return
	 */
	static boolean hasBuildTypeAndTestPack(URL snap_url, EBuildType build_type, ECPUArch cpu_arch) {
		HtmlCleaner cleaner = new HtmlCleaner();
		def node = cleaner.clean(snap_url);
		String xml_str = new SimpleXmlSerializer(cleaner.getProperties()).getXmlAsString(node);
		
		def root = new XmlSlurper(false, false).parseText(xml_str);
		return root.depthFirst().findAll { it.name() == 'a' && it.text().toLowerCase().contains("-"+build_type.toString().toLowerCase()+"-") }.size() > 0;
	}
		
	/** finds the newest snapshot build and test-pack pair for the specific build type
	 * 
	 * some snapshot releases may have some build types but not all.
	 * 
	 * for example, searching for both TS and NTS builds may return different releases (revision numbers)
	 * 
	 * @param build_type
	 * @param cpu_arch
	 * @param download_url
	 * @return
	 */
	static FindBuildTestPackPair findNewestPair(EBuildType build_type, ECPUArch cpu_arch, URL download_url) {
		FindBuildTestPackPair pair;
		
		// keep searching until specific build type is found. some build types might be missing from a release,
		// while others are there. also, some releases may just be empty.
		for ( URL snap_url : getSnapshotURLSNewestFirst(download_url) ) {
			pair = findPair(build_type, cpu_arch, snap_url)
			if (pair != null)
				return pair;
		}
		
		return null;
	}
	
	/** 
	 * 
	 * @param build_type
	 * @param cpu_arch
	 * @param download_url
	 * @return
	 */
	static FindBuildTestPackPair findPreviousPair(EBuildType build_type, ECPUArch cpu_arch, URL download_url) {
		FindBuildTestPackPair pair;
		
		// keep searching until specific build type is found. some build types might be missing from a release,
		// while others are there. also, some releases may just be empty.
		boolean first = true;
		for ( URL snap_url : getSnapshotURLSNewestFirst(download_url) ) {
			pair = findPair(build_type, cpu_arch, snap_url)
			if (pair != null) {
				if (first) {
					first = false; // skip first pair
					continue; // find next pair
				}
				return pair;
			}
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
		HtmlCleaner cleaner = new HtmlCleaner();
		def node;
		try {
			node = cleaner.clean(download_url);
		} catch ( Exception ex ) {
			return new ArrayList<URL>(0);
		}
		// turn HTML into XML for it to work (its really HTML not XHTML)
		String xml_str = new SimpleXmlSerializer(cleaner.getProperties()).getXmlAsString(node);
		
		Node root = new XmlParser().parseText(xml_str);
		
		// this is IIS's old school virtual directory listing:
		// a <pre> contains a bunch of: <br><br>date <dir><a...>
		// (link doesn't include the server name... includes path from server name though)
		//
		// find the pre
		ArrayList links = new ArrayList();
		root.depthFirst().each {
			if (it.name()=='pre')
				// then find br, the date, and <a>, 
				// the <a> is AFTER the <br> its associated with
				// this gets the dates and links
				handlePre(links, it);
		}
		
		// now, finally have links and dates... sort by date (newest first)
		def link_comparator = [
			compare: { a, b ->
				b['date'].compareTo(a['date'])
			}
		  ] as Comparator;
	  	Collections.sort(links, link_comparator);
		
		// add server name to these links (it includes path from server name though, so don't need whole base url... but do it anyway)
		ArrayList<URL> urls = new ArrayList<URL>(links.size());
		for ( def link : links )
			urls.add(new URL(download_url.toString()+"/"+getRevision(link['link'])));
		
		return urls;
	}
	private static void handlePre(List links, Node pre) {
		String date_str = null;
		Date date = null;
		for ( Object child : pre.value() ) {
			if (child instanceof Node) {
				if (child.name()=='a' && child.text().startsWith('r')) {
					if (date_str==null) {
						date = null;
					} else {
						date_str = date_str.replace("<dir>", "").trim();
						
						try {
							date = new Date(date_str);
						} catch ( IllegalArgumentException ex ) {
							date = null; // just in case, shouldn't happen
						}
					}
					
					links.add([
							link: child.@href, 
							date: date
						])
					
					date_str = null;
				}	
			} else if (child instanceof String) {
				date_str = child;
			}
		}
	} // end static void handlePre
	static String getRevision(URL url) {
		return getRevision(url.getPath());
	}
	static String getRevision(String path) {
		if (path.endsWith("/"))
			path = path.substring(0, path.length()-1);
		int i = path.lastIndexOf('/');
		return i == -1 ? path : path.substring(i+1);
	}
	
	private WindowsSnapshotDownloadUtil() {}
} // end final class WindowsSnapshotDownloadUtil
