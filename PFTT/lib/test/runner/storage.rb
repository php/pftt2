
#@db.query("SELECT LAST_INSERT_ID();")
module Test
  module Runner
    
class Storage

  def initialize
#      @mysql = Mysql2::Client.new(:host=>'127.0.0.1', :username=>'root', :password=>'password01!', :database=>'pftt')
#            
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS hosts (
#              host_id int(11) NOT NULL,
#              host_name varchar(45) DEFAULT NULL,
#              host_address varchar(45) DEFAULT NULL,
#              os varchar(45) DEFAULT NULL,
#              username varchar(45) DEFAULT NULL,
#              password varchar(45) DEFAULT NULL,
#              PRIMARY KEY (host_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS php_builds (
#              php_build_id int(11) NOT NULL,
#              php_version_major int(10) unsigned DEFAULT NULL,
#              php_version_minor int(10) unsigned DEFAULT NULL,
#              revision int(10) unsigned DEFAULT NULL,
#              threadsafe tinyint(1) DEFAULT NULL,
#              platform enum('Windows','Posix') DEFAULT NULL,
#              compiler varchar(10) DEFAULT NULL,
#              version varchar(45) DEFAULT NULL,
#              php_branch varchar(10) DEFAULT NULL,
#              ctime timestamp NULL DEFAULT NULL,
#              PRIMARY KEY (php_build_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS phpt_exceptions (
#              phpt_exception_id int(11) NOT NULL,
#              phpt_results_id int(11) DEFAULT NULL,
#              backtrace text,
#              PRIMARY KEY (phpt_exception_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS phpt_results (
#              phpt_results_id int(11) NOT NULL AUTO_INCREMENT,
#              telemetry_folder varchar(45) DEFAULT NULL,
#              windows_ini text,
#              posix_ini text,
#              run_time_seconds int(10) unsigned DEFAULT NULL,
#              exceptions int(10) unsigned DEFAULT NULL,
#              pass int(10) unsigned DEFAULT NULL,
#              pass_rate float unsigned DEFAULT NULL,
#              fail int(10) unsigned DEFAULT NULL,
#              total int(10) unsigned DEFAULT NULL,
#              unsupported int(10) unsigned DEFAULT NULL,
#              bork int(10) unsigned DEFAULT NULL,
#              skip_percent float unsigned DEFAULT NULL,
#              skip int(10) unsigned DEFAULT NULL,
#              xfail_pass int(10) unsigned DEFAULT NULL,
#              xfail_works int(10) unsigned DEFAULT NULL,
#              xskip int(10) unsigned DEFAULT NULL,
#              extensions_skipped int(10) unsigned DEFAULT NULL,
#              extensions_run int(10) unsigned DEFAULT NULL,
#              extensions_all int(10) unsigned DEFAULT NULL,
#              test_ctx_id int(11) unsigned DEFAULT NULL,
#              mw_name varchar(10) DEFAULT NULL,
#              php_build_id int(11) DEFAULT NULL,
#              systeminfo text,
#              host_id int(11) DEFAULT NULL,
#              host_name varchar(20) DEFAULT NULL,
#              scenario_set_id int(10) unsigned DEFAULT NULL,
#              PRIMARY KEY (phpt_results_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS phpt_test (
#              phpt_test_id int(11) NOT NULL,
#              ext_name varchar(30) DEFAULT NULL,
#              full_name varchar(60) DEFAULT NULL,
#              phpt_results_id int(10) unsigned DEFAULT NULL,
#              status enum('pass','fail','unsupported','bork','skip','xfail','works','xskip') DEFAULT NULL,
#              PRIMARY KEY (phpt_test_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS scenario_set (
#              scenario_set_id int(11) NOT NULL,
#              PRIMARY KEY (scenario_set_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS scenarios (
#              scenario_id int(11) NOT NULL,
#              scenario_set_id int(11) DEFAULT NULL,
#              scenario_name varchar(45) NOT NULL,
#              PRIMARY KEY (scenario_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS test_ctx (
#              test_ctx_id int(11) NOT NULL,
#              start_time timestamp NULL DEFAULT NULL,
#              end_time timestamp NULL DEFAULT NULL,
#              PRIMARY KEY (test_ctx_id)
#            )")
  end # def initialze
  
  def add_result
    # TODO store tally in xml
    # TODO store in mysql

    #                        @mysql.query("INSERT INTO phpt_results(
    #                          windows_ini, 
    #                            posix_ini, 
    #                            exceptions, 
    #                            pass, 
    #                            pass_rate, 
    #                            fail, 
    #                            total, 
    #                            unsupported, 
    #                            bork, 
    #                            skip_percent, 
    #                            skip, 
    #                            xfail_pass, 
    #                            xfail_works, 
    #                            xskip, 
    #                            extensions_skipped, 
    #                            extensions_run, 
    #                            extensions_all, 
    #                            test_ctx_id, 
    #                            mw_name, 
    #                            php_build_id, 
    #                            systeminfo, 
    #                            host_id, 
    #                            host_name,
    #                            scenario_set_id)
    #                            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
    #                              results.windows_ini,
    #                              results.posix_ini,
    #                              0, # TODO
    #                              results.pass,
    #                              results.rate,
    #                              results.fail,
    #                              results.pass_plus_fail,
    #                              results.unsupported,
    #                              results.bork,
    #                              results.skip_percent,
    #                              results.skip,
    #                              results.xfail_pass,
    #                              results.xfail_works,
    #                              results.xskip,
    #                              0,
    #                              0,
    #                              0,
    #                              1, # TODO TUE test_ctx
    #                              '', 
    #                              0,
    #                              '',
    #                              0,
    #                              host.name,
    #                              '1'
    #                              )
    #                      end
        
  end
end

  end # module Runner
end # module Test
