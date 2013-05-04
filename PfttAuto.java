package com.mostc.pftt.main;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.commons.net.ftp.FTPClient;
import org.columba.ristretto.message.Address;
import org.columba.ristretto.smtp.SMTPProtocol;

import com.github.mattficken.io.ArrayUtil;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EBuildType;
import com.mostc.pftt.model.core.ECPUArch;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.util.EMailUtil;
import com.mostc.pftt.util.HostEnvUtil;
import com.mostc.pftt.util.EMailUtil.ESMTPAuthMethod;
import com.mostc.pftt.util.EMailUtil.ESMTPSSL;

/*

e-mail report
	-attach file to upload to each email msg
			-with file name already generated
compress and upload result-pack
cron task
	running on JET1

 */


public class PfttAuto {
	public static void main3(String[] args) throws Exception {
		SMTPProtocol smtp = EMailUtil.connect(
				"smtp.gmail.com", 
				465,
				Address.parse("tomattficken@gmail.com"), 
				ESMTPSSL.IMPLICIT_SSL,//.EXPLICIT_SSL, 
				ESMTPAuthMethod.DIGEST_MD5,//.LOGIN, 
				"tomattficken@gmail.com", 
				"plasticmouse".toCharArray()
			);
		System.out.println("49");
		EMailUtil.sendHTMLMessage(
				smtp, 
				Address.parse("tomattficken@gmail.com"), 
				ArrayUtil.toList(Address.parse("v-mafick@microsoft.com")), 
				"test subject", 
				"<html><body><h1>HI</h1></body></html>"
			);
	}
	
	public static void main2(String[] args) throws Exception {
		//FTPSClient ftp = new FTPSClient();
		FTPClient ftp = new FTPClient();
		ftp.connect("131.107.220.66", 21);
		System.err.println(ftp.getReplyString());
		ftp.login("pftt", "1nter0pLAb!!");
		System.err.println(ftp.getReplyString());
		
		ftp.makeDirectory("/PFTT-Results/PHP_5_3/test_dir");
		
		ftp.storeFile("/PFTT-Results/PHP_5_3/test.html", new ByteArrayInputStream("test_file_contents".getBytes()));
		System.err.println(ftp.getReplyString());
		ftp.logout();
		System.err.println(ftp.getReplyString());
		ftp.disconnect();
		System.err.println(ftp.getReplyString());
	}
	
	public static void main(String[] args) throws Exception {
		//Thread.sleep(3*60*60*1000); // TODO temp
		//for (;;) {
		LocalHost host = new LocalHost();
		
		LocalConsoleManager cm = new LocalConsoleManager();
		PfttMain p = new PfttMain(cm);
		Config config, config2;
		EBuildBranch branch;
		EBuildType build_type;
		PfttMain.BuildTestDebugPack btd_pack;
		
		
		
		config = Config.loadConfigFromFiles(cm, "apache", "cli", "opcache", "builtin_web", "no_code_cache");
		config2 = Config.loadConfigFromFiles(cm, "symfony", "apache", "cli", "opcache", "builtin_web", "no_code_cache");
		//filterScenarioSets(config);
		//filterScenarioSets(config2);
		branch = EBuildBranch.PHP_5_5;
		build_type = EBuildType.TS;
		btd_pack = p.releaseGetNewest(false, false, branch, ECPUArch.X86, build_type);
		testBuild(branch, build_type, p, config, config2, btd_pack, cm, host);
		branch = EBuildBranch.PHP_5_5;
		build_type = EBuildType.NTS;
		config = Config.loadConfigFromFiles(cm, "cli", "opcache", "builtin_web", "no_code_cache");
		config2 = Config.loadConfigFromFiles(cm, "symfony", "cli", "opcache", "builtin_web", "no_code_cache");
		btd_pack = p.releaseGetNewest(false, false, branch, ECPUArch.X86, build_type);
		testBuild(branch, build_type, p, config, config2, btd_pack, cm, host);
		// TODO print locations of result packs tested (don't include skipped)
		//    can then manually run cmp-report
		
		
		config = Config.loadConfigFromFiles(cm, "apache", "cli", "opcache", "builtin_web", "no_code_cache");
		config2 = Config.loadConfigFromFiles(cm, "symfony", "apache", "cli", "opcache", "builtin_web", "no_code_cache");
		//filterScenarioSets(config);
		//filterScenarioSets(config2);
		branch = EBuildBranch.PHP_5_5;
		build_type = EBuildType.TS;
		btd_pack = p.releaseGetNewest(false, false, branch, ECPUArch.X64, build_type);
		testBuild(branch, build_type, p, config, config2, btd_pack, cm, host);
		branch = EBuildBranch.PHP_5_5;
		build_type = EBuildType.NTS;
		config = Config.loadConfigFromFiles(cm, "cli");
		config2 = Config.loadConfigFromFiles(cm, "symfony", "cli");
		btd_pack = p.releaseGetNewest(false, false, branch, ECPUArch.X64, build_type);
		testBuild(branch, build_type, p, config, config2, btd_pack, cm, host);
		// TODO print locations of result packs tested (don't include skipped)
		//    can then manually run cmp-report
		
		config = Config.loadConfigFromFiles(cm, "apache", "cli", "opcache", "builtin_web", "no_code_cache");
		config2 = Config.loadConfigFromFiles(cm, "symfony", "apache", "cli", "opcache", "builtin_web", "no_code_cache");
		branch = EBuildBranch.PHP_5_4;
		build_type = EBuildType.TS;
		btd_pack = p.releaseGetNewest(false, false, branch, ECPUArch.X86, build_type);
		testBuild(branch, build_type, p, config, config2, btd_pack, cm, host);
		branch = EBuildBranch.PHP_5_4;
		build_type = EBuildType.NTS;
		// TODO automatically skip apache with NTS
		config = Config.loadConfigFromFiles(cm, "cli", "opcache", "no_code_cache");
		config2 = Config.loadConfigFromFiles(cm, "symfony", "cli", "opcache", "no_code_cache");
		btd_pack = p.releaseGetNewest(false, false, branch, ECPUArch.X86, build_type);
		testBuild(branch, build_type, p, config, config2, btd_pack, cm, host);
		// TODO print locations of result packs tested (don't include skipped)
		//    can then manually run cmp-report
		
		config = Config.loadConfigFromFiles(cm, "apache", "cli", "opcache", "no_code_cache");
		config2 = Config.loadConfigFromFiles(cm, "symfony", "apache", "cli", "opcache", "no_code_cache");
		branch = EBuildBranch.PHP_5_3;
		build_type = EBuildType.TS;
		btd_pack = p.releaseGetNewest(false, false, branch, ECPUArch.X86, build_type);
		testBuild(branch, build_type, p, config, config2, btd_pack, cm, host);
		branch = EBuildBranch.PHP_5_3;
		build_type = EBuildType.NTS;
		// TODO automatically skip apache with NTS
		config = Config.loadConfigFromFiles(cm, "cli", "opcache", "no_code_cache");
		config2 = Config.loadConfigFromFiles(cm, "symfony", "cli", "opcache", "no_code_cache");
		btd_pack = p.releaseGetNewest(false, false, branch, ECPUArch.X86, build_type);
		testBuild(branch, build_type, p, config, config2, btd_pack, cm, host);
		
		
		exit();
		
		//System.err.println("done one iteration of testing. will check for new builds in 10 minutes...");
		//Thread.sleep(10*60*1000); // check every 10 minutes
		
		//}
	}
	static void testBuild(EBuildBranch branch, EBuildType build_type, PfttMain p, Config config, Config config2, PfttMain.BuildTestDebugPack btd_pack, LocalConsoleManager cm, LocalHost host) {
		if (btd_pack==null)
			return;
		try {
			PhpBuild build = new PhpBuild(btd_pack.build);
			build.open(cm, host);
			//
			PhptSourceTestPack test_pack = new PhptSourceTestPack(btd_pack.test_pack);
			test_pack.open(cm, host);
			System.out.println("BUILD: "+build);
			System.out.println("TEST-PACK: "+test_pack);
			PhpResultPackWriter tmgr = new PhpResultPackWriter(host, cm, new File(host.getPhpSdkDir()+"/PFTT-Auto"), build, test_pack);
			if (!tmgr.isFirstForBuild()) {
				cm.println(EPrintType.CLUE, PfttAuto.class, "Already tested this build");
				return; // don't exit, try next build/build-type
			}
				
			// TODO if result-pack exists, assume its already tested and skip this
			p.coreAll(build, test_pack, config, tmgr);
			p.appAll(build, config2, tmgr);
			tmgr.close();
			exit(); // don't use finally
		} catch ( Throwable t ) {
			t.printStackTrace();
			exit(); // don't use finally
		}
	}
	static void exit() {
		System.exit(0);
		try {
			Thread.sleep(60000);
		} catch ( InterruptedException ex ) {}
		Runtime.getRuntime().halt(0);
	}
}
