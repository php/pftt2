
module Util
  class SwingOptions
    def show
    end
    def on(label, type, help, &block)
      
    end
    
    protected
    
    def add_opt(label, help, comp)
      comp.setToolTipText(help)
      add('p left', JLabel.new(label))
      add('left hfill', comp)
    end
    def checkbox(label, help)
      add_opt(help, JCheckBox.new)
    end
    def combobox(label, help)
      add_opt(help, JComboBox.new)
    end
    def textfield(label, help)
      add_opt(help, JTextField.new)
    end
    def filefield
    end
  end
end
