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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.columba.ristretto.message.Address;
import org.columba.ristretto.parser.AddressParser;
import org.columba.ristretto.parser.ParserException;
import org.columba.ristretto.smtp.SMTPException;
import org.columba.ristretto.smtp.SMTPProtocol;

import com.github.mattficken.io.ArrayUtil;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.results.AbstractPhpUnitRW;
import com.mostc.pftt.results.AbstractPhptRW;
import com.mostc.pftt.results.AbstractTestResultRW;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
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
	static class PublishReport implements IRecvr {

		@Override
		public void recv(AbstractPhptRW base, AbstractPhptRW test,
				PHPTMultiHostTwoBuildSingleScenarioSetReportGen phpt_report,
				String html_str) throws IOException, SMTPException, Exception {
			sendReport(generateFileName(base, test), html_str, test.getBuildInfo(), test.count(EPhptTestStatus.FAIL)+test.count(EPhptTestStatus.CRASH));
		}
		
		protected void sendReport(final String filename, final String html_str, PhpBuildInfo test_build_info, int fail_crash_count) throws ParseException, IOException {
			HttpClient httpclient = new DefaultHttpClient();
			httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

			HttpPost httppost = new HttpPost("http://qa.php.net/pftt_report.php");
			//HttpPost httppost = new HttpPost("http://10.0.0.31/pftt_report.php");

			MultipartEntity mpEntity = new MultipartEntity();
			ContentBody cbFile = new StringBody(html_str) {
					@Override
					public String getFilename() {
						return filename;
					}
					@Override
					public String getMimeType() {
						return "text/html";
					}
				};
			
			// TODO share/configure token
			// TODO note in PHP script on qa.php.net a link to this code in git.php.net
			mpEntity.addPart("token", new StringBody("PFTT-ResultsReport"));
			mpEntity.addPart("report_file", cbFile);
			if (test_build_info.isX64()) {
				// don't flag the revision due to x64 failures
				fail_crash_count = 0;
			}
			mpEntity.addPart("fail_crash_count", new StringBody(""+0));// TODO temp fail_crash_count));
			// TODO temp
			mpEntity.addPart("branch", new StringBody(test_build_info.getBuildBranch().toString().toUpperCase()));
			mpEntity.addPart("revision", new StringBody(test_build_info.getVersionRevision()));

			httppost.setEntity(mpEntity);
			System.out.println("executing request " + httppost.getRequestLine());
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity resEntity = response.getEntity();

			System.out.println(response.getStatusLine());
			if (resEntity != null) {
				System.out.println(EntityUtils.toString(resEntity));
			}
			if (resEntity != null) {
				resEntity.consumeContent();
			}

			httpclient.getConnectionManager().shutdown();
		}

		@Override
		public void recv(
				AbstractPhpUnitRW base,
				AbstractPhpUnitRW test,
				PhpUnitMultiHostTwoBuildSingleScenarioSetReportGen php_unit_report,
				String html_str) throws IOException, SMTPException, Exception {
			sendReport(generateFileName(base, test), html_str, test.getBuildInfo(), 0);
		}

		@Override
		public void start(PhpResultPack test_pack) throws Exception {
			// n/a
		}

		@Override
		public void stop(PhpResultPack test_pack) throws Exception {
			// n/a
		}
		
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
			//from = AddressParser.parseAddress("ostc-php@microsoft.com");
			from = AddressParser.parseAddress("ostcpftt2@outlook.com");
			this.puts_users = puts_users;
			puts_admin = AddressParser.parseAddress("v-mafick@microsoft.com");
		}
		
		protected void connect() throws IOException, SMTPException {
			smtp = EMailUtil.connect(
					"smtp.live.com", 
					587, 
					from, 
					ESMTPSSL.IMPLICIT_SSL, 
					ESMTPAuthMethod.PLAIN, 
					"ostcpftt2@outlook.com", 
					"php868841433".toCharArray()
				);
			/*smtp = EMailUtil.connect(
					"smtphost.redmond.corp.microsoft.com", 
					587, 
					from, 
					ESMTPSSL.IMPLICIT_SSL, 
					ESMTPAuthMethod.LOGIN, // IMPORTANT: ESMTPAuthMethod.LOGIN 
					"ostc-php@microsoft.com", 
					"1nter0pLAb!!5".toCharArray()
				);*/
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
		protected void sendMail(boolean too_much_change, String subject, String html_str, String text_msg_str) throws IOException, SMTPException, Exception {
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
				EMailUtil.sendTextAndHTMLMessage(smtp, from, recipients, subject, html_str, text_msg_str);
			} catch ( Exception ex ) {
				// errors seen:
				// 5.3.4 Requested action not taken; We noticed some unusual activity in your Hotmail account. To help protect you, we've temporarily blocked your account.
				//      -can't send email to puts_admin in this case (can't send email)
				// TODO should log these errors - especially 5.3.4
				ConsoleManagerUtil.printStackTrace(CmpReport.class, ex);
				
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
					html_str,
					null
				);
		}

		@Override
		public void recv(AbstractPhpUnitRW base, AbstractPhpUnitRW test, PhpUnitMultiHostTwoBuildSingleScenarioSetReportGen php_unit_report, String html_str) throws IOException, SMTPException, Exception {
			sendMail(
					!force && test.isTooMuchChange(base),
					"Application PhpUnit Report "+createSubject(test.getBuildInfo()),
					html_str,
					null
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
		/** aka Implicit SSL */
		REQUIRE_SSL,
		/** aka Explicit SSL */
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
			// TODO temp
			// TODO if reconnecting, use previous values
			connect(EFTPSSL.DETECT_SSL, false, "40.123.43.193", 21, "ftp_user", "1nter0pLAb!!");
		}
		
		protected void connect(EFTPSSL use_ssl, boolean passive, String hostname, int port, String username, String password) throws IOException {
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
			
			ftp.connect(hostname, port);
			ftp.login(username, password);
			ftp.changeWorkingDirectory("/");
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			if (passive) {
				ftp.enterLocalPassiveMode();
			}
		}
		
		// TODO temp
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
								host.mMove(fhost.getAbsolutePath(), target_file.getAbsolutePath());
								fhost = target_file;
							} else {
								host.mCopy(fhost.getAbsolutePath(), target_file.getAbsolutePath());
							}
						}
						break;
					}
				}
			}
			
			final String temp_file = host.mCreateTempName("Uploader", ".7z");
			
			System.out.println("Compressing result-pack: "+test_pack.getResultPackPath());
			
			host.compress(null, host, test_pack.getResultPackPath().getAbsolutePath(), temp_file);
			
			final String remote_file = generateFolder(test_pack)+"/"+test_pack.getResultPackPath().getName()+".7z";
			
			System.out.println("Compressed. Uploading "+temp_file+" to "+remote_file);
			
			retryStore(generateFolder(test_pack), remote_file, new BufferedInputStream(new FileInputStream(temp_file)));
			
			System.out.println("Uploaded result-pack "+test_pack.getResultPackPath()+" to "+remote_file);
			
			ftp.logout();
			ftp.disconnect();
			
			host.mDelete(temp_file);
		}
		
		protected void retryStore(String folder, String file, InputStream in) throws IOException {
			for ( int i=0 ; i < 10 ; i++ ) {
				try {
					if (ftp==null||!ftp.isConnected())
						connect();
					ftp.mkd(folder);
					ftp.changeWorkingDirectory(folder);
					ftp.storeFile(file, in);
					break;
				} catch ( Exception ex ) {
					ConsoleManagerUtil.printStackTrace(CmpReport.class, ex);
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
					host.mDelete(fhost.getAbsolutePath());
				}
			}
		}
	}
	public static void main(String[] args) throws Exception {
		IRecvr recvr = new Verify();
		//
		// mssql uses a different test-pack so reports only for that test-pack can be mailed
		// whereas wincacheu is a bunch of scenarios for both core and mssql test-packs
		//         therefore all reports must go to wincache
		//         -shows how wincacheu scenarios compare to other scenarios
		//         -shows the mssql driver with wincacheu
		final Mailer summary_mailer = new Mailer(false, false, new Address[]{AddressParser.parseAddress("php-qa@lists.php.net")}); 
		final IRecvr CORE_PRODUCTION_SNAP = new Mail(false, false, new Address[]{AddressParser.parseAddress("wincache@microsoft.com"), AddressParser.parseAddress("ostcphp@microsoft.com")});
		//final IRecvr CORE_PRODUCTION_SNAP = new Mail(false, false, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com")});
		// TODO final IRecvr MSSQL_PRODUCTION_SNAP = new Mail("MSSQL", false, false, AddressParser.parseAddress("wincache@microsoft.com;jaykint@microsoft.com;ostcphp@microsoft.com"),AddressParser.parseAddress("shekharj@microsoft.com"));
		final IRecvr MSSQL_PRODUCTION_SNAP = new Mail("MSSQL", false, true, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com"), AddressParser.parseAddress("jaykint@microsoft.com")});
		// TODO final IRecvr CORE_PRODUCTION_RELEASE = new Mail(false, true, AddressParser.parseAddress("wincache@microsoft.com;jaykint@microsoft.com;ostcphp@microsoft.com"));
		final IRecvr CORE_PRODUCTION_RELEASE = new Mail(false, true, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com")});
		final IRecvr CORE_PRODUCTION_QA = new Mail(false, true, new Address[]{AddressParser.parseAddress("php-qa@lists.php.net")});
		final IRecvr TEST = new Mail(
				true, 
				false, 
				new Address[]{AddressParser.parseAddress("v-mafick@microsoft.com")}
			);
		//IRecvr recvr = CORE_PRODUCTION_SNAP; // TODO
		//IRecvr recvr = CORE_PRODUCTION_RELEASE;
		//IRecvr recvr = MSSQL_PRODUCTION_SNAP;
		//IRecvr recvr = TEST;
		//IRecvr recvr = new Upload();
		
		LocalHost host = LocalHost.getInstance();
		LocalConsoleManager cm = new LocalConsoleManager();
		
		// TODO check if a smoke test failed!

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
		
		File base_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-ra0244a6-NTS-X86-VC11"));
		File test_dir = (new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_6-Result-Pack-5.6.0-50333-NTS-X86-VC11"));
	
		
		// clean_hosts
		clean_hosts(host, base_dir);
		clean_hosts(host, test_dir);
		PhpResultPackReader base_pack = PhpResultPackReader.open(cm, host, base_dir);
		PhpResultPackReader test_pack = PhpResultPackReader.open(cm, host, test_dir);
		
		// TODO temp 
		//
		report("Core", cm, recvr, base_pack, test_pack);
		
		{
			CmpReport2 cmp = new CmpReport2();
			cmp.add(base_pack);
			cmp.add(test_pack);
			StringWriter text_sw = new StringWriter();
			TextBuilder text = new TextBuilder(text_sw);
			
			String filename = generateSummaryFileName(test_pack);
			new CmpReport2G().run("http://windows.php.net/downloads/snaps/ostc/pftt/Summary/"+filename, text, cmp, cm);
			
			
			host.mSaveTextFile("c:\\php-sdk\\test.txt", text_sw.toString());
			host.exec("notepad c:\\php-sdk\\test.txt", AHost.FOUR_HOURS);
			
			
		}
		//summary(summary_mailer, cm, base_pack, test_pack);
		
	}
	static String generateSummaryFileName(PhpResultPackReader test_pack) {
		return "PFTT_Summary_"+test_pack.getBuildInfo().getBuildBranch()+"-"+test_pack.getBuildInfo().getVersionRevision()+"-"+test_pack.getBuildInfo().getBuildType()+"-"+test_pack.getBuildInfo().getCPUArch()+"-"+test_pack.getBuildInfo().getCompiler()+".html";
	}
	static void summary(Mailer m, ConsoleManager cm, PhpResultPackReader base_pack, PhpResultPackReader test_pack) throws IOException, SMTPException, Exception {
		CmpReport2 cmp = new CmpReport2();
		cmp.add(base_pack);
		cmp.add(test_pack);
		
		AHost host = LocalHost.getInstance();
		
		{
			StringWriter text_sw = new StringWriter();
			StringWriter html_sw = new StringWriter();
			MarkupBuilder html = new MarkupBuilder(html_sw);
			TextBuilder text = new TextBuilder(text_sw);
			
			new CmpReport2G().run("", html, cmp, cm);
			String url = "";
			try {
				String filename = generateSummaryFileName(test_pack);
				
				((Upload)PfttAuto.UPLOAD).connect(EFTPSSL.DETECT_SSL, true, "windows.php.net", 21, "windows.php.net_ostc", "0$t3kQW#");
				((Upload)PfttAuto.UPLOAD).retryStore("/pftt/Summary/", filename, new ByteArrayInputStream(html_sw.toString().getBytes()));
				url = "http://windows.php.net/downloads/snaps/ostc/pftt/Summary/"+filename;
				System.out.println(filename);
			} catch ( Exception ex ) {
				ConsoleManagerUtil.printStackTrace(CmpReport.class, cm, ex);
			}
			System.out.println(url);
		
			new CmpReport2G().run(url, text, cmp, cm);
			
			
			// TODO temp
			m.connect(); 
			m.sendMail(false, "Summary "+m.createSubject(test_pack.getBuildInfo()), html_sw.toString(), text_sw.toString());
			//host.saveTextFile("c:\\php-sdk\\test.txt", text_sw.toString());
			//host.exec("notepad c:\\php-sdk\\test.txt", AHost.FOUR_HOURS);
			
			
		}
		{
			StringWriter sw = new StringWriter();
			MarkupBuilder html = new MarkupBuilder(sw);
		
			new CmpReport2G().run("", html, cmp, cm);
			//m.connect();
			//m.sendMail(false, "Summary "+m.createSubject(test_pack.getBuildInfo()), sw.toString());
			host.mSaveTextFile("c:\\php-sdk\\test.html", sw.toString());
			//host.exec("start c:\\php-sdk\\test.html", AHost.FOUR_HOURS);
		}
	}
	static void report(String phpt_prefix, ConsoleManager cm, IRecvr recvr, PhpResultPack base_pack, PhpResultPack test_pack) {
		try {
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
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(CmpReport.class, cm, ex);
		}
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
