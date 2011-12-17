
require 'java'
include_class 'javax.swing.JLabel'
include_class 'javax.swing.JPanel'
include_class 'javax.swing.JTextPane'
include_class 'org.jruby.Ruby'
include_class 'org.jruby.RubyInstanceConfig'
include_class 'org.jruby.internal.runtime.ValueAccessor'
include_class 'org.jruby.demo.TextAreaReadline'
include_class 'java.io.PrintStream'
include_class 'java.util.ArrayList'
include_class 'java.awt.BorderLayout'
include_class 'java.lang.System'

module Tracing
  module Dashboard
  
class Console < BaseDashboard
  
  def initialize(host)
    super(host)
    
    add('p left hfill vfill', ConsolePanel.new())
    # LATER open button, save button
  end
  
  def label
    'Console'
  end
  
  class Panel < JPanel
    
    def initialize(script=nil, heading=" Welcome to the JRuby IRB Console \n\n")
      super(BorderLayout.new)
        
      text = JTextPane.new
      add(JScrollPane.new(text))
        
      tar = TextAreaReadline.new(text, heading)
      # call tar.shutdown() when window closed
      
      config = RubyInstanceConfig.new()
      config.setInput(tar.getInputStream())
      config.setOutput(PrintStream.new(tar.getOutputStream()))
      config.setError(PrintStream.new(tar.getOutputStream()))
      config.setArgv([])       
          
      runtime = Ruby.newInstance(config)
      runtime.getGlobalVariables().defineReadonly("$$", ValueAccessor.new(runtime.newFixnum(System.identityHashCode(runtime))))
      runtime.getLoadService().init(ArrayList.new())
      tar.hookIntoRuntime(runtime)
              
      Thread.start do
        if script
          runtime.evalScriptlet(script)
        end
        runtime.evalScriptlet("ARGV << '--readline' << '--prompt' << 'inf-ruby';" + "require 'irb'; require 'irb/completion';" + "IRB.start")
      end    
    end
        
  end # class Panel

end # class Console
    
  end # module Dashboard
end # module Tracing
