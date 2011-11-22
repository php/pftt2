
require 'java'
include_class 'javax.swing.JButton'
include_class 'javax.swing.JFrame'
include_class 'javax.swing.JPanel'
include_class 'javax.swing.JScrollPane'
include_class 'javax.swing.JTextArea'
require 'RiverLayout.jar'
include_class 'se.datadosen.component.RiverLayout'

module Tracing
module Dashboard
  class Diff
    def initialize
frame = JPanel.new(RiverLayout.new)

prev_button = JButton.new('Prev')
frame.add('p left', prev_button)

next_button = JButton.new('Next')
frame.add('right', next_button)

close_button = JButton.new('Close')
frame.add('right', close_button)

# expected
expected_text_area = JTextArea.new
frame.add('p left hfill vfill', JScrollPane.new(expected_text_area))

# actual
actual_text_area = JTextArea.new
frame.add('p left hfill vfill', JScrollPane.new(actual_text_area))

# frame
jf = JFrame.new('Diff')
jf.setContentPane(frame)
jf.pack
jf.setVisible(true)

sleep(100)
    end
  end
end
end
