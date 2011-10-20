require File.join( File.dirname(__FILE__),'..','bootstrap.rb')

shared_examples_for 'threadsafety detector' do
  it 'should report threadsafe when it is' do
    @one[:threadsafe].should be_true
  end
  it 'should not report threadsafe when it is not' do
    @two[:threadsafe].should be_false
  end
end
shared_examples_for 'branch detector' do
  it 'should detect 5.3' do
    @one[:branch].should == '5.3'
  end
  it 'should detect 5.4' do
    @two[:branch].should == '5.4'
  end
end
shared_examples_for 'compiler detector' do
  it 'should detect VC6' do
    @one[:compiler].should == 'VC6'
  end
  it 'should detect VC9' do
    @two[:compiler].should == 'VC9'
  end
end
shared_examples_for 'not revisioned' do
  it 'should not have revision information' do
    @one[:revision].should be_nil
  end
end
shared_examples_for 'revisioned' do
  it 'should have revision information' do
    @one[:revision].should == 'r310546'
  end
end

describe PhpBuild do
  context 'when loading release builds' do
    before :each do
      @one = PhpBuild.new('php-5.3.6-Win32-VC6-x86')
      @two = PhpBuild.new('php-5.4.6-nts-Win32-VC9-x86')
    end

    it_should_behave_like 'threadsafety detector'
    it_should_behave_like 'branch detector'
    it_should_behave_like 'compiler detector'
    it_should_behave_like 'not revisioned'

    it 'should build a readable version' do
      @one[:version].should == '5.3.6'
      @two[:version].should == '5.4.6'
    end

    specify{ @one[:type].should == :release }
  end

  context 'when loading release candidate builds' do
    before :each do
      @one = PhpBuild.new('php-5.3.7RC1-Win32-VC6-x86')
      @two = PhpBuild.new('php-5.4.7RC1-nts-Win32-VC9-x86')
    end

    it_should_behave_like 'threadsafety detector'
    it_should_behave_like 'branch detector'
    it_should_behave_like 'compiler detector'
    it_should_behave_like 'not revisioned'
    
    it 'should build a readable version' do
      @one[:version].should == '5.3.7RC1'
      @two[:version].should == '5.4.7RC1'
    end

    specify{ @one[:type].should == :release_candidate }
  end

  context 'when loading snap builds' do
    before :each do
      @one = PhpBuild.new('php-5.3-nt-windows-vc6-x86-r310546')
      @two = PhpBuild.new('php-5.4-nts-windows-vc9-x86-r310546')
    end

    it_should_behave_like 'threadsafety detector'
    it_should_behave_like 'branch detector'
    it_should_behave_like 'compiler detector'
    it_should_behave_like 'revisioned'
    
    it 'should build a readable version' do
      @one[:version].should == '5.3-r310546'
      @two[:version].should == '5.4-r310546'
    end
    

    specify{ @one[:type].should == :snap }

  end

  context 'when loading prerelease builds' do
    # http://windows.php.net/downloads/qa/php-5.4.0alpha1-nts-Win32-VC9-x86.zip
    before :each do
      @one = PhpBuild.new('php-5.3.0alpha1-Win32-VC6-x86')
      @two = PhpBuild.new('php-5.4.0beta2-nts-Win32-VC9-x86')
    end

    it_should_behave_like 'threadsafety detector'
    it_should_behave_like 'branch detector'
    it_should_behave_like 'compiler detector'
    it_should_behave_like 'not revisioned'

    it 'should build a readable version' do
      @one[:version].should == '5.3.0alpha1'
      @two[:version].should == '5.4.0beta2'
    end

    specify{ @one[:type].should == :prerelease }
    
  end

end
