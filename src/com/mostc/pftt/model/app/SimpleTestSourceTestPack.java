package com.mostc.pftt.model.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.main.Config;
import com.mostc.pftt.model.ApplicationSourceTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpParser;
import com.mostc.pftt.model.core.PhpParser.ClassDefinition;
import com.mostc.pftt.model.core.PhpParser.PhpScript;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.scenario.LocalFileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;

public abstract class SimpleTestSourceTestPack extends ApplicationSourceTestPack<SimpleTestActiveTestPack, SimpleTestCase> {

	public abstract String getNameAndVersionString();
	
	@Override
	public void cleanup(ConsoleManager cm) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void read(Config config, List<SimpleTestCase> test_cases, ConsoleManager cm, ITestResultReceiver twriter, PhpBuild build, SAPIScenario sapi_scenario) throws FileNotFoundException, IOException, Exception {
		this.test_pack_root = LocalFileSystemScenario.getInstance().fixPath(this.test_pack_root);
		
		findTestFiles(test_cases, new File(getSourceDirectory()));
		// TODO Auto-generated method stub
		System.out.println(test_cases);
	}
	
	protected void findTestFiles(List<SimpleTestCase> test_cases, File dir) {
		File[] files = dir.listFiles();
		if (files==null)
			return;
		for ( File f : files ) {
			if (f.isDirectory()) {
				findTestFiles(test_cases, f);
			} else if (f.getName().endsWith(".test")) {
				readTestFile(test_cases, f);
			}
		}
	}
	
	protected void readTestFile(List<SimpleTestCase> test_cases, File file) {
		PhpScript script = PhpParser.parseScript(file);
		
		for ( ClassDefinition clazz : script.getClasses() ) {
			test_cases.add(new SimpleTestCase(
					clazz.getName(), 
					Host.pathFrom(getRoot(), file.getAbsolutePath()),
					file.getAbsolutePath()
				));
		}
	}

	@Override
	public void read(Config config, List<SimpleTestCase> test_cases,
			List<String> names, ConsoleManager cm, PhpResultPackWriter twriter,
			PhpBuild build, boolean ignore_missing, SAPIScenario sapi_scenario)
			throws FileNotFoundException, IOException, Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void read(Config config, List<SimpleTestCase> test_cases,
			List<String> names, ConsoleManager cm, PhpResultPackWriter twriter,
			PhpBuild build, SAPIScenario sapi_scenario) throws FileNotFoundException, IOException,
			Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SimpleTestActiveTestPack installInPlace(ConsoleManager cm, AHost host) throws IOException, Exception {
		doInstallInPlace(cm, host);
		final String src_root = getSourceRoot(cm, host);
		
		return new SimpleTestActiveTestPack(src_root, src_root);
	}

	@Override
	public SimpleTestActiveTestPack installNamed(ConsoleManager cm, AHost host, String string, List<SimpleTestCase> test_cases) throws IllegalStateException, IOException, Exception {
		doInstallNamed(cm, host);
		final String src_root = getSourceRoot(cm, host);
		
		return new SimpleTestActiveTestPack(src_root, src_root);
	}

	@Override
	public SimpleTestActiveTestPack install(ConsoleManager cm, AHost host,
			String local_test_pack_dir, String remote_test_pack_dir, SAPIScenario sapi_scenario)
			throws IllegalStateException, IOException, Exception {
		doInstall(null, cm, host, local_test_pack_dir, remote_test_pack_dir);
		
		return new SimpleTestActiveTestPack(local_test_pack_dir, remote_test_pack_dir);
	}

} // end public abstract class SimpleTestSourceTestPack
