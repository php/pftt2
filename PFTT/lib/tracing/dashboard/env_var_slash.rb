
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
include_class 'java.awt.datatransfer.StringSelection'
include_class 'java.awt.Toolkit'

module Tracing
module Dashboard
  class EnvVarSlash < BaseDashboard
    
    def self.selected?(host, cmd_line)
      if host.windows?
        cmd_line.ends_with?('\\') or cmd_line.ends_with?('%') or cmd_line.ends_with?('-') or cmd_line.ends_with?('=')
      else
        cmd_line.ends_with?('/') or cmd_line.ends_with?('-') or cmd_line.ends_with?('=')
      end
    end
    
    def self.get_base(host, cmd_line)      
      # base is the last ' ' until cmd line length
      i = cmd_line.rindex('=')
      unless i
        i = cmd_line.rindex(' ')
      end
      if i
        c = cmd_line[i+1..cmd_line.length]
      end      
      #
      if c
        if host.posix?
          if c.starts_with?('$') or c.ends_with?('-')
            # get environment variable
            return host.env_value(c)
          elsif c.ends_with?('/')
            return c
          end
        else # windows
          if c.ends_with?('\\') or c.ends_with?('-')
            return c
          end
        end
      end
      #
      return host.systemdrive # fallback
    end # def self.get_base
    
    def initialize(host, cmd_line, base=nil)
      super(host)
      
      puts cmd_line
      @base = base || EnvVarSlash.get_base(@host, cmd_line)
      puts @base
      
      @text_area = JTextArea.new("\n\n\n")
            
      add('p left hfill', JLabel.new(@base))
      
      add('p left hfill', JLabel.new('Choose Directory'))
      
      @tree = JTree.new()
      add('p left hfill vfill', JScrollPane.new(@tree))
      
      @clear_button = JButton.new('Clear')
      @clear_button.addActionListener(ClearClick.new(@text_area))
      add('p left', @clear_button)
            
      @filechooser_button = JButton.new('File Chooser')
      @filechooser_button.addActionListener(FileChooserClick.new(self, @text_area, @base))
      add('left hfill', @filechooser_button)
            
      @close_button = JButton.new('Close')
      @close_button.addActionListener(CloseClick.new)
      add('right', @close_button)
      
      add('p left hfill', JScrollPane.new(@text_area))
      
      @clipboard_button = JButton.new('Copy to Clipboard')
      @clipboard_button.addActionListener(ClipboardClick.new(@text_area))
      add('p left hfill', @clipboard_button)
    end
        
    def label
      return (@base? @base : 'Env Var')+@host.separator
    end    
    
    def show
      super
      
      root = Folder.new(@base)
      @tree.getModel().setRoot(root)
      
      ctx = nil
      
      base = @base
      if not @host.directory?(base)
        i = base.rindex(@host.separator)
        if i
          base = base[0..i]
        end
      end
      
      # TODO make list work
      @host.list(base, ctx).each do |file|
        if @host.directory?(file)
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
      
      def initialize(text_area)
        @text_area = text_area
      end
      
      def actionPerformed(event)
        text = @text_area.getText
        text = text.lstrip.rstrip
        
        ss = StringSelection.new(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, nil);
      end
    end
  
    class CloseClick
      include ActionListener
    
      def actionPerformed(event)
        java.lang.System.exit(0)
      end
    end
  
    class FileChooserClick
      include ActionListener
    
      def initialize(display, text_area, base)
        @display = display
        @text_area = text_area
        @fc = JFileChooser.new(base)
        @fc.setFileSelectionMode(JFileChooser::DIRECTORIES_ONLY)
      end
      def actionPerformed(event)
        rv = @fc.showOpenDialog(@display)
        
        if rv == JFileChooser::APPROVE_OPTION
          @text_area.setText(@fc.getSelectedFile().getPath());
        end
      end
    end # class FileChooserClick
  
  end # class EnvVarSlashViewer
  
end # module Dashboard
end # module Tracing
