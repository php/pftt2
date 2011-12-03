
require 'net/ssh'
require 'net/sftp'
require 'host/remote'

# Supported SSH Servers:
# * OpenSSH on Linux (sshd)
# * PFTT's SSHD for Windows - based on Apache SSHD
#

module Host 
  module Remote
  class Ssh < RemoteBase
    #instantiable 'ssh'
     
    def rebooting?
      @rebooting
    end
    
    def reboot_wait(seconds, ctx)
      super(seconds, ctx)
      
      @reboot_reconnect_tries = ( seconds / 60 ).to_i + 1
      if @reboot_reconnect_tries < 3
        @reboot_reconnect_tries = 3
      end
      
      @rebooting = true
      # will have to recreate sockets when it comes back up
      # ssh() and sftp() will block until then (so any method 
      # using sftp() or ssh() will be automatically blocked during reboot(good))
      close
    end
    
    def address
      @credentials[:host_name]
    end

    def close
      begin
        @ssh.close()
        @sftp.close()
      rescue 
      ensure
        @ssh = nil
        @sftp = nil
      end
    end
    
    attr_reader :credentials
    
    def initialize opts={}
      options = opts.dup
      @credentials = {
        :host_name => options.delete(:address),
        :user => options.delete(:username),
        :password => options.delete(:password)
      }
      @lock = Mutex.new
      @rebooting = false
      super options
    end
    
    def clone
      clone = Host::Remote::Ssh.new(
          :address => @credentials[:host_name],
          :username => @credentials[:user],
          :password => @credentials[:password]
        )
      clone.rebooting = @rebooting
      clone.rebooting_reconnect_tries = @rebooting_reconnect_tries
      super(clone)
    end
        
    def alive?(ctx=nil)
      begin
        return exist?(cwd(ctx), ctx)
      rescue
        if_closed
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
          return cwd(ctx)
        end
      end
      
      @cwd ||= case
      when posix? then line!('pwd', ctx)
      else env_value('CD', ctx)
      end
    end
    
    def cd path, hsh={}, ctx=nil
      if ctx
        ctx.fs_op1(self, :cd, path) do |path|
          return cd(path, hsh, ctx)
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
          return directory?(path, ctx)
        end
      end
      
      begin
        a = wait_for(sftp(ctx).stat(path), :attrs)

        if a.nil?
          return false
        else
          return a.type == 2
        end
      rescue 
        if_closed
        return false
      end
    end
    
    def list(path, ctx)
      if ctx
        ctx.fs_op1(self, :list, path) do |path|
          return list(path, ctx)
        end
      end
      
      begin
        list = []
        sftp(ctx).dir.foreach(path) do |entry|
          next nil if ['.','..'].include? entry.name
          list.push(File.join( path, entry.name ))
        end
        return list
      rescue 
        if_closed
        if ctx
          ctx.pftt_exception(self, $!, self)
        else
          Tracing::Context::Base.show_exception($!)
        end
        raise ex
      end
    end

    def write(string, path, ctx)
      if ctx
        ctx.write_file(self, string, path) do |string, path|
          return write(string, path, ctx)
        end
      end
      
      begin
        sftp(ctx).open(path) do |response|
          request = sftp(ctx).write(response[:handle], 0, string)
          request.wait
          sftp(ctx).close(response[:handle])
        end
        sftp(ctx).loop
      rescue 
        if_closed
        if ctx
          ctx.pftt_exception(self, $!, self)
        else
          Tracing::Context::Base.show_exception($!)
        end
        raise ex
      end
    end

    def open_file path, flags='r', ctx=nil, &block
      if ctx
        ctx.fs_op1(self, :open, path) do |path|
          return open_file(path, flags, ctx, block)
        end
      end
      
      begin
        return sftp(ctx).file.open(path, flags, &block)
      rescue 
        if_closed
        if ctx
          ctx.pftt_exception(self, $!, self)
        else
          Tracing::Context::Base.show_exception($!)
        end
        raise ex
      end
    end

    def upload local_file, remote_path, ctx, mk=true
      if ctx
        ctx.fs_op2(self, :upload, local_file, remote_path) do |local_file, remote_path|
          return upload(local_file, remote_path, mk, ctx)
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
        local_file = to_windows_path(local_file)
        remote_path = to_windows_path(remote_path)
      else
        local_file = to_posix_path(local_file)
        remote_path = to_posix_path(remote_path)
      end
      #
      
      remote_path = no_trailing_slash(remote_path)
      
      # ensure the target directory exists (or we'll get an error)
      if mk
        mkdir(File.dirname(remote_path), ctx)
      end 
      begin
        # TODO exc
        sftp(ctx).upload!(local_file, remote_path)#, {:read_size=>1024})
      rescue 
        if_closed
        if ctx
          ctx.pftt_exception(self, $!, self)
        else
          Tracing::Context::Base.show_exception($!)
        end
        raise ex
      end
    end

    def download remote_file, local_path, ctx
      if ctx
        ctx.fs_op2(self, :download, remote_file, local_path) do |remote_file, local_path|
          return download(remote_file, local_path, ctx)
        end
      end
      
      begin
        # TODO exc
        sftp(ctx).download!(remote_file, local_path)#, {:read_size=>1024})
      rescue
        if_closed
        if ctx
          ctx.pftt_exception(self, $!, self)
        else
          Tracing::Context::Base.show_exception($!)
        end
        raise ex
      end
    end

    protected
    
    def _exist?(path, ctx)
      # see T_* constants in Net::SFTP::Protocol::V01::Attributes
      # v04 and v06 attributes don't have a directory? or file? method (which v01 does)
      # doing it this way will work for all 3 (v01, v04, v06 attributes)
      begin
        a = wait_for(sftp(ctx).stat(path), :attrs)
        # types: regular(1), directory(2), symlink, special, unknown, socket, char_device, block_device, fifo
        #        # if type is any of those, then path exists
        if a.nil?
          return false
        else
          return ( a.type > 0 and a.type < 10 )
        end
      rescue
        if_closed
        return false
      end
    end
    
    def move_file(from, to, ctx)
      move_cmd(from, to, ctx)
    end
    
    def copy_file(from, to, ctx, mk)
      to = File.dirname(to)
      if mk
        mkdir(to, ctx)
      end
      
      copy_cmd(from, to, ctx)
    end
    
    # for #clone()
    attr_accessor :rebooting, :rebooting_reconnect_tries
    
    def wait_for(request, property=nil)
      request.wait
      if request.response.eof?
        nil
      elsif !request.response.ok?
        # replace original wait_for to avoid: raise StatusException.new(request.response)
        nil
      elsif property
        request.response[property.to_sym]
      else
        request.response
      end
    end # def wait_for
    
    def if_closed
      if @ssh and @ssh.closed?
        @ssh = nil
      end
      if @sftp and ( @sftp.closed? or !@sftp.open? )
        @sftp = nil
      end
    end
    
    class SshExecHandle < ExecHandle
      def initialize(channel)
        @channel = channel
        @stderr = ''
        @stdout = ''
        @stderr_len = 0
        @stdout_len = 0
      end
      def write_stdin(stdin_data)
        @channel.send_data(stdin_data)
      end
      def has_stderr?
        @stderr.length > 0
      end
      def has_stdout?
        @stdout.length > 0
      end
      def read_stderr
        x = @stderr
        @stderr = ''
        return x
      end
      def read_stdout
        x = @stdout
        @stdout = ''
        return x
      end
      def post_stdout(data)
        @stdout = data
        @stdout_len += data.length
      end
      def post_stderr(data)
        @stderr = data
        @stderr_len += data.length
      end
      def stdout_full?(opts)
        opts[:max_len] > 0 and @stdout_len >= opts[:max_len]
      end
      def stderr_full?(opts)
        opts[:max_len] > 0 and @stderr_len >= opts[:max_len]
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
      stdin_data = opts[:stdin_data]
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
              if sh.stdout_full?(opts)
                # don't exceed the output size limit (don't bother downloading beyond it)
                channel.close
              end
            else
              stdout += data
              if opts[:max_len] > 0 and stdout.length >= opts[:max_len]
                # don't exceed the output size limit (don't bother downloading beyond it)
                channel.close
              end
            end
          end
          channel.on_extended_data do |ch, type, data|
            case type
            when 1 then
              if block
                sh.post_stderr(data)
                block.call(sh)
                if sh.stderr_full?(opts)
                  # don't exceed the output size limit (don't bother downloading beyond it)
                  channel.close
                end
              else
                stderr += data
                if opts[:max_len] > 0 and stderr.length >= opts[:max_len]
                  # don't exceed the output size limit (don't bother downloading beyond it)
                  # LESSON: never have an unlimited buffer
                  channel.close
                end
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
      if windows?
        path = to_windows_path(path)
        
        cmd!("DEL /Q /F \"#{path}\"", ctx)
      else
        path = to_posix_path(path)
        
        exec!("rm -rf \"#{path}\"", ctx)
      end
    end

    def _mkdir path, ctx
      begin
        sftp(ctx).mkdir!(path)
      rescue
        if_closed
        if ctx
          ctx.pftt_exception(self, $!, self)
        else
          Tracing::Context::Base.show_exception($!)
        end
        raise ex
      end
    end
         
    def ssh ctx
      if @ssh
        return @ssh
      end
      
      if ctx
        ctx.connection_start(self)
      end
      
      reboot_count = 0
      while true do
        begin
        
          @ssh = Net::SSH.start( nil, nil, {
            :host_name => @credentials[:host_name],
            :user => @credentials[:user],
            :password => @credentials[:password]
          })
        
          if ctx
            ctx.connection_established(self)
          end
        
        rescue 
          if rebooting?
            reboot_count += 1
            if reboot_count < @reboot_reconnect_tries
              # ignore failure, try again
              next
            end
          end
          ex = $!
          if ctx
            ctx.connection_failure(self) do
              begin
                ex = nil
                return ssh(nil)
              rescue 
                ex = $!
              end
            end
          end # if ctx
        
          if ex
            raise ex
          end
          
          
        end # begin
        
        break # give up trying
      end # while
      
      return @ssh
    end # def ssh
    
    def sftp ctx
      if @sftp
        return @sftp
      end
      
      reboot_count = 0
      if ctx
        ctx.connection_start(self)
      end
        
      while true do
        begin
      
          # create a separate SSH session instead of sharing with exec() in case something
          # gets messed up on either session it won't interfere with the other operations
          s = Net::SSH.start( nil, nil, {
            :host_name => @credentials[:host_name],
            :user => @credentials[:user],
            :password => @credentials[:password]
          })
          @sftp = s.sftp.connect()
      
          if ctx
            ctx.connection_established(self)
          end
      
        rescue
          if rebooting?
            reboot_count += 1
            if reboot_count > @reboot_reconnect_tries
              next # try again
            end
          end
          ex = $!
          if ctx
            ctx.connection_failure(self) do
              begin
                ex = nil
                return sftp(nil)
              rescue
                ex = $!
              end
            end
          end
        
          if ex
            raise ex
          end
        end
      
        break
      end # while
      
      return @sftp
    end # def sftp

  end
  end
end

