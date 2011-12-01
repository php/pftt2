
module Tracing
  module Prompt
    
class Base
      
  def initialize(test_ctx, action_type, result, host, file, code, do_over, msg)
    @test_ctx = test_ctx
    @action_type = action_type
    @result = result
    @host = host
    @file = file
    @code = code
    @do_over = do_over
    @msg = msg
  end
  
  def prompt_str
    self.class.to_s.gsub('Tracing::Prompt', '')+'> '
  end
      
  def help
    puts ' h  - help'
    puts ' n  - next test case set'
    puts ' Q  - quit | exit'
    puts ' z  - re-run test case set'
    puts ' Z  - re-run test case set (not interactive)'
    puts ' N  - skip to next host'
    puts ' f  - show FCR report (text)'
    puts ' F  - show FCR report (HTML)'
  end
      
  def execute(ans)
    if ans == 'h'
      help
    elsif ans=='N'
      # N next test case set
      @test_ctx.next_test_case()
    elsif ans=='Q'
      if @test_ctx.prompt_yesno('Are you sure you want to quit?')
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
    else
      help
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

class TryAgainPftt < Pftt
  
  def help
    super
    puts ' A - run it again'
  end
  
  def execute(ans)
    if ans == 'A'
      # TODO
      return true
    end
    return super(ans)
  end # def execute
  
end # class TryAgainPftt
    
class GenericException < TryAgainPftt
  
  def help
    super
    if @msg.is_a?(Exception)
      puts ' e  - show exception'
    end
  end
  
  def execute(ans)
    if ans == 'e'
      show_exception
          
      return false # re-prompt
    end
    return super(ans)
  end # def execute
  
  def show_exception
    # e
    unless @msg.is_a?(Exception)
      return
    end

    puts @msg.message
    @msg.backtrace.each do |frame|
      puts frame.inspect 
    end
  end
    
end # class GenericException
    
    module Network
      
class Connect < GenericException
  
  def help
    super
    puts ' u - username'
    puts ' U - change username'
    puts ' p - password'
    puts ' P - change password'
    puts ' a - address'
    puts ' A - change address'
    puts ' n - port number'
    puts ' N - change port number'
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
    puts ' c  - view command'
    puts ' C  - change command'
  end
  
  def execute(ans)
    if ans == 'c'
      # TODO
    elsif ans == 'C'
      change_command

      return false # re-prompt
    end
    return super(ans)
  end # def execute

  def change_command
    # C
    puts 'Current Command: '+@file
    new_cmd = prompt('New Command: ')
    
    @do_over.call(new_cmd)
  end
  
  def prompt(label)
    # TODO use @test_ctx.prompt
    STDOUT.write(label)
    return STDIN.gets.chomp
  end
  
end # class CmdExecute
    
class FSOp < GenericException
      
end # class CmdExecute
    
class FS0Op < FSOp
end
    
class FS1Op < FSOp
  
  def help
    super
    puts ' J  - change file'
  end
  
  def execute(ans)
    if ans == 'J'
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
    puts ' J  - change src file'
    puts ' K  - change dst file'
  end
  
  def execute(ans)
    if ans == 'J'
      change_src
    elsif ans == 'K'
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

class TestCaseRunPrompt < TryAgainPftt
  # each phpt is a test case
  # for performance and stress testing, each app+middleware+php is a test case
  def help
    super
    puts ' I - ignore test case, count as Failure' # TODO 
    puts ' S - skip test case - ignore failure, count as Pass'
    puts ' X - xdebug this test case'
    puts ' D - use system debugger (windbg or gdb) on this test case'
  end # def help
        
  def execute(ans)
    if ans == 'I'
      # TODO
      return true
    elsif ans == 'S'
      # TODO
      return true
    elsif ans == 'X'
      # TODO
      return true
    elsif ans == 'D'
      # TODO
      return true
    end
    return super(ans)
  end # def execute
  
end # class TestCaseRunPrompt
    
    
  end
end
