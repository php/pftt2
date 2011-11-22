
module Tracing
  module Prompt
    
class Base
      
  def initialize(test_ctx)
    @test_ctx = test_ctx
  end
      
  def help
    puts ' g  - go'
    puts ' G  - go (interactive mode off)'
    puts ' h  - help'
    puts ' n  - next test case set'
    puts ' X  - exit'
    puts ' z  - re-run test case set'
    puts ' Z  - re-run test case set (not interactive)'
    puts ' N  - skip to next host'
    puts ' f  - show FCR report (text)'
    puts ' F  - show FCR report (HTML)'
  end
      
  def execute(ans)
    if ans == 'g' or ans == 'G'
      # TODO
    elsif ans == 'h'
      help
    elsif ans=='N'
      # N next test case set
      @test_ctx.next_test_case()
    elsif ans=='X'
      if @test_ctx.prompt_yesno('Are you sure you want to exit?')
        exit
      end
      return false # re-prompt
    elsif ans=='z'
      # z - re-run test case set
      @test_ctx.rerun_combo(@host, @middleware, @php, @scn_set)
    elsif ans=='Z'
      # Z - re-run test case set (not interactive)
      $interactive_mode = false
      @test_ctx.rerun_combo(@host, @middlware, @php, @scn_set)
    end
    return false # re-prompt
  end # def execute
  
end # class Base
    
class Pftt < Base
  
  def help
    super
    puts ' b  - show dashboard (Swing UI)'
    puts ' w  - change wait time for prompt'
  end
  
  def execute(ans)
    if ans == 'b'
      dashboard
          
      return false # re-prompt
    elsif ans == 'b'
      change_wait_time
          
      return false # re-prompt
    else
      return super(ans)
    end
  end # def execute
  
  def dashboard
    # b - tk (if break (diff prompt or trace prompt) show its position on the dashboard)
    # TODO
  end
  
  def change_wait_time
    while true do
      w = @test_ctx.prompt("Wait Time(>=0):")
      if w.is_a?(Integer) and w >= 0
        # TODO
        break
      end
    end
  end # def change_wait_time
  
end # class Pftt
    
class GenericException < Pftt
  
  def help
    super
    puts ' e  - show exception'
    puts ' o  - do it over'
  end
  
  def execute(ans)
    if ans == 'e'
      show_exception
          
      return false # re-prompt
    elsif ans == 'o'
      do_over
          
      return false # re-prompt
    end
    return super(ans)
  end # def execute
  
  def show_exception
    # e
    # TODO
  end
  
  def do_over
    # o or g
    # TODO
  end
  
end # class GenericException
    
    module Network
      
class Connect < GenericException
  
  def help
    super
    puts ' u - set username'
    puts ' v - view credentials'
    puts ' p - set password'
    puts ' a - set address'
  end
        
  def execute(ans)
    # TODO
    return super(ans)
  end # def execute
  
end # class Connect

    end # module Network
      
class CmdExecute < GenericException
  
  def help
    super
    puts ' c  - change the command to execute'
  end
  
  def execute(ans)
    if ans == 'c'
      change_command

      return false # re-prompt
    end
    return super(ans)
  end # def execute

  def change_command
    # c
    # TODO
  end
  
end # class CmdExecute
    
class FSOp < GenericException
      
end # class CmdExecute
    
class FS0Op < FSOp
end
    
class FS1Op < FSOp
  
  def help
    super
    puts ' j  - change file'
  end
  
  def execute(ans)
    if ans == 'j'
      change_file
    end
    return super(ans)
  end
  
  def change_file
    while true do
      c = @test_ctx.prompt("Change File to(current: #{current}:")
      if c.length > 0
        # TODO
                  
        break
      end
    end
  end
  
end # class FS1Op
    
class FS2Op < FSOp
  
  def help
    super
    puts ' j  - change src file'
    puts ' k  - change dst file'
  end
  
  def execute(ans)
    if ans == 'j'
      change_src
    elsif ans == 'k'
      change_dst
    end
    return super(ans)
  end # def execute
  
  def change_src
    while true do
      c = @test_ctx.prompt("Change Src to(current: #{current}:")
      if c.length > 0
        # TODO
            
            
        break
      end
    end
  end
  
  def change_dst
    while true do
      c = @test_ctx.prompt("Change Dst to(current: #{current}:")
      if c.length > 0
        # TODO
                    
                    
        break
      end
    end
  end
  
end # class FS2Op 
      
class ThreadControl < Pftt
  
  def help
    super
    puts ' u  - unlock host'
    puts ' t  - list threads'
    puts ' T  - add new worker thread'
  end
  
  def execute(ans)
    if ans == 'u'
      # TODO
      return false # re-prompt
    elsif ans == 't'
      # TODO
      return false # re-prompt
    elsif ans == 'T'
      # TODO
      return false # re-prompt
    end
    return super(ans)
  end # def execute
  
  def unlock_host
    # u
  end
  
  def list_threads
    # t
  end
  
  def new_thread
    # T
  end
  
end # class ThreadControl
    
    
  end
end
