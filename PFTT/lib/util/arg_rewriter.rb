
module Util
  class ArgRewriter  
    def initialize(args)
      @args = Array.new(args)
    end
    
    def cmd(cmd)
      @args[0] = cmd
    end
    
    def add(arg, value)
    end
    
    def delete(idx)
      @args.delete(idx)
    end
    
    def remove(arg)
      @args.delete_if {|x| x == arg }
    end
    
    def replace(arg, value)
      remove(arg)
      add(arg, value)
    end
    
    def join
      'pftt '+@args.join(' ')
    end
  end
end