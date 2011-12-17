
module Diff
  module ZD
    module Extension
      module SingleCombo

class BaseRun < BaseSingleRunZD2
  # Base
  include SingleComboExt
  include TestSingleRun
end

class TestRun < BaseSingleRunZD2
  # Test
  include SingleComboExt
  include BaseSingleRun
end

class TestMinusBase < BaseCompareDiffMultiRuns
  # Test-Base
  include SingleComboExt
  include BaseMultiFileTestMinusBase
end

class BaseMinusTest < BaseCompareDiffMultiRuns
  # Base-Test
  include SingleComboExt
  include BaseMultiFileBaseMinusTest
end
      
class BasePlusTest < BaseCompareDiffMultiRuns
  # Base+Test
  include SingleComboExt
  include BaseMultiFileBasePlusTest
end
      
class BaseEqTest < BaseCompareDiffMultiRuns
  # Base=Test
  include SingleComboExt
  include BaseMultiFileBaseEqTest
end

      end # module SingleCombo
      
      module AllCombos
        
class BaseRun < BaseSingleRunZD2
  # Base
  include AllComboExt
  include TestSingleRun
end

class TestRun < BaseSingleRunZD2
  # Test
  include AllComboExt
  include BaseSingleRun
end

class TestMinusBase < BaseCompareDiffMultiRuns
  # Test-Base
  include AllComboExt
  include BaseMultiFileTestMinusBase
end

class BaseMinusTest < BaseCompareDiffMultiRuns
  # Base-Test
  include AllComboExt
  include BaseMultiFileBaseMinusTest
end
      
class BasePlusTest < BaseCompareDiffMultiRuns
  # Base+Test
  include AllComboExt
  include BaseMultiFileBasePlusTest
end
      
class BaseEqTest < BaseCompareDiffMultiRuns
  # Base=Test
  include AllComboExt
  include BaseMultiFileBaseEqTest
end        
        
      end # module AllCombos
              
    end # module Extension
  end # module ZD
end
