
module Host
  module Remote
    class RemoteBase < HostBase
      # see Host::Remote::ClientToHostedClientInterface
      attr_accessor :remote_interface
      def remote?
        true
      end
    end
  end
end
  