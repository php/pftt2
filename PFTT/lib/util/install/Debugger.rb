
module Util
  module Install
    class Debugger < Base
      
      def label
        if @host
          if @host.windows?
            'WinDbg'
          else
            'GDB'
          end
        else
          'Debugger'
        end
      end
      
      protected
      
      def check_windows(ctx)
        check_files([@host.systemdrive+'\\Program Files (x86)\\Debugging Tools for Windows (x86)', @host.systemdrive+'\\Program Files (x86)\\Debugging Tools for Windows', @host.systemdrive+'\\Program Files\\Debugging Tools for Windows (x86)', @host.systemdrive+'\\Program Files\\Debugging Tools for Windows'], ctx)
      end
      
      def check_posix(ctx)
        has_cmd('gdb', ctx)
      end
      
      def install_windows(ctx)
        msi_install('Dbg', ctx)
      end
      
      def install_emerge(ctx)
        emerge_install('gdb', ctx)
      end
      
      def install_yum(ctx)
        yum_install('gdb', ctx)
      end
      
      def install_apt_get(ctx)
        apt_get_install('gdb', ctx)
      end
      
    end
  end
end
