package com.mostc.pftt.main;

import java.util.Map;

import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

public interface IENVINIFilter {
	public void prepareEnv(ConsoleManager cm, Map<String,String> env);
	public void prepareIni(ConsoleManager cm, PhpIni ini);
}
