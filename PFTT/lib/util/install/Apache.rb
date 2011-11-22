
module Util
  module Install
    class Apache < Base
      
      def label
        'Apache HTTPD'
      end
      
      protected
      
      def check_windows(ctx)
        check_files([@host.systemdrive+'/Program Files (x86)/Apache Software Foundation/Apache2.2/', @host.systemdrive+'/Program Files/Apache Software Foundation/Apache2.2/'], ctx)
      end
    
      def check_posix(ctx)
        has_cmd('httpd', ctx)
      end
    
      def install_windows(ctx)
        msi_install('httpd-2.2.21-win32-x86-openssl-0.9.8r.msi', '/Q', ctx)
      end
    
      def install_emerge(ctx)
        emerge_install('apache-httpd', ctx)
      end
    
      def install_yum(ctx)
        yum_install('apache-httpd', ctx)
      end
    
      def install_apt_get(ctx)
        apt_get_install('apache-httpd', ctx)
      end
      
    end
  end
end
