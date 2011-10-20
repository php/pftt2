require File.join( File.dirname(__FILE__),'..','bootstrap.rb')

require 'tmpdir'
require 'fileutils'

describe PhptTestCase do
  before :each do
    @tempdir = Dir.mktmpdir 'test'
  end

  after :each do
    FileUtils::rm_rf @tempdir
  end

  context 'Scenario 1: Most basic' do
    before :each do
      @testfile = File.join @tempdir, 'test.phpt'
      File.open @testfile, 'w' do |file|
        file.write <<-EOF
--TEST--
description of the test
--FILE--
<?php echo 'foo' ?>
--EXPECT--
foo
EOF
      end
      @phpt = PhptTestCase.new @testfile
    end

    it 'should parse TEST section properly' do
      @phpt.get_section(:test).should == "description of the test\n"
    end
    it 'should parse FILE section properly' do
      @phpt.get_section(:file).should == "<?php echo 'foo' ?>\n"
    end
    it 'should parse EXPECT section properly' do
      @phpt.get_section(:expect).should == "foo\n"
    end
    it 'should return no-directive ini object' do
      @phpt.ini.to_s.should == ''
    end
    it 'should return an empty hash for options' do
      @phpt.options.should == {}
    end
    it 'should not report itself borked' do
      @phpt.borked?.should be_false
    end
    it 'should not report itself unsupported' do
      @phpt.borked?.should be_false
    end
  end

  context 'Scenario 2: Missing required section(s)' do
    before :each do
      @testfile = File.join @tempdir, 'test.phpt'
      File.open @testfile, 'w' do |file|
        file.write <<-EOF
--TEST--
description of the test
--FILE--
<?php echo 'foo' ?>
EOF
      end
      @phpt = PhptTestCase.new @testfile
    end

    it 'should parse TEST section properly' do
      @phpt.get_section(:test).should == "description of the test\n"
    end
    it 'should parse FILE section properly' do
      @phpt.get_section(:file).should == "<?php echo 'foo' ?>\n"
    end
    it 'should parse EXPECT section properly' do
      @phpt.get_section(:expect).should be_nil
    end
    it 'should report itself borked' do
      @phpt.borked?.should be_true
    end
    it 'should not report itself unsupported' do
      @phpt.unsupported?.should be_false
    end
  end

  context 'Scenario 3: Unsupported section(s)' do
    before :each do
      @testfile = File.join @tempdir, 'test.phpt'
      File.open @testfile, 'w' do |file|
        file.write <<-EOF
--TEST--
description of the test
--FILE--
--EXPECT--
--FOO--
<?php echo 'foo' ?>
EOF
      end
      @phpt = PhptTestCase.new @testfile
    end

    it 'should not report itself borked' do
      @phpt.borked?.should be_false
      @phpt.bork_reasons.should == []
    end

    it 'should report itself unsupported' do
      @phpt.unsupported?.should be_true
    end
  end

  context 'Scenario 4: options' do
    before :each do
      @testfile = File.join @tempdir, 'test.phpt'
      File.open @testfile, 'w' do |file|
        file.write <<-EOF
--TEST--
description of the test
--PFTT--
foo: [food, fool, foot]
bar: [barn, bars, bart]
num: 1
--FILE--
--EXPECT--
--FOO--
<?php echo 'foo' ?>
EOF
      end
      @phpt = PhptTestCase.new @testfile
    end

    it 'should parse the PFTT section as YAML' do
      @phpt.options.should == {'foo'=>['food','fool','foot'],'bar'=>['barn','bars','bart'],'num'=>1}
      #@phpt.get_section(:pftt).should == 'asdasd'
    end
  end
end