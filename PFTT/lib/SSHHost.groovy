package com.mostc.pftt;

// SSH - Secure Shell
// for Linux/UNIX and Windows systems 
//
// Supported SSH Servers:
// * OpenSSH
// * Apache SSHD (Apache MINA)
// * SSH Tools
//
// Note:
// * KTS-SSH may have occasional problems with some SFTP operations

import java.io.*
import java.util.ArrayList

import com.sshtools.j2ssh.transport.HostKeyVerification
import com.sshtools.j2ssh.SshClient
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient

class SSHHost extends RemoteHost {

	@Override
    def canStream() {
      true
    }

	@Override
    def close() {
      try {
        ssh.close()
        sftp.close()
      } finally {
        ssh = null
        sftp = null
      }
    }
    
//    attr_reader :credentials
//    
//    def SSHHost (opts={}) {
//      @sftp            = opts[:sftp]
//      @session     = opts[:sftp_client]
//      @sftp_client = opts[:sftp_client]
//      
//      @credentials = {
//        :address     => opts[:address],
//        :port            => opts[:port]||22,
//        :username => opts[:username],
//        :password  => opts[:password],
//      }
//      
//      super
//    }
    
//    def clone() {
//      clone = Host::Remote::Ssh.new(
//        // pass session to clone so it'll share the ssh client (if its already connected)
//          :session      => @session,
//          :address     => @credentials[:address],
//          :port            => @credentials[:port],
//          :username => @credentials[:username],
//          :password   => @credentials[:password]
//        )
//        super(clone)
//    }
        
	@Override
    def isAlive(ctx=null) {
      try {
        return exist(cwd(ctx), ctx)
      } catch (Throwable t) {
        if_closed()
        return false
      }
    }
    
//    def toString() {
//      if (isPosix()) {
//        "Remote Posix //{@credentials[:address] || name()}"
//      } else {
//        "Remote Windows //{@credentials[:address] || name()}"
//      }
//    }

    // checks for the current working directory
    // be aware that a command being executed may possibly change this value at some point
    // during its execution (in which case, PFTT will only detect that after the command has
    //  finished execution)
	@Override
    def cwd(ctx=null) {
      if (ctx) {
        ctx.fs_op0(self, EFSOp.cwd) {
          return cwd(ctx)
        }
      }
      
//      cwd ||= format_path(sftp_client(ctx).pwd)
    }
//    alias :pwd :cwd
    
	@Override
	def cd(path, ctx=null) {
		if (!path) {
			// popd may have been called when dir_stack empty
			throw new NullPointerException("path not specified")
		}
      if (ctx) {
        ctx.fs_op1(self, EFSOp.cd, path) |new_path| {
          return cd(new_path, hsh, ctx)
        }
      }
      
	  path = format_path(path)
      path = make_absolute(path)
      
      
      // e-z same command on both
      cmd("cd \"$path\"", ctx)
        
      // cwd is cleared at start of exec, so while in exec, cwd will be empty unless cwd() called in another thread
      cwd = path
        
// TODO      dir_stack.clear unless hsh.delete(:no_clear) || false
        
      return path
    }
    
	@Override
    def env_values(ctx=null) {
      env_str = cmd('set', ctx).output
      
      def env = []
      for (def line : env_str.split("\n")) {
        def i = line.index('=')
        if (i) {
          def name = line.substring(0, i)
          def value = line.substring(i+1, line.length())
          
          if (isPosix(ctx)) {
			  if (value.startsWith('"')||value.startsWith("'"))
			  	value = value.substring(1, value.length()-2)
          }
          
          env[name] = value
        }
      }
      
      return env
    } // end def env_values
    
	@Override
    def env_value(name, ctx=null) {
      // get the value of the named environment variable from the host
      if (isPosix(ctx)) {
        return unquote_line('echo $'+name, ctx)
      } else {
        def out = unquote_line("echo %$name%", ctx)
        if (out == "%$name%") {
          // variable is not defined
          return ''
        } else {
          return out
        }
      }
    } // end def env_value
    
	@Override
    def mtime(file, ctx=null) {
      if (ctx) {
        ctx.fs_op1(self, EFSOp.mtime, file) |new_path| {
          return mtime(new_path, ctx)
        }
      }
      
      try {
        return sftp(ctx).getAttributes(file).getModifiedTime().longValue()
      } catch (Exception ex) {
        return 0
      }
    }

	@Override
    def directory(path, ctx=null) {
      if (ctx) {
        ctx.fs_op1(self, EFSOp.is_dir, path) |new_path| {
          return directory(new_path, ctx)
        }
      }
      
      try {
        return sftp(ctx).getAttributes(file).isDir
      } catch(Exception ex) {
        if_closed()
        return false
      }
    }
    
//    def glob(path, spec, ctx, &blk) {
//      if (ctx) {
//        ctx.fs_op2(self, :glob, path, spec) |new_path, new_spec| {
//          return glob(new_path, new_spec, ctx, blk)
//        }
//      }
//			
//      l = list(path, ctx)
//      unless spec.nil? or spec.length == 0
//        l.delete_if do |e|
//          !(e.include?(spec))
//        end
//      end      
//      return l
//    }
    
	@Override
    def list(path, ctx) {
      if (ctx) {
        ctx.fs_op1(self, EFSOp.list, path) |new_path| {
          return list(new_path, ctx)
        }
      }
      
      try {
        list = []
        _sftp = sftp(ctx)
        dir = _sftp.openDirectory(path)
        children = ArrayList.new()
      
        while (_sftp.listChildren(dir, children) > -1) {
        }
              
        dir.close()
        
        i = 0 
        while (i < children.size) {
          list.push(format_path(children.get(i).getAbsolutePath()))
            i += 1
        }
        
        return list
      } catch (Exception ex) { 
//        if_closed()
////        if (ctx) {
////          ctx.pftt_exception(self, $!, self)
////        } else {
////          // TODO Tracing::Context::Base.show_exception($!)
////        }
        throw ex
      }
    } // end def list
    
	@Override
    def read_lines(path, ctx=null, max_lines=16384) {
      def output = new ByteArrayOutputStream()
      
      sftp(ctx).get(path, output)
      
      def reader = new BufferedReader(new InputStreamReader(output))
	  while ( ( line = reader.readLine() ) != null && !(lines.size()> max_lines)) {
		  lines.add(line)
	  }
	  reader.close()
	  
	  lines
    }
    
	@Override
    def read(path, ctx=null) {
      def output = new ByteArrayOutputStream()
      sftp(ctx).get(path, output)
            
      if (ctx) {
        ctx.read_file(self, out, path) |new_path| {
          return read(new_path, ctx)
        }
      }
      
      output.toString()
    }

	@Override
    def write(string, path, ctx) {
      if (ctx) {
        ctx.write_file(self, string, path) |new_string; new_path| {
          return write(new_string, new_path, ctx)
        }
      }
      
      mkdir(File.dirname(path), ctx)
      
      // TODO
      try {
        def input = new ByteArrayInputStream(string.length)
        input.write(string, 0, string.length)
        sftp(ctx).put(input, path)
        return true
      } catch (Exception ex) { 
        if_closed()
//        if (ctx) {
//          ctx.pftt_exception(self, $!, self)
//        } else {
//          Tracing::Context::Base.show_exception($!)
//        }
        throw ex
      }
    }

    // uploads file/folder to remote host
    //
    // local_file - local file/folder to copy
    // remote_path - remote path to store at
    // ctx - context
    // opts - hash of
    //
    // :mkdir - true|false - default=true
    //           makes a directory to contain multiple files or a folder when uploading
    //
    // :transport_archive - true|false - default=false
    //           will (attempt to) archive the local files/folders into a 7zip archive file, 
    //           upload it to the remote host and then decompress it. will install 7zip on
    //           the remote host if it is not present. If can't upload and execute 7zip on
    //           remote host, operation will fail. 
    //             see transport_normal_if_decompress_failed
    //
    // :skip_cached_archive_mtime_check - true|false - default=false
    //           if true, if archive is already in cache, will use it no matter what.
    //           if false, will check if the original file(s)/folder(s) have been modified,
    //             and if yes, will replace the cached archive with the changes.
    //
    // :transport_normal_if_decompress_failed - true|false - default=true
    //           if can't decompress fail on remote host, automatically falls back on
    //           uploading it without archiving
    //
    // :prearchived - true|false - default=false
    //           indicates local file has already been archived AOT. archive will be
    //           uploaded and decompressed IF :transport_archive=true
    //
    // :transport_archive_no_local_cache - true|false - default=false
    //           if true, doesn't keep archive in local cache. otherwise, will keep archive in
    //           local cache to avoid recompressing it for next time.
    //
	@Override
    def upload(local_file, remote_path, ctx, opts=[]) {
      if (ctx) {
        ctx.fs_op2(self, EFSOp.upload, local_file, remote_path) |new_local_file; new_remote_path| {
          return upload(new_local_file, new_remote_path, ctx, opts)
        }
      }
      
      // if remote_path exists operation will fail!
      //   therefore, you should check if it exists first
      //    then, depending on need, delete() or skip the upload
      //
      // if local_file is a file, remote_path MUST be a file too!!!!!!!!!!!!!
      // (if local_file is a dir, remote_path can be a dir)
      // LATER remove this gotcha/rule/limitation (implement using File.basename)
      //
      if (manager && manager.local_host.isWindows()) {
        local_file = to_windows_path(local_file)
      } else {
        local_file = to_posix_path(local_file)
      }
      if (isWindows()) {
        remote_path = to_windows_path(remote_path)
      } else {
        remote_path = to_posix_path(remote_path)
      }
      //
      
      remote_path = no_trailing_slash(remote_path)
      
      //
      transport_archive = false
      if (opts.transport_archive) {
        // ensure 7zip is installed on remote host so it can be decompressed
        if (ensure_7zip_installed(ctx)) {
          unless (opts.prearchived) {
            // archive file
            local_path = manager.cache_archive(local_path, remote_path, ctx, opts)
          }
          transport_archive = true
        }
      }
      //
      
      // ensure the target directory exists (or we'll get an error)
      if (opts.mkdir!=false) {
        mkdir(File.dirname(remote_path), ctx)
      }
      try {
        
        // TODO
        sftp(ctx).put(BufferedInputStream.new(FileInputStream.new(local_file)), remote_path)
        
        return true
      } catch (Exception ex) { 
        if_closed()
//        if ctx
//          ctx.pftt_exception(self, $!, self)
//        else
//          Tracing::Context::Base.show_exception($!)
        throw ex
      }
      
      //
      if (transport_archive) {
        // decompress archive
        decompress_ret = exec!("7za a -y -o//{File.dirname(remote_path)} //{remote_path}")
        
//        if (opts[:transport_archive_no_local_cache]) {
//          unless (opts[:prearchived]) {
//            if (!opts.has_key?(:multi_host) or is_last?) {
//              manager.local_host.delete(local_cache_archive, ctx)
//            }
//            // TODO sync
//            
//          }
//        // else: leave archive in cache for next time
//        }
        
//        if (!decompress_ret[2]) {
//          if (opts[:transport_normal_if_decompress_failed]) {
//            // upload normal file
//            return upload(local_file, remote_path, ctx, {:mkdir=>opts[:mkdir]!=false})
//          } else {
//            raise 'RemoteDecompressFail', decompress_ret
//          }
//        }
        
        // file uploaded and decompressed ok
        
        // leave file on remote server if decompression failed for manual triage purposes later
        delete(remote_archive, ctx)
      }
      //
      
    } // end def upload
    
    def download(remote_file, local_path, ctx) {
      if (ctx) {
//        ctx.fs_op2(self, EFsOp.download, remote_file, local_path) do |new_remote_file, new_local_path| {
//          return download(new_remote_file, new_local_path, ctx)
//        }
      }
      
      //
      try {
        // TODO
        sftp(ctx).get(remote_file, BufferedOutputStream.new(FileOutputStream.new(local_path)))
        
        return true
      } catch (Exception ex) {
        if_closed()
//        if ctx
//          ctx.pftt_exception(self, $!, self)
//        else
//          Tracing::Context::Base.show_exception($!)
        throw ex
      }
      //
    } // end def download

    protected
    
    def _exist(path, ctx) {
      try {
        def a = sftp(ctx).getAttributes(path)
        return ( a.isFile() || a.isDirectory() )
      } catch (Exception ex) {
        if_closed()
        return false
      }
    }
    
    def move_file(from, to, ctx) {
      move_cmd(from, to, ctx)
    }
    
    def copy_file(from, to, ctx, mkdir) {
      to = dirname(to)
      if (mkdir) {
        mkdir(to, ctx)
      }
      
      copy_cmd(from, to, ctx)
    }
    
    def if_closed() {
      try {
        if (session && session.isConnected()) {
          session.disconnect()
          session = null
        }
        if (sftp_client && sftp_client.isClosed()) {
          sftp_client.quit()
          sftp_client = null
        }
        if (sftp && ( sftp.isClosed() || !sftp.isOpen() ) {
          sftp.close()
          sftp = null
        }
      } catch (Exception ex) {
      }
    } // end def is_closed
    
    class SshExecHandle extends ExecHandle {
		def channel, stderr, stdout, stderr_len, stdout_len
      def SshExecHandle(channel) {
        channel = channel
        stderr = ''
        stdout = ''
        stderr_len = 0
        stdout_len = 0
		}
      
      def isOpen() {
       channel.isOpen
      }
      
      def close() {
        // LATER begin
        channel.close
        // rescue
        // end
      }
      
      def write_stdin(stdin_data) {
        channel.send_data(stdin_data)
      }
      def hasStderr() {
        stderr.length() > 0
      }
      def hasStdout() {
        stdout.length() > 0
      }
      def read_stderr() {
        def x = stderr
        stderr = ''
        return x
      }
      def read_stdout() {
        def x = stdout
        stdout = ''
        return x
      }
      def post_stdout(data) {
        stdout = data
        stdout_len += data.length
      }
      def post_stderr(data) {
        stderr = data
        stderr_len += data.length
      }
      def stdout_full(opts) {
        opts.max_len > 0 && stdout_len >= opts.max_len
      }
      def stderr_full(opts) {
        opts.max_len > 0 && stderr_len >= opts.max_len
      }
    } // end class SshExecHandle
    
    def _exec_impl(command, opts, ctx, block) {
      //
      if (opts.hasProperty('chdir') && opts.chdir) {
        if (isWindows(ctx))
          command = "CMD /C pushd $opts[:chdir] & $command & popd"
        else
          command = "pushd $opts[:chdir] && $command && popd"
      }
      
      // type com.sshtools.j2ssh.session.SessionChannelClient
      exec = session(ctx).openSessionChannel
      
	  if (opts.hasProperty('env')) {
		  for (def k:opts.env.keySet()) {
			  exec.setEnvironmentVariable(k, opts.env[k])
		  }
	  }
      
      def output = new ByteArrayOutputStream(1024)
      def error = new ByteArrayOutputStream(1024)
      
      exec.executeCommand(command)
      
	  if (opts.hasProperty('stdin_data')) {
	      stdin_data = opts.stdin_data
	      if (stdin_data) {
	        exec.getOutputStream().write(stdin_data, 0, stdin_data.length)
	      }
	  }
            
      //
      def lh = new SshExecHandle(exec)
      
      //
	  def o, e
      if (opts.hasProperty('by_line') && opts.by_line) {
        o = exec_copy_lines(new InputStreamReader(exec.getInputStream()), false, lh, block, opts.max_len)
        e = exec_copy_lines(new InputStreamReader(exec.getStderrInputStream()), true, lh, block, opts.max_len)
      } else {
        exec_copy_stream(new BufferedInputStream(exec.getInputStream()), output, false, lh, block, opts.max_len)
        exec_copy_stream(new BufferedInputStream(exec.getStderrInputStream()), error, true, lh, block, opts.max_len)
            
        o = output.toString()
        e = error.toString()
      }
      //
      
      exec.close()
      
      exit_code = exec.getExitCode()
                      
      if (opts.hasProperty('exec_handle') && opts.exec_handle)
	  	lh
      else
        [output:output.toString(), error: error.toString(), exit_code: exit_code]
    } // end def _exec_impl
    
    def _delete(path, ctx) {
      if (isWindows(ctx)) {
        path = to_windows_path(path)
        
        cmd("DEL /Q /F \"//{path}\"", ctx)
      } else {
        path = to_posix_path(path)
        
        exec("rm -rf \"//{path}\"", ctx)
      }
    }

    def _mkdir(path, ctx) {
      try {
        sftp_client(ctx).mkdir(path)
        true
      } catch (Exception ex) { 
        if_closed
//        if (ctx) {
//          ctx.pftt_exception(self, ex, self)
//        } else {
//          Tracing::Context::Base.show_exception(ex)
//        }
		throw ex
      }
    }
    
    class AllowAnyHostKeyVerification implements HostKeyVerification {
      def verifyHost(host, pk) {
        true
      }
    }
    
    def session(ctx) {
      if (session) {
        return session
      }
      
      session = new SshClient()
      session.connect(credentials[:address], @credentials[:port], AllowAnyHostKeyVerification.new)
      def pwd = new PasswordAuthenticationClient()
      pwd.setUsername(credentials[:username])
      pwd.setPassword(credentials[:password])

      def result = session.authenticate(pwd)
      
      if (result != AuthenticationProtocolState.COMPLETE) {
        throw new IllegalStateException('WrongPassword')
      }
      
      session
    } // end def session
    
    def sftp_client(ctx) {
      if (sftp_client) {
        return sftp_client
      }
      
      sftp_client = session(ctx).openSftpClient
    }
    
    def sftp(ctx) {
      if (sftp) {
        return sftp
      }
      
      sftp = session(ctx).openSftpChannel //SftpSubsystemClient      
    } // end def sftp

} // end public class SSHHost
			