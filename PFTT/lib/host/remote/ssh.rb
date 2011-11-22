
require 'net/ssh'
require 'net/sftp'
require 'host/remote'

# Supported SSH Servers:
# * OpenSSH on Linux (sshd)
# * PFTT's SSHD for Windows - based on Apache SSHD
#
# SSH Gotchas
#
# 1. in ruby, use / or \\ not \  !!!
# 2. only use / for program name in exec!, file operations or if you know the host is posix
#     don't use / for command line args except as a switch to a windows command line program
# 3. for windows, be sure to use #systemdrive in place of C:

module Host 
  module Remote
  class Ssh < RemoteBase
    #instantiable 'ssh'
     
    def rebooting?
      false # TODO
    end
    
    def reboot_wait(seconds, ctx)
      super(seconds, ctx)
      
      # TODO wait
    end
    
    def address
      @credentials[:host_name]
    end

    def close
      begin
        #@ssh.close()
        @sftp.close()
      rescue Exception => ex
        #puts ex.backtrace.inspect
      ensure
        #@ssh = nil
        @sftp = nil
      end
    end
    
    def initialize opts={}
      options = opts.dup
      @credentials = {
        :host_name => options.delete(:address),
        :user => options.delete(:username),
        :password => options.delete(:password)
      }
      @lock = Mutex.new
      super options
    end
    
    def clone
      clone = Host::Remote::Ssh.new(
          :address => @credentials[:host_name],
          :username => @credentials[:user],
          :password => @credentials[:password]
        )
      super(clone)
    end
    
    def alive?
      puts 'alive?'
      return true
      begin
        return exist?(cwd())
      rescue Exception => ex
        close
        return false
      end
    end 
    
    def to_s
      if posix?
        "Remote Posix "+@credentials[:host_name]
      else
        "Remote Windows "+@credentials[:host_name]
      end
    end

    # checks for the current working directory
    # be aware that a command being executed may possibly change this value at some point
    # during its execution (in which case, PFTT will only detect that after the command has
    #  finished execution)
    def cwd(ctx=nil)
      if ctx
        ctx.fs_op0(self, :cwd) do
          return cwd(nil) # nil => or you'll get an infinite loop
        end
      end
      
      @cwd ||= case
      when posix? then line!('pwd', ctx)
      else unquote_line!("ECHO %CD%", ctx)
      end
    end
    
    def cd path, hsh={}, ctx=nil
      if ctx
        ctx.fs_op1(self, :cd, path) do |path|
          return cd(path, hsh, nil)
        end
      end
      
      make_absolute! path
      if not path
        # popd may have been called when @dir_stack empty
        raise "path not specified"
      end
      format_path!(path)
      
      # e-z, same command on posix and windows
      cmd!("cd \"#{path}\"", ctx)
        
      # @cwd is cleared at start of exec, so while in exec, @cwd will be empty unless cwd() called in another thread
      @cwd = path
        
      @dir_stack.clear unless hsh.delete(:no_clear) || false
        
      return path
    end

    def directory? path, ctx=nil
      if ctx
        ctx.fs_op1(self, :is_dir, path) do |path|
          return directory?(path, nil)
        end
      end
      
      begin
        a = sftp(ctx).stat!(path)

        return a.type == 2
      rescue Exception => ex
        close
        return false
      end
    end

    def exist? path, ctx=nil
      if ctx
        ctx.fs_op1(self, :exist, path) do |path|
          return exist?(path, nil)
        end
      end
      
      # see T_* constants in Net::SFTP::Protocol::V01::Attributes
      # v04 and v06 attributes don't have a directory? or file? method (which v01 does)
      # doing it this way will work for all 3 (v01, v04, v06 attributes)
      begin      
        a = sftp(ctx).stat!(path)
        # types: regular(1), directory(2), symlink, special, unknown, socket, char_device, block_device, fifo
        #        # if type is any of those, then path exists
        return ( a.type > 0 and a.type < 10 )
      rescue Exception => ex
        close
        return false
      end
    end

    def list(path, ctx)
      if ctx
        ctx.fs_op1(self, :list, path) do |path|
          return list(path, nil)
        end
      end
      
      begin
      return sftp(ctx).dir.entries(path).map do |entry| 
        next nil if ['.','..'].include? entry.name
        File.join( path, entry.name )
      end.compact
      rescue Exception => ex
        close
        if ctx
          ctx.pftt_exception(self, ex)
        else
          Tracing::Context::Base.show_exception(ex)
        end
        raise ex
      end
    end

    def write(string, path, ctx)
      if ctx
        ctx.write_file(string, path) do |string, path|
          return write(string, path, nil)
        end
      end
      
      begin
      sftp(ctx).open(path) do |response|
        #raise "fail!" unless response.ok?
        request = sftp(ctx).write(response[:handle], 0, string)
        #@lock.synchronize do 
          request.wait
        #end
        sftp(ctx).close(response[:handle])
      end
      sftp(ctx).loop
      rescue Exception => ex
        close
        if ctx
          ctx.pftt_exception(self, ex)
        else
          Tracing::Context::Base.show_exception(ex)
        end
        raise ex
      end
    end

    def open_file path, flags='r', ctx, &block
      if ctx
        ctx.fs_op1(self, :open, path) do |path|
          return open_file(path, flags, nil, block)
        end
      end
      
      begin
        sftp(ctx).file.open path, flags, &block
      rescue Exception => ex
        close
        if ctx
          ctx.pftt_exception(self, ex)
        else
          Tracing::Context::Base.show_exception(ex)
        end
        raise ex
      end
    end

    def upload local_file, remote_path, mk=true, ctx
      if ctx
        ctx.fs_op2(self, :upload, local_file, remote_path) do |local_file, remote_path|
          return upload(local_file, remote_path, mk, nil)
        end
      end
      
      # if remote_path exists operation will fail!
      #   therefore, you should check if it exists first
      #    then, depending on need, delete() or skip the upload
      #
      # if local_file is a file, remote_path MUST be a file too!!!!!!!!!!!!!
      # (if local_file is a dir, remote_path can be a dir)
      # LATER remove this gotcha/rule/limitation (implement using File.basename)
      #
      if windows?
        to_windows_path!(local_file)
        to_windows_path!(remote_path)
      else
        to_posix_path!(local_file)
        to_posix_path!(remote_path)
      end
      #
      
      remote_path = no_trailing_slash(remote_path)
      
      # ensure the target directory exists (or we'll get an error)
      if mk
        mkdir(File.dirname(remote_path), ctx)
      end 
      begin
        sftp(ctx).upload!(local_file, remote_path)
      rescue Exception => ex
        close
        if ctx
          ctx.pftt_exception(self, ex)
        else
          Tracing::Context::Base.show_exception(ex)
        end
        raise ex
      end
    end

    def download remote_file, local_path, ctx
      if ctx
        ctx.fs_op2(self, :download, remote_file, local_path) do |remote_file, local_path|
          return download(remote_file, local_path, nil)
        end
      end
      
      begin
        sftp(ctx).download!(remote_file, local_path)
      rescue Exception => ex
        close
        if ctx
          ctx.pftt_exception(self, ex)
        else
          Tracing::Context::Base.show_exception(ex)
        end
        raise ex
      end
    end

    protected
    
    class SshExecHandle < ExecHandle
      def initialize(channel)
        @channel = channel
        @stderr = ''
        @stdout = ''
      end
      def write_stdin(stdin_data)
        @channel.send_data(stdin_data)
      end
      def read_stderr
        @stderr
      end
      def read_stdout
        @stdout
      end
      def post_stdout(data)
        @stdout = data
      end
      def post_stderr(data)
        @stderr = data
      end
    end # class SshExecHandle
    
    def _exec_impl command, opts, ctx, block
      if opts.has_key?(:chdir)
        command = "CMD /C pushd #{opts[:chdir]} & #{command} & popd"
      end
      # LATER opts[:env] support
      #  - primary: generate shell/batch script with all envs in it, then execute that
      #  - fallback: put ENV vars into one line command
                          
      stdout, stderr = '',''
      exit_code = -254 # assume error unless success
      
      ssh(ctx).open_channel do |channel|
        channel.exec(command) do |channel, success|
          unless success
            exit_code = -255
            raise "could not execute command #{command}"
          end
          
          sh = SshExecHandle.new(channel)
          
          channel.on_data do |ch, data|
            # important: don't do data.inspect!
            # that'll replace \ characters with \\ and \r\n with \\r\\n (bad!!)
            if stdin_data
              ch.send_data(stdin_data)
              stdin_data = nil
            end
            if block
              sh.post_stdout(data)
              block.call(sh)
            else
              stdout += data
            end
          end
          channel.on_extended_data do |ch, type, data|
            case type
            when 1 then
              if block
                sh.post_stderr(data)
                block.call(sh)
              else
                stderr += data
              end
            end
          end
          channel.on_request 'exit-status' do |ch, data|
            exit_code = data.read_long
          end
          channel.on_request 'exit-signal' do |ch, data|
            # if remote process killed, etc... might not get a normal exit code
            # instead, try to generate exit_code from exit-signal (which might not be provided either)
            exit_code = case
            when data.inspect.include?('KILL') then 9  # SIGKILL
            when data.inspect.include?('SEGV') then 11 # SIGSEGV (crash)
            when data.inspect.include?('TERM') then 15 # SIGTERM
            when data.inspect.include?('HUP')  then 1  # SIGHUP
            else data.inspect
            end
          end
                        
        end # channel.exec
                      
        channel.wait # cause this thread to wait
                      
      end # open_channel
                      
      # loop until exit signal received
      ssh(ctx).loop { exit_code == -254 }
                      
      return [stdout, stderr, exit_code]
    end # def _exec_impl
    

    def _delete path, ctx
      if ctx
        ctx.fs_op1(self, :delete, path) do |path|
          return _delete(path, nil)
        end
      end
      
      #sftp.remove! path
      if windows?
        to_windows_path!(path)
        
        cmd!("DEL /Q /F \"#{path}\"")
      else
        to_posix_path!(path)
        
        exec!("rm -rf \"#{path}\"")
      end
    end

    def _mkdir path, ctx
      if ctx
        ctx.fs_op1(self, :mkdir, path) do |path|
          return _mkdir(path, nil)
        end
      end
      
      begin
      sftp(ctx).mkdir!(path)
#      do |response|
#        #@lock.synchronize do
#          response.wait
#        #end
#      end
      rescue Exception => ex
        close
        if ctx
          ctx.pftt_exception(self, ex)
        else
          Tracing::Context::Base.show_exception(ex)
        end
        raise ex
      end
    end
         
    def ssh ctx
      if @ssh
        return @ssh
      end
      
      begin
        if ctx
          ctx.connection_start(self)
        end
      
        @ssh = Net::SSH.start( nil, nil, {
          :host_name => @credentials[:host_name],
          :user => @credentials[:user],
          :password => @credentials[:password]
        })
        
        if ctx
          ctx.connection_established(self)
        end
        
      rescue Exception => ex
        if ctx
          ctx.connection_failure(self) do
            begin
              ex = nil
              return ssh(nil)
            rescue Exception => ex2
              ex = ex2
            end
          end
        end
        
        if ex
          raise ex
        end
      end
      
      return @ssh
    end # def ssh
    
    def sftp ctx
      if @sftp
        return @sftp
      end
      
      begin
        if ctx
          ctx.connection_start(self, @credentials)
        end
      
        s = Net::SSH.start( nil, nil, {
          :host_name => @credentials[:host_name],
          :user => @credentials[:user],
          :password => @credentials[:password]
        })
        @sftp = s.sftp.connect()
      
        if ctx
          ctx.connection_established(self, @credentials)
        end
      
      rescue Exception => ex
        if ctx
          ctx.connection_failure(self) do
            begin
              ex = nil
              return sftp(nil)
            rescue Exception => ex2
              ex = ex2
            end
          end
        end
        
        if ex
          raise ex
        end
      end
      
      return @sftp
    end # def sftp

  end
  end
end

