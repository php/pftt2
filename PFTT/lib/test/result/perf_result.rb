
module Test
module Result
  class Perf
    # means we can't tell if page load failed other than by return code
    attr_accessor :first_two_loads_mismatched
    attr_reader :runs
    
    def initialize
      @runs = []
      @first_two_loads_mismatched = false
    end
        
  end # class Perf
end # module Result
end # module Test
