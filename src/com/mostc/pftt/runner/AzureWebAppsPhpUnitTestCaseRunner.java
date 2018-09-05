package com.mostc.pftt.runner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.PhpUnitActiveTestPack;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.LocalFileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;

public class AzureWebAppsPhpUnitTestCaseRunner extends HttpPhpUnitTestCaseRunner {
	
	public AzureWebAppsPhpUnitTestCaseRunner(FileSystemScenario fs,
			SAPIScenario sapi_scenario, PhpUnitThread thread,
			ITestResultReceiver tmgr, HttpParams params,
			HttpProcessor httpproc, HttpRequestExecutor httpexecutor,
			WebServerManager smgr, WebServerInstance web,
			Map<String, String> globals, Map<String, String> env,
			ConsoleManager cm, AHost host, ScenarioSetSetup scenario_set_setup,
			PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir,
			Map<String, String> constants, String include_path,
			String[] include_files, PhpIni ini, boolean reflection_only) {
		super(fs, sapi_scenario, thread, tmgr, params, httpproc, httpexecutor, smgr,
				web, globals, env, cm, host, scenario_set_setup, build, test_case,
				my_temp_dir, constants, include_path, include_files, ini,
				reflection_only);
	}
	
	@Override
	protected boolean use_cgi() {
		// TODO temp hacky
		return true;
	}
	
	public void prepareFirst() {
		try {
		String php_script = super.generatePhpScript();
		
		fs.saveTextFile("/phpunit.php", php_script);
		
		// TODO temp
		LocalFileSystemScenario.getInstance().saveTextFile("c:\\inetpub\\wwwroot\\phpunit.php", php_script);
		
		// TODO temp cleanup
		
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	}

	@Override
	protected void prepareTemplate(String template_file) throws IllegalStateException, IOException {
		// don't do this per each test-case
	}
	
	protected static String url_encode(String str) throws UnsupportedEncodingException {
		if (StringUtil.isEmpty(str))
			return "";
		
		//str = str.replace("C:/php-sdk/PFTT/Current/cache/working/", "D:/home/site/wwwroot/symfony-standard/");
		//str = str.replace("C:\\php-sdk\\PFTT\\Current\\cache\\working\\", "D:\\home\\site\\wwwroot\\symfony-standard\\");
		//str = str.replace("C:/php-sdk/PFTT/Current/cache/working/", "D:/home/site/wwwroot/wordpress-tests/");
		//str = str.replace("C:\\php-sdk\\PFTT\\Current\\cache\\working\\", "D:\\home\\site\\wwwroot\\wordpress-tests\\");
		
		//str = str.replace("C:/php-sdk/PFTT/Current/cache/working/", "D:/home/site/wwwroot/joomla-platform/");
		//str = str.replace("C:\\php-sdk\\PFTT\\Current\\cache\\working\\", "D:\\home\\site\\wwwroot\\joomla-platform\\");
		
		//str = str.replace("C:/php-sdk/PFTT/Current/cache/working/", "D:/home/site/wwwroot/drupal-8/");
		//str = str.replace("C:\\php-sdk\\PFTT\\Current\\cache\\working\\", "D:\\home\\site\\wwwroot\\drupal-8\\");
		
		//str = str.replace("C:/php-sdk/PFTT/Current/cache/working/", "D:/home/site/wwwroot/mediawiki/");
		//str = str.replace("C:\\php-sdk\\PFTT\\Current\\cache\\working\\", "D:\\home\\site\\wwwroot\\mediawiki\\");
		
		return URLEncoder.encode(str, "utf-8");
	}
	
	@Override
	protected String execute(String template_file) throws IOException, Exception {
		// TODO comment
		
		String url = "/phpunit.php?className="+url_encode(test_case.getClassName())+
			"&methodName="+url_encode(test_case.getMethodName())+
			"&abs_filename="+url_encode(test_case.getAbsoluteFileName())+
			"&bootstrap_file="+url_encode(PhpUnitActiveTestPack.norm(sapi_scenario, test_case.getPhpUnitDist().getBootstrapFile()!=null?test_case.getPhpUnitDist().getBootstrapFile().getAbsolutePath():""))+
			"&dependsMethodName="+url_encode(test_case.getDependsMethodName())+
			"&dataProviderMethodName="+url_encode(test_case.getDataProviderMethodName());
		
		int gcount = 0;
		// Note: if too many globals, URL may get to 2048+ bytes long which may be a problem
		for (String gn : globals.keySet()) {
			String gv = globals.get(gn);
			
			url += "&gn_"+gcount+"="+url_encode(gn);
			url += "&gv_"+gcount+"="+url_encode(gv);
			gcount++;
		}
		
		url += "&gcount="+gcount;
		
		web = smgr.getWebServerInstance(cm, fs, host, scenario_set.getScenarioSet(), build, ini, env, my_temp_dir, web, false, test_case);
		
		//http://ostc-pftt01.azurewebsites.net/test.php?className=Symfony\Component\HttpFoundation\Tests\HeaderBagTest&methodName=testCacheControlDirectiveParsingQuotedZero&abs_filename=D%3A%2Fhome%2Fsite%2Fwwwroot%2Fsymfony-standard%2Fsymfony-standard%2Fvendor%2Fsymfony%2Fsymfony%2Fsrc%2FSymfony%2FComponent%2FHttpFoundation%2FTests%2FHeaderBagTest.php&dependsMethodName=&dataProviderMethodName=
		String str = do_http_get(url);
		//str = StringUtil.max(str, 4096);
		System.out.println(str);
		//System.exit(0);
		
		return str;
	}
	
}
