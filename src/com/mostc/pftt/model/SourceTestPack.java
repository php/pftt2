package com.mostc.pftt.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;

public interface SourceTestPack<A extends ActiveTestPack, T extends TestCase> {
	void cleanup(ConsoleManager cm);
	String getSourceDirectory();
	void read(List<T> test_cases, ConsoleManager cm, ITestResultReceiver twriter, PhpBuild build) throws FileNotFoundException, IOException, Exception;
	A installInPlace();
	A installNamed(AHost host, String string, List<T> test_cases) throws IllegalStateException, IOException, Exception;
	A install(ConsoleManager cm, AHost host, String local_test_pack_dir, String remote_test_pack_dir) throws IllegalStateException, IOException, Exception;
}
