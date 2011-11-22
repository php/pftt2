
require 'java'
include_class 'javax.swing.JButton'
include_class 'javax.swing.JFileChooser'
include_class 'javax.swing.JFrame'
include_class 'javax.swing.JLabel'
include_class 'javax.swing.JPanel'
include_class 'javax.swing.JTextArea'
include_class 'javax.swing.JTree'
include_class 'javax.swing.JProgressBar'
require 'RiverLayout.jar'
include_class 'se.datadosen.component.RiverLayout'

# - compress PHPs, PHPTs
# Tracing::Dashboard::Package (1)
# - upload to each
# Tracing::Dashboard::Package (1 for each host) TODO
# - install
# Tracing::Dashboard::Package (1 for each host) TODO
# - run
# Tracing::Dashboard::Phpt (1 for all hosts

module Tracing
module Dashboard
  class Package
    def initialize
      
      frame = JPanel.new(RiverLayout.new)
      
      frame.add(JLabel.new('Stage Start Time'))
        
      frame.add(JLabel.new('Stage Remaining Time'))
      
      # compression progress
      frame.add('p left hfill', JProgressBar.new)

      # copy
      frame.add('p left hfill', JProgressBar.new)
      
      # filter
      frame.add('p left hfill', JProgressBar.new)

      jf = JFrame.new('Progress')
      jf.setContentPane(frame)
      jf.pack
      jf.setVisible(true)
      
      sleep(100)
    end
  end
end
end
