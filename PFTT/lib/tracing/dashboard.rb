
require 'java'
include_class 'javax.swing.JButton'
include_class 'javax.swing.JFrame'
include_class 'javax.swing.JPanel'
include_class 'javax.swing.JTabbedPane'
include_class 'java.awt.BorderLayout'
include_class 'java.awt.event.ActionListener'
require 'RiverLayout.jar'
include_class 'se.datadosen.component.RiverLayout'

module Tracing
  module Dashboard

class BaseDashboard < JPanel
  
  def initialize(host)
    super(RiverLayout.new)
    @host = host
  end
  
  def label
    # Override
  end
  
  def to_s
    label
  end
  
  def label_component
    @lc ||= LabelComponent.new(self)
  end
  
  class LabelComponent < JPanel
    
    def initialize(tab)
      super(BorderLayout.new)
      setOpaque(true)
      
      add(JLabel.new(tab.label), BorderLayout::CENTER)
      add(close_button = JButton.new('X'), BorderLayout::LINE_END)
      close_button.addActionListener(CloseClick.new(tab))
    end
    
    class CloseClick 
      include ActionListener
      
      def initialize(tab)
        @tab = tab
      end
      
      def actionPerformed(e)
        @tab.getParent().removeTab(@tab)
      end
    end
    
  end # class LabelComponent
  
end # class BaseDashboard
  
class Display
  
  def initialize()
    @lock = Mutex.new
    @win = JFrame.new('PFTT')
    @tabs = JTabbedPane.new
    @win.setContentPane(@tabs)
  end

  def add(panel)
    @lock.synchronize do
      @tabs.addTab(nil, panel)
      @tabs.setTabComponentAt(0, panel.label_component)
      # focus on tab when added (don't have window grab focus, but move to front)
      panel.requestFocusInWindow
      @win.toFront
    end
  end
  
  def remove(panel)
    # remove tabs when no longer needed (ex: when progress == 100%)
    @tabs.removeTab(panel)
  end
 
  def show
    c = @tabs.getComponentCount
    i = 0
    while i <  c do
      comp = @tabs.getComponent(i)
      if comp.is_a?(BaseDashboard)
        comp.show
      end
      i += 1
    end
    
    @win.pack
    @win.setVisible(true)
  end

end # class Display

  end # module Dashboard
end # module Tracing
