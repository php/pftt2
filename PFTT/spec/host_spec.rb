require File.join( File.dirname(__FILE__),'..','bootstrap.rb')

describe Host do
	hosts = []
  hosts.push Host::Local.new
  #hosts.push Host::Ssh.new # configuration

  hosts.each do |host|
    context host do
      context "#exec!" do
        context "`whoami`" do
          before :all do
            @stdout,@stderr,@status = host.exec! 'whoami'
          end
          subject { @stdout }
          it { should_not be_empty }
          
          subject { @stderr }
          it { should be_empty }

          subject { @status }
          it { should be_a_success }
        end
        context "`asdfasdfasdf`" do
          before :all do
            @stdout,@stderr,@status = host.exec! 'asdfasdfasdf'
          end
          subject { @stdout }
          it { should be_empty }
          
          subject { @stderr }
          it { should_not be_empty }

          subject { @status }
          it { should_not be_a_success }
        end
      end

      context '#cwd' do
        subject { host.cwd }
        it { should_not be_empty }
      end

      context '#pushd' do
        before :all do
          @original_cwd = host.cwd
          @new_cwd = host.windows? ? %q{C:\Windows} : %q{/var}
          host.pushd @new_cwd
        end
        
        subject { host.cwd }
        it { should_equal @new_cwd }

        it "should change cwd to the new directory" do
          host.cwd.should_equal
        end

        context '#popd' do
          before :all do
            host.popd
          end

          it "should revert to previous item in stack" do 
            host.cwd.should equal @new_cwd
          end
        end
      end
    end
  end
end