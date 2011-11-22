
require 'java'
include_class 'javax.swing.JButton'
include_class 'javax.swing.JFileChooser'
include_class 'javax.swing.JFrame'
include_class 'javax.swing.JLabel'
include_class 'javax.swing.JPanel'
include_class 'javax.swing.JTextArea'
include_class 'javax.swing.JTree'
require 'RiverLayout.jar'
include_class 'se.datadosen.component.RiverLayout'

module Tracing
module Dashboard

  class BaseTestRunner
    
    class BaseHostRow
      def do_menu
        menu = JMenu.new('host.name')
        menu.add(JLabel.new('Start Time: '))
        menu.add(JLabel.new('Run Time: '))
        menu.add(JLabel.new('Status: ')) #<Running|Finished|Dead>
        menu.add(JLabel.new('Message Count: '))#<0+>
        menu.addSeparator
        menu.add(JLabel.new('PHP: '))#<build info>
        menu.add(JLabel.new('Middleware: '))#<mw name>
        menu.addSeparator
        menu.add(JMenuItem.new('Stop'))
        menu.add(JMenuItem.new('Resume'))
        menu.addSeparator
      end
    
      def initialize(combo)
        do_menu
                
        JLabel.new('host.os_name')
    
        pass_rate = JProgressBar.new(JProgressBar::HORIZONTAL, 0, 100)
        completion_progress_bar = JProgressBar.new(JProgressBar::HORIZONTAL, 0, 100)
        seconds_per_test_label = JLabel.new
        cpu_load_progress_bar = JProgressBar.new(JProgressBar::HORIZONTAL, 0, 100)
        messages_total = JLabel.new
        messages_per_minute = JLabel.new
      end
     
    end # class BaseHostRow
     
    def initialize
      JButton.new('Close')
      
      JButton.new('Exit')
      
      disk_usage_pb = JProgressBar.new
      
      disk_usage_warn_label = JLabel.new("Warning: Disk space is low!\nFree disk space to continue.")
      
      Thread.start do
        while true do
          delete_all_rows
          
          test_ctx.combos.each do |combo|
            create_row(combo)
          end # each
          
          sleep(60)
        end # while
      end # Thread.start
    end
    
  end # class TestRunner
  
  class PhptTestRunner < BaseTestRunner 
    
    class PhptHostMenu < BaseHostMenu
      
      def do_menu
        super
        
        JMenuItem.new('FCR Report (HTML)')
      end
      
    end # class PhptHostMenu
    
    def create_row(combo)
      PhptHostMenu.new(combo)
    end
    
  end # class PhptTestRunner
  
  class WcatTestRunner < BaseTestRunner
  
    class WCatHostMenu < BaseHostMenu
      
      def do_menu
        super
        JLabel.new('App Name')
        JMenuItem.new('PCR Report (HTML)')
      end
      
    end # class WCatHostMenu
    
    def create_row(combo)
      WcatHostMenu.new(combo)
    end
    
  end # class WcatTestRunner
  
end # module Dashboard
end # module Tracing
