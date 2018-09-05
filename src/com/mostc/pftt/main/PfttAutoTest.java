package com.mostc.pftt.main;

import java.io.File;
import java.io.FileNotFoundException;

import org.columba.ristretto.message.Address;
import org.columba.ristretto.parser.AddressParser;
import org.columba.ristretto.parser.ParserException;

import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.main.CmpReport.Mailer;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPackReader;

public class PfttAutoTest {
	public static void main(String[] args) throws FileNotFoundException, ParserException, Exception {
		final LocalConsoleManager cm = new LocalConsoleManager();
		final LocalHost host = LocalHost.getInstance();
		PhpResultPackReader base_packr = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-r43289d6-TS-X86-VC11"));
		PhpResultPackReader test_packr = PhpResultPackReader.open(cm, host, new File("C:\\php-sdk\\PFTT-Auto\\PHP_Master-Result-Pack-r141c2cb-TS-X86-VC11"));
		final Mailer summary_mailer = new Mailer(false, false, new Address[]{AddressParser.parseAddress("v-mafick@microsoft.com")});
		
		CmpReport.summary(summary_mailer, cm, base_packr, test_packr);
	}
}
