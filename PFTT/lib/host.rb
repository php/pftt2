require 'Open3'

module Host
  class << self
    def Factory( options )
      (Host.hosts[options.delete('type')||'local']).new options
    end

    def hosts
      @@hosts||={}
    end
  end

  class Base
    include TestBenchFactor
    include PhpIni::Inheritable

    def systemroot
      if posix?
        return '/'
      elsif @_systemroot
        return @_systemroot
      else
        @_systemroot = unquote_line!('echo %SYSTEMROOT%')
        return @_systemroot
      end
    end
    
    def systemdrive
      if posix?
        return '/'
      elsif @_systemdrive
        return @_systemdrive
      else
        @_systemdrive = unquote_line!('echo %SYSTEMDRIVE%')
        return @_systemdrive
      end
    end
    
    def systeminfo
      unless @_systeminfo
        if posix?
          @_systeminfo = exec!('uname -a') # LATER?? glibc version
        else
          @_systeminfo = exec!('systeminfo')
        end
      end
      return @_systeminfo
    end
    
    def osname
      unless @_osname
        if posix?
          @_osname = line!('uname -a')
        else
          # LATER use systeminfo and parse it locally (otherwise windows has to run systeminfo 2 or 3 times, and its slow)
          osname = line_prefix!('OS Name:', 'systeminfo | findstr /B /C:"OS Name"')
          osname += ' '
          osname += line_prefix!('System Type:', 'systeminfo | findstr /B /C:"System Type"') # x86 or x64
          @_osname = osname
        end
      end
      return @_osname
    end
    
    def line_prefix!(prefix, cmd)
      line = line!(cmd)
      if line.starts_with?(prefix)
        line = line[prefix.length...line.length]
      end
      return line.lstrip.rstrip
    end
    
    def write(string, path)
      f = open_file(path, 'wb')
      f.puts(string)
      f.close()
    end
    
    def read(path)
      f = open_file(path, 'rb')
      out = f.gets()
      f.close()
      return out
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
    
    def has_debugger?
      return debugger != nil
    end
    
    def debug_wrap cmd_line
      dbg = self.debugger
      if dbg
        if posix?
          return dbg+" --args "+cmd_line
        elsif windows?
          return dbg+" "+cmd_line
        end
      end
      return cmd_line
    end
    
    def debugger
      if posix?
        return 'gdb'
      elsif windows?
        # windbg must be the x86 edition (not x64) because php is only compiled for x86
        if exists? '%ProgramFiles(x86)%\\Debugging Tools For Windows (x86)\windbg.exe'
          return '%ProgramFiles(x86)%\\Debugging Tools For Windows (x86)\windbg.exe'
        elsif exists? '%ProgramFiles(x86)%\\Debugging Tools For Windows\windbg.exe'
          return '%ProgramFiles(x86)%\\Debugging Tools For Windows\windbg.exe'
        elsif exists? '%ProgramFiles%\\Debugging Tools For Windows (x86)\windbg.exe'
          return '%ProgramFiles%\\Debugging Tools For Windows (x86)\windbg.exe'
        elsif exists? '%ProgramFiles%\\Debugging Tools For Windows (x86)\windbg.exe'
          return '%ProgramFiles%\\Debugging Tools For Windows (x86)\windbg.exe'
        end
      end
      return nil # signal that host has no debugger
    end

    def initialize opts={}
      #set the opts as properties in the TestBenchFactor sense
      opts.each_pair do |key,value|
        property key => value
      end
      
      @dir_stack = []
    end

    def describe
      @description ||= self.properties.values.join('-').downcase
    end

    def exec! *args
      exec(*args).value
    end
    
    # executes command or program on the host
    #
    # can be a DOS command, Shell command or a program to run with options to pass to it
    def cmd! cmdline
      if windows?
        cmdline = "CMD /C #{cmdline}"
      end
      return exec!(cmdline)
    end
    
    # executes command using cmd! returning the first line of output (STDOUT) from the command,
    # with the new line character(s) chomped off
    def line! cmdline
      cmd!(cmdline)[0].chomp
    end
    
    def unquote_line! cmdline
      line!(cmdline).gsub(/\"/, '')
    end
    
    def windows?
      # avoids having to check for c:\windows|c:\winnt if we've already found /usr/local
      @is_windows ||= ( !posix? and ( exist? "C:\\Windows" or exist? "C:\\WinNT" ) )
    end

    def posix?
      @posix ||= self.properties[:platform] == :posix
    end

    def make_absolute! *paths
      paths.map do |path|
        unless path.start_with?('c:/') and path.start_with?('C:/')
          path = "C:/Users/v-mafick/Desktop/sf/workspace/PFTT/#{path}"
        end
        return path
      end
      return paths # TODO temp
      paths.map do |path|
        #escape hatch for already-absolute windows paths
        # TODO temp return path if !posix? && path =~ /\A[A-Za-z]:\//
        return path if path =~ /\A[A-Za-z]:\//  
        
        path.replace( File.absolute_path( path, cwd ) )
        path
      end
    end

    def format_path path
      case
      when windows? then path.gsub('/','\\')
      else path.gsub('\\', '/')
      end
    end

    def pushd path
      cd(path, {:no_clear=>true})
      @dir_stack.push(path)
    end
    
    def popd
      popped = @dir_stack.pop
      if popped
        # if nil, fail silently
        cd(popped, {:no_clear=>true})
      end
    end
    
    def peekd
      @dir_stack.last
    end
    
    def separator
      if windows?
        return "\\"
      else
        return '/'
      end
    end
    
    def join *path_array
      path_array.join(separator)
    end

    def delete glob_or_path
      return true # TODO temp
      make_absolute! glob_or_path
      glob( glob_or_path ) do |path|
        raise Exception unless sane? path
        if directory? path
          exec! case
          when posix? then %Q{rm -rf "#{path}"}
          else %Q{CMD /C RMDIR /S /Q "#{path}"}
          end
        else
          _delete path #implementation specific
        end
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

    def mkdir path
      make_absolute! path
      parent = File.dirname path
      mkdir parent unless directory? parent
      _mkdir path unless directory? path
    end

    def mktmpdir path
      make_absolute! path
      tries = 10
      begin
        dir = File.join( path, String.random(4) )
        raise 'exists' if directory? dir
        mkdir dir
      rescue
        retry if (tries -= 1) > 0
        raise $!
      end
      dir
    end
    
    def tmpdir
      if windows?
        systemdrive + '/temp'
      else
        '/tmp'
      end
    end
    
    def mktmpfile suffix, content=nil
      tries = 10
      begin
        path = File.join( tmpdir(), String.random(4) + suffix )
        
        raise 'exists' if exists? path
        
        f = File.open(path, 'wb')
        if content
          f.puts(content)
        end
        f.close()
        
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
    
    def name
      unless @_name
        # find a name that other hosts on the network will use to reference localhost
        if windows?
          @_name = line!('echo %COMPUTERNAME%')
        else
          @_name = line!('echo $HOSTNAME')
        end
      end
      @_name
    end
    
    protected
    
    attr_accessor :_name, :_osname, :_systeminfo, :_systemdrive, :_systemroot, :posix, :is_windows
    
    def clone(clone)
      clone._osname = @_osname
      clone._systemroot = @_systemroot
      clone._systeminfo = @_systeminfo
      clone._systemdrive = @_systemdrive
      clone.posix = @posix
      clone.is_windows = @is_windows
      clone._name = @_name
      clone
    end
    
  end # class Base

  require 'typed-array'
  
  class Array < TypedArray(Base)
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
