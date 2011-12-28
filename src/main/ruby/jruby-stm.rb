module JRubySTM
  VERSION = '0.0.1'
end

require "jruby-stm-#{JRubySTM::VERSION}.jar"
require 'jruby'

org.jruby.stm.STMLibrary.new.load(JRuby.runtime, false);