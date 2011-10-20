#bootstrap.rb
require "rubygems"
require "bundler"
Bundler.setup

# Load up the monkeypatches
Dir.glob(File.join( File.dirname(__FILE__), 'lib/monkeypatch/**/*.rb')).reverse_each &method(:require)


require 'active_support/dependencies'

APPROOT = File.absolute_path( File.dirname( __FILE__ ) ) 
libdir = File.join( APPROOT, 'lib' )
ActiveSupport::Dependencies.autoload_paths << libdir
$: << libdir
