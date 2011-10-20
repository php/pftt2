require 'timeout'
class IO
  include Timeout

  def ready?
    timeout 0.05 do
      got = getbyte
      return nil if got.nil?
      ungetbyte got
      return true
    end
  rescue Timeout::Error
    return false
  end

  def read_nonblock(limit=nil)
    buff = ''
    begin
      timeout 0.01 do
        c = getc
        raise :nothing_to_read if c.nil?
        buff += c
      end
    end while true
    raise :limit_reached if !limit.nil? and buff.length >= limit
  rescue Timeout::Error
  rescue :nothing_to_read
  rescue :limit_reached
  ensure
    return buff
  end
end