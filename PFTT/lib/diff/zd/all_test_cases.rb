
module Diff
  module ZD
    module AllTestCases
      module SingleCombo
            
class BaseRun < BaseSingleRunZD2
  # Base
  include SingleComboAll
  include TestSingleRun
end

class TestRun < BaseSingleRunZD2
  # Test
  include SingleComboAll
  include BaseSingleRun
end

class TestMinusBase < BaseCompareDiffMultiRuns
  # Test-Base
  include SingleComboAll
  include BaseMultiFileTestMinusBase
end

class BaseMinusTest < BaseCompareDiffMultiRuns
  # Base-Test
  include SingleComboAll
  include BaseMultiFileBaseMinusTest
end
      
class BasePlusTest < BaseCompareDiffMultiRuns
  # Base+Test
  include SingleComboAll
  include BaseMultiFileBasePlusTest
end
      
class BaseEqTest < BaseCompareDiffMultiRuns
  # Base=Test
  include SingleComboAll
  include BaseMultiFileBaseEqTest
end

      end # module SingleCombo
      
      module AllCombos
        
class BaseRun < BaseSingleRunZD2
  # Base
  include AllComboAll
  include TestSingleRun
end

class TestRun < BaseSingleRunZD2
  # Test
  include AllComboAll
  include BaseSingleRun
end

class TestMinusBase < BaseCompareDiffMultiRuns
  # Test-Base
  include AllComboAll
  include BaseMultiFileTestMinusBase
end

class BaseMinusTest < BaseCompareDiffMultiRuns
  # Base-Test
  include AllComboAll
  include BaseMultiFileBaseMinusTest
end
      
class BasePlusTest < BaseCompareDiffMultiRuns
  # Base+Test
  include AllComboAll
  include BaseMultiFileBasePlusTest
end
      
class BaseEqTest < BaseCompareDiffMultiRuns
  # Base=Test
  include AllComboAll
  include BaseMultiFileBaseEqTest
end        
        
      end # module AllCombos      
    end # module AllTestCases
  end
end
