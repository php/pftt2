
module Util
  module Install
    class VC9 < Base
      
      def label
        'VC9 Runtime'
      end
      
      def posix?
        false # windows only
      end
      
      protected
      
      def install_windows(ctx)
        # if windows host, ensure it has the VC9 Runtime (x86) installed
        # (it is required for PHP to run)
        msi_install('vc9_vcredist_x86.exe', '/Q', ctx) 
      end
      
      def check_windows(ctx)
        check_files({:count=>2, :base=>@host.systemdrive+'\\windows\\winsxs\\', :glob=>'x86_microsoft.vc90'}, ctx)
      end
      
    end
  end
end
