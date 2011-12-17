
module Diff
  module ZD
    module TestCase
      module SingleCombo

class BaseRun < BaseSingleRunZD2
  # Base
  include SingleComboSingleFile
  include TestSingleRun
end

class TestRun < BaseSingleRunZD2
  # Test
  include SingleComboSingleFile
  include BaseSingleRun
end

class TestMinusBase < BaseCompareDiffMultiRuns
  # Test-Base
  include SingleComboSingleFile
  include BaseMultiFileTestMinusBase
end

class BaseMinusTest < BaseCompareDiffMultiRuns
  # Base-Test
  include SingleComboSingleFile
  include BaseMultiFileBaseMinusTest
end
      
class BasePlusTest < BaseCompareDiffMultiRuns
  # Base+Test
  include SingleComboSingleFile
  include BaseMultiFileBasePlusTest
end
      
class BaseEqTest < BaseCompareDiffMultiRuns
  # Base=Test
  include SingleComboSingleFile
  include BaseMultiFileBaseEqTest
end

      end # module SingleCombo
      
      module AllCombos
        
class BaseRun < BaseSingleRunZD2
  # Base
  include AllComboSingleFile
  include TestSingleRun
end

class TestRun < BaseSingleRunZD2
  # Test
  include AllComboSingleFile
  include BaseSingleRun
end

class TestMinusBase < BaseCompareDiffMultiRuns
  # Test-Base
  include AllComboSingleFile
  include BaseMultiFileTestMinusBase
end

class BaseMinusTest < BaseCompareDiffMultiRuns
  # Base-Test
  include AllComboSingleFile
  include BaseMultiFileBaseMinusTest
end
      
class BasePlusTest < BaseCompareDiffMultiRuns
  # Base+Test
  include AllComboSingleFile
  include BaseMultiFileBasePlusTest
end
      
class BaseEqTest < BaseCompareDiffMultiRuns
  # Base=Test
  include AllComboSingleFile
  include BaseMultiFileBaseEqTest
end        
        
      end # module AllCombos              
    end # module TestCase
  end # module ZD
end
