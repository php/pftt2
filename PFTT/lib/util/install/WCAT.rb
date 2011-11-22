
module Util
  module Install
    class WCAT < Base
         
      def label
        'WCAT'
      end
      
      def posix?
        false # windows only
      end
      
      protected
      
      def check_windows(ctx)
        # TODO
      end
      
      def install_windows(ctx)
        msi_install('wcat.amd64.msi', '/Q', ctx)
      end
      
    end
  end
end
