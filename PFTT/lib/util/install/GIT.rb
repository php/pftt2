
module Util
  module Install
    class GIT < Base
      
      def label
        'GIT'
      end
      
      protected
      
      def check_windows(ctx)
        check_files([@host.systemdrive+'\\Program Files (x86)\\Git', @host.systemdrive+'\\Program Files\\Git'], ctx)
      end
      
      def check_posix(ctx)
        has_cmd('git', ctx)
      end

      def install_windows(ctx)
        msi_install("msysgit-v111.msi", "/Q", ctx)
      end      
      
      def install_emerge(ctx)
        emerge_install('git', ctx)
      end
      
      def install_yum(ctx)
        yum_install('git', ctx)
      end
      
      def install_apt_get(ctx)
        apt_get_install('git', ctx)
      end  
      
    end
  end
end
