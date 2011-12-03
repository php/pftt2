
require 'host/remote/psc.rb' # temp

module Host
  module Remote
    module PSC
      
class RemoteHost < BaseRemoteHostAndClient
  attr_accessor :phps, :middlewares, :test_cases, :scn_sets
        
  def initialize(host)
    super(host, host, host, host, nil)
    @lock = Mutex.new
    @start_block = nil
    @phps = []
    @middlewares = []
    @test_cases = []
    @scn_sets = []
    @run = false
  end
  def send_full_block(block)
    block += "<Boundary>\n"
            
    # send via STDERR stream since normally PFTT will write to STDOUT
    # keeps any unexpected call to puts() from getting mixed in
    @lock.synchronize do
      STDERR.write(block)
    end
  end
  def running?
    @run
  end
  def stop
    @run = false
  end
  def run(&start_block)
    @start_block = start_block
    @run = true
          
    while @run
      line = STDIN.gets()
      # TODO fucked up recv_ssh_block(line)
      xml = to_simple(line)
      dispatch_recvd_xml(xml)
      dispatch_recvd_xml({'@msg_type'=>'start'}) # TODO
    end
  end
  def send_result(result)
    xml = result.to_xml
    xml['@msg_type'] = 'result'
    send_xml(xml)
  end
  def dispatch_recvd_xml(xml)
    msg_type = xml['@msg_type']
    if msg_type == 'start'
      # important part of protocol: tell client this host has started (it'll expect this within 60 seconds)
      send_xml({}, 'started')
      if @start_block
        @start_block.call
      end
    elsif msg_type == 'stop'
      stop()
    elsif msg_type == 'middleware'
      # TODO
      @middlewares.push(Middleware.from_xml(xml, @host, @phps.first, @scn_sets.first))
    elsif msg_type == 'test_case'
      @test_cases.push(Test::Case::Phpt.from_xml(xml))
    elsif msg_type == 'scn_set'
      @scn_sets.push(Scenario::Set.from_xml(xml))
    elsif msg_type == 'php'
      @phps.push(PhpBuild.from_xml(xml))
    end
  end # def dispatch_recvd_xml
  def exe_prompt(prompt_type, prompt_timeout)
    # TODO
  end
end # class RemoteHost
      
    end # module PSC
  end
end
