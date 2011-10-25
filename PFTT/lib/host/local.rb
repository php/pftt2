require 'fileutils'

module Host
  class Local < Base
    instantiable 'local'
    
    def address
      '127.0.0.1'
    end
    
    def clone
      clone = Host::Local.new()
      # clone may have a different @dir_stack, so it really must be cloned
      super(clone)
    end
    
    def close
      # nothing to close
    end
    
    def to_s
      if posix?
        return 'Localhost (Posix)'
      elsif windows?
        return 'Localhost (Windows)'
      else
        return 'Localhost (Platform-Unknown)'
      end
    end
            
    def alive?
      true
    end

    #
    # command:
    #   command line to run. program name and arguments to program as one string
    # options: a hash of
    #  :env
    #     keys and values of ENV must be strings (not ints, or anything else!)
    #  :chdir
    #     the current directory of the command to run
    #  :binmode  true|false
    #     will set the binmode of STDOUT and STDERR. binmode=false on Windows may affect STDOUT or STDERR output
    #  :debug   true|false
    #     runs the command with the host's debugger. if host has no debugger installed, command will be run normally
    #  :stdin   ''
    #     feeds given string to the commands Standard Input
    # other options are silently ignored
    #
    # returns array of 3 elements. 0=> STDOUT output as string 1=>STDERR 2=>command's exit code (0==success)
    def exec command, opts={}
      
      watcher = Thread.start do
        retries = 3
        begin
          if opts.has_key?(:env)
            env = opts[:env]
          else
            env = {}
          end
          
          # run command in platform debugger
          if opts.has_key?(:debug) and opts[:debug] == true
            command = debug_wrap(command)
          end
          
          new_opts = {}
          if opts.has_key?(:chdir)
            new_opts[:chdir] = opts[:chdir]
          end
          if opts.has_key?(:binmode)
            new_opts[:binmode] = opts[:binmode]
          end
          if opts.has_key?(:stdin)
            new_opts[:stdin_data] = opts[:stdin]
          end
          
          o,e,w = Open3.capture3(env, command, new_opts)
          
          [o, e, w]
        rescue
          if (retries-=1) >= 0
            sleep 2
            retry
          end
          raise $!
        end
      end # Thread.start
    end
    
    def cwd
      return Dir.getwd()
    end
        
    def cd path, hsh
      make_absolute! path
      if not path
        # popd may have been called when @dir_stack empty
        raise "path not specified"
      end
      
      Dir.chdir(path)
          
      @dir_stack.clear unless hsh.delete(:no_clear) || false
          
      return path
    end

    def copy src, dest
      make_absolute! src, dest
      #puts %Q{copy( #{src.inspect}, #{dest.inspect} )}
      # copy does this normally, but we will ensure it 
      # happens consistently before we descend
      if directory? dest
        dest = File.join( dest, File.basename(src) )
      end
      
      FileUtils.cp_r( src, dest, :preserve=>false )

      return dest
    end
    alias :upload :copy
    alias :download :copy

    def exist? file
      make_absolute! file
      File.exist? file
    end

    def directory? path
      make_absolute! path
      exist?(path) && File.directory?(path)
    end

    def open_file path, flags='r', &block
      make_absolute! path
      File.open path, flags, &block
    end
    
    # list the immediate children of the given path
    def list path
      make_absolute! path
      Dir.entries( path ).map do |entry|
        next nil if ['.','..'].include? entry
        entry
      end.compact
    end

    def glob spec, &block
      make_absolute! spec
      Dir.glob spec, &block
    end
    
    protected

    def _delete path
      make_absolute! path
      File.delete path
    end

    def _mkdir path
      make_absolute! path
      Dir.mkdir path
    end
    
  end # class Local
end