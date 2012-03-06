package com.mostc.pftt

//
//require 'tracing.rb'
//
//module Base
//  
//class RunOptions
//  
//  def self.msg_type
//    'run_options'
//  end
//  
//end
//  
//class TestPack < Tracing::FileManager
//  
//  attr_reader :correct_test_count
//  
//  def initialize correct_test_count
//    @correct_test_count = correct_test_count
//  end
//  
//  def self.msg_type
//    'test_pack'
//  end
//  
//  def load_test_case_by_name case_name
//    []
//  end
//  
//  def load_all_test_cases
//    []
//  end
//  
//end # class TestPack
//  
//  class CaseName
//    
//    attr_reader :name_pattern
//    
//    def initialize name_pattern
//      @name_pattern = name_pattern
//    end
//    
//    def self.msg_type
//      'case_name'
//    end
//    
//    def self.fromXML
//    end
//    
//    def toXML
//    end
//    
//  end # class CaseName
//  
//  class AllCases
//    
//    attr_reader :correct_test_case_count
//    
//    def initialize correct_test_case_count
//      @correct_test_case_count = correct_test_case_count
//    end
//    
//    def self.msg_type
//      'all_cases'
//    end
//    
//    def self.fromXML
//    end
//    
//    def toXML
//    end
//    
//  end # class AllCases
//
//class Build < Tracing::FileManager
//  
//  def self.msg_type
//    'build'
//  end
//  def msg_type # TODO
//    'build'
//  end
//  
//end # class Build
//
//class Configuration < Tracing::FileManager
//  
//  def self.msg_type
//    'configuration'
//  end
//  
//end # class Configuration
//  
//end # module Base
