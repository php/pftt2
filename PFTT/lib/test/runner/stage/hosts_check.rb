
module Test
  module Runner
    module Stage

class HostsCheck < Tracing::Stage
  
  def run(hosts, middlewares, phps, scenarios, test_ctx)
    notify_start
    
    ctx = nil # TODO
    
    hosts_alive = []
    hosts_dead = []
    thread_pool_hosts = Util::ThreadPool.new(hosts.length, ctx)
    hosts_unique = {}
    hosts.each do |host|
      thread_pool_hosts.add do
        begin
          puts "PFTT: checking #{host.address}"
          if host.alive?
            puts 'test_bench 77'
            hosts_alive.push(host)
            puts 'test_bench 79'
            if hosts_unique.has_key?(host.name.upcase)
              puts "PFTT: #{host.name} is a duplicate"
              return
            end
                          
            puts 'test_bench 84'
            begin
              puts 'test_bench _ 2'
              #host.name(nil)#.upcase
              puts 'test_bench _ 3'
              hosts_unique[host.name.upcase] = host
            rescue
              puts 'test_bench _ 4'
              puts $!.message+" "+$!.inspect
              puts 'test_bench _ 5'
            end
            puts 'test_bench 86'
          
            middlewares.each do |middleware_spec|
                            puts 'test_bench 88'
                            phps.each do |php|
                              puts 'test_bench 90'
                              scenarios.each do |scn_set|
                                puts 'test_bench 92'
                                test_ctx.add_legend_label(host, php, middleware_spec, scn_set)
                              end
                            end
                          end
                        puts 'test_bench 97'
                        else
                          hosts_dead.push(host)
                          puts "PFTT:dead-host: #{host.name}"
                        end
                      rescue 
                          puts $!.message+" "+$!.inspect
                      end            
                      puts 'test_bench 102'
                      end # Thread
                      puts 'test_bench 104'
                    end
                    puts 'test_bench 106'
                    thread_pool_hosts.join_seconds(60)
                    
                    puts "PFTT: #{hosts_dead.length-hosts_alive.length} TIMED OUT! #{hosts_alive.length} hosts alive. #{hosts_dead.length} hosts DEAD! #{hosts_unique.length} unique hosts."
                    hosts = hosts_unique.values
                    
                    notify_end(true)
                    
                    return hosts
end
                    
          end # class HostsCheck
               
    end
  end
end
