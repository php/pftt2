package com.mostc.pftt.main;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.columba.ristretto.message.Address;
import org.columba.ristretto.parser.AddressParser;
import org.columba.ristretto.smtp.SMTPException;

import com.github.mattficken.io.ArrayUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.main.CmpReport.IRecvr;
import com.mostc.pftt.main.CmpReport.Mail;
import com.mostc.pftt.main.CmpReport.Mailer;
import com.mostc.pftt.main.CmpReport.PublishReport;
import com.mostc.pftt.main.CmpReport.Upload;
import com.mostc.pftt.main.PfttMain.ERevisionGetOption;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EBuildType;
import com.mostc.pftt.model.core.ECPUArch;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPack;
import com.mostc.pftt.results.PhpResultPackReader;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.util.TimerUtil;

// @see http://yajsw.sourceforge.net/
// TODO cleanup temp dirs
// TODO cleanup really old build, test, debug packs
public class PfttAuto {
	static final BuildSpec[] BUILD_SPECS = new BuildSpec[] {
			new BuildSpec(EBuildBranch.PHP_7_1, EBuildType.TS, ECPUArch.X64),
			new BuildSpec(EBuildBranch.PHP_7_1, EBuildType.NTS, ECPUArch.X64),	
			new BuildSpec(EBuildBranch.PHP_7_1, EBuildType.TS, ECPUArch.X86),
			new BuildSpec(EBuildBranch.PHP_7_1, EBuildType.NTS, ECPUArch.X86),			
			new BuildSpec(EBuildBranch.PHP_7_0, EBuildType.TS, ECPUArch.X64),
			new BuildSpec(EBuildBranch.PHP_7_0, EBuildType.NTS, ECPUArch.X64),	
			new BuildSpec(EBuildBranch.PHP_7_0, EBuildType.TS, ECPUArch.X86),
			new BuildSpec(EBuildBranch.PHP_7_0, EBuildType.NTS, ECPUArch.X86),
			new BuildSpec(EBuildBranch.PHP_Master, EBuildType.TS, ECPUArch.X64),
			new BuildSpec(EBuildBranch.PHP_Master, EBuildType.NTS, ECPUArch.X64),	
			new BuildSpec(EBuildBranch.PHP_Master, EBuildType.TS, ECPUArch.X86),
			new BuildSpec(EBuildBranch.PHP_Master, EBuildType.NTS, ECPUArch.X86),
			new BuildSpec(EBuildBranch.PHP_5_6, EBuildType.TS, ECPUArch.X86),
			new BuildSpec(EBuildBranch.PHP_5_6, EBuildType.NTS, ECPUArch.X86),
			new BuildSpec(EBuildBranch.PHP_5_5, EBuildType.TS, ECPUArch.X86),
			new BuildSpec(EBuildBranch.PHP_5_5, EBuildType.NTS, ECPUArch.X86),
			new BuildSpec(EBuildBranch.PHP_5_6, EBuildType.TS, ECPUArch.X64),
			new BuildSpec(EBuildBranch.PHP_5_6, EBuildType.NTS, ECPUArch.X64),
			new BuildSpec(EBuildBranch.PHP_5_5, EBuildType.TS, ECPUArch.X64),
			new BuildSpec(EBuildBranch.PHP_5_5, EBuildType.NTS, ECPUArch.X64),
			new BuildSpec(EBuildBranch.PHP_5_4, EBuildType.TS, ECPUArch.X86),
			new BuildSpec(EBuildBranch.PHP_5_4, EBuildType.NTS, ECPUArch.X86),
			//new BuildSpec(EBuildBranch.PHP_5_3, EBuildType.TS, ECPUArch.X86),
			//new BuildSpec(EBuildBranch.PHP_5_3, EBuildType.NTS, ECPUArch.X86)
		};
	
	static class BuildSpec {
		final EBuildBranch branch;
		final EBuildType type;
		final ECPUArch cpu;
		
		BuildSpec(EBuildBranch branch, EBuildType type, ECPUArch cpu) {
			this.branch = branch;
			this.type = type;
			this.cpu = cpu;
		}
		
		boolean equals(File dir) {
			String name = dir.getName().toLowerCase();
			return name.contains(branch.toString().toLowerCase())
				&& name.contains("-"+type.toString().toLowerCase()+"-")
				&& name.contains(cpu.toString().toLowerCase());
		}
	}
	static File getMostRecentDir(File dir) {
		File[] files = dir.listFiles();
		if (files==null||files.length==0)
			return null;
		File out = files[0];
		for ( File file : files ) {
			// convention: ignore any result-pack starting with '_'
			if (!file.getName().startsWith("_") && file.lastModified()>out.lastModified())
				out = file;
		}
		return out;
	}
	static void cleanup(LocalHost host) throws Exception {
		if (host.isWindows()) {
			// cleanup any stray php processes
			host.exec("taskkill /im:windbg.exe /f /t", LocalHost.ONE_MINUTE);
			host.exec("taskkill /im:php.exe /f /t", LocalHost.ONE_MINUTE);
			host.exec("taskkill /im:php-cgi.exe /f /t", LocalHost.ONE_MINUTE);
			host.exec("taskkill /im:httpd.exe /f /t", LocalHost.ONE_MINUTE);
			host.exec("taskkill /im:mysqld.exe /f /t", LocalHost.ONE_MINUTE);
		}
	}
	static void exit(LocalHost host) throws Exception {
		cleanup(host);
		PfttMain.exit();
	}
	public static void main(String[] args) throws Exception {
		//Mail m = new Mail(false, false, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com")});
		//m.connect();
		//m.sendMail(false, "[PUTS] test", "<html><body>test</body></html>", "test");
		//System.exit(0);
		
		
		LocalConsoleManager cm = new LocalConsoleManager() {
			@Override
			public boolean isSkipSmokeTests() {
				return true;
			}
		};
		PfttMain pftt = new PfttMain(cm, null);
		PfttMain.is_puts = true;
		final LocalHost host = LocalHost.getInstance();
		cleanup(host);
		
		// make sure PUTS never runs for more than 16 hours
		Thread exit_thread = new Thread() {
				public void run() {
					if (TimerUtil.trySleepSeconds(16*60*60)) {
						try {
							exit(host);
						} catch ( Exception ex ) {}
					}
				}
			};
		exit_thread.setDaemon(true);
		exit_thread.start();
		
		File most_recent_dir = getMostRecentDir(new File(host.getPhpSdkDir()+"/PFTT-Auto"));
		
		boolean b = false;
		for (BuildSpec bs : BUILD_SPECS) {
			if (most_recent_dir!=null && bs.equals(most_recent_dir))
				continue;
			
			try {
				b = testBuild(bs, pftt, cm, host);
			} catch ( Throwable t ) {
				ConsoleManagerUtil.printStackTrace(PfttAuto.class, t);
			}
		}
		
		if (b) {
			// service will be restarted immediately, so wait for 15 minutes before restarting
			TimerUtil.trySleepSeconds(15 * 60);
		} else {
			// no builds available, check once an hour
			TimerUtil.trySleepSeconds(60 * 60);
		}
		exit(host);
	}
	static boolean testBuild(BuildSpec bs, PfttMain pftt, LocalConsoleManager cm, LocalHost host) throws Exception {
		return testBuild(bs, bs.branch, bs.type, bs.cpu, pftt, cm, host);
	}
	static boolean testBuild(BuildSpec bs, EBuildBranch branch, EBuildType build_type, ECPUArch cpu_arch, PfttMain pftt, LocalConsoleManager cm, LocalHost host) throws Exception {
		PfttMain.BuildTestDebugPack btd_pack = null;
		// try several times to download in case download fails
		for ( int i=0 ; i < 3 ; i++ ) {
			try {
				btd_pack = pftt.releaseGetNewest(true, false, branch, cpu_arch, build_type, ERevisionGetOption.ALL);
				break;
			} catch ( Exception ex ) {
				ConsoleManagerUtil.printStackTrace(PfttAuto.class, cm, ex);
				// try again
			}
		}
		if (btd_pack==null)
			// download failed
			return false;
		
		PhpBuild build;
		try {
			build = new PhpBuild(btd_pack.build);
			//build = new PhpBuild("C:\\php-sdk\\php-5.5-ts-windows-vc11-x86-r5e3da04");
			build.open(cm, host);
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(PfttAuto.class, ex);
			return false;
		}
		
		ET a = testCore(bs, build, btd_pack.test_pack, pftt, cm, host);
		ET b = null;
		if (build.isX86()&&branch!=EBuildBranch.PHP_5_6) {
			b = testMSSQL(bs, build, btd_pack.test_pack, pftt, cm, host);
		}
		if (a==ET.FINISHED_TESTING_NEWEST||b==ET.FINISHED_TESTING_NEWEST) {
			exit(host);
			return true;
		}
		return false;
	}
	enum ET {
		NEWEST_ALREADY_TESTED, FINISHED_TESTING_NEWEST
	}
	static final IRecvr UPLOAD = new Upload();
	static ET testMSSQL(BuildSpec bs, PhpBuild build, String test_pack_path, PfttMain pftt, LocalConsoleManager cm, LocalHost host) throws SMTPException, Exception {
		final IRecvr MSSQL_EMAIL = (
				// TODO wincache@microsoft.com;jaykint@microsoft.com,shekharj@microsoft.com,ostcphp@microsoft.com
				//MSSQL_PRODUCTION_SNAP = 
				new Mail("MSSQL", false, false, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com"),AddressParser.parseAddress("jaykint@microsoft.com")})
				//TEST
				//new Mail("MSSQL", true, false, new Address[]{AddressParser.parseAddress("v-mafick@microsoft.com")})
			);
		
		final File auto_dir = new File(host.getPhpSdkDir()+"/PFTT-MSSQL"); // TODO for MSSQL
		
		Config sql_config = Config.loadConfigFromFiles(cm, "mssql10", "mssql11", "cli", "opcache", "no_code_cache", "builtin_web");
		
		PhptSourceTestPack sql_test_pack = new PhptSourceTestPack("C:\\php-sdk\\php_sqlsvr-test-pack-2013-09-03");
		sql_test_pack.open(cm, sql_config, Scenario.LOCALFILESYSTEM_SCENARIO, host);
		
		PhpResultPack base_pack = findBaseResultPack(sql_test_pack, bs, auto_dir, cm, host);
		if (base_pack!=null && build.getBuildInfo(cm, host).equals(base_pack.getBuildInfo())) {
			return ET.NEWEST_ALREADY_TESTED;
		}
		PhpResultPackWriter result_pack = new PhpResultPackWriter(host, cm, auto_dir, build, sql_test_pack, sql_config);
		
		pftt.coreAll(build, sql_test_pack, sql_config, result_pack);
	
		result_pack.close();
		
		if (base_pack!=null) {
			CmpReport.report("MSSQL", cm, MSSQL_EMAIL, base_pack, result_pack);
			CmpReport.report("MSSQL", cm, UPLOAD, base_pack, result_pack);
		}
		return ET.FINISHED_TESTING_NEWEST;
	}
	static ET testCore(BuildSpec bs, PhpBuild build, String test_pack_path, PfttMain pftt, LocalConsoleManager cm, LocalHost host) throws IOException, SMTPException, Exception {
		Config config = Config.loadConfigFromFiles(cm, "snap_test");
		
		//
		LinkedList<String> coptions = new LinkedList<String>();
		config.processConsoleOptions(cm, coptions);
		Iterator<String> coptions_it = coptions.iterator();
		LinkedList<String> cfiles = new LinkedList<String>();
		String coption;
		while (coptions_it.hasNext()) {
			coption = coptions_it.next();
			if (coption.equalsIgnoreCase("-c")||coption.equalsIgnoreCase("-config")) {
				for ( String cfile : coptions_it.next().split(",") )
					cfiles.add(cfile);
			}
		}
		/*if (bs.cpu==ECPUArch.X64) {
			// these scenario-sets don't work on x64 builds
			cfiles.add("not_opcache_cli");
			cfiles.add("not_opcache_builtin_web");
			cfiles.add("not_builtin_web");
			cfiles.add("not_opcache_apache");
		}*/
		if (!cfiles.isEmpty()) {
			cfiles.add("snap_test");
			config = Config.loadConfigFromFiles(cm, ArrayUtil.toArray(cfiles));
		}
		
		final IRecvr PUBLISH_REPORT = new PublishReport();
		final IRecvr CORE_EMAIL = (
					// TODO wincache@microsoft.com,ostcphp@microsoft.com
					//CORE_PRODUCTION_SNAP
					new Mail(false, false, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com")})
					//TEST
					//new Mail(true, false, new Address[]{AddressParser.parseAddress("v-mafick@microsoft.com")})
				);
		
		PhptSourceTestPack core_test_pack = new PhptSourceTestPack(test_pack_path);
		core_test_pack.open(cm, config, Scenario.LOCALFILESYSTEM_SCENARIO, host);
		final File auto_dir = new File(host.getPhpSdkDir()+"/PFTT-Auto");
		PhpResultPack base_pack = findBaseResultPack(core_test_pack, bs, auto_dir, cm, host);
		if (base_pack!=null && build.getBuildInfo(cm, host).equals(base_pack.getBuildInfo())) {
			return ET.NEWEST_ALREADY_TESTED;
		}
		PhpResultPackWriter result_pack = new PhpResultPackWriter(host, cm, auto_dir, build, core_test_pack, config);
		
		pftt.coreAll(build, core_test_pack, config, result_pack);
		pftt.appAll(build, config, result_pack);
		
		result_pack.close();
	
		if (base_pack!=null) {
			// TODO temp CmpReport.report("Core", cm, PUBLISH_REPORT, base_pack, result_pack);
			// TODO temp CmpReport.report("Core", cm, CORE_EMAIL, base_pack, result_pack);
			CmpReport.report("Core", cm, UPLOAD, base_pack, result_pack);
		} 
		if (base_pack!=null) {
			final Mailer summary_mailer = new Mailer(false, false, new Address[]{AddressParser.parseAddress("qa-reports@lists.php.net")});
			PhpResultPackReader base_packr = PhpResultPackReader.open(cm, host, base_pack.getResultPackPath());
			PhpResultPackReader test_packr = PhpResultPackReader.open(cm, host, result_pack.getResultPackPath());
			CmpReport.summary(summary_mailer, cm, base_packr, test_packr);
		}
		return ET.FINISHED_TESTING_NEWEST;
	}
	private static PhpResultPack findBaseResultPack(PhptSourceTestPack src_test_pack, BuildSpec bs, File auto_dir, ConsoleManager cm, AHost host) throws IllegalStateException, IOException {
		if (auto_dir==null)
			return null;
		File[] files = auto_dir.listFiles();
		if (files==null)
			return null;
		File last = null;
		for ( File result_pack : files ) {
			// TODO temp sql prefix in result-pack name
			if (result_pack!=null&&result_pack.isDirectory() 
					//&& src_test_pack.getNameAndVersionString().contains("sql")==result_pack.getName().contains("sql") 
					&& bs.equals(result_pack) 
					&& (last==null||result_pack.lastModified()>last.lastModified())) {
				last = result_pack;
			}
		}
		System.out.println("Base: "+bs+" "+src_test_pack+" "+auto_dir+" "+last);
		if (last==null)
			return null;
		
		CmpReport.clean_hosts(host, last);
		return PhpResultPackReader.open(cm, host, last);
	}
}
