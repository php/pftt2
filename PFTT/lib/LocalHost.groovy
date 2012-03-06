package com.mostc.pftt

import java.util.Timer
import java.util.TimerTask
import java.io.*
import java.lang.ProcessBuilder
import java.io.BufferedReader
import java.io.FileReader
import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.BufferedInputStream
import java.util.HashMap
import java.lang.Byte

import com.STDIN;
import com.mostc.pftt.*

//LocalHost.metaClass {
//	upload = {copy}
//	download = {copy}	
//}
class LocalHost extends Host {
	
	@Override
	def upload(local_file, remote_path, ctx, opts=[]) {
		copy(local_file,  remote_path, ctx)
	}
	
	@Override
	def env_values(ctx=null) {
      //Hash.new(System.getenv()) //ENV.keys
	}
    
	@Override
    def env_value(name, ctx=null) {
		if (name=='HOSTNAME' && isPosix(ctx)) {
			// Linux: for some reason this env var is not available
			//
			// execute program 'hostname' instead
			return line('hostname', ctx)
		}
		System.getenv(name) // LATER ENV[name]
    } // end def env_value
    
	@Override
    def reboot_wait(seconds, ctx) {
//      if (ctx) {
//        // get approval before rebooting localhost
//        if (ctx.new(Tracing::Context::SystemSetup::Reboot).approve_reboot_localhost()) {
//          return super.reboot_wait(seconds, ctx)          
//		}
//      } else {
        return super.reboot_wait(seconds, ctx)
//      }
    }
    
//    def clone
//      clone = Host::Local.new()
//      super(clone)
//    end
    
	@Override
    def close() {
      // nothing to close
    }
    
//    def toString() {
//      if (isPosix())
//        return 'Localhost (Posix)'
//      else if (isWindows())
//        return 'Localhost (Windows)'
//      else
//        return 'Localhost (Platform-Unknown)'
//      }
    
	@Override        
    def isAlive() {
		// was able to call this method therefore must be true (if false, couldn't have called this method)
      true
    }
    
	@Override
    def write(string, path, ctx) {
      // writes the given string to the given file/path
      // overwrites the file if it exists or creates it if it doesn't exist
      //
//      if (ctx) {
//        ctx.write_file(self, string, path) |new_string, new_path| {
//          return write(new_string, new_path, ctx)
//        }
//      }
      
      mkdir(File.dirname(path), ctx)
            
      output = BufferedOutputStream.new(FileOutputStream.new(path))
      output.write(string, 0, string.length)
      output.close
    }
            
	@Override
    def read_lines(path, ctx=null, max_lines=16384) {
      def lines = []
	  def line
        
      def reader = new BufferedReader(new FileReader(path))
	  while ( ( line = reader.readLine() ) != null && !(lines.size()> max_lines)) {
		  lines.add(line)
      }
      reader.close()
      
      lines
    }
    
	@Override
    def read(path) {
      def output = new ByteArrayOutputStream(1024)
      def input = new BufferedInputStream(new FileInputStream(path))

      copy_stream(input, output)      
      
      input.close
      
      output.toString
    }
	
	@Override
	def getTime(ctx=null) {
		new Date()	
	}

	@Override
    def cwd(ctx=null) {
      if (ctx) { 
        ctx.fs_op0(self, EFSOp.cwd) {
          return cwd(ctx)
        }
      }
      return Dir.getwd()
    }
        
	@Override
    def cd(path, hsh, ctx=null) {
      if (ctx) {
        ctx.fs_op1(self, EFSOp.cd, path) |new_path| {
          return cd(new_path, hsh, ctx)
        }
      }
	  
      make_absolute(path)
	  
      if (!path) {
        // popd may have been called when @dir_stack empty
        throw new IllegalArgumentException("path not specified")
      }
      
//      Dir.chdir(path)
          
	  // TODO dir_stack.clear unless hsh.delete(:no_clear) || false
          
      return path
    } // end def cd

	@Override
    def directory(path, ctx=null) {
      if (ctx) {
        ctx.fs_op1(self, EFSOp.is_dir, path) |new_path| {
          return directory(new_path, ctx)
        }
      }
	  
      make_absolute(path)
	  
      new File(path).isDirectory()
    }

    // list the immediate children of the given path
	@Override
    def list(path, ctx) {
      if (ctx) {
        ctx.fs_op1(self, EFSOp.list, new_path) |new_path| {
          return list(new_path, ctx)
        }
      }
	  
      make_absolute(path)
	  
//      Dir.entries( path ).map! do |entry|
//        next null if ['.','..'].include? entry
//        entry
//      end.compact
      }
    
//    def glob(path, spec, ctx, &blk) {
//      if (ctx) {
//        ctx.fs_op2(self, :glob, path, spec) |new_path, new_spec| {
//          return glob(new_path, new_spec, ctx, blk)
//        }
//      }
//	  
//      make_absolute(path)
//	  
//      Dir.glob("//{path}///{spec}", &blk)
//    }
    
	@Override
    def isRemote() {
      false
    }
    
	@Override
    def mtime(file, ctx=null) {
      File.mtime(file).to_i
    }
    
    static def copy_stream(input, output) {
      def tmp = new  byte[1024]
      while (true) {
        //input.available
        def len = input.read(tmp, 0, 1024)
        if (len < 0) {
          break
        }
        output.write(tmp, 0, len)
//          len = 0// TODO input.read(tmp, 0, 1024)
//          if len < 0
            break
//          end
//          // TODO output.write(tmp, 0, len)
//        end
      }
    } // end def copy_stream
    
    protected
    
    def _exist(file, ctx) {
      new File(file).exists()
    }
    
    def move_file(from, to, ctx) {
      new File(from).renameTo(new File(to))
    }
    
    def _exec_impl(command, opts, ctx, block) {
		// TODO
      def lh = STDIN.exec_impl(command, null, null, 0, null)
        
      if (opts.exec_handle) {
        return lh
      } else {
        def ret = lh.run_read_streams()
        return [output:ret[0], error:ret[1], exit_code:ret[2]]
      }
    } // end def _exec_impl
        
    def _delete(path, ctx) {
      make_absolute(path)

      new File(path).delete()
    }

    def _mkdir(path, ctx) {
      make_absolute(path)

      new File(path).mkdir()
    }
	
	def _make_absolute(path) {
		new File(path).getAbsolutePath()
	}
    
} // end public class LocalHost
