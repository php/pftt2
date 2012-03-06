package com.mostc.pftt

import groovy.lang.Closure;

import java.util.ArrayList;


abstract class AbstractHostList extends ArrayList {
	
//	def size(value=null) {
//	//length() returns number of hosts
//	// //length(value) returns number of hosts that returned given value
//	case value.null?
//	when true
//	super
//	when false
//	count_values(value)
//	end
//	}

//	def count_values(value) {
//		// count number of hosts that returned given value
//		c = 0
//		each { k, v ->
//		if eq_value(v, value)
//		c += 1
//		end
//		end
//		c
//	}

//	def keys(value=null) {
//	// //keys() returns an array of hosts
//	// //keys(value) returns a Host::Array of hosts that returned given value
//	case value.null?
//	when true
//	super
//	when false
//	slice_value(value)
//	end
//	}

//	def slice_value(value) {
//	// returns a Host::Array of hosts that returned given value
//	ret = Host::Array.new
//	each do |k, v|
//	if eq_value(v, value)
//	ret.push(k)
//	end
//	end
//	ret
//	}

	def eq_value(a, b) {
	a == b
	}
		
	def which(cmd, ctx=null) {
		each_ret(cmd) { host, sub_cmd ->
			host.which(sub_cmd, ctx)
		}
	}

	def hasCmd(cmd, ctx=null) {
		each_ret(cmd) { host, sub_cmd ->
			host.hasCmd(sub_cmd, ctx)
		}
	}

	def exec_pw(posix_cmd, windows_cmd, ctx, opts={}) {
		each_ret { host ->
			if (host.isPosix(ctx))
				host.exec(sub_cmd(host, posix_cmd), ctx, opts)
			else
				host.exec(sub_cmd(host, windows_cmd), ctx, opts)
		}
	}
	
	def exec_ok(cmd, ctx, opts={}) {
		each_ret(cmd) { host, sub_cmd ->
			host.exec_ok(sub_cmd, ctx, opts)
		}
	}
	
	def exec_pw_ok(posix_cmd, windows_cmd, ctx, opts={}) {
		each_ret { host ->
			if (host.isPosix(ctx))
				host.exec_ok(sub_cmd(host, posix_cmd), ctx, opts)
			else
				host.exec_ok(sub_cmd(host, windows_cmd), ctx, opts)
		}
	}
	
	def cmd_pw(posix_cmd, windows_cmd, ctx, opts={}) {
		each_thread { host ->
			if (host.isPosix(ctx))
				host.cmd(sub_cmd(host, posix_cmd), ctx, opts)
			else
				host.cmd(sub_cmd(host, windows_cmd), ctx, opts)
		}
	}
	
	def exec(command, ctx, opts={}) {
		each_ret(command) { host, sub_cmd ->
			host.exec(sub_cmd, ctx, opts)
		}
	}
	
	def cmd(cmdline, ctx) {
		each_ret(cmdline) { host, sub_cmdline ->
			host.cmd(sub_cmdline, ctx)
		}
	}
	
	def line(cmdline, ctx) {
		each_ret(cmdline) { host, sub_cmdline ->
			host.line(sub_cmdline, ctx)
		}
	}
	
	def isPosix(ctx=null) {
		each_ret { host ->
			host.isPosix(ctx)
		}
	}
	
	def isWindows(ctx=null) {
		each_ret { host ->
			host.isWindows(ctx)
		}
	}
	
	def isLonghorn(ctx=null) {
		each_ret { host ->
			host.isLonghorn(ctx)
		}
	}
	
	def isBSD(ctx=null) {
		each_ret { host ->
			host.isBSD(ctx)
		}
	}
	
	def isFreeBSD(ctx=null) {
		each_ret { host ->
			host.isFreeBSD(ctx)
		}
	}
	
	def isLinux(ctx=null) {
		each_ret { host ->
			host.isLinux(ctx)
		}
	}
	
	def isRedhat(ctx=null) {
		each_ret { host ->
			host.isRedhat(ctx)
		}
	}
	
	def isFedora(ctx=null) {
		each_ret { host ->
			host.isFedora(ctx)
		}
	}
	
	def isDebian(ctx=null) {
		each_ret { host ->
			host.isDebian(ctx)
		}
	}
	
	def isUbuntu(ctx=null) {
		each_ret { host ->
			host.isUbuntu(ctx)
		}
	}
	
	def isGentoo(ctx=null) {
		each_ret { host ->
			host.isGentoo(ctx)
		}
	}
	
	def isSUSE(ctx=null) {
		each_ret { host ->
			host.isSUSE(ctx)
		}
	}
	
	def isSolaris(ctx=null) {
		each_ret { host ->
			host.isSolaris(ctx)
		}
	}
	
	//    def utype? ctx=null
	//      each_ret do |host|
	//        host.utype?(ctx)
	//      end
	//    end
	//
	//    def windows_and_utype ctx=null
	//      // TODO
	//    end
	
	def unquote_line(cmdline, ctx) {
		each_ret(cmdline) { host, sub_cmdline ->
			host.unquote_line(sub_cmdline, ctx)
		}
	}
	
	def line_prefix(prefix, cmd, ctx) {
		each_ret(cmd) { host, sub_cmd ->
			host.line_prefix(prefix, sub_cmd, ctx)
		}
	}
		
	def reboot(ctx=null) {
		each_ret { host ->
			host.reboot(ctx)
		}
	}
	
	def reboot_wait(seconds, ctx=null) {
		each_ret { host ->
			host.reboot_wait(seconds, ctx)
		}
	}
	
	def isRebooting(ctx=null) {
		each_ret { host ->
			host.rebooting(ctx)
		}
	}
	
	def env_values(ctx=null) {
		each_ret { host ->
			host.env_values(ctx)
		}
	}
	
	def env_value(name, ctx=null) {
		each_ret { host ->
			host.env_value(name, ctx)
		}
	}
	
	def name(ctx=null) {
		each_ret { host ->
			host.name(ctx)
		}
	}
	
	def mkdir(path, ctx) {
		each_ret(path) { host, sub_path ->
			host.mkdir(sub_path, ctx)
		}
	}
	
	def mktmpdir(ctx, path=null, suffix='') {
		each_ret(path) { host, sub_path ->
			host.mktmpdir(ctx, sub_path, suffix)
		}
	}
	
	def mktmpfile(suffix, ctx, content=null) {
		each_ret { host ->
			host.mktmpfile(suffix, ctx, content)
		}
	}
	
	//    alias :mktempfile :mktmpfile
	//    alias :mktempdir :mktmpdir
	
	def delete_if(path, ctx) {
		each_ret(path) { host, sub_path ->
			host.delete_if(sub_path, ctx)
		}
	}
	
	def upload(from, to, ctx, opts={}) {
		each_ret(to) { host, sub_to ->
			host.upload(from, sub_to, ctx, opts)
		}
	}
	
	def download(from, to, ctx) {
		each_ret(to) { host, sub_to ->
			host.download(from, sub_to, ctx)
		}
	}
	
	def upload_force(local, remote, ctx, opts={}) {
		each_ret(remote) { host, sub_remote ->
			host.upload_force(local, sub_remote, ctx, opts)
		}
	}
	
	def upload_if_not(local, remote, ctx) {
		each_ret(remote) { host, sub_remote ->
			host.upload_if_not(local, sub_remote, ctx)
		}
	}
	
	def glob(path, spec, ctx) {
		each_ret(path) { host, sub_path ->
			host.glob(sub_path, spec, ctx)
		}
	}
	
	def list(path, ctx) {
		each_ret(path) { host, sub_path ->
			host.list(sub_path, ctx)
		}
	}
	
	def mtime(file, ctx=null) {
		each_ret(file) { host, sub_file ->
			host.mtime(sub_file, ctx)
		}
	}
	
	def write(string, path, ctx) {
		// TODO
		each_ret(path) { host, sub_path ->
			host.write(string, sub_path, ctx)
		}
	}
	
	def read(path, ctx) {
		each_ret(path) { host, sub_path ->
			host.read(sub_path, ctx)
		}
	}
	
	def cwd(ctx=null) {
		each_ret { host ->
			host.cwd(ctx)
		}
	}
	
	def cd(path, hsh, ctx=null) {
		// TODO
		each_ret(path) { host, sub_path ->
			host.cd(sub_path, hsh, ctx)
		}
	}
	
	def directory(path, ctx=null) {
		each_ret(path) { host, sub_path ->
			host.directory(sub_path, ctx)
		}
	}
	
	def open_file(path, flags='r', ctx=null, Closure block=null) {
		each_ret(path) { host, sub_path ->
			host.open_file(sub_path, flags, ctx, block)
		}
	}
	
	def isRemote() {
		each_ret { host ->
			host.isRemote()
		}
	}
	
	def delete(path, ctx=null) {
		// TODO
		each_ret(path) { host, sub_path ->
			host.delete(sub_path, ctx)
		}
	}
	
	def exist(path, ctx=null) {
		each_ret(path) { host, sub_path ->
			host.exist(sub_path, ctx)
		}
	}
	
	//    alias :exist :exist?
	//    alias :exists :exist?
	//    alias :exists? :exist?
	
	def copy(from, to, ctx, opts={}) {
		each_ret(to) { host, sub_to ->
			host.copy(from, sub_to, ctx, opts)
		}
	}
	
	def move(from, to, ctx) {
		each_ret(to) { host, sub_to ->
			host.move(from, sub_to, ctx)
		}
	}
	
	def time(ctx=null) {
		each_ret { host ->
			host.time(ctx)
		}
	}
	
	def setTime(time, ctx=null) {
//	// TODO
//	ret = VSA.new
//	each_thread(ret) do |host|
//	ret[host] = host.time = time
//	end
//	ret
	}
	
	def shell(ctx=null) {
		each_ret { host ->
			host.shell(ctx)
		}
	}
	
	def systeminfo(ctx=null) {
		each_ret { host ->
			host.systeminfo(ctx)
		}
	}
	
	def processor(ctx=null) {
		each_ret { host ->
			host.processor(ctx)
		}
	}
	
	def isX86(ctx=null) {
		each_ret { host ->
			host.isX86(ctx)
		}
	}
	
	def isX64(ctx=null) {
		each_ret { host ->
			host.isX64(ctx)
		}
	}
	
	def isARM(ctx=null) {
		each_ret { host ->
			host.isARM(ctx)
		}
	}
	
	def number_of_processors(ctx=null) {
		each_ret { host ->
			host.number_of_processors(ctx)
		}
	}
	
	def systemroot(ctx=null) {
		each_ret { host ->
			host.systemroot(ctx)
		}
	}
	
	def systemdrive(ctx=null) {
		each_ret { host ->
			host.systemdrive(ctx)
		}
	}
	
	def desktop(ctx=null) {
		each_ret { host ->
			host.desktop(ctx)
		}
	}
	
	def userprofile(ctx=null) {
		each_ret { host ->
			host.userprofile(ctx)
		}
	}
	
	def appdata(ctx=null) {
		each_ret { host ->
			host.appdata(ctx)
		}
	}
	
	def appdata_local(ctx=null) {
		each_ret { host ->
			host.appdata_local(ctx)
		}
	}
	
	def tempdir(ctx) {
		each_ret { host ->
			host.tempdir(ctx)
		}
	}
	
	//    alias :tmpdir :tempdir
	
	def osname(ctx=null) {
		each_ret { host ->
			host.osname(ctx)
		}
	}
	
	//    alias :os_name osname
	
	def hasDebugger(ctx) {
		each_ret { host ->
			host.hasDebugger(ctx)
		}
	}
	
	def on_error(host, ex) {
	}
	
	def sub_cmd(host, cmd) {
		// important that 'host' variable is named 'host'
		eval("$cmd")
	}
	
	def each_ret(path=null) {// TODO , &block) {
//	ret = VSA.new
//	each_thread do |host|
//	ret[host] = block.call(host, path.null? ? null : sub_cmd(host, path))
//	end
//	ret
	}
	
	protected def each_thread(ret=null) {// TODO , &block) {
//	tq = @thread_manager.new_queue
//
//	this.each do |host|
//	// TODO coordinate with the threads in exec! system
//	//
//	// TODO @thread_queue
//	tq.add_task do
//	begin
//	block.call(host)
//	rescue
//	if !ret.null? and !ret.has_key?(host)
//	// caller is supposed to return true|false for host
//	// to indicate success|failure
//	//
//	// make sure this gets set to false (since this failed)
//	ret[host] = false
//	end
//	on_error(host, $!)
//	end // begin
//	end // add_task
//
//	end // each
//
//	tq.execute
	} // def each_thread
	
} // class AbstractHostList
