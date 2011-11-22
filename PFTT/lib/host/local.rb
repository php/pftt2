
module Host
  class Local < HostBase
    #instantiable 'local'
    
    def reboot_wait(seconds, ctx)
      if ctx
        # get approval before rebooting localhost
        if Tracing::Context::SystemSetup::Reboot.new(ctx).approve_reboot_localhost()
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
        ctx.write_file(string, path) do |string, path|
          return write(string, path, nil)
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
        ctx.read_file(out, path) do |path|
          return read(path, nil)
        end
      end
      return out
    end

    def cwd ctx=nil
      if ctx
        ctx.fs_op0(self, :cwd) do
          return cwd(nil)
        end
      end
      return Dir.getwd()
    end
        
    def cd path, hsh, ctx=nil
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
      
      Dir.chdir(path)
          
      @dir_stack.clear unless hsh.delete(:no_clear) || false
          
      return path
    end

    alias :upload :copy
    alias :download :copy

    def exist? file, ctx=nil
      if ctx
        ctx.fs_op1(self, :exist, file) do |file|
          return exist?(file, nil)
        end
      end
      make_absolute! file
      File.exist? file
    end

    def directory? path, ctx=nil
      if ctx
        ctx.fs_op1(self, :is_dir, path) do |path|
          return directory?(path, nil)
        end
      end
      make_absolute! path
      exist?(path) && File.directory?(path)
    end

    def open_file path, flags='r', ctx, &block
      if ctx
        ctx.fs_op1(self, :open, path) do |path|
          return open_file(path, flags, nil, block)
        end
      end
      make_absolute! path
      File.open path, flags, &block
    end
    
    # list the immediate children of the given path
    def list path, ctx
      if ctx
        ctx.fs_op1(self, :list, path) do |path|
          return list(path, nil)
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
      def initialize(stdout, stderr)
        # TODO 
      end
      def write_stdin(stdin_data)
      end
      def read_stderr
        ''
      end
      def read_stdout
        ''
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
         
        #     
        process = builder.start()
        
        #
        if opts[:timeout].is_a?(Integer) and opts[:timeout] > 0
          # monitor the process to make sure it exits in time, or kill it
          Thread.start do
            sleep(opts[:timeout])
                      
            begin
              process.exitValue()
              # process is not running anymore
                      
            rescue java.lang.IllegalThreadStateException => ex
              # process is still running, kill it
                      
              process.destroy
                      
            end
          end
        end
        #
              
        def copy_stream(src, dst)
          # see https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby
          block = Java::byte[128].new
          len = 0
          while ( ( len = src.read(block)) != -1 ) do
            dst.write(block, 0, len)
          end
        end
                
        if opts[:stdin_data]
          process.getOutputStream().write(opts[:stdin_data].toByteArray())
        end
              
        output = java.io.ByteArrayOutputStream.new()
        error = java.io.ByteArrayOutputStream.new()
        copy_stream(java.io.BufferedInputStream.new(process.getInputStream()), output)
        copy_stream(java.io.BufferedInputStream.new(process.getErrorStream()), error)
              
        o = output.toString()
        e = error.toString()
        
        # wait for the process to exit
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
      end
      
      if block
        lh = LocalExecHandle.new(o, e)
        o = nil
        e = nil
        # TODO block.call(lh)
      end
            
      return [o, e, w]
    end # def _exec_impl

    def _delete path, ctx
      if ctx
        ctx.fs_op1(self, :delete, path) do |path|
          return _delete(path, nil)
        end
      end
      make_absolute! path
      File.delete path
    end

    def _mkdir path, ctx
      if ctx
        ctx.fs_op1(self, :mkdir, path) do |path|
          return _mkdir(path, nil)
        end
      end
      make_absolute! path
      Dir.mkdir path
    end
    
  end # class Local
end
