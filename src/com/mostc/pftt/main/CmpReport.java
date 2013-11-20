package com.mostc.pftt.main;

import groovy.xml.MarkupBuilder;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.columba.ristretto.message.Address;
import org.columba.ristretto.parser.AddressParser;
import org.columba.ristretto.parser.ParserException;
import org.columba.ristretto.smtp.SMTPException;
import org.columba.ristretto.smtp.SMTPProtocol;

import com.github.mattficken.io.ArrayUtil;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.results.AbstractPhpUnitRW;
import com.mostc.pftt.results.AbstractPhptRW;
import com.mostc.pftt.results.AbstractTestResultRW;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPack;
import com.mostc.pftt.results.PhpResultPackReader;
import com.mostc.pftt.results.TextBuilder;
import com.mostc.pftt.util.EMailUtil;
import com.mostc.pftt.util.EMailUtil.ESMTPAuthMethod;
import com.mostc.pftt.util.EMailUtil.ESMTPSSL;

public class CmpReport {
	static String generateFileName(AbstractPhptRW base, AbstractPhptRW test) {
		String file_name = "PHPT_CMP_"
				+base.getBuildInfo().getBuildBranch()+"-"+base.getBuildInfo().getVersionRevision()+"-"+base.getBuildInfo().getBuildType()+"-"+base.getBuildInfo().getCPUArch()+"-"+base.getBuildInfo().getCompiler()+"_"+base.getScenarioSetNameWithVersionInfo()+
				"_v_"
				+test.getBuildInfo().getBuildBranch()+"-"+test.getBuildInfo().getVersionRevision()+"-"+test.getBuildInfo().getBuildType()+"-"+test.getBuildInfo().getCPUArch()+"-"+test.getBuildInfo().getCompiler()+"_"+test.getScenarioSetNameWithVersionInfo();
		file_name = StringUtil.max(file_name, 100);
		
		return file_name + ".html";
	}
	static String generateFileName(AbstractPhpUnitRW base, AbstractPhpUnitRW test) {
		String file_name = "PhpUnit_CMP_"+test.getTestPackNameAndVersionString()+"_"
				+base.getBuildInfo().getBuildBranch()+"-"+base.getBuildInfo().getVersionRevision()+"-"+base.getBuildInfo().getBuildType()+"-"+base.getBuildInfo().getCPUArch()+"-"+base.getBuildInfo().getCompiler()+"_"+base.getScenarioSetNameWithVersionInfo()+
				"_v_"
				+test.getBuildInfo().getBuildBranch()+"-"+test.getBuildInfo().getVersionRevision()+"-"+test.getBuildInfo().getBuildType()+"-"+test.getBuildInfo().getCPUArch()+"-"+test.getBuildInfo().getCompiler()+"_"+test.getScenarioSetNameWithVersionInfo();
		file_name = StringUtil.max(file_name, 80);
		
		return file_name + ".html";
	}
	static class Verify implements IRecvr {

		@Override
		public void recv(AbstractPhptRW base, AbstractPhptRW test, PHPTMultiHostTwoBuildSingleScenarioSetReportGen phpt_report, String html_str) throws IOException {
			File html_file = new File("c:\\php-sdk\\"+generateFileName(base, test));
			FileWriter fw = new FileWriter(html_file);
			fw.write(html_str);
			fw.close();
			
			Desktop.getDesktop().browse(html_file.toURI());
		}

		@Override
		public void recv(AbstractPhpUnitRW base, AbstractPhpUnitRW test, PhpUnitMultiHostTwoBuildSingleScenarioSetReportGen php_unit_report, String html_str) throws IOException {
			File html_file = new File("c:\\php-sdk\\"+generateFileName(base, test));
			FileWriter fw = new FileWriter(html_file);
			fw.write(html_str);
			fw.close();
			Desktop.getDesktop().browse(html_file.toURI());
		}

		@Override
		public void start(PhpResultPack test_pack) throws Exception {}

		@Override
		public void stop(PhpResultPack test_pack) throws Exception {}
		
	}
	static class Mailer {
		final Address from, puts_admin;
		final Address[] puts_users;
		SMTPProtocol smtp;
		final boolean debug, force;
		LinkedList<String> message_bodies = new LinkedList<String>();
		final String phpt_prefix;
		
		Mailer(boolean debug, boolean force, Address[] puts_users) throws ParserException {
			this("Core", debug, force, puts_users);
		}
		
		Mailer(String phpt_prefix, boolean debug, boolean force, Address[] puts_users) throws ParserException {
			this.phpt_prefix = phpt_prefix;
			this.debug = debug;
			this.force = force;
			from = AddressParser.parseAddress("ostcpftt@outlook.com");
			this.puts_users = puts_users;
			puts_admin = AddressParser.parseAddress("v-mafick@microsoft.com");
		}
		
		protected void connect() throws IOException, SMTPException {
			smtp = EMailUtil.connect("smtp.live.com", 587, from, ESMTPSSL.IMPLICIT_SSL, ESMTPAuthMethod.PLAIN, "ostcpftt@outlook.com", "php868841432".toCharArray());
		}
		
		protected String getSubjectPrefix(Address to) {
			String ma = to.getMailAddress();
			int i = ma.indexOf('@');
			if (i==-1)
				return "";
			ma = ma.substring(0, i);
			ma = ma.trim();
			if (ma.length()==0)
				return "";
			else
				return "["+ma.toUpperCase()+"] ";
		}
		protected void sendMail(boolean too_much_change, String subject, String html_str) throws IOException, SMTPException, Exception {
			// prevent sending duplicate messages
			if (message_bodies.contains(html_str)) {
				return;
			}
			message_bodies.add(html_str);
			Collection<Address> recipients;
			if (!force && too_much_change) {
				System.err.println("Debug: too much change, sending to admin only");
				recipients = ArrayUtil.toList(puts_admin);
				subject = "[PUTS] Review " + subject;
			} else {
				// this is probably going to a mailing list
				// a common convention for mailing lists is for automated mail to
				// have a subject prefixed with the mailing list's name
				//subject = getSubjectPrefix(report_to)+subject;
				//
				// instead, promote the `PUTS` name
				subject = "[PUTS] "+subject;
				if (debug)
					recipients = ArrayUtil.toList(puts_admin);
				else
					recipients = ArrayUtil.toList(puts_users);
			}
			try {
				EMailUtil.sendHTMLMessage(
						smtp, 
						from, 
						recipients, 
						subject,
						html_str
					);
			} catch ( Exception ex ) {
				// errors seen:
				// 5.3.4 Requested action not taken; We noticed some unusual activity in your Hotmail account. To help protect you, we've temporarily blocked your account.
				//      -can't send email to puts_admin in this case (can't send email)
				// TODO should log these errors - especially 5.3.4
				ex.printStackTrace();
				
				// reconnect and retry once
				smtp.dropConnection();
				
				connect();
				
				EMailUtil.sendHTMLMessage(
						smtp, 
						from, 
						recipients, 
						subject,
						html_str
					);
			}
		}
		
		public String createSubject(PhpBuildInfo info) {
			return info.getBuildBranch()+" "+info.getVersionRevision()+"-"+info.getBuildType()+"-"+info.getCPUArch();
		}
	}
	static class Mail extends Mailer implements IRecvr {
		LinkedList<String> message_bodies = new LinkedList<String>();
		
		Mail(boolean debug, boolean force, Address[] puts_users) throws ParserException {
			super(debug, force, puts_users);
		}
		
		Mail(String phpt_prefix, boolean debug, boolean force, Address[] puts_users) throws ParserException {
			super(phpt_prefix, debug, force, puts_users);
		}
		
		@Override
		public void recv(AbstractPhptRW base, AbstractPhptRW test, PHPTMultiHostTwoBuildSingleScenarioSetReportGen phpt_report, String html_str) throws IOException, SMTPException, Exception {
			if (test.count(EPhptTestStatus.PASS)<100)
				return;
			sendMail(
					!force && test.isTooMuchChange(base),
					phpt_prefix+" PHPT Report "+createSubject(test.getBuildInfo()),
					html_str
				);
		}

		@Override
		public void recv(AbstractPhpUnitRW base, AbstractPhpUnitRW test, PhpUnitMultiHostTwoBuildSingleScenarioSetReportGen php_unit_report, String html_str) throws IOException, SMTPException, Exception {
			sendMail(
					!force && test.isTooMuchChange(base),
					"Application PhpUnit Report "+createSubject(test.getBuildInfo()),
					html_str
				);
		}

		@Override
		public void start(PhpResultPack test_pack) throws Exception {
			connect();
		}

		@Override
		public void stop(PhpResultPack test_pack) throws Exception {
			message_bodies.clear();
		}
	}
	enum EFTPSSL {
		NO_SSL,
		REQUIRE_SSL,
		DETECT_SSL
	}
	static class Upload implements IRecvr {
		static FTPClient configureClient(FTPSClient ftps) {
			ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
			return ftps;
		}
		
		FTPClient ftp;
		
		@Override
		public void start(PhpResultPack test_pack) throws IOException {
			connect();
		}
		
		protected void connect() throws IOException {
			EFTPSSL use_ssl = EFTPSSL.NO_SSL;
			switch(use_ssl) {
			case REQUIRE_SSL:
				ftp = configureClient(new FTPSClient(true));
				break;
			case DETECT_SSL:
				ftp = configureClient(new FTPSClient(false));
				break;
			default:
				ftp = new FTPClient();
			}
			
			ftp.connect("131.107.220.66", 21); // TODO
			ftp.login("pftt", "1nter0pLAb!!");
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
		}
		
		static final String[] HOSTS86 = new String[]{"2008r2sp0", "2008r2sp1", "Win7sp0-x64", "Win7sp1-x64", "Win7sp0-x86", "Win7sp1-x86", "Vistasp2-x86", "Vistasp2-x64", "2008sp0-x86", "2008sp0-x64", "2008sp1-x86", "2008sp1-x64", "2012sp0", "8sp0-x64", "2012r2"};
		static final String[] HOSTS64 = new String[]{"2008r2sp0", "2008r2sp1", "Win7sp0-x64", "Win7sp1-x64", "Vistasp2-x64", "2008sp0-x64", "2008sp1-x64", "2012sp0", "8sp0-x64", "2012r2"};
		@Override
		public void stop(PhpResultPack test_pack) throws IllegalStateException, Exception {
			LocalHost host = LocalHost.getInstance();
			// copy_hosts
			{
				File[] fhosts = test_pack.getResultPackPath().listFiles();
				for ( File fhost : fhosts ) {
					if (fhost.isDirectory()) {
						//
						final String prefix = Character.toString((char)( 74 + new java.util.Random().nextInt(3)));
						final String[] hosts = test_pack.getBuildInfo().isX64() ? HOSTS64 : HOSTS86;
						
						for ( int i=0 ; i < hosts.length ; i++ ) {
							final String hostname = prefix + "-" +hosts[i];
							
							final File target_file = new File(fhost.getParentFile(), hostname);
							System.out.println(fhost+" "+target_file+" "+i);
							if (i==0) {
								host.move(fhost.getAbsolutePath(), target_file.getAbsolutePath());
								fhost = target_file;
							} else {
								host.copy(fhost.getAbsolutePath(), target_file.getAbsolutePath());
							}
						}
						break;
					}
				}
			}
			
			final String temp_file = host.mktempname("Uploader", ".7z");
			
			System.out.println("Compressing result-pack: "+test_pack.getResultPackPath());
			
			host.compress(null, host, test_pack.getResultPackPath().getAbsolutePath(), temp_file);
			
			final String remote_file = generateFolder(test_pack)+"/"+test_pack.getResultPackPath().getName()+".7z";
			
			System.out.println("Compressed. Uploading "+temp_file+" to "+remote_file);
			
			retryStore(generateFolder(test_pack), remote_file, new BufferedInputStream(new FileInputStream(temp_file)));
			
			System.out.println("Uploaded result-pack "+test_pack.getResultPackPath()+" to "+remote_file);
			
			ftp.logout();
			ftp.disconnect();
			
			host.delete(temp_file);
		}
		
		protected void retryStore(String folder, String file, InputStream in) throws IOException {
			for ( int i=0 ; i < 10 ; i++ ) {
				try {
					ftp.mkd(folder);
					ftp.storeFile(file, in);
					break;
				} catch ( Exception ex ) {
					ex.printStackTrace();
					// these methods can block forever if there was an exception
					//ftp.logout();
					//ftp.disconnect();
					// this too? ftp.quit();
					ftp = null;
					connect();
				}
			}
		}
		
		static String generateFolder(PhpResultPack test_pack) {
			return "/PFTT-Results/"+test_pack.getBuildInfo().getBuildBranch()+"/"+test_pack.getBuildInfo().getVersionRevision();
		}
		static String generateFolder(AbstractTestResultRW test) {
			return "/PFTT-Results/"+test.getBuildInfo().getBuildBranch()+"/"+test.getBuildInfo().getVersionRevision();
		}
		
		@Override
		public void recv(AbstractPhptRW base, AbstractPhptRW test, PHPTMultiHostTwoBuildSingleScenarioSetReportGen phpt_report, String html_str) throws IOException {
			final String folder = generateFolder(test);
			
			final String file = generateFileName(base, test);
			
			retryStore(folder, folder+"/"+file, new ByteArrayInputStream(html_str.getBytes()));
		}

		@Override
		public void recv(AbstractPhpUnitRW base, AbstractPhpUnitRW test, PhpUnitMultiHostTwoBuildSingleScenarioSetReportGen php_unit_report, String html_str) throws IOException {
			final String folder = generateFolder(test);
			
			final String file = generateFileName(base, test);
			
			retryStore(folder, folder+"/"+file, new ByteArrayInputStream(html_str.getBytes()));
		}
		
	}
	interface IRecvr {
		void recv(AbstractPhptRW base, AbstractPhptRW test, PHPTMultiHostTwoBuildSingleScenarioSetReportGen phpt_report, String html_str) throws IOException, SMTPException, Exception;
		void recv(AbstractPhpUnitRW base, AbstractPhpUnitRW test, PhpUnitMultiHostTwoBuildSingleScenarioSetReportGen php_unit_report, String html_str) throws IOException, SMTPException, Exception;
		void start(PhpResultPack test_pack) throws Exception;
		void stop(PhpResultPack test_pack) throws Exception;
	}
	static void clean_hosts(AHost host, File pack) throws IllegalStateException, IOException {
		File[] hosts = pack.listFiles();
		boolean is_first = true;
		for ( File fhost : hosts ) {
			if ( fhost.isDirectory() ) {
				if (is_first) {
					// don't delete all, leave first folder found
					is_first = false;
				} else {
					System.err.println("delete "+fhost);
					host.delete(fhost.getAbsolutePath());
				}
			}
		}
	}
	public static void main(String[] args) throws Exception {
		//IRecvr recvr = new Verify();
		//
		// mssql uses a different test-pack so reports only for that test-pack can be mailed
		// whereas wincacheu is a bunch of scenarios for both core and mssql test-packs
		//         therefore all reports must go to wincache
		//         -shows how wincacheu scenarios compare to other scenarios
		//         -shows the mssql driver with wincacheu
		final Mailer summary_mailer = new Mailer(false, false, new Address[]{AddressParser.parseAddress("v-mafick@microsoft.com")}); 
		final IRecvr CORE_PRODUCTION_SNAP = new Mail(false, false, new Address[]{AddressParser.parseAddress("wincache@microsoft.com"), AddressParser.parseAddress("ostcphp@microsoft.com")});
		//final IRecvr CORE_PRODUCTION_SNAP = new Mail(false, false, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com")});
		// TODO final IRecvr MSSQL_PRODUCTION_SNAP = new Mail("MSSQL", false, false, AddressParser.parseAddress("wincache@microsoft.com;jaykint@microsoft.com;ostcphp@microsoft.com"),AddressParser.parseAddress("shekharj@microsoft.com"));
		final IRecvr MSSQL_PRODUCTION_SNAP = new Mail("MSSQL", false, true, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com"), AddressParser.parseAddress("jaykint@microsoft.com")});
		// TODO final IRecvr CORE_PRODUCTION_RELEASE = new Mail(false, true, AddressParser.parseAddress("wincache@microsoft.com;jaykint@microsoft.com;ostcphp@microsoft.com"));
		final IRecvr CORE_PRODUCTION_RELEASE = new Mail(false, true, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com")});
		final IRecvr CORE_PRODUCTION_QA = new Mail(false, true, new Address[]{AddressParser.parseAddress("php-qa@lists.php.net")});
		final IRecvr TEST = new Mail(true, false, new Address[]{AddressParser.parseAddress("v-mafick@microsoft.com")});
		//IRecvr recvr = CORE_PRODUCTION_SNAP; // TODO
		//IRecvr recvr = CORE_PRODUCTION_RELEASE;
		//IRecvr recvr = MSSQL_PRODUCTION_SNAP;
		IRecvr recvr = new Upload();
		
		LocalHost host = LocalHost.getInstance();
		LocalConsoleManager cm = new LocalConsoleManager();
		
		// TODO check if a smoke test failed!
		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_3-Result-Pack-5.3.27rc1-nTS-X86-VC9"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_3-Result-Pack-5.3.27-nTS-X86-VC9"));
		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_3-Result-Pack-re2e002d-nTS-X86-VC9"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_3-Result-Pack-r7c9bb87-nTS-X86-VC9"));
		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_3-Result-Pack-re2e002d-TS-X86-VC9"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_3-Result-Pack-r7c9bb87-TS-X86-VC9"));
		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-5.4.20rc1-TS-X86-VC9"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-5.4.20-TS-X86-VC9"));
		
		//File base_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-rd487f5e-TS-X86-VC9");
		//File test_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-r6c48c6b-TS-X86-VC9");
		//File base_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-ra03f094-nTS-X86-VC9");
		//File test_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-r72aacbf-nTS-X86-VC9");

		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-rc8b0da6-TS-X64-VC11"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-rc8b0da6-TS-x64-VC11"));
		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-r82dd6b9-NTS-X64-VC11"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-rc8b0da6-NTS-X64-VC11"));
		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-rb2ee1b6-NTS-X86-VC11"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-rfc9d886-NTS-X86-VC11"));
		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-rc8b0da6-TS-X86-VC11"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-rfc9d886-TS-X86-VC11"));
		
		//File base_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack--TS-X86-VC11");
		//File base_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-r43289d6-TS-X86-VC11");
		//File test_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_6-Result-Pack-5.6.0-dev-TS-X86-VC11-keyword916");
		//File base_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-r89c4aba-NTS-X64-VC11");
		//File base_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-r82bb2a2-NTS-X86-VC11");
		//File test_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_6-Result-Pack-5.6.0-dev-NTS-X86-VC11-keyword916");
		//File base_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-r5e1ac55-NTS-X86-VC11");
		//File test_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-r82bb2a2-NTS-X86-VC11");
		//File base_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-rd515455-TS-X64-VC11");
		//File test_dir = new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-r04fcf6a-TS-X64-VC11");
		
		File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-5.5.6RC1-TS-X64-VC11"));
		File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-5.5.6-TS-X64-VC11"));
		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-5.4.22rc1-NTS-X86-VC9-SQLSVR"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-5.4.22-NTS-X86-VC9-SQLSVR"));
		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-5.4.22rc1-TS-X86-VC9"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-5.4.22-TS-X86-VC9"));
		//File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-ra0244a6-NTS-X86-VC11"));
		//File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_6-Result-Pack-5.6.0-50333-NTS-X86-VC11"));
	
		
		// clean_hosts
		clean_hosts(host, base_dir);
		clean_hosts(host, test_dir);
		PhpResultPackReader base_pack = PhpResultPackReader.open(cm, host, base_dir);
		PhpResultPackReader test_pack = PhpResultPackReader.open(cm, host, test_dir);
		
		// TODO temp 
		//report("Core", cm, recvr, base_pack, test_pack);
		summary(summary_mailer, cm, base_pack, test_pack);
		
	}
	static void summary(Mailer m, ConsoleManager cm, PhpResultPackReader base_pack, PhpResultPackReader test_pack) throws IOException, SMTPException, Exception {
		CmpReport2 cmp = new CmpReport2();
		cmp.add(base_pack);
		cmp.add(test_pack);
		
		AHost host = LocalHost.getInstance();
		
		{
			StringWriter sw = new StringWriter();
			TextBuilder text = new TextBuilder(sw);
		
			new CmpReport2G().run(text, cmp, cm);
			//m.connect();
			//m.sendMail(false, "Summary "+m.createSubject(test_pack.getBuildInfo()), sw.toString());
			host.saveTextFile("c:\\php-sdk\\test.txt", sw.toString());
			host.exec("notepad c:\\php-sdk\\test.txt", AHost.FOUR_HOURS);
		}
		{
			StringWriter sw = new StringWriter();
			MarkupBuilder html = new MarkupBuilder(sw);
		
			new CmpReport2G().run(html, cmp, cm);
			//m.connect();
			//m.sendMail(false, "Summary "+m.createSubject(test_pack.getBuildInfo()), sw.toString());
			host.saveTextFile("c:\\php-sdk\\test.html", sw.toString());
			host.exec("start c:\\php-sdk\\test.html", AHost.FOUR_HOURS);
		}
	}
	static void report(String phpt_prefix, ConsoleManager cm, IRecvr recvr, PhpResultPack base_pack, PhpResultPack test_pack) throws IOException, SMTPException, Exception {
		recvr.start(test_pack);
		
		// TODO turn off phpt or phpunit reports or turn off all but a specific test-pack
		for ( AbstractPhpUnitRW base : base_pack.getPhpUnit() ) {
			for ( AbstractPhpUnitRW test : test_pack.getPhpUnit() ) {
				System.out.println("PhpUnit "+base.getScenarioSetNameWithVersionInfo()+" "+test.getScenarioSetNameWithVersionInfo());
				//if (!base.getTestPackNameAndVersionString().contains("Wordpress"))
					//continue;
				if (!eq(base.getTestPackNameAndVersionString(), test.getTestPackNameAndVersionString()))
					continue;
				// TODO mysql
				if (!base.getScenarioSetNameWithVersionInfo().replace("MySQL-5.6_", "").replace("7.0.1", "7.0.2").equals(test.getScenarioSetNameWithVersionInfo().replace("MySQL-5.6_", "")))
						/*
						!(base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("opcache")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("opcache")
						&&(
								test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("cli")||
								test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("builtin")||
								test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("apache")
								) 
						&&base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("cli")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("cli")
						&&base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("builtin")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("builtin")
						&&base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("apache")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("apache")
						&&base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("local")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("local")
						&&base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("smb-dfs")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("smb-dfs")
					&&base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("smb-dedup")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("smb-dedup")
					&&base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("smb-basic")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("smb-basic")
						))*/
					continue;
				
				PhpUnitMultiHostTwoBuildSingleScenarioSetReportGen php_unit_report = new PhpUnitMultiHostTwoBuildSingleScenarioSetReportGen(base, test);
				
				String html_str = php_unit_report.getHTMLString(cm, !(recvr instanceof Upload));
				
				recvr.recv(base, test, php_unit_report, html_str);
				
			}
		}
		//System.exit(0);
		for ( AbstractPhptRW base : base_pack.getPHPT() ) {
			for ( AbstractPhptRW test : test_pack.getPHPT() ) {
				System.out.println("PHPT "+base.getScenarioSetNameWithVersionInfo()+" "+test.getScenarioSetNameWithVersionInfo()+" "+base+" "+test);
				if (!eq(base.getScenarioSetNameWithVersionInfo(), test.getScenarioSetNameWithVersionInfo()))
					continue;
				
				PHPTMultiHostTwoBuildSingleScenarioSetReportGen phpt_report = new PHPTMultiHostTwoBuildSingleScenarioSetReportGen(phpt_prefix, base, test);
				String html_str = phpt_report.getHTMLString(cm, !(recvr instanceof Upload));

				recvr.recv(base, test, phpt_report, html_str);
			}
		}
		recvr.stop(test_pack);
	}
	static boolean eq(String base, String test) {
		if (base==null||test==null)
			return false;
		base = base.replace("MySQL-5.6_", "");
		base = base.replace("7.0.1", "7.0.2");
		base = base.replace("-1.3.4", "");
		test = test.replace("7.0.1", "7.0.2");
		test = test.replace("MySQL-5.6_", "");
		test = test.replace("-1.3.4", "");
		return base.startsWith(test)||test.startsWith(base);
	}
}
