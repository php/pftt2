
require 'tracing/command.rb'

module Tracing
  module Context
    class Base
#      def initialize(test_ctx)
#        @test_ctx = test_ctx
#        @combo_ctx = combo_ctx
#      end
      
      def find_debugger(host)
        nil
      end
      
      def self.system_info?(context)
        context.is_instance?(Tracing::Context::SystemSetup)
      end
      
      def connection_start host
        handle(:connect, :start, host, nil, nil, Tracing::Prompt::Network::Connect, nil)
      end
      
      def connection_established host
        handle(:connect, :established, host, nil, nil, Tracing::Prompt::Network::Connect, nil)
      end
      
      def connection_failure host, &do_over
        handle(:connect, :failure, host, nil, nil, Tracing::Prompt::Network::Connect, do_over)
      end
          
      def cmd_exe_start host, cmd_line, opts, &do_over
        handle(:cmd_exe, :start, host, cmd_line, nil, Tracing::Prompt::CmdExecute, do_over, 5, opts)
      end
          
      def cmd_exe_failure host, cmd_line, opts, exit_code, output, &do_over
        handle(:cmd_exe, :failure, host, cmd_line, exit_code, Tracing::Prompt::CmdExecute, do_over, 10, opts)
      end
      
      def cmd_exe_success host, cmd_line, opts, exit_code, output, &do_over
        handle(:cmd_exe, :success, host, cmd_line, exit_code, Tracing::Prompt::CmdExecute, do_over, 5, opts)
      end
          
      def fs_op0 host, op, &do_over
        handle(:cmd_exe, :success, host, nil, nil, Tracing::Prompt::FS0Op, do_over, 5)
      end
      
      def fs_op1 host, op, path, &do_over
        handle(:cmd_exe, :success, host, nil, nil, Tracing::Prompt::FS1Op, do_over, 5)
      end
      
      def fs_op2 host, op, src, dst, &do_over
        handle(:cmd_exe, :success, host, nil, nil, Tracing::Prompt::FS2Op, do_over, 5)
      end
      
      def self.show_exception(ex)
        puts ex.inspect+" "+ex.backtrace.inspect
      end
          
      def pftt_exception(obj, ex)
        Tracing::Context::Base.show_exception(ex)
        handle(:cmd_exe, :success, obj.is_a?(Host) ? obj : nil, nil, nil, Tracing::Prompt::GenericException, nil, 5, obj)
      end
          
      def write_file host, content, file, &do_over
        handle(:write_file, :success, host, file, nil, Tracing::Prompt::FS1Op, do_over, 5, content)
      end
          
      def read_file host, content, file, &do_over
        handle(:read_file, :success, host, file, nil, Tracing::Prompt::FS1Op, do_over, 5, content)
      end
          
      def output_mismatch host, expected, actual
        # TODO
        handle(:output, :mismatch, host, nil, nil, Tracing::Prompt::Diff, do_over, 0, expected, actual)
      end
      
      def section_name
        self.class.to_s
      end
      
      protected
      
      def do_break?(action_type, initial_result, initial_file, initial_code, msg=nil)
        # OVERRIDE: to control what to break on
        # TODO
        initial_result == :failure or ( msg.is_a?(Tracing::Command::Expected) and initial_code.is_a?(Tracing::Command::Actual) and msg.validate(initial_code) )
      end
      
      def exe_prompt(prompt_type, prompt_timeout)
        prompt = prompt_type.new
        
        while true do
          # show the prompt line to the user
          puts prompt.prompt_str
          
          # TODO use test_ctx to lock console
        
          # get user input (wait only for timeout if given)
          if timeout.is_a?(Integer) and timeout > 0
            STDIN.select(timeout)
          end
          ans = STDIN.gets()
        
          # execute answer
          if prompt.execute(ans)
            # if true, break out of loop, if false, re-prompt
            timeout = nil # user will be providing input now, so block
            break
          end
          
        end # while
        
        # send back result that handle will be interested in
        return :none
      end # def exe_prompt
      
      def handle(action_type, initial_result, host, initial_file, initial_code, prompt_type, do_over, prompt_timeout=10, msg=nil)
        puts initial_file
        #return
        # TODO @combo_ctx.record(self, action_type, initial_result, initial_file, initial_code, msg)
        
        # TODO
        # TODO do_over.call
               
        result = initial_result
        file = initial_file
        code = initial_code
              
        pl = nil
        begin
          # loop for each attempt/prompt
          while true do
            # decide if should break on this result
            unless false # TODO do_break?(action_type, initial_result, initial_file, initial_code, msg)
              break
            end
          
            # want to lock the prompt so while between multiple exe_prompt() calls, a prompt doesn't appear for a different
            # handle() call
            unless pl
              pl = prompt_lock(prompt_timeout)
            end
            #
           
            # prompt user for answer
            answer = exe_prompt(prompt_type, prompt_timeout)
          
            @combo_ctx.record(self, :break_prompt, answer, file, 0)
                
            if answer==:none or answer==:skip or answer==:ignore
              #                  
              break
            else
              new_ctx = PromptedContext.new # TODO
                  
              if answer == :do_over
                do_over.call(initial_file, new_ctx)
                      
                @combo_ctx.record(self, :break_prompt, answer, file, 0)
              elsif answer == :change
                do_over.call(changed_file, new_ctx)
                
                @combo_ctx.record(self, :break_prompt, answer, file, 0)
              end
                  
              if new_ctx.success?
                # now it worked
                      
                @combo_ctx.record(self, :break_prompt, answer, file, 0)
                break
              else
                # prompt the user again (for the new failure)
                result = new_ctx.result
                file = new_ctx.file
                code = new_ctx.code
                      
                @combo_ctx.record(self, :break_prompt, answer, file, 0)
              end
            
            end
          end # while
          
        rescue Exception => ex
          puts ex.inspect+" "+ex.backtrace.inspect
          
        ensure
          # ensure prompt gets unlocked
          if pl
            prompt_unlock(pl)
          end
        end
      end # def handle
        
    end # class Base
    
    class PromptedContext < Base
      def success?
        # TODO
      end
                
      def result
      end
                
      def file
      end
                
      def code
      end
    end # class PromptedContext
    
    class SystemSetup < Base
      
      class TempDirectory <  SystemSetup
      
      end
      
      class Reboot < SystemSetup
        def approve_reboot_localhost
          true
        end
      end
      
    end
  
    module Dependency 
      class Base < SystemSetup
      end
      
      class Detect < Base
        class OS < Detect
          class Version < OS
            def check_os_generation_detect(check_generation, actual_generation)
              actual_generation
            end
          end
          
          class Type < OS
            def check_os_type_detect(check_os, actual_os)
              actual_os
            end
          end
        end
      end
      
      class Check < Detect
      end
      class Install < Base
      end
    end
  
    module Middleware
      class Base < SystemSetup
      end
      class Start < Base
      end
      class Stop < Base
      end
      class Config < Base
      end
    end
    
    module Scenario
      class Base < SystemSetup
      end
      class Deploy < Base
      end
      class Teardown < Base
      end
    end
  
    module PhpBuild
      class Base < SystemSetup
      end
      class Compress < Base
      end
      class Upload < Base
      end
      class Decompress < Base
      end
    end
  
    module Phpt
      class Base < SystemSetup
      end
      class Compress < Base
      end
      class Upload < Base
      end
      class Decompress < Base
      end
      class RunHost < Base
      end
    end

    module Test
      class TestBase < Base
      end
      class Run < TestBase
      end
      class Result < TestBase
      end
    end
    
  end
end
