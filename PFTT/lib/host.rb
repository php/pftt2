
# Important Notes
# 1. for windows, be sure to use #systemdrive in place of C:
# 2. don't rely on Windows automatically assume Windows will always convert / to \
#     -it won't for current-working-directory of a process you're creating
#     -it won't for EXE paths when creating a new process on Win Vista SP0 and before
#     -even recent versions of Windows may fail file ops if wrong / is used
#    -always use #format_path or #to_windows_path

module Host
  class << self
    def Factory( options )
      (Host.hosts[options.delete('type')||'local']).new options
    end

    def hosts # LATER what is this method or class (<<) for??
      @@hosts||={}
    end
  end
  
  def self.sub base, path
    if path.starts_with?(base)
      return path[base.length+1..path.length]
    end
    return path
  end
  
  def self.join *path_array
    path_array.join('/')
  end

  def self.administrator_user (platform)
    if platform == :windows
      return 'administrator'
    else
      return 'root'
    end
  end
  
  def self.no_trailing_slash(path)
    if path.ends_with?('/') or path.ends_with?('\\')
      path = path[0..path.length-1]
    end
    return path
  end
    
  def self.to_windows_path(path)
    # remove \\ from path too. they may cause problems on some Windows SKUs
    return path.gsub('/', '\\').gsub('\\\\', '\\').gsub('\\\\', '\\')
  end
        
  def self.to_posix_path(path)
    return path.gsub('\\', '/')
  end
    
  def self.to_windows_path!(path)
    # remove \\ from path too. they may cause problems on some Windows SKUs
    path.gsub!('/', '\\')
    path.gsub!('\\\\', '\\')
    path.gsub!('\\\\', '\\')
  end
            
  def self.to_posix_path!(path)
    path.gsub!('\\', '/')
    path.gsub!('//', '/')
    path.gsub!('//', '/')
  end
  
  def self.fs_op_to_cmd(fs_op, src, dst)
    # TODO
    return case fs_op
    when :move
    when :copy
    when :delete
    when :mkdir
    when :list
    when :glob
    when :cwd
    when :cd
    when :exist
    when :is_dir
    when :open
    when :upload
      nil
    when :download
      nil
    else
      nil
    end
  end
  
  class HostBase
    include Test::Factor
    include PhpIni::Inheritable
    
    def wdw_os?
      !wdw_os.nil?
    end
    
    def wdw_os
      # OS::WDW::MS::Win::Win7::x64::SP1
      # OS::WDW::MS::Win::Win7::x86::SP0
      nil # LATER
    end
    
    def is_vm_guest?
      false # LATER
    end
    
    def vm_host
      nil # LATER
    end
    
    def vm_host_mgr
      if is_vm_guest?
        # LATER how to share vm_host() instances amongst guest host instances?
        h = vm_host()
        if h
          return h.vm_host_mgr()
        end
      end
      # Host::VMManager.new (save and share w/ #clone too!)
      nil # LATER
    end
    
    def no_trailing_slash(path)
      Host.no_trailing_slash(path)
    end
    
    def to_windows_path(path)
      Host.to_windows_path(path)
    end
        
    def to_posix_path(path)
      Host.to_posix_path(path)
    end
    
    def to_windows_path!(path)
      Host.to_windows_path!(path)
    end
            
    def to_posix_path!(path)
      Host.to_posix_path!(path)
    end
    
    def number_of_processors(ctx=nil)
      # counts number of CPUs in host
      p = nil
      if windows?(ctx)
        p = env_value('NUMBER_OF_PROCESSORS', ctx)
        if p
          p = p.to_i
        end
      else
        cpuinfo = read('/proc/cpuinfo', ctx)
        
        p = 0
        # each processor will have a line like 'processor   : #', followed by lines of info
        # about that processor
        # 
        # count number of those lines == number of processors
        cpuinfo.split('\n').each do |line|
          if line.starts_with?('processor')
            p += 1
          end
        end
        
      end
      
      if p.is_a?(Integer) and p > 0
        return p
      else
        return 1 # ensure > 0 returned
      end
    end # def number_of_processors
    
    def rebooting?
      false
    end
    
    def reboot(ctx)
      # reboots host and waits 120 seconds for it to become available again
      reboot_wait(120, ctx)
    end
    
    def reboot_wait(seconds, ctx)
      # 
      if windows?(ctx)
        exec!("shutdown /r /t 0")
      else
        exec!("shutdown -r -t 0")
      end
    end

    def nt_version(ctx)
      if !windows?(ctx)
        return nil
      end
      
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
      
      upload(local, remote, ctx, mk)
    end
    
    def copy from, to, ctx, mk=true
      if ctx
        ctx.fs_op2(self, :copy, from, to) do |from, to|
          return copy(from, to, mk, ctx)
        end
      end
      
      if !directory?(from)
        copy_file(from, to, ctx, mk)
        return
      elsif mk
        mkdir(File.dirname(to), ctx)
      end
      
      copy_cmd(from, to, ctx)
    end
    
    def move from, to, ctx
      if ctx
        ctx.fs_op2(self, :move, from, to) do |from, to|
          return move(from, to, ctx)
        end
      end
      
      if !directory?(from)
        move_file(from, to, ctx)
        return
      end
      
      move_cmd(from, to, ctx)
    end
    
    def time=(time)
      # sets the host's time/date
      exec!("date #{time.month}-#{time.day}-#{time.year} && time #{time.hour}:#{time.minute}-#{time.second}")
    end
    
    def time
      # gets the host's time/date
      Time.new(unquote_line!('date /T')+' '+unquote_line!('time /T'))
    end
    
    def env_value(name, ctx=nil)
      # TODO if local, get using a local API
      
      # get the value of the named environment variable from the host
      if posix?(ctx)
        return unquote_line!("echo $#{name}", ctx)
      else 
        out = unquote_line!("echo %#{name}%", ctx)
        if out == "%#{name}%"
          # variable is not defined
          return ''
        else
          return out
        end
      end
    end # def env_value
    
    def systemroot(ctx=nil)
      # get's the file system path pointing to where the host's operating system is stored
      if posix?
        return '/'
      elsif @_systemroot
        return @_systemroot
      else
        @_systemroot = env_value('SYSTEMROOT', ctx)
        return @_systemroot
      end
    end
    
    def systemdrive(ctx=nil)
      # gets the file system path to the drive where OS and other software are stored
      if posix?
        return '/'
      elsif @_systemdrive
        return @_systemdrive
      else
        @_systemdrive = env_value('SYSTEMDRIVE', ctx)
        return @_systemdrive
      end
    end
    
    def desktop(ctx=nil)
      return join(userprofile(ctx), 'Desktop')
    end
    
    def userprofile(ctx=nil)
      unless @_userprofile.nil?
        return @_userprofile
      end
      p = nil
      if posix?
        p = env_value('HOME', ctx)
      else
        p = env_value('USERPROFILE', ctx)
      end
      
      if exists?(p, ctx)
        return @_userprofile = p
      else
        return @_userprofile = systemdrive(ctx)
      end
    end
    
    def appdata(ctx=nil)
      unless @_appdata.nil?
        return @_appdata
      end
      if posix?
        p = env_value('HOME', ctx)
        if p and exists?(p, ctx)
          return @_appdata = p
        end  
      else
        p = env_value('USERPROFILE', ctx)
        if p
          q = p + '\\AppData\\'
          if exists?(q, ctx)
            return @_appdata = q
          elsif exists?(p, ctx)
            mkdir(q, ctx)
            return @_appdata = q
          end
        end
      end
      
      return @_appdata = systemdrive(ctx)
    end # def appdata
    
    def appdata_local(ctx=nil)
      unless @_appdata_local.nil?
        return @_appdata_local
      end
      if posix?
        p = env_value('HOME', ctx)
        if p
          q = p + '/PFTT'
          if exists?(q, ctx)
            return @_appdata_local = q
          elsif exists?(p, ctx)
            mkdir(q, ctx)
            return @_appdata_local = q
          end
        end
      else
        p = env_value('USERPROFILE', ctx)
        if p
          q = p + '\\AppData\\Local'
          if exists?(q, ctx)
            return @_appdata_local = q
          elsif exists?(p, ctx)
            mkdir(q, ctx)
            return @_appdata_local = q
          end
        end
      end
            
      return @_appdata_local = systemdrive(ctx)
    end # def appdata_local
    
    def tempdir ctx
      unless @_tempdir.nil?
        return @_tempdir
      end
      if posix?
        p = '/usr/local/tmp'
        q = p + '/PFTT'
        if exists?(q, ctx)
          return @_tempdir = q
        elsif exists?(p, ctx)
          mkdir(q, ctx)
          return @_tempdir = q
        end
        p = '/tmp'
        q = p + '/PFTT'
        if exists?(q, ctx)
          return @_tempdir = q
        elsif exists?(p, ctx)
          mkdir(q, ctx)
          return @_tempdir = q
        end
      else
        # try %TEMP%\\PFTT
        p = env_value('TEMP', ctx)
        if p
          q = p + '\\PFTT'
          if exists?(q, ctx)
            return @_tempdir = q
          elsif exists?(p, ctx)
            mkdir(q, ctx)
            return @_tempdir = q
          end
        end
        
        # try %TMP%\\PFTT
        p = env_value('TMP', ctx)
        if p
          q = p + '\\PFTT'
          if exists?(q, ctx)
            return @_tempdir = q
          elsif exists?(p, ctx)
            mkdir(q, ctx)
            return @_tempdir = q
          end
        end
        
        # try %USERPROFILE%\\AppData\\Local\\Temp\\PFTT
        p = env_value('USERPROFILE', ctx)
        if p
          p = '\\AppData\\Local\\Temp\\' + p
          q = p + '\\PFTT'
          if exists?(q, ctx)
            return @_tempdir = q
          elsif exists?(p, ctx)
            mkdir(q, ctx)
            return @_tempdir = q
          end
        end
        
        # try %SYSTEMDRIVE%\\temp\\PFTT
        p = systemdrive(ctx)+'\\temp'
        q = p + '\\PFTT'
        if exists?(q, ctx)
          return @_tempdir = q
        elsif exists?(p, ctx)
          mkdir(q, ctx)
          return @_tempdir = q
        end
        
      end
                  
      return @_tempdir = systemdrive(ctx)
    end # def tempdir
    
    alias :tmpdir :tempdir
    
    def systeminfo(ctx=nil)
      # gets information about the host (as a string) including CPUs, memory, operating system (OS dependent format)
      unless @_systeminfo
        if posix?
          @_systeminfo = exec!('uname -a', ctx)[0] + "\n" + exec!('cat /proc/meminfo', {}, ctx)[0] +"\n" + exec!('cat /proc/cpuinfo', {}, ctx)[0] # LATER?? glibc version
        else
          @_systeminfo = exec!('systeminfo', ctx)[0]
        end
      end
      return @_systeminfo
    end
    
    def osname_short(ctx=nil)
      require 'util.rb'
      
      return os_short_name(osname(ctx))
    end
    
    def osname(ctx=nil)
      return name # TODO TUE
      # returns the name, version and hardware architecture of the Host's operating system
      # ex: Windows 2008r2 SP1 x64
      unless @_osname
        if posix?
          @_osname = line!('uname', ctx) + ' ' + line!('uname -r', ctx)
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
    
    # TODO os_short_name
    
    alias :os_name osname
    
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
      
      #set the opts as properties in the Test::Factor sense
      opts.each_pair do |key,value|
        property key => value
        # LATER merge Test::Factor and host info (name, os_version)
        # so both Test::Factor and host info can access each other
      end
      
      # allow override what #name() returns
      # so at least that can be used even
      # when a host is not accessible
      if opts.has_key?(:hostname)
        @_name = opts[:hostname]
      end
      #
      
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
    
    def shell(ctx=nil)
      # returns the name of the host's shell
      # ex: /bin/bash /bin/sh /bin/csh /bin/tcsh cmd.exe command.com
      if posix?
        return env_value('SHELL', ctx)
      else
        return File.basename(env_value('ComSpec', ctx))
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
    #  :stdin_data   ''
    #     feeds given string to the commands Standard Input
    #  :null_output true|false
    #     if true, returns '' for both STDOUT and STDERR. (if host is remote, STDOUT and STDERR are not sent over
    #     the network)
    #  :max_len  0+ bytes   default=128 kilobytes (128*1024)
    #     maximum length of STDOUT or STDERR streams(limit for either, STDERR.length+STDOUT.length <= :max_len*2).
    #     0 = unlimited
    #  :timeout  0+ seconds  default=0 seconds
    #     maximum run time of process (in seconds)
    #     process will be sent SIGKILL if it is still running after that amount of time
    #  :success_exit_code int, or [int] array   default=0
    #     what exit code(s) defines success
    #     note: this is ignored if Command::Expected is used (which evaluates success internally)
    #  :ignore_failure true|false
    #     ignores exit code and always assumes command was successful unless
    #     there was an internal PFTT exception (ex: connection to host failed)
    # LATER :elevate and :sudo support for windows and posix
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
    #
    # some DOS commands (for Windows OSes) are not actual programs, but rather just commands
    # to the command processor(cmd.exe or command.com). those commands can't be run through
    # exec!( since exec! is only for actual programs).
    # 
    def cmd! cmdline, ctx
      if windows?(ctx)
        cmdline = "CMD /C #{cmdline}"
      end
      return exec!(cmdline, ctx)
    end
    
    # executes command using cmd! returning the output (STDOUT) from the command,
    # with the new line character(s) chomped off
    def line! cmdline, ctx
      cmd!(cmdline, ctx)[0].chomp
    end
    
    def unquote_line! cmdline, ctx
      line!(cmdline, ctx).gsub(/\"/, '')
    end
    
    def longhorn?(ctx=nil)
      # checks if its a longhorn(Windows Vista/2008) or newer version of Windows
      # (longhorn added new stuff not available on win2003 and winxp)
      unless @is_longhorn.nil?
        return @is_longhorn
      end
      ctx = ctx==nil ? nil : ctx.new(Dependency::Detect::OS::Version)
      
      @is_longhorn = ( windows?(ctx) and nt_version(ctx) >= 6 )
      
      if ctx
        @is_longhorn = ctx.check_os_generation_detect(:longhorn, @is_longhorn)
      end
      
      return @is_longhorn
    end
    
    def windows?(ctx=nil)
      # returns true if this host is Windows OS 
      unless @is_windows.nil?
        return @is_windows
      end
      if @posix
        return @is_windows = false
      end
      ctx = ctx==nil ? nil : ctx.new(Tracing::Context::Dependency::Detect::OS::Type)
      
      # Windows will always have a C:\ even if the C:\ drive is not the systemdrive
      # posix doesn't have C: D: etc... drives
      @is_windows = _exist?('C:\\', ctx)
      
      if ctx
        # cool stuff: allow user to override OS detection
        @is_windows = ctx.check_os_type_detect(:windows, @is_windows)
      end
      
      return @is_windows 
    end

    def posix?(ctx=nil)
      unless @posix.nil?
        return @posix
      end
      if @is_windows
        return @posix = false
      end
      ctx = ctx==nil ? nil : ctx.new(Tracing::Context::Dependency::Detect::OS::Type)
      
      @posix = _exist?('/usr', ctx)
        
      if ctx
        @posix = ctx.check_os_type_detect(:posix, @posix)
      end
      
      return @posix
    end

    def make_absolute! *paths
      paths.map do |path|
        # support for Windows drive letters
        # (if drive letter present, path is absolute)
        return path if !posix? && path =~ /\A[A-Za-z]:\//
        return path if path =~ /\A[A-Za-z]:\//
        #  
        
        path.replace( File.absolute_path( path, cwd() ) )
        path
      end
    end
    
    def exist? path, ctx=nil
      make_absolute! path
      
      if ctx
        ctx.fs_op1(self, :exist, path) do |path|
          return exist?(path, ctx)
        end
      end
      
      _exist?(path, ctx)
    end
    
    alias :exists? :exist?
    alias :exist :exist?
    alias :exists :exist?

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
    
    def separator(ctx=nil)
      if windows?(ctx)
        return "\\"
      else
        return '/'
      end
    end 
    
    def sub base, path
      Host.sub(base, path)
    end
    
    def join *path_array
      Host.join(path_array)
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
          return delete(glob_or_path, ctx)
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
          return glob(path, spec, ctx, blk)
        end
      end
      l = list(path, ctx)
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

    def mktmpdir ctx, path=nil, suffix=''
      ctx = ctx==nil ? nil : ctx.new(Tracing::Context::SystemSetup::TempDirectory)
      unless path
        path = tempdir(ctx)
      end
      
      make_absolute! path
      tries = 10
      begin
        dir = File.join( path, String.random(6)+suffix )
        raise 'exists' if directory? dir
        mkdir(dir, ctx)
      rescue
        retry if (tries -= 1) > 0
        raise $!
      end
      dir
    end
    
    def mktmpfile suffix, ctx, content=nil
      ctx = ctx==nil ? nil : ctx.new(SystemSetup::TempDirectory)
      
      tries = 10
      begin
        path = File.join( tmpdir(ctx), String.random(6) + suffix )
        
        raise 'exists' if exists?(path, ctx) 
        
        if content
          write(content, path, ctx)
        end
        
        return path
      rescue
        retry if (tries -= 1) > 0
        raise $!
      end
    end
    
    alias :mktempfile :mktmpfile
    alias :mktempdir :mktmpdir

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
    
    def administrator_user ctx=nil
      if windows?(ctx)
        # LATER? should actually look this up??(b/c you can change it)
        return 'administrator'
      else
        return 'root'
      end
    end
    
    def name ctx=nil 
      unless @_name
        # find a name that other hosts on the network will use to reference localhost
        if windows?(ctx)
          @_name = env_value('COMPUTERNAME', ctx)
        else
          @_name = env_value('HOSTNAME', ctx)
        end
      end
      @_name
    end
    
    protected
    
    def move_cmd(from, to, ctx)
      from = no_trailing_slash(from)
      to = no_trailing_slash(to)
      
      cmd!(case
      when posix? then %Q{mv "#{from}" "#{to}"}
      else
        from = to_windows_path(from)
        to = to_windows_path(to)
        
        %Q{move "#{from}" "#{to}"}        
      end, ctx)
    end
    
    def copy_cmd(from, to, ctx)
      cmd!(case
      when posix? then %Q{cp -R "#{from}" "#{to}"}
      else
        from = to_windows_path(from)
        to = to_windows_path(to)
                      
        %Q{xcopy /Y /s /i /q "#{from}" "#{to}"}
      end, ctx)
    end
    
    def _exec in_thread, command, opts, ctx, block
      @cwd = nil # clear cwd cache
      
      orig_cmd = command
      if command.is_a?(Tracing::Command::Expected)
        command = command.cmd_line
      end
      if ctx 
        ctx.cmd_exe_start(self, command, opts) do |command|
          return _exec(in_thread, command, opts, ctx, block)
        end
      end
      
      # if the program being run in this command (the part of the command before " ")
      # has / in the path, convert to \\  for Windows (or Windows might not be able to find the program otherwise)
      if windows?(nil)
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
      
      #
      if opts[:null_output]
        command = silence(command)
      end
      #
      
      if !opts.has_key?(:max_len) or !opts[:max_len].is_a?(Integer) or opts[:max_len] < 0
        opts[:max_len] = 128*1024 
      end
      
      # run command in platform debugger
      if opts.has_key?(:debug) and opts[:debug] == true
        command = debug_wrap(command)
      end
      
      if in_thread
        Thread.start do
          ret = _exec_thread(command, opts, ctx, block)
        end
      else
        ret = _exec_thread(command, opts, ctx, block)
      end  
    
      if orig_cmd.is_a?(Tracing::Command::Expected)
        #
        return ret[2] # shared instance of ::Actual with _exec_thread
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
        if opts[:max_len] > 0
          if stdout.length > opts[:max_len]
            stdout = stdout[0..opts[:max_len]]
          end
          if stderr.length > opts[:max_len]
            stderr = stderr[0..opts[:max_len]]
          end
        end
        #
                  
        # execution done... evaluate and report success/failure
        if ctx
          success = false
          if opts[:ignore_failure]
            success = true
          else
            c_exit_code = exit_code
            #
            # decide if command was successful
            #
            success = exit_code == 0
            if command.is_a?(Tracing::Command::Expected)
              # custom evaluation
              #
              exit_code = Tracing::Command::Actual.new(command.cmd_line, stdout, stderr, exit_code, opts)
              # exit_code => share with _exec
              success = command.success?(exit_code)
              
            elsif opts.has_key?(:success_exit_code)
              #
              if opts[:success_exit_code].is_a?(Array)
                # an array of succesful values
                #
                success = false # override exit_code==0 above
                opts[:success_exit_code].each do |sec|
                  if exit_code == sec
                    success = true
                    break
                  end
                end
              elsif opts[:success_exit_code].is_a?(Integer)
                # a single successful value
                success = exit_code == opts[:success_exit_code]
              end
            end
          end
          #
          
          if success
            ctx.cmd_exe_success(self, command, opts, c_exit_code, stdout+stderr) do |command|
              return _exec_thread(command, opts, ctx, block)
            end
          else
            ctx.cmd_exe_failure(self, command, opts, c_exit_code, stdout+stderr) do |command|
              return _exec_thread(command, opts, ctx, block)
            end
          end
        end
                
      rescue        
        # try to include host name (don't call #host b/c that may exec! again which could fail)
        stderr = command+" "+((@_name.nil?) ?'nil':@_name)+" "+$!.inspect+" "+$!.backtrace.inspect
        exit_code = -253
        
        raise $!
      end
      return [stdout, stderr, exit_code]
    end # def _exec_thread
    
    attr_accessor :_systeminfo, :_name, :_osname, :_systeminfo, :_systemdrive, :_systemroot, :posix, :is_windows, :_appdata, :_appdata_local, :_tempdir, :_userprofile
    
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
      clone._appdata = @_appdata
      clone._appdata_local = @_appdata_local 
      clone._tempdir = @_tempdir
      clone._userprofile = @_userprofile
      clone
    end
    
    def systeminfo_line(target, ctx)
      out_err = systeminfo(ctx)
      
      out_err.split("\n").each do |line|
        if line.starts_with?("#{target}:")
          line = line["#{target}:".length...line.length]
          line.chomp!
          line = line.lstrip.rstrip
          
          return line
        end
      end
      return nil
    end
    
  end # class Base

  require 'typed-array'
  
  class Array < TypedArray(HostBase)
    # TODO make it filterable and #count(:posix) etc...
    include Test::FactorArray

    # TODO ensure hosts are locked
    def exec! command, ctx, opts={}
      ret = {}
      each_thread do |host|
        ret[host] = host.exec!(command, ctx, opts)
      end
      ret
    end
    
    def cmd! cmdline, ctx
      ret = {}
      each_thread do |host|
        ret[host] = host.cmd!(cmdline, ctx)
      end
      ret
    end

    def line! cmdline, ctx
      ret = {}
      each_thread do |host|
        ret[host] = host.line!(cmdline, ctx)
      end
      ret
    end
    
    def unquote_line! cmdline, ctx
      ret = {}
      each_thread do |host|
        ret[host] = host.unquote_line!(cmdline, ctx)
      end
      ret
    end
    
    def line_prefix!(prefix, cmd, ctx)
      ret = {}
      each_thread do |host|
        ret[host] = host.line_prefix!(prefix, cmd, ctx)
      end
      ret
    end
    
    def reboot!(prefix, cmd, ctx) # TODO
      each_thread do |host|
        host.reboot(prefix, cmd, ctx)
      end
    end
    
def line_prefix!(prefix, cmd, ctx)
      ret = {}
      each_thread do |host|
        ret[host] = host.line_prefix!(prefix, cmd, ctx)
      end
      ret
    end
    
    
    # rebooting?
    # env_value
def line_prefix!(prefix, cmd, ctx)
      ret = {}
      each_thread do |host|
        ret[host] = host.line_prefix!(prefix, cmd, ctx)
      end
      ret
    end
    
#    
#    
#    def time=
#      # TODO
#    end
#    
#    def name
#    end
#    
#    def mkdir
#    end
#    
#    
#    mktmpdir
#    mktmpfile
#    mktempfile
#    mktempdir
#    delete
#    delete_if
#    upload
#    upload_if_not
#    glob
    def exist?(path, ctx=nil)
      ret = {}
      each_thread do |host|
        ret[host] = host.exist?(path, ctx)
      end
      ret
    end
    
    alias :exist :exist?
    alias :exists :exist?
    alias :exists? :exist?
    
    def copy from, to, ctx, mk=true
      each_thread do |host|
        host.copy(from, to, ctx, mk)
      end
    end
    
    def move from, to, ctx
      each_thread do |host|
        host.move(from, to, ctx)
      end
    end

#    upload_force
#    time
#    shell
    # systeminfo
    # 
    def number_of_processors(ctx=nil)
      ret = {}
      each_thread do |host|
        ret[host] = host.number_of_processors(ctx)
      end
      ret
    end
    
    def systemroot(ctx=nil)
      ret = {}
      each_thread do |host|
        ret[host] = host.systemroot(ctx)
      end
      ret
    end
    
    def systemdrive(ctx=nil)
      ret = {}
      each_thread do |host|
        ret[host] = host.systemdrive(ctx)
      end
      ret
    end
    
    def desktop(ctx=nil)
      ret = {}
      each_thread do |host|
        ret[host] = host.desktop(ctx)
      end
      ret
    end
    
    def userprofile(ctx=nil)
      ret = {}
      each_thread do |host|
        ret[host] = host.userprofile(ctx)
      end
      ret
    end
    
    def appdata(ctx=nil)
      ret = {}
      each_thread do |host|
        ret[host] = host.appdata(ctx)
      end
      ret
    end
    
    def appdata_local(ctx=nil)
      ret = {}
      each_thread do |host|
        ret[host] = host.appdata_local(ctx)
      end
      ret
    end
    
    def tempdir(ctx)
      ret = {}
      each_thread do |host|
        ret[host] = host.tempdir(ctx)
      end
      ret
    end
    
    alias :tmpdir :tempdir
    
    protected
    
    def each_thread &block
      # TODO
    end
    
    public
    
    def osname(ctx=nil)
      ret = {}
      each do |host|
        ret[host] = host.osname(ctx)
      end
      ret
    end
    
    alias :os_name osname
        
    def has_debugger?(ctx)
      ret = {}
      each do |host|
        ret[host] = host.has_debugger?(ctx)
      end
      return ret
    end

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
