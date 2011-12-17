
module Tracing
  module Dashboard
    
    # TODO try to show this if no action was given on command line
class ActionChooser
  
  def initialize(host)
    super(host)
    
    JButton.new('core_part') # TODO launch Tracing::Dashboard::OptionChooser
    JButton.new('core_full')
  
    JButton.new('console') # TODO launch Tracing::Dashboard::Console
  end
  
  
  
end # class ActionChooser
    
  end # module Dashboard
end # module Tracing
