
require 'net/ssh'
require 'net/sftp'

# Supported SSH Servers:
# * OpenSSH on Linux (sshd)
# * PFTT's SSHD for Windows - based on Apache SSHD
#

module Host
  module Remote
  class Ssh < Base
    instantiable 'ssh'
    
    def name
      n = @credentials[:host_name]
      if n.include?('.')
        # name is an address, try getting the host's name for itself
        return super
      else
        return n
      end 
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
    
    def initialize opts={}
      options = opts.dup
      @credentials = {
        :host_name => options.delete(:address),
        :user => options.delete(:username),
        :password => options.delete(:password)
      }
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
      begin
        return exist?(cwd())
      rescue
        closed_sftp
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

    #
    # command:
    #   command line to run. program name and arguments to program as one string
    # options: a hash of
    #  :chdir
    #     the current directory of the command to run
    #  :debug   true|false
    #     runs the command with the host's debugger. if host has no debugger installed, command will be run normally
    #  :stdin   ''
    #     feeds given string to the commands Standard Input
    # other options are silently ignored
    #
    # returns array of 3 elements. 0=> STDOUT output as string 1=>STDERR 2=>command's exit code (0==success)
    def exec command, opts={}
      @cwd = nil # clear cwd cache
      
      if opts.has_key?(:chdir)
        restore_cd = cwd
        cd(opts[:chdir])
      else
        restore_cd = nil
      end
      
      Thread.start do
        
        stdin_data = (opts.has_key?(:stdin))? opts[:stdin] : nil 
        
        stdout, stderr = '',''
        exit_code = -254 # assume error unless success
        
        ssh.open_channel do |channel|
          channel.exec(command) do |channel, success|
            unless success
              exit_code = -255
              raise "could not execute command #{command}"
            end
            channel.on_data do |ch, data|
              # important: don't do data.inspect!
              # that'll replace \ characters with \\ and \r\n with \\r\\n (bad!!)
              stdout += data
              if stdin_data
                ch.send_data(stdin_data)
                stdin_data = nil
              end
            end
            
            channel.on_extended_data do |ch, type, data|
              case type
              when 1 then stderr += data
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
              when exit_code == -254 then data.inspect
              else exit_code
              end
            end
            
          end # channel.exec
          
          channel.wait # cause this thread to wait
          
        end # open_channel
        ssh.loop
        
        if restore_cd
          cd(restore_cd) # (this updates cwd cache too)
        else
          @cwd = nil # clear cwd cache a 2nd time (in case it was set in another thread)
        end

        [stdout, stderr, exit_code]
      end # Thread.start
    end # def
    
    # checks for the current working directory
    # be aware that a command being executed may possibly change this value at some point
    # during its execution (in which case, PFTT will only detect that after the command has
    #  finished execution)
    def cwd
      @cwd ||= case
      when posix? then line!('pwd')
      else unquote_line!("ECHO %CD%")
      end
    end
      
    def cd path, hsh={}
      make_absolute! path
      if not path
        # popd may have been called when @dir_stack empty
        raise "path not specified"
      end
      # e-z, same command on posix and windows
      cmd!("cd \"#{path}\"")
        
      # @cwd is cleared at start of exec, so while in exec, @cwd will be empty unless cwd() called in another thread
      @cwd = path
        
      @dir_stack.clear unless hsh.delete(:no_clear) || false
        
      return path
    end

    def copy from, to
      cmd! case
      when posix? then %Q{cp -R \""#{from}"\" \""#{to}\""}
      else %Q{copy \""#{from}"\" \""#{to}\""}
      end
    end
    
    def move from, to
      cmd! case
      when posix? then %Q{mv \""#{from}"\" \""#{to}\""}
      else %Q{move \""#{from}"\" \""#{to}\""}
      end
    end

    def deploy local_file, remote_path
      sftp.upload local_file, remote_path
    end

    def directory? path
      begin
        a = sftp.stat!(path)

        return a.type == 2
      rescue
        closed_sftp
        return false
      end
    end

    def exist? path
      # see T_* constants in Net::SFTP::Protocol::V01::Attributes
      # v04 and v06 attributes don't have a directory? or file? method (which v01 does)
      # doing it this way will work for all 3 (v01, v04, v06 attributes)
      begin
        a = sftp.stat!(path)
        
        # types: regular(1), directory(2), symlink, special, unknown, socket, char_device, block_device, fifo
        # if type is any of those, then path exists
        return ( a.type > 0 and a.type < 10 )
      rescue
        closed_sftp
        return false
      end
    end

    def list(path)
      return sftp.dir.entries(path).map do |entry| 
        next nil if ['.','..'].include? entry.name
        File.join( path, entry.name )
      end.compact
    end

    def glob path, spec, &blk
      # TODO return list('C:\\')
      puts path
      return list(path)#'C:/users/v-mafick/desktop/sf/workspace')
#      cwd = "G:\\" # TODO
#      matches sftp.dir.glob( cwd, spec ).map do |entry|
#        next nil if ['.','..'].include? entry.name
#        File.absolute_path( entry.name, cwd )
#      end.compact
#      if block_given?
#        matches.each &blk
#        nil
#      else
#        matches
#      end
    end


    def open_file path, flags='r',&block
      sftp.file.open path, flags, &block
    end

    def upload local_file, remote_path
      sftp.upload! local_file, remote_path
    end

    def download remote_file, local_path
      sftp.download! remote_file, local_path
    end

    protected

    def _delete path
      #sftp.remove! path
      if windows?
        cmd!("DEL /Q /F \"#{path}\"")
      else
        exec!("rm -rf \"#{path}\"")
      end
    end

    def _mkdir path
      sftp.mkdir! path
    end
    
    def closed_ssh
      @ssh = nil
    end
    
    def closed_sftp
      @sftp = nil
    end
     
    def ssh
      @ssh ||= Net::SSH.start( nil, nil, {
        :host_name => @credentials[:host_name],
        :user => @credentials[:user],
        :password => @credentials[:password]
      })
      @ssh
    end

    def sftp
      @sftp ||= Net::SFTP.start( nil, nil, {
        :host_name => @credentials[:host_name],
        :user => @credentials[:user],
        :password => @credentials[:password]
      })
      @sftp
    end
  end
  end
end
