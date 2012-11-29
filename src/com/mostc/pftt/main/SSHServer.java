package com.mostc.pftt.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.apache.sshd.server.shell.ProcessShellFactory;

import com.mostc.pftt.host.LocalHost;

public class SSHServer {
  public static void main(String[] args) throws IOException {
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

  private static ProcessShellFactory psf(String command) {
    String[] commands;
    if (command.contains(" ")) {
      commands = LocalHost.splitCmdString(command);
    } else
      commands = new String[] { command };
    
    if (System.getProperty("os.name").contains("Windows")) {

      return new ProcessShellFactory(
        commands, 
        EnumSet.of(ProcessShellFactory.TtyOptions.Echo, ProcessShellFactory.TtyOptions.ICrNl, ProcessShellFactory.TtyOptions.ONlCr));
    }

    return new ProcessShellFactory(commands);
  }
  
} // end public class SSHServer
