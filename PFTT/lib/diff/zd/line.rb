
module Diff
  module ZD
    module Line

module SingleLine
  #
  # TODO 
end 

module CompareLine
  #
  # TODO
end     
      
class BaseRun < BaseSingleRunZD2
  # Base
  include TestSingleRun
  include SingleLine
end

class TestRun < BaseSingleRunZD2
  # Test
  include BaseSingleRun
  include SingleLine
end

class TestMinusBase < BaseCompareDiffMultiRuns
  # Test-Base
  include BaseMultiFileTestMinusBase
  include CompareLine
end

class BaseMinusTest < BaseCompareDiffMultiRuns
  # Base-Test
  include BaseMultiFileBaseMinusTest
  include CompareLine
end
      
class BasePlusTest < BaseCompareDiffMultiRuns
  # Base+Test
  include BaseMultiFileBasePlusTest
  include CompareLine
end
      
class BaseEqTest < BaseCompareDiffMultiRuns
  # Base=Test
  include BaseMultiFileBaseEqTest
  include CompareLine
end     
              
    end # module Line
  end # module ZD
end
