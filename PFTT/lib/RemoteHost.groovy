package com.mostc.pftt
 
abstract class RemoteHost extends Host {
      
      def isRemote() {
        true
      }
	  
	  @Override
	  def getTime(ctx=null) {
		  if (isPosix(ctx)) {
			  new Date(line('date', ctx))
		  } else {
			  // gets the host's time/date
			  new Date(unquote_line('date /T', ctx)+' '+unquote_line('time /T', ctx))
		  }
	  }
      
      def canStream() {
        false // ssh can
      }
      
      def ensure_7zip_installed(ctx) {
        if (tried_install_7zip) {
          return installed_7zip
        }
        
        // TODO Util::Install::7zip.install(self)
        
        installed_7zip = false
      } // end def ensure_7zip_installed ctx
      
      def isRebooting() {
        rebooting
      }
      
      def reboot_wait(seconds, ctx) {
        super.reboot_wait(seconds, ctx)
        
        reboot_reconnect_tries = ( seconds / 60 ).to_i + 1
        if (reboot_reconnect_tries < 3) {
          reboot_reconnect_tries = 3
        }
        
        rebooting = true
        // will have to recreate sockets when it comes back up
        // session() and sftp() will block until then (so any method 
        // LATER using sftp() or session() will be automatically blocked during reboot(good))
        close
      }
      
      def RemoteHost(opts={}) {
        reconnected_after_reboot
      }
      
//      def clone(clone) {
//        clone.rebooting                              = rebooting
//        clone.rebooting_reconnect_tries = rebooting_reconnect_tries
//        clone.tried_install_7zip                 = tried_install_7zip
//        clone.installed_7zip                       = installed_7zip
//        super(clone)
//      }
      
      protected
      
      // for //clone()
//      attr_accessor :rebooting, :rebooting_reconnect_tries, :tried_install_7zip, :installed_7zip
      
      def reconnected_after_reboot() {
        tried_install_7zip = false
        // maybe reverted to a snapshot (if host is a virtual machine)
        installed_7zip = false
        rebooting = false
        rebooting_reconnect_tries = 0
        }
      
} // end public class RemoteHost
