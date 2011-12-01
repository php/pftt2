
module Test
  module Runner
    module Stage
      module PHPT

class Install < Tracing::Stage::ByHostMiddleware
  
  def run(scn_set, test_ctx)
    begin
      notify_start
      
      @middleware.install(test_ctx, scn_set.working_fs.docroot(@middleware))
            
      scn_set.deploy(@host)
          
      @middleware.start!(test_ctx)
      
      notify_end
    rescue
      notify_failed
      
      if test_ctx
        test_ctx.pftt_exception(self, $!, @host)
      else
        Tracing::Context::Base.show_exception($!)
      end
    end
  end # def run
  
end # class Install

      end # module PHPT
    end # module Stage
  end
end
