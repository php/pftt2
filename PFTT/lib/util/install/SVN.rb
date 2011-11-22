
module Util
  module Install
    class SVN < Base
      
      def label
        'SVN'
      end
      
      protected
      
      def check_windows(ctx)
        check_files([@host.systemdrive+'\\Program Files (x86)\\Subversion\\bin', @host.systemdrive+'\\Program Files\\Subversion\\bin'], ctx)
      end
      
      def check_posix(ctx)
        has_cmd('svn', ctx)
      end
      
      def install_windows(ctx)
        msi_install('Setup-Subversion-1.6.17.msi', '/Q', ctx)
      end
      
      def install_emerge(ctx)
        emerge_install('svn', ctx)
      end
      
      def install_yum(ctx)
        yum_install('svn', ctx)
      end
      
      def install_apt_get(ctx)
        apt_get_install('svn', ctx)
      end
      
    end
  end
end
