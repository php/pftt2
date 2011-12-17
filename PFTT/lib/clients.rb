
#require 'clients/lock.rb'

module Clients
  
class ClientBase
  include Clients::Lock
  
  def _common_ruby_runtime_prep
    # share with #run_script and Console::Panel
  end
  
  def run_script(filename)
    # TODO
  end
  
end

end # module Clients
