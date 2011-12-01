
module Test
  module Runner
    
class Fuzz
	METHOD_ONLY   = 1
	FUNCTION_ONLY = 2
	CLASS_ONLY    = 4
		
	def initialize(config, host)
	  @config = config
	  @host = host
	  
	  @flags = 0
	  @fuzzers = [] 
	  @exts = []
	  @classes = []
	  @func_names = []
	  @config = []
	  @logger = Logger.new([config['stdout'], config['stderr']])
	end
	
	def setFlag(value)
	  @flags |= value
	end
	
	def setExtension(ext_name)
	  @exts.push(ReflectionExtension.new(ext_name, @host))
	end
	
	def setClass(class_name)
	  @classes.push(ReflectionClass.new(class_name, @host))
	end
	
	def setFunction(func_name)
	  @func_names.push(ReflectionFunction.new(func_name, @host))
	end
		
	def setFuzzer(fuzzer)
	  fuzzer.init(@logger, @config)
	  
	  @fuzzers.push(fuzzer)
	end
	
	def run
	  @logger.write_start
	  
	  @fuzzers.each do |fuzzer|
	    
	    @exts.each do |ext|
	      unless @flags & METHOD_ONLY|CLASS_ONLY
	        ext.getFunctions().each do |func|
	          metadata = {
	            'function' => func.name,
	            'name'     => func.name,
	            'type'     => 'function'
	          }
	          
	          fuzzer.runFuzzer(metadata)
	        end # getFunctions
	      end
	      
	      unless @flags & FUNCTION_ONLY
	        ext.getClasses().each do |clazz|
	          runClass(fuzzer, clazz)
	        end
	      end
	    end # exts.each
	    
	    @classes.each do |clazz|
	      runClazz(fuzzer, clazz)
	    end
	    
	    @func_names.each do |func_name|
	      metadata = {
          'function' => func_name,
	        'name'     => func_name,
	        'type'     => 'function'
	      }
	      
	      runFuzzer(metadata)
	    end
	  end # fuzzers.each
	  
	  @logger.write_end
	end # def run
	
	protected
	
  def runClass(fuzzer, clazz)
    if @flags & METHOD_ONLY|FUNCTION_ONLY
      metadata = {
        'class' => clazz.name,
        'name' => clazz.name,
        'type' => 'class'
      }
      @fuzzer.runFuzzer(metadata)
    end
    if @flags & CLASS_ONLY|FUNCTION_ONLY
      clazz.getMethods.each do |method|
        metadata = {
          'class' => clazz.name,
          'method' => method.name,
          'name' => clazz.name + '::' + method.name,
          'type' => 'method'
        }
        @fuzzer.runFuzzer(metadata)
      end
    end
  end # def runClass
  
  class ReflectionBase
    # internal class
    # 
    # to get reflection info from PHP, this executes a PHP script which prints out the
    # info (XML) and then parses that info
    #
    # provides
    # * functions from an extension
    # * classes
    # * methods from a class
    #
    def initialize(php_binary, php_ini_cli_string, host)
      # php_binary => path to php.exe
      # php_ini_cli_string => php INI ready to be passed to php.exe as command line parameters
      #   (each directive in a -d cli parameter)
      # host => host to execute on
      #
      @host = host
      @php_binary = php_binary
      @php_ini_cli_string = php_ini_cli_string
    end
    
    protected
    
    def reflect(php_code)
      file = @host.mktempfile(php_code, '.php')
      
      ret = @host.exec!(@php_binary+' '+@php_ini_cli_string+' '+file)
      
      @host.delete(file)
      
      return ret[0].split("\n")
    end
  end # class ReflectionBase
	
  class ReflectionExtension < ReflectionBase
    def initialize(ext_name, php_binary, php_ini_cli_string, host)
      super(php_binary, php_ini_cli_string, host)
      @ext_name = ext_name
      @functions = nil
      @classes = nil
    end
    def getFunctions
      if @functions
        return @functions
      end
      
      php_code <<-PHP
<?php
$rf = new ReflectionExtension('#{@ext_name}');
foreach ( $rf->getFunctions() as $func ) {
echo $func->getName() . "\n";
}
?>
PHP
      
      @functions = reflect(php_code)
      return @functions
    end
    def getClasses
      if @classes
        return @classes
      end
      
      php_code <<-PHP
<?php
$rf = new ReflectionExtension('#{@ext_name}');
foreach ( $rf->getClasses() as $clazz ) {
echo $clazz->getName() . "\n";
}
?>
PHP
      @classes = reflect(php_code)
      return @classes
    end
  end # class ReflectionExtension
  
  class ReflectionClass < ReflectionBase
    def initialize(class_name, php_binary, php_ini_cli_string, host)
      super(php_binary, php_ini_cli_string, host)
      @class_name = class_name
      @methods = nil
    end
    def getMethods
      if @methods
        return @methods
      end
      
      php_code <<-PHP
<?php
$rf = new ReflectionClass('#{@class_name}');
foreach ( $rf->getMethods() as $method ) {
echo $method->getName() . "\n";
}
?>
PHP

      @methods = reflect(php_code)
      
      return @methods
    end
  end # class ReflectionClass
  

class Logger
  STDOUT = 0
  STDERR = 1

  def initialize(arr)
    @files = {}
    @fds = {}
      
    arr.map do |type, file|
      @files[type] = file
      @fds[type] = File.open(file, 'w')
    end
  end 
  
  def write_start
    @fds.each do |fd|
      fd.write("<results>\n")
    end
  end 
  
  def write_end
    @fds.each do |fd|
      fd.write("</results>\n")
    end
  end
  
  def log(type, data)
    tag = type == STDOUT ? 'stdout' : 'stderr'
    
    xml = "\t<#{tag}>\n"
    xml += "\t\t<name>#{data['name']}</name>\n"
    xml += "\t\t<code><![CDATA[\n#{data['src']}\n]]></code>\n"
    xml += "\t\t<result><![CDATA[#{data['out']}]]></result>\n"
    xml += "\t</#{tag}>\n"
    
    @fds[type].write(xml)
  end
  
end # class Logger

class TemplateFuzzer
   
  def initialize(logger, config)
    @types = {
      1 => 'function', 
      2 => 'method', 
      3 => 'class'
    }
    @templates = {}
    
    # load templates
    host.glob().each do |tmp_name|
      id = tmp_name[0].to_i
      type = @types[id]
      
      @templates[type]||= []
      @templates[type].push(Template.new(tmp_name))
    end
    #
        
    @logger = logger
    @config = config
    
    # blacklisted functions, methods and classes
    @blacklist = {
       'function' => ['sleep', 'usleep', 'time_sleep_until', 'similar_text', 'leak', 'leak_variable', 'error_log'],
       'method' => [],
       'class' => []
    }
      
    # pre-defined set of arguments
    @args = genArgs()
  end # def initialize
  
  def execute(tag, src) 
    
    cmd = @config['php'] + ' ' + @config['args']
      
    ret = @host.exec!(cmd)

    @logger.log(Logger::STDOUT, ret[0])
    @logger.log(Logger::STDERR, ret[1])
    
    return ret[2]
  end # def execute
  
  def genArgs
    types = [
      '',
      '1',
      '-1',
      'NULL',
      'fopen("php://temp")',
      '"abc://foobar"',
      '"phar:///usr/local/bin/phar.phar/*%08x-%08x-%08x-%08x-%08x-%08x-%08x-%08x-%08x"',
      '"php://filter/resource=http://www.example.com"',
      '"php://temp"',
      '"strtoupper"',
      'array("reflectionclass", "export")',
      'function () { }',
      '"%08x-%08x-%08x-%08x-%08x-%08x-%08x-%08x-%08x%s"',
      'chr(0)',
      'getcwd().chr(0)."foo"',
      'PHP_INT_MAX',
      'PHP_INT_MAX-1',
      'array(new stdClass)',
      'str_repeat("A", 10000)',
      '&$x'
    ]
  
    args = []
    types.each do |arg|
      args.push(arg)
      if arg.length > 0
        # add permutations using multiple occurances of same argument 
        args.push(Array.fill(0, 2, arg).join(','))
        args.push(Array.fill(0, 3, arg).join(','))
        args.push(Array.fill(0, 4, arg).join(','))
        args.push(Array.fill(0, 5, arg).join(','))
      end
    end # types.each
    
    return args
  end # def getArgs
  
  def runTest(metadata, template)
    test_args = template.hasArgs
    
    args.map do |key, arg|
      template.clear
     
      template.replace('funcname', metadata['function'])
      template.replace('classname', metadata['class'])
      template.replace('methodname', metadata['method'])
        
      template.replace('args2', arg.empty? ? arg : arg + ',' )
      
      template.replace('args', arg)
      
      if test_args
        printf("- %s - Args: %s\n", $metadata['name'], $arg);
      else
        printf("- %s:\n", $metadata['name']);
      end
      
      exit_code = execute(metadata['name'], template.getSource)
      
      case exit_code
      when 139: # signall 11
        puts "SIGSEGV #{exit_code}"
      else
        puts "Exit status = #{exit_code}"
      end
      
      unless test_args
        break
      end
    end # args.map 
    
  end # def runTest
  
  def runFuzzer(metadata)
    type = $metadata['type'];

    if @blacklist[type].include?($metadata['name'])     
      put "#{metadata['name']} is in the blacklist of #{type}!"
      return;
     end

    put "Testing #{type} #{metadata['name']}"
      
    @templates[type].each do |template|
      put "Using template #{template.getPath()}:"
        
      runTest(metadata, template)
    end
  end # def runFuzzer
  
end # class TemplateFuzzer
	
end # class Fuzz
end # module Runner
end # module Test
