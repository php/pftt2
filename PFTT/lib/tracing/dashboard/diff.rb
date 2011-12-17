
require 'java'
include_class 'javax.swing.JButton'
include_class 'javax.swing.JComboBox'
include_class 'javax.swing.JFrame'
include_class 'javax.swing.JList'
include_class 'javax.swing.JMenu'
include_class 'javax.swing.JMenuBar'
include_class 'javax.swing.JMenuItem'
include_class 'javax.swing.JPanel'
include_class 'javax.swing.JScrollPane'
include_class 'javax.swing.JTextArea'
include_class 'javax.swing.JTextField'
include_class 'javax.swing.JTree'

module Tracing
  module Dashboard
  
class Diff < BaseDashboard
  
  def initialize(host)
    super(host)
    
    add('p left', JLabel.new('Base'))
    add('p left', JLabel.new('Test'))
    
    add('p left', JLabel.new('Run'))
    add('tab', @run_combo = JComboBox.new)
    add('tab', JLabel.new('Host'))
    add('tab', @host_combo = JComboBox.new)
    add('tab', JLabel.new('Zoom'))
    add('tab', @zoom_combo = JComboBox.new)
    add('tab', JLabel.new('Diff'))
    add('tab', @diff_combo = JComboBox.new)
    add('tab', JButton.new('Reset'))# TODO
    add('tab', JButton.new('Update'))# TODO
    
    @run_combo.addItem('Base')
    @run_combo.addItem('Test')
    @run_combo.addItem('Base+Test')
    @run_combo.addItem('Base=Test')
    @run_combo.addItem('Base-Test')
    @host_combo.addItem('All')
    @zoom_combo.addItem('A - All Tests (All Combos)')
    @zoom_combo.addItem('E - One Extension (All Combos)')
    @zoom_combo.addItem('a - all tests (one combo)')
    @zoom_combo.addItem('e - one extension (one combo)')
    @zoom_combo.addItem('T - one test case (All Combos)')
    @zoom_combo.addItem('t - one test case')
    @zoom_combo.addItem('l - one line (L-by-L)')
    @zoom_combo.addItem('C - one chunk (C-by-C)')
    @diff_combo.addItem('E - original Expected Output')
    @diff_combo.addItem('A - original Actual Output')
    @diff_combo.addItem('+ - added Output')
    @diff_combo.addItem('- - removed Output')
    @diff_combo.addItem('d - + and -')
    @diff_combo.addItem('e - Expected output')
    @diff_combo.addItem('a - Actual Output')
    
    #
    add('p left vfill', JButton.new('<'))
    add('left vfill', JButton.new('Prev'))
      
    add('left', JLabel.new('Type:'))
    add('left', @type_combo = JComboBox.new)
    @type_combo.addItem('Insert')
    @type_combo.addItem('Delete')
    @type_combo.addItem('Equals')
    add('left', JTextField.new('<Test>                                        '))
    add('left', JTextField.new('0'))
    add('left', JTextField.new('0'))
    add('left', JTextField.new('0'))
    add('left', JTextField.new('0'))
      
    @action_menu = JMenu.new('Action')
    add('left', @action_jmb = JMenuBar.new)
    @action_jmb.add(@action_menu)
    @action_menu.add(JMenuItem.new('Save Diff'))# TODO
    @action_menu.add(JMenuItem.new('Load Diff'))# TODO
    @action_menu.addSeparator
    @action_menu.add(JMenuItem.new('Save Override'))# TODO
    @action_menu.add(JMenuItem.new('Load Override'))# TODO
    @action_menu.addSeparator
    @action_menu.add(JMenuItem.new('Save New Test Case'))# TODO
    @action_menu.add(JMenuItem.new('Patch Existing Test Case'))# TODO
    @action_menu.add(JMenuItem.new('Load Test Case'))# TODO
    
    add('right vfill', JButton.new('Next'))
    add('right vfill', JButton.new('>'))
    #
    
    # should be a hex editor instead
    add('p left hfill vfill', JTextArea.new())
    
    # checkbox for each node to select if it should be included in Diff, Override or Test Case
    add('p left vfill', JScrollPane.new(JTree.new))
    
    add('center vfill', JScrollPane.new(JList.new))
    
    add('right vfill', JScrollPane.new(JList.new))
    
    add('p left hfill vfill', Tracing::Dashboard::Console::Panel.new)
  end
  
  def label
    'Diff'
  end
  
end # class Diff
  
  end # module Dashboard
end # module Tracing
