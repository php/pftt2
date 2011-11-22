module Host
  class << self
    def Factory( options )
      (Host.hosts[options.delete('type')||'local']).new options
    end

    def hosts # LATER what is this method or class (<<) for??
      @@hosts||={}
    end
  end

  class HostBase
    include TestBenchFactor
    include PhpIni::Inheritable
    
    def self.no_trailing_slash(path)
      if path.ends_with?('/') or path.ends_with?('\\')
        path = path[0..path.length-1]
      end
      return path
    end
    
    def self.to_windows_path(path)
      return path.gsub('/', '\\')
    end
        
    def self.to_posix_path(path)
      return path.gsub('\\', '/')
    end
    
    def self.to_windows_path!(path)
      path.gsub!('/', '\\')
    end
            
    def self.to_posix_path!(path)
      path.gsub!('\\', '/')
    end
    
    def no_trailing_slash(path)
      if path.ends_with?('/') or path.ends_with?('\\')
        path = path[0..path.length-1]
      end
      return path
    end
    
    def to_windows_path(path)
      return path.gsub('/', '\\')
    end
        
    def to_posix_path(path)
      return path.gsub('\\', '/')
    end
    
    def to_windows_path!(path)
      path.gsub!('/', '\\')
    end
            
    def to_posix_path!(path)
      path.gsub!('\\', '/')
    end
    
    def rebooting?
      false
    end
    
    def reboot(ctx)
      reboot_wait(90, ctx)
    end
    
    def reboot_wait(seconds, ctx)
      if windows?(ctx)
        exec!("shutdown /r /t 0")
      else
        exec!("shutdown -r -t 0")
      end
    end

    def nt_version(ctx)
      nt_version = systeminfo_line('OS Version', ctx)
      
      return nt_version.nil? ? 0 : nt_version.to_f 
    end
    
    def eol(ctx)
      (windows?(ctx)) ? "\r\n" : "\n"
    end
    
    def eol_escaped(ctx)
      (windows?(ctx)) ? "\\r\\n" : "\\n"
    end
    
    def upload_force local, remote, ctx, mk=true
      delete_if(remote, ctx)
      
      upload(local, remote, mk, ctx)
    end
    
    def copy from, to, ctx, mk=true
      if ctx
        ctx.fs_op2(self, :copy, from, to) do |from, to|
          return copy(from, to, mk, nil)
        end
      end
      
      cmd!(case
      when posix? then %Q{cp -R "#{from}" "#{to}"}
      else
        to_windows_path!(from)
        to_windows_path!(to)
              
        %Q{xcopy /s /i /q "#{from}" "#{to}"}
      end, ctx)
    end
    
    def move from, to, ctx
      if ctx
        ctx.fs_op2(self, :move, from, to) do |from, to|
          return move(from, to, nil)
        end
      end
      
      from = no_trailing_slash(from)
      to = no_trailing_slash(to)
      
      cmd!(case
      when posix? then %Q{mv "#{from}" "#{to}"}
      else
        to_windows_path!(from)
        to_windows_path!(to)
        
        %Q{move "#{from}" "#{to}"}        
      end, ctx)
    end
    
    def systemroot(ctx=nil)
      if posix?
        return '/'
      elsif @_systemroot
        return @_systemroot
      else
        @_systemroot = unquote_line!('echo %SYSTEMROOT%', ctx)
        return @_systemroot
      end
    end
    
    def systemdrive(ctx=nil)
      if posix?
        return '/'
      elsif @_systemdrive
        return @_systemdrive
      else
        @_systemdrive = unquote_line!('echo %SYSTEMDRIVE%', ctx)
        return @_systemdrive
      end
    end
    
    def systeminfo(ctx=nil)
      unless @_systeminfo
        if posix?
          @_systeminfo = exec!('uname -a', {}, ctx)[0] + "\n" + exec!('cat /proc/meminfo', {}, ctx)[0] +"\n" + exec!('cat /proc/cpuinfo', {}, ctx)[0] # LATER?? glibc version
        else
          @_systeminfo = exec!('systeminfo', {}, ctx)[0]
        end
      end
      return @_systeminfo
    end
    
    def osname(ctx=nil)
      unless @_osname
        if posix?
          @_osname = line!('uname -a', ctx)
        else
          osname = systeminfo_line('OS Name', ctx)
          osname += ' '
          # get service pack too
          osname += systeminfo_line('OS Version', ctx)
          osname += ' '
          # and cpu arch
          osname += systeminfo_line('System Type', ctx) # x86 or x64
          @_osname = osname
        end
      end
      return @_osname
    end
    
    def line_prefix!(prefix, cmd, ctx)
      # executes the given cmd, splits the STDOUT output by lines and then returns
      # only the (last) line that starts with the given prefix
      #
      line = line!(cmd, ctx)
      if line.starts_with?(prefix)
        line = line[prefix.length...line.length]
      end
      return line.lstrip.rstrip
    end
    
    def silence_stderr str
      %Q{#{str} 2> #{devnull}}
    end

    def silence_stdout str
      %Q{#{str} > #{devnull}}
    end

    def silence str
      %Q{#{str} > #{devnull} 2>&1}
    end

    def devnull
      posix? ? '/dev/null' : 'NUL'
    end
    
    def has_debugger?(ctx)
      return debugger(ctx) != nil
    end
    
    def debug_wrap cmd_line, ctx
      dbg = debugger(ctx)
      if dbg
        if posix?
          return dbg+" --args "+cmd_line
        elsif windows?
          return dbg+" "+cmd_line
        end
      end
      return cmd_line
    end
    
    def debugger ctx
      if posix?
        if exists?('/usr/bin/gdb') or exists?('/usr/local/gdb')
          return 'gdb'
        end
      elsif windows?
        # windbg must be the x86 edition (not x64) because php is only compiled for x86
        if exists?('%ProgramFiles(x86)%\\Debugging Tools For Windows (x86)\windbg.exe', ctx)
          return '%ProgramFiles(x86)%\\Debugging Tools For Windows (x86)\windbg.exe'
        elsif exists?('%ProgramFiles(x86)%\\Debugging Tools For Windows\windbg.exe', ctx)
          return '%ProgramFiles(x86)%\\Debugging Tools For Windows\windbg.exe'
        elsif exists?('%ProgramFiles%\\Debugging Tools For Windows (x86)\windbg.exe', ctx)
          return '%ProgramFiles%\\Debugging Tools For Windows (x86)\windbg.exe'
        elsif exists?('%ProgramFiles%\\Debugging Tools For Windows (x86)\windbg.exe', ctx)
          return '%ProgramFiles%\\Debugging Tools For Windows (x86)\windbg.exe'
        end
      end
      if ctx
        # prompt context to get debugger for this host (or nil)
        return ctx.find_debugger(self)
      else
        return nil # signal that host has no debugger
      end
    end
    
    # TODO is this still used?
    attr_accessor :lock # LATER can this just be attr_reader

    def initialize opts={}
      @lock = []#Mutex.new
      
      #set the opts as properties in the TestBenchFactor sense
      opts.each_pair do |key,value|
        property key => value
      end
      
      @dir_stack = []
    end

    def describe
      @description ||= self.properties.values.join('-').downcase
    end
    
    class ExecHandle
      def write_stdin(stdin_data)
      end
      def read_stderr
        ''
      end
      def read_stdout
        ''
      end
      def has_stderr?
        read_stdout.length > 0
      end
      def has_stdout?
        read_stderr.length > 0
      end
    end
    
    # command:
    #   command line to run. program name and arguments to program as one string
    #   this may be a string or Tracing::Command::Expected
    # 
    # options: a hash of
    #  :env
    #     keys and values of ENV must be strings (not ints, or anything else!)
    #  :binmode  true|false
    #     will set the binmode of STDOUT and STDERR. binmode=false on Windows may affect STDOUT or STDERR output
    #  :chdir
    #     the current directory of the command to run
    #  :debug   true|false
    #     runs the command with the host's debugger. if host has no debugger installed, command will be run normally
    #  :stdin   ''
    #     feeds given string to the commands Standard Input
    # other options are silently ignored
    #
    #
    # returns once command has finished executing
    #
    # if command is a string returns array of 3 elements. 0=> STDOUT output as string 1=>STDERR 2=>command's exit code (0==success)
    # if command is a Command::Expected, returns an Command::Actual
    def exec! command, ctx, opts={}, &block
      _exec(false, command, opts, ctx, block)
    end
    
    # same as exec! except it returns immediately
    def exec command, ctx, opts={}, &block
      _exec(true, command, opts, ctx, block)
    end
    
    # executes command or program on the host
    #
    # can be a DOS command, Shell command or a program to run with options to pass to it
    def cmd! cmdline, ctx
      if windows?(ctx)
        cmdline = "CMD /C #{cmdline}"
      end
      return exec!(cmdline, ctx)
    end
    
    # executes command using cmd! returning the first line of output (STDOUT) from the command,
    # with the new line character(s) chomped off
    def line! cmdline, ctx
      cmd!(cmdline, ctx)[0].chomp
    end
    
    def unquote_line! cmdline, ctx
      line!(cmdline, ctx).gsub(/\"/, '')
    end
    
    def longhorn?(ctx=nil)
      unless @is_longhorn.nil?
        return @is_longhorn
      end
      ctx = ctx==nil ? nil : Tracing::Context::Dependency::Detect::OS::Version.new(ctx)
      
      # checks if its a longhorn(Windows Vista/2008) or newer version of Windows
      # (longhorn added new stuff not available on win2003 and winxp)
      @is_longhorn = ( windows?(ctx) and nt_version(ctx) >= 6 )
      
      if ctx
        @is_longhorn = ctx.check_os_generation_detect(:longhorn, @is_longhorn)
      end
      
      return @is_longhorn
    end
    
    def windows?(ctx=nil)
      unless @is_windows.nil?
        return @is_windows
      end
      ctx = ctx==nil ? nil : Tracing::Context::Dependency::Detect::OS::Type.new(ctx)
      
      # avoids having to check for c:\windows|c:\winnt if we've already found /usr/local
      @is_windows = ( !posix?(ctx) and ( exist?("C:\\Windows", ctx) or exist?("C:\\WinNT", ctx) ) )
      
      if ctx
        @is_windows = ctx.check_os_type_detect(:windows, @is_windows)
      end
      
      return @is_windows 
    end

    def posix?(ctx=nil)
      unless @posix.nil?
        return @posix
      end
      ctx = ctx==nil ? nil : Tracing::Context::Dependency::Detect::OS::Type.new(ctx)
      
      @posix = self.properties[:platform] == :posix
        
      if ctx
        @posix = ctx.check_os_type_detect(:posix, @posix)
      end
      
      return @posix
    end

    def make_absolute! *paths
      paths.map do |path|
        return path if !posix? && path =~ /\A[A-Za-z]:\//
        return path if path =~ /\A[A-Za-z]:\//  
        
        path.replace( File.absolute_path( path, cwd() ) )
        path
      end
    end

    def format_path path, ctx=nil
      case
      when windows?(ctx) then to_windows_path(path)
      else to_posix_path(path)
      end
    end
    
    def format_path! path, ctx=nil
      case
      when windows?(ctx) then to_windows_path!(path)
      else to_posix_path!(path)
      end
    end

    def pushd path, ctx=nil
      #puts 'pushd '+path
      cd(path, {:no_clear=>true}, ctx)
      @dir_stack.push(path)
    end
    
    def popd ctx=nil
      popped = @dir_stack.pop
      if popped
        # if nil, fail silently
        cd(popped, {:no_clear=>true}, ctx)
      end
    end
    
    def peekd
      @dir_stack.last
    end
    
    def separator(ctx)
      if windows?(ctx)
        return "\\"
      else
        return '/'
      end
    end
    
    def join *path_array
      path_array.join(separator)
    end
    
    def upload_if_not(local, remote, ctx)
      if exist?(remote, ctx)
        return false
      else
        upload(local, remote, ctx)
        return true
      end
    end
    
    def delete_if path, ctx
      if exist?(path, ctx)
        delete(path, ctx)
        return true
      else
        return false
      end
    end

    def delete glob_or_path, ctx
      if ctx
        ctx.fs_op1(self, :delete, glob_or_path) do |glob_or_path|
          return delete(glob_or_path, nil)
        end
      end
      
      make_absolute! glob_or_path
      
      raise Exception unless sane? glob_or_path
        
      if directory?(glob_or_path, ctx)
        exec!(case
        when posix? then %Q{rm -rf "#{glob_or_path}"}
        else %Q{CMD /C RMDIR /S /Q "#{glob_or_path}"}
        end, ctx)
      else
        _delete(glob_or_path, ctx) #implementation specific
      end
    end

    def escape(str)
      if !posix?
        s = str.dup
        s.replace %Q{"#{s}"} unless s.gsub!(/(["])/,'\\\\\1').nil?
        s.gsub!(/[\^&|><]/,'^\\1')
        s
      else
        raise NotImplementedYet
      end
    end
    
    def glob path, spec, ctx, &blk
      if ctx
        ctx.fs_op2(self, :glob, path, spec) do |path, spec|
          return glob(path, spec, nil, blk)
        end
      end
      l = list(path)
      unless spec.nil? or spec.length == 0
        l.delete_if do |e|
          !(e.include?(spec))
        end
      end
      
      return l
    end

    def mkdir path, ctx
      make_absolute! path
      parent = File.dirname path
      mkdir(parent, ctx) unless directory? parent
      _mkdir(path, ctx) unless directory? path
    end

    def mktmpdir path, ctx
      ctx = ctx==nil ? nil : Tracing::Context::SystemSetup::TempDirectory.new(ctx)
      
      make_absolute! path
      tries = 10
      begin
        dir = File.join( path, String.random(4) )
        raise 'exists' if directory? dir
        mkdir(dir, ctx)
      rescue
        retry if (tries -= 1) > 0
        raise $!
      end
      dir
    end
    
    def tmpdir ctx
      if windows?(ctx)
        # TODO   %USERPROFILE%\\AppData\\Local\\Temp
        systemdrive(ctx) + '/temp'
      else
        '/tmp'
      end
    end
    
    def mktmpfile suffix, ctx, content=nil
      ctx = ctx==nil ? nil : Tracing::Context::SystemSetup::TempDirectory.new(ctx)
      
      tries = 10
      begin
        path = File.join( tmpdir(), String.random(6) + suffix )
        
        raise 'exists' if exists?(path, ctx) 
        
        write(content, path, ctx)
        
        return path
      rescue
        retry if (tries -= 1) > 0
        raise $!
      end
    end

    def sane? path
      make_absolute! path
      insane = case        
      when posix?
        /\A\/(bin|var|etc|dev|usr)\Z/
      else
        /\A[A-Z]:(\/(Windows)?)?\Z/
      end =~ path
      !insane
    end
    
    class << self
      # create a way for a class to register itself as instantiable by the Factory function
      def instantiable name
        Host.hosts.merge! name => self
      end
    end
    
    def name ctx=nil
      unless @_name
        # find a name that other hosts on the network will use to reference localhost
        if windows?(ctx)
          @_name = unquote_line!('echo %COMPUTERNAME%', ctx)
        else
          @_name = line!('echo $HOSTNAME', ctx)
        end
      end
      @_name
    end
    
    protected
    
    def _exec in_thread, command, opts, ctx, block
      @cwd = nil # clear cwd cache
      
      orig_cmd = command
      if command.is_a?(Tracing::Command::Expected)
        command = command.cmd_line
      end
      
      if ctx 
        ctx.cmd_exe_start(self, command, opts) do |command|
          return _exec(in_thread, command, opts, nil, block)
        end
      end
      
      # if the program being run in this command (the part of the command before " ")
      # has / in the path, convert to \\  for Windows (or Windows might not be able to find the program otherwise)
      if windows?
        if command.starts_with?('"')
          i = command.index('"', 1)
        else
          i = command.index(' ', 1)
        end
        if i and i > 0
          command = to_windows_path(command[0..i]) + command[i+1..command.length]
        end
      end
      # 
      
      # run command in platform debugger
      if opts.has_key?(:debug) and opts[:debug] == true
        command = debug_wrap(command)
      end
      
      stdin_data = (opts.has_key?(:stdin))? opts[:stdin] : nil 
    
      if in_thread
        Thread.start do
          ret = _exec_thread(command, opts, ctx, block)
        end
      else
        ret = _exec_thread(command, opts, ctx, block)
      end  
    
      if orig_cmd.is_a?(Tracing::Command::Expected)
        #
        return Tracing::Command::Actual.new(orig_cmd.cmd_line, ret[0], ret[1], ret[2], opts)
      else
        return [ret[0], ret[1], ret[2]]
      end
    end # def _exec
    
    def _exec_thread command, opts, ctx, block
      begin
        
        ret = _exec_impl(command, opts, ctx, block)
        
        stdout = ret[0]
        stderr = ret[1]
        exit_code = ret[2]
        
        #
        # don't let output get too large
        # LATER configuration parameter for this
        if stdout.length > 128*1024
          stdout = stdout[0..128*1024]
        end
        if stderr.length > 128*1024
          stderr = stderr[0..128*1024]
        end
        #
                  
        if ctx
          if exit_code == 0
            ctx.cmd_exe_success(self, command, opts, exit_code, stdout+stderr) do |command|
              return _exec_thread(command, opts, nil, block)
            end
          else
            ctx.cmd_exe_failure(self, command, opts, exit_code, stdout+stderr) do |command|
              return _exec_thread(command, opts, nil, block)
            end
          end
        end
                
      rescue Exception => ex
        close
        stdout = ''
        stderr = command+" "+name+" "+ex.inspect+" "+ex.backtrace.inspect
        exit_code = -253
        
        raise ex
      end
      return [stdout, stderr, exit_code]
    end # def _exec_thread
    
    attr_accessor :_systeminfo, :_name, :_osname, :_systeminfo, :_systemdrive, :_systemroot, :posix, :is_windows
    
    def clone(clone)
      clone._systeminfo = @systeminfo
      clone.lock = @lock
      clone._osname = @_osname
      clone._systemroot = @_systemroot
      clone._systeminfo = @_systeminfo
      clone._systemdrive = @_systemdrive
      clone.posix = @posix
      clone.is_windows = @is_windows
      clone._name = @_name
      clone
    end
    
    def systeminfo_line(target, ctx)
      out_err = systeminfo(ctx)
      
      out_err.split("\n").each do |line|
        if line.starts_with?("#{target}:")
          line = line["#{target}:".length...line.length]
          parts = line.split(' ')
          return parts[0]
        end
      end
      return nil
    end
    
  end # class Base

  require 'typed-array'
  
  class Array < TypedArray(HostBase)
    # make it filterable
    include TestBenchFactorArray


    def load( path )
      path = File.absolute_path( path )
      config = Hash.new
      return self unless File.exist? path or Dir.exist? path
      if File.directory? path
        Dir.glob( File.join( path, '**', '*.yaml' ) ) do |file|
          config[File.basename( file, '.yaml' )]= YAML::load( File.open file )
        end
      else
        config.merge! YAML::load( File.open path )
      end
# TODO       config.each_pair do |name,spec|
#          self << Host::Factory( spec.merge(:name=>name) )
#        end
      self
    end
  end
end

# Load up all of our middleware classes right away instead of waiting for the autoloader
# this way they are actually available in Middleware::All
# although it technically does not matter the order in which they are loaded (as they will trigger
# autoload events on missing constants), reverse tends to get shallow before deep and should improve
# performance, if only marginally.
Dir.glob(File.join( File.dirname(__FILE__), 'host/**/*.rb')).reverse_each &method(:require)
