package com.mostc.pftt.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.apache.sshd.server.shell.ProcessShellFactory;

import com.mostc.pftt.util.StringUtil;

// NOTE: modified NativeSshFile#getPhysicalName to check for [letter]:\ on Windows
//       and NativeSshFile#<init>

public class SSHServer {
	
	public static void main(String[] args) throws IOException {
		BasicConfigurator.configure();
        
		SshServer sshd = SshServer.setUpDefaultServer();

		ArrayList<NamedFactory<Command>> f = new ArrayList<NamedFactory<Command>>(1);
		f.add(new SftpSubsystem.Factory());
		sshd.setSubsystemFactories(f);

		if (System.getProperty("os.name").contains("Windows"))
			sshd.setShellFactory(psf("cmd.exe"));
		else
			sshd.setShellFactory(psf("bash"));

		sshd.setCommandFactory(new CommandFactory() {
			public Command createCommand(String command) {
				return psf(command).create();
			}
		});
		sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
			public boolean authenticate(String username, String password, ServerSession session) {
				return (username.equals("administrator")) && (password.equals("password01!"));
			}
		});
		sshd.setPort(22);
		sshd.setReuseAddress(true);
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkeys.txt"));
		sshd.start();
	}
	
	public static String[] splitCmdString(String command) {
		LinkedList<String> parts = new LinkedList<String>();
		String buf = "";
		char c;
		boolean in_quote = false;
		for ( int i=0 ; i < command.length() ; i++ ) {
			c = command.charAt(i);
			if (c=='\"' && (i==0||command.charAt(i-1) != '\\')) {
				in_quote = !in_quote;
			}
			if (c == ' ' && !in_quote) {
				buf = buf.trim();
				if (buf.length() > 0) {
					buf = StringUtil.unquote(buf);
					
					buf = StringUtil.replaceAll(PAT_QUOTE, "\"", buf);
					
					parts.add(buf);
				}
				buf = "";
				continue;
			}
			buf += c;
		}
		buf = buf.trim();
		if (buf.length() > 0) {
			if (buf.startsWith("\"")) {
				buf = buf.substring(1, buf.length()-1);
				
				buf = StringUtil.replaceAll(PAT_QUOTE, "\"", buf);
			}
			parts.add(buf);
		}
		
		return (String[])parts.toArray(new String[]{});
	} // end public static String[] splitCmdString
	private static final Pattern PAT_QUOTE = Pattern.compile("\\\"");
	
	private static ProcessShellFactory psf(String command) {
		String[] commands;
		if (command.contains(" ")) {
			commands = splitCmdString(command);
		} else {
			commands = new String[] { command };
		}

		if (System.getProperty("os.name").contains("Windows")) {
			return new ProcessShellFactory(
					commands, 
					EnumSet.of(ProcessShellFactory.TtyOptions.Echo, ProcessShellFactory.TtyOptions.ICrNl, ProcessShellFactory.TtyOptions.ONlCr));
		}

		return new ProcessShellFactory(commands);
	}

} // end public class SSHServer
