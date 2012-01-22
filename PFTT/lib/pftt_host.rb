
#
# PFTT internal executable

# Host
# Test::Case
# Test::Telemetry
# Test::Telemetry::Folder
# Test::Runner
# CaseRunner
# Middleware
# Scenario
# Scenario::Part
# ----
# Php::Build
# Php::Ini
# Php::TestPack

#TODO begin
import 'java.lang.System'
import 'java.io.File'
require 'run_config.rb'
require 'host/local.rb'
require 'test/host_runner.rb'
require 'scenario.rb'
#
require 'php/run_context.rb'
require 'php/middleware/cli.rb'
require 'php/test_pack.rb'
require 'php/build.rb'
require 'php/scenario/working_file_system/local.rb'
require 'php/runner/phpt.rb'
require 'php/run_options.rb'

class PHPHostRunner < Test::HostRunner
  
  def create_runner
    # TODO get OverrideManager from test pack
    @om = nil # TODO Diff::OverrideManager
    
    Php::Runner::Phpt.new(@host_int, @run_ctx, @om, @thread_pool_size, @run_options)
  end
  
  def create_run_context
    Php::RunContext.new(runner, '/tmp/1') 
    # TODO
  end
  
  def load_all_test_cases
    @test_pack.load_all_test_cases
  end
  
  def load_test_case_by_name case_name
    @test_pack.load_case_by_name(case_name)
  end
  
  def read_start_msgs
    # TODO
    @scenarios = [Scenario.new(1, Php::Scenario::WorkingFileSystem::Local.new)]
    @middlewares = [Php::Middleware::Cli]
    @test_pack = Php::TestPack.new("c:\\PFTT\\php-test-pack", 11904)#/home/matt/php/php5.4-201201041830', 11904)
    @build = Php::Build.new("c:\\PFTT\\php-5.4-nts\\")#/home/matt/php/php5.4-201201041830/')
        
    @run_options = Php::RunOptions.new
  end # def read_start_msgs
  
  def run_combo(combo)
    @runner.run(@test_cases, combo.middleware, combo.scenario)

    # TODO @host_int.host_check('Results-Sum-Equal-Host-Result-Count', !results.nil? and results.sum == results.length )            
  end # def run_combo  
  
end # class PHPHostRunner

hr = PHPHostRunner.new
hr.run

#ensure
# important: ensure process exits or client won't see PSC stream end/close
System.exit(0)
#end
