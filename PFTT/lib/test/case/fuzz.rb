
# LATER .rft support
# code
# <?php
#  $args = rand_seed(50)
#  %{function}($args);
# ?>
#
# <expect template="1">
# <function name="abc" i="125">
# <stdout/>
# <stderr/>
# <exit_code/>
# </function>
# <function name="abc" i="125">
# <stdout/>
# <stderr/>
# <exit_code/>
# </function>
# </expect>
#
# <actual template="1">
# <function name="abc" i="125">
# <stdout/>
# <stderr/>
# <exit_code/>
# </function>
# <function name="abc" i="125">
# <stdout/>
# <stderr/>
# <exit_code/>
# </function>
# </actual>
# --code-- section
# add require 'support_functions.php' when executing (definining rand_seed, etc...)
#  do $args1 = rand_seed(50)
#
# --expect-- section or expectf or expectregex 
# with expected output (same format as PHPT expect or expectf or expectregex)
# 
module Test
  module Case
class Fuzz
  def initialize(path)
    @path = path
    @src = file_get_contents(path) # TODO
    @tmp = @src
  end
  
  def clear
    @tmp = @src
  end
  
  def getSource
    @tmp
  end
	
  def getPath
    File.basename(@path)
  end
  	
	def hasArgs
	  @src.includes?('args2')
	end
	
	def replace(tag, value)
	  @tmp = preg_replace('/%\{' + preg_quote(tag) + '\}/', value, @tmp)
	end
	
end # class Fuzz
  end # module Case
end # module Test
