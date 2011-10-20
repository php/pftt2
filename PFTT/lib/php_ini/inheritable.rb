$: << 'lib'
require 'monkeypatch/string/unindent.rb'

class PhpIni
  module Inheritable

    # this is *very* overloaded.
    # objekt.ini() -> read out full inheritance
    # objekt.ini(false) -> read out this one.
    # objekt.ini(anything_else) -> pass to objekt.ini= anything_else since it needs an explicit receiver
    def ini(arg=nil)
      case
      when arg.nil?
        if self.is_a? Class
          compiled = PhpIni.new
          ancestors.to_a.reverse_each do |ancestor|
            next true unless ancestor.respond_to? :ini
            compiled.configure ancestor.ini(false)
          end
          compiled
        else
          self.class.ini.clone + ini(false)
        end
      when arg == false
        @ini ||= PhpIni.new
      else
        self.ini= arg  
      end
    end

    def ini=arg
      arg.unindent! if arg.is_a? String
      @ini = PhpIni.new(arg)
    end

    def self.included(base)
      base.class_exec(self) do |mod|
        extend mod
      end
    end
  end
end
