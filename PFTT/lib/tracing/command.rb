
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
      def validate(actual)
        @cmd_line == actual.cmd_line and @stdout == actual.stdout and @stderr == actual.stderr and @exit_code == actual.exit_code
      end
    end
  end
end
