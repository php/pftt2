
require 'java'
include_class 'java.awt.event.ActionListener'
include_class 'javax.swing.JButton'
include_class 'javax.swing.JFileChooser'
include_class 'javax.swing.JFrame'
include_class 'javax.swing.JLabel'
include_class 'javax.swing.JPanel'
include_class 'javax.swing.JScrollPane'
include_class 'javax.swing.JTextArea'
include_class 'javax.swing.JTree'
include_class 'javax.swing.tree.DefaultMutableTreeNode'
require 'RiverLayout.jar'
include_class 'se.datadosen.component.RiverLayout'

module Tracing
module Dashboard
  class EnvVarSlash
    def selected?(local_host, cmd_line)
      puts cmd_line
      if local_host.windows?
        cmd_line.ends_with?('\\') or cmd_line.ends_with?('%') or cmd_line.ends_with?('-') or cmd_line.ends_with?('=')
      else
        cmd_line.ends_with?('/') or cmd_line.ends_with?('-') or cmd_line.ends_with?('=')
      end
    end
    
    def select(local_host, orig_cmd_line)
#      puts orig_cmd_line.inspect
#      exit
#      
#      unless selected?
#        return false
#      end
#      
#      i = orig_cmd_line.rindex(' ')
#      base = orig_cmd_line[i..orig_cmd_line]
#      
      base = 'c:/'
      panel = EnvVarSlashViewer.new() # TODO base)
      
      jf = JFrame.new('PFTT - Slash Viewer')
      jf.setDefaultCloseOperation(JFrame::EXIT_ON_CLOSE)
      jf.setContentPane(panel)
      jf.pack()
      jf.setVisible(true)
      
      sleep(100)
            
      return true
    end
  end
  class EnvVarSlashViewer < JPanel
    def initialize()
      super(RiverLayout.new)
      @base = 'C:/' # TODO
      
      add_components
      
      root = Folder.new(@base)
      @tree.getModel().setRoot(root)
      
      host = Host::Local.new()
      ctx = nil
      host.list(@base, ctx).each do |file|
        if host.directory?(file)
          root.add(Folder.new(file))
        else
          root.add(File.new(file))
        end
      end
    end
    
    class Folder < DefaultMutableTreeNode
      def initialize(path)
        @path = path
      end
      def toString
        @path
      end
    end
    
    class File < DefaultMutableTreeNode
      def initialize(path)
        @path = path
      end
      def toString
        @path
      end
    end
    
    def add_components
      # define 'subject' first
      @text_area = JTextArea.new("\n\n\n")
      
      # then, define 'verbs' and 'subjects'
      add('p left hfill', JLabel.new(@base))
      
      add('p left hfill', JLabel.new('Choose Directory'))
      
      @tree = JTree.new()
      add('p left hfill vfill', JScrollPane.new(@tree))
      
      @clear_button = JButton.new('Clear')
      @clear_button.addActionListener(ClearClick.new(@text_area))
      add('p left', @clear_button)
            
      @filechooser_button = JButton.new('File Chooser')
      @filechooser_button.addActionListener(FileChooserClick.new)
      add('left hfill', @filechooser_button)
            
      @close_button = JButton.new('Close')
      @close_button.addActionListener(CloseClick.new)
      add('right', @close_button)
      
      add('p left hfill', JScrollPane.new(@text_area))
      
      @clipboard_button = JButton.new('Copy to Clipboard')
      @clipboard_button.addActionListener(ClipboardClick.new)
      add('p left hfill', @clipboard_button)
    end
    
    class ClearClick
      include ActionListener
      
      def initialize(text_area)
        @text_area = text_area
      end
      
      def actionPerformed(event)
        @text_area.setText('')
      end
    end
  
    class ClipboardClick
      include ActionListener
      
      def actionPerformed(event)
        # TODO
      end
    end
  
    class CloseClick
      include ActionListener
    
      def actionPerformed(event)
        # TODO
      end
    end
  
    class FileChooserClick
      include ActionListener
    
      def initialize
        @fc = JFileChooser.new('C:/') # TODO
      end
      def actionPerformed(event)
        @fc.showOpenDialog(nil) # TODO nil
      end
    end # class FileChooserClick
  
  end # class EnvVarSlashViewer
  
end # module Dashboard
end # module Tracing
