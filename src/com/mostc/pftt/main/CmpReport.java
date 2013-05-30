package com.mostc.pftt.main;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.results.AbstractPhpUnitRW;
import com.mostc.pftt.results.AbstractPhptRW;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPack;
import com.mostc.pftt.results.PhpResultPackReader;

public class CmpReport {
	public static void main(String[] args) throws Exception {
		LocalHost host = new LocalHost();
		LocalConsoleManager cm = new LocalConsoleManager();
		
		//PhpResultPack base_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_3-Result-Pack-5.3.25rc1-nTS-X86-VC9"));
		//PhpResultPack test_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_3-Result-Pack-5.3.25-nTS-X86-VC9"));
		//PhpResultPack base_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_3-Result-Pack-r46b05bc-TS-X86-VC9"));
		//PhpResultPack test_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_3-Result-Pack-r4cb25d2-TS-X86-VC9"));
		//PhpResultPack base_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-5.4.15RC1-TS-X86-VC9"));
		//PhpResultPack test_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-5.4.15-TS-X86-VC9"));
		//PhpResultPack base_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-rcd74b7d-nTS-X86-VC9"));
		//PhpResultPack test_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_4-Result-Pack-rbcdac75-nTS-X86-VC9"));
		//PhpResultPack base_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-r40d5458-NTS-X86-VC11"));
		//PhpResultPack test_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-rb262787-NTS-X86-VC11"));
		PhpResultPack base_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-r07bd1fa-nTS-X64-VC11"));
		PhpResultPack test_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-rb262787-nTS-X64-VC11"));
		//PhpResultPack base_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-5.5.0beta4-TS-X64-VC11"));
		//PhpResultPack test_pack = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_5_5-Result-Pack-5.5.0rc1-TS-X64-VC11"));
		
		for ( AbstractPhpUnitRW base : base_pack.getPhpUnit() ) {
			for ( AbstractPhpUnitRW test : test_pack.getPhpUnit() ) {
				System.err.println("PhpUnit "+base.getScenarioSetNameWithVersionInfo()+" "+test.getScenarioSetNameWithVersionInfo());
				if (!(base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("opcache")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("opcache")
						&&(
								test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("cli")||
								test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("builtin")||
								test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("apache")
								) 
						&&base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("cli")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("cli")
						&&base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("builtin")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("builtin")
						&&base.getScenarioSetNameWithVersionInfo().toLowerCase().contains("apache")==test.getScenarioSetNameWithVersionInfo().toLowerCase().contains("apache")))
					continue;
				 
				PhpUnitReportGen php_unit_report = new PhpUnitReportGen(base, test);
				String html_str = php_unit_report.getHTMLString(cm, false);

				String file_name = "PhpUnit_CMP_"+test.getTestPackNameAndVersionString()+"_"
						+base.getBuildInfo().getBuildBranch()+"-"+base.getBuildInfo().getVersionRevision()+"-"+base.getBuildInfo().getBuildType()+"-"+base.getBuildInfo().getCPUArch()+"-"+base.getBuildInfo().getCompiler()+"_"+base.getScenarioSetNameWithVersionInfo()+
						"_v_"
						+test.getBuildInfo().getBuildBranch()+"-"+test.getBuildInfo().getVersionRevision()+"-"+test.getBuildInfo().getBuildType()+"-"+test.getBuildInfo().getCPUArch()+"-"+test.getBuildInfo().getCompiler()+"_"+test.getScenarioSetNameWithVersionInfo();
				file_name = StringUtil.max(file_name, 80);
				File html_file = new File("c:\\php-sdk\\"+file_name+".html");
				FileWriter fw = new FileWriter(html_file);
				fw.write(html_str);
				fw.close();
				Desktop.getDesktop().browse(html_file.toURI());
			}
		}
		
		for ( AbstractPhptRW base : base_pack.getPHPT() ) {
			for ( AbstractPhptRW test : test_pack.getPHPT() ) {
				System.err.println("PHPT "+base.getScenarioSetNameWithVersionInfo()+" "+test.getScenarioSetNameWithVersionInfo());
				if (!(base.getScenarioSetNameWithVersionInfo().contains("Opcache")==test.getScenarioSetNameWithVersionInfo().contains("Opcache")
						&&base.getScenarioSetNameWithVersionInfo().contains("CLI")==test.getScenarioSetNameWithVersionInfo().contains("CLI")
						&&base.getScenarioSetNameWithVersionInfo().contains("Apache")==test.getScenarioSetNameWithVersionInfo().contains("Apache")))
					continue;
				
				PHPTReportGen phpt_report = new PHPTReportGen(base, test);
				String html_str = phpt_report.getHTMLString(cm, false);

				String file_name = "PHPT_CMP_"
						+base.getBuildInfo().getBuildBranch()+"-"+base.getBuildInfo().getVersionRevision()+"-"+base.getBuildInfo().getBuildType()+"-"+base.getBuildInfo().getCPUArch()+"-"+base.getBuildInfo().getCompiler()+"_"+base.getScenarioSetNameWithVersionInfo()+
						"_v_"
						+test.getBuildInfo().getBuildBranch()+"-"+test.getBuildInfo().getVersionRevision()+"-"+test.getBuildInfo().getBuildType()+"-"+test.getBuildInfo().getCPUArch()+"-"+test.getBuildInfo().getCompiler()+"_"+test.getScenarioSetNameWithVersionInfo();
				file_name = StringUtil.max(file_name, 80);
				File html_file = new File("c:\\php-sdk\\"+file_name+".html");
				FileWriter fw = new FileWriter(html_file);
				fw.write(html_str);
				fw.close();
				Desktop.getDesktop().browse(html_file.toURI());
			}
		}
	}
}
