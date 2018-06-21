package com.mostc.pftt.model.app;

import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.scenario.AzureWebsitesScenario;
import com.mostc.pftt.scenario.SAPIScenario;

public class PhpUnitActiveTestPack extends ActiveTestPack {

	public PhpUnitActiveTestPack(String running_dir, String storage_dir) {
		super(running_dir, storage_dir);
	}

	public static String norm(SAPIScenario sapi_scenario, String str) {
		
		/*if (AzureWebsitesScenario.check(sapi_scenario)) {
		
		str = str.replace("C:/php-sdk/PFTT/Current/cache/working/", "D:/home/site/wwwroot/");
		str = str.replace("C:\\php-sdk\\PFTT\\Current\\cache\\working\\", "D:\\home\\site\\wwwroot\\");
		
		str = str.replace("C:\\php-sdk\\PFTT\\Current\\/cache/working/", "D:\\home\\site\\wwwroot\\");
		
		str = str.replace("C:\\php-sdk\\PFTT\\Current\\cache\\util", "D:\\HOME\\SITE\\WWWROOT");
		str = str.replace("C:\\php-sdk\\PFTT\\Current\\cache\\\\util", "D:\\HOME\\SITE\\WWWROOT");
		
			
		} TODO temp */
		
		return str;
	}

	public String[] norm(SAPIScenario sapi_scenario, String[] f) {
		if (f==null)
			return null;
		for (int i=0;i<f.length;i++) {
			f[i] = norm(sapi_scenario, f[i]);
		}
		return f;
	}
	
}
