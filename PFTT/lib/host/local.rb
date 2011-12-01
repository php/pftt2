
require 'java'
include_class 'java.util.Timer'
include_class 'java.util.TimerTask'

module Host
  class Local < HostBase
    #instantiable 'local'
    
    def reboot_wait(seconds, ctx)
      if ctx
        # get approval before rebooting localhost
        if ctx.new(SystemSetup::Reboot).approve_reboot_localhost()
          super(seconds, ctx)          
        end
      else
        super(seconds, ctx)
      end
    end
    
    def address
      '127.0.0.1'
    end
    
    def clone
      clone = Host::Local.new()
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
    
    def write(string, path, ctx)
      # writes the given string to the given file/path
      # overwrites the file if it exists or creates it if it doesn't exist
      #
      if ctx
        ctx.write_file(self, string, path) do |string, path|
          return write(string, path, ctx)
        end
      end
      f = open_file(path, 'wb')
      f.puts(string)
      f.close()
    end
        
    def read(path, ctx)
      f = open_file(path, 'rb', ctx)
      out = f.gets()
      f.close()
      if ctx
        ctx.read_file(self, out, path) do |path|
          return read(path, ctx)
        end
      end
      return out
    end

    def cwd ctx=nil
      if ctx
        ctx.fs_op0(self, :cwd) do
          return cwd(ctx)
        end
      end
      return Dir.getwd()
    end
        
    def cd path, hsh, ctx=nil
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
      
      Dir.chdir(path)
          
      @dir_stack.clear unless hsh.delete(:no_clear) || false
          
      return path
    end # def cd

    alias :upload :copy
    alias :download :copy
    
    def exist? file, ctx=nil
      if ctx
        ctx.fs_op1(self, :exist, file) do |file|
          return exist?(file, ctx)
        end
      end
      make_absolute! file
      File.exist? file
    end
    
    alias :exists? :exist?
    alias :exist :exist?
    alias :exists :exist?

    def directory? path, ctx=nil
      if ctx
        ctx.fs_op1(self, :is_dir, path) do |path|
          return directory?(path, ctx)
        end
      end
      make_absolute! path
      exist?(path) && File.directory?(path)
    end

    def open_file path, flags='r', ctx=nil, &block
      if ctx
        ctx.fs_op1(self, :open, path) do |path|
          return open_file(path, flags, ctx, block)
        end
      end
      make_absolute! path
      File.open path, flags, &block
    end
    
    # list the immediate children of the given path
    def list path, ctx
      if ctx
        ctx.fs_op1(self, :list, path) do |path|
          return list(path, ctx)
        end
      end
      make_absolute! path
      Dir.entries( path ).map do |entry|
        next nil if ['.','..'].include? entry
        entry
      end.compact
    end
    
    def remote? 
      false
    end
    
    protected
    
    class LocalExecHandle < ExecHandle
      def initialize(stdout, stderr, process=nil)
        @stdout = stdout
        @stderr = stderr
        @process = process
      end
      def write_stdin(stdin_data)
        if @process
          @process.getOutputStream().write(opts[:stdin_data].toByteArray())
        end
      end
      def post(type, buf)
        case type
        when :stdout
          @stdout = buf
        when :stderr
          @stderr = buf
        end
      end
      def read_stderr
        @stderr
      end
      def read_stdout
        @stdout
      end
    end # class LocalExecHandle
    
    def _exec_impl command, opts, ctx, block
      o = ''
      e = ''
      w = -255
      
      #
      if opts.has_key?(:env)
        env = opts[:env]
      else
        env = {}
      end
      #
                   
      #
      # == begin JRuby specific block ==
      if RUBY_PLATFORM == 'java'
        # JRuby's Open3 implementation (specifically IO.popen3 (which uses ShellLauncher))
        # adds cmd /C (and possibly other manipulations) to the command, which prevents it
        # from finding the file to execute
        require 'java'
              
        builder = java.lang.ProcessBuilder.new(command.split(' '))
        if env
          builder.environment().putAll(java.util.HashMap.new(env))
        end
        if opts[:chdir]
          builder.directory(java.io.File.new(opts[:chdir]))
        end
         
        # start the process
        process = builder.start()
        
        #
        if opts[:timeout].is_a?(Integer) and opts[:timeout] > 0
          # share a thread amongst all hosts rather than one thread per exec!() call
          # (that can lead to thread exhaustion)
          #
          # LATER MRI support
          @@timer.schedule(ExitMonitorTask.new(process), opts[:timeout]*1000)
        end # if
        #
        
        lh = LocalExecHandle.new('', '', process)
              
        # 
        def copy_stream(src, dst, type, lh, block, max_len)
          # see https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby
          buf = Java::byte[128].new
          len = 0
          total_len = 0
          while ( ( len = src.read(buf)) != -1 ) do
            dst.write(buf, 0, len)
            
            if block
              lh.post(type, buf)
              block.call(lh)
            end
            
            if max_len > 0
              total_len += 1
              # when output limit reached, stop copying automatically
              if total_len > max_len
                break
              end
            end
          end
        end
        #
                
        if opts[:stdin_data]
          lh.write_stdin(opts[:stdin_data])
        end
              
        output = java.io.ByteArrayOutputStream.new()
        error = java.io.ByteArrayOutputStream.new()
        # copy the output streams
        copy_stream(java.io.BufferedInputStream.new(process.getInputStream()), output, :stdout, lh, block, opts[:max_len])
        copy_stream(java.io.BufferedInputStream.new(process.getErrorStream()), error, :stderr, lh, block, opts[:max_len])
              
        o = output.toString()
        e = error.toString()
        
        # wait while the process runs/wait for the process to exit
        w = process.waitFor()
        
        #
        # == end JRuby Specific block ==
        #
      else
        #
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
        
        # may get Errno:E2BIG (arg list too long)
        o, e, w = Open3.capture3(env, command, new_opts)
      
        if block
          lh = LocalExecHandle.new(o, e)
          o = nil
          e = nil
          block.call(lh)
        end
      end
            
      return [o, e, w]
    end # def _exec_impl
    
    # used as a timer/shared thread to monitor all processes started by exec!() to
    # ensure they end within the timeout or are killed
    @@timer = Timer.new
    
    class ExitMonitorTask < TimerTask
      
      def initialize(process)
        super()
        @process = process
      end
      
      def run
        # check the process to make sure exited in time. if not, kill it
        begin
          @process.exitValue()
          # process is not running anymore
                        
        rescue java.lang.IllegalThreadStateException => ex
          # process is still running, kill it
                        
          @process.destroy
        end # begin
              
      end # def run
      
    end # class ExitMonitorTask
    
    def _delete path, ctx
      make_absolute! path
      File.delete path
    end

    def _mkdir path, ctx
      make_absolute! path
      Dir.mkdir path
    end
    
  end # class Local
end
