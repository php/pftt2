
module Util
  module Install
    class Zip7 < Base
      
      def label
        '7Zip'
      end
      
      protected
      
      def check_posix
        has_cmd('7zip')
      end
      
      def check_windows(ctx)
        @host.exist?(@host.systemdrive+'\\Program Files\\7-Zip', ctx)
      end
      
      def install_windows(ctx)
        msi_install('', '', ctx)
      end
      
    end
  end
end
