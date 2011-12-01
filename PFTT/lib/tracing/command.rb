
module Tracing
  module Command
    class Actual
      attr_reader :cmd_line, :stdout, :stderr, :exit_code, :opts
      def initialize(cmd_line, stdout, stderr=nil, exit_code=0, opts={})
        @cmd_line = cmd_line
        @stdout = stdout
        @stderr = stderr
        @exit_code = exit_code
        @opts = opts
      end
      def not_found?
        # "bash: fail: command not found"
        return ( shell == :bash and @exit_code == 127 )
      end
      def shell
        if @stderr.include?('bash:')
          :bash
        else
          :unknown
        end
      end
    end
    class Expected
      attr_reader :cmd_line, :stdout, :stderr, :exit_code, :opts
      def initialize(cmd_line, stdout, stderr=nil, exit_code=0, opts={})
        @cmd_line = cmd_line
        @stdout = stdout
        @stderr = stderr
        @exit_code = exit_code
        @opts = opts
      end
      def success?(actual)
        # override this function to provide custom evaluation of a command's success
        # 
        #
        @cmd_line == actual.cmd_line and @stdout == actual.stdout and @stderr == actual.stderr and @exit_code == actual.exit_code
      end
    end
  end
end
