package com.mostc.pftt

/** Host handles file system, system info and program execution for local and remote hosts abstracting
 * and automatically handling differences between operating systems. 

1. use #systemdrive in place of C:

 Windows 2003r2 and above will use a different drive letter (not C)
 if its not installed on the first *FAT or NTFS Partition (first partition with type set to
 FAT or NTFS during installation process).

 host.exec_pw!("/usr/bin/unzip", "$host.systemdrive/php-sdk/bin/unzip")
 host.exec_pw!("/usr/bin/p7zip", "$host.programfiles/7-Zip/7zFM")
 host.exec_pw!("/usr/bin/p7zip", "$host.programfiles86/7-Zip/7zFM")

2. Multiple hosts variable substitution

 when using host.systemdrive, host.programfiles, host.tempdir, etc... with Hosts::Array, pass in using ' ' not " "
 and name the variable 'host'. then it will be automatically substituted for each host.

 hosts.exec_pw!("/usr/bin/unzip", '$host.systemdrive/php-sdk/bin/unzip')
 hosts.exec_pw!("/usr/bin/p7zip", '$host.programfiles/7-Zip/7zFM')
 hosts.exec_pw!("/usr/bin/p7zip", '$host.programfiles86/7-Zip/7zFM')

3. Important Methods

Host#systemdir
Host#programfiles
Host#tempdir
Host#homedir
Host#documents
Host#downloads
Host#desktop
Host#exec_pw
Host#exec
Host#cmd_pw
Host#exist
Host#directory
Host#mktempfile
Host#mktempdir
Host#copy
Host#move
Host#upload
Host#download
Host#list
Host#glob


*/

abstract class Host {

	static def sub(String base, String path) {
		if ( path.startsWith(base) ) {
			return path.substring(base.length()+1);
		}
		return path;
	}

	static def join(String... array) {
		StringBuilder sb = new StringBuilder()
		for (def e : array ) {
			if (sb.length() > 0)
				sb.append('/')
			sb.append(e.toString())
		}
		sb.toString()
	}
//	  def this.join *path_array
//		path_array.join('/')
//	  end

//	  static def administrator_user (platform) {
//		if platform == :windows {
//		  return 'administrator'
//		} else {
//		  return 'root'
//		}
//	  }

	static def no_trailing_slash(path) {
		if (path.endsWith('/') || path.endsWith('\\')) {
			path = path[0..path.length-1]
		}
		return path
	}

	static def to_windows_path(path) {
		// remove \\ from path too. they may cause problems on some Windows SKUs
		path = path.replaceAll('/', '\\').replaceAll('\\\\', '\\').replaceAll('\\\\', '\\')
		if (path[0] == "\\" || path[0] == '/') {
			// TODO
			path = path[1..path.length]
		}
		path
	}

	static def to_posix_path(path) {
		path.replaceAll('\\\\', '/')
		path.replaceAll('//', '/')
		path.replaceAll('//', '/')
	}

//	  static def fs_op_to_cmd(fs_op, src, dst) {
//		// TODO
//		return case fs_op
//		when :move
//		when :copy
//		when :delete
//		when :mkdir
//		when :list
//		when :glob
//		when :cwd
//		when :cd
//		when :exist
//		when :is_dir
//		when :open
//		when :upload
//		  null
//		when :download
//		  null
//		else
//		  null
//		end
//	  }

	def registry_query(key, value, type, ctx) {
		for ( def line : host.lines('REG QUERY "'+key+'" /v '+value, ctx) ) {
			if ( line.trim().startsWith("$value ")) {
				def parts = line.split("$type ")
				if ( parts.length > 1 ) {
					return parts[1].trim()
				}
			}
		}
		null
	}
	
	def registry_update(key, value, data, type, ctx) {
		registry_add(key, value, data, type, ctx)
	}
	
	def registry_add(key, value, data, type, ctx) {
		host.exec('REG ADD "'+key+'" /v '+value+' /d '+data+' /t '+type+' /f', ctx)
	}
	
	def registry_delete(key, value, ctx) {
		host.exec('REG DELETE "'+key+'" /v '+value+' /f', ctx)
	}
	
	def read_config(path, ctx=null) {
	// /etc/[program]/
	// %ProgramFiles%/[program]/
	// TODO
	}
	
	class Config {
	def write() {
	}
	}
	
	def write_config(path, config, ctx=null) {
	// TODO
	}
	
	def registry_import(file, ctx=null) {
	// TODO
	}
	
	def registry(key, ctx=null) {
	// TODO
	}
	
	class RegistryKey {
	def hive() {
	}
	def export(file) {
	}
	def add() {
	}
	def query() {
	}
	def delete() {
	}
	}
	
	def isWDWOS() {
	false // LATER
	}
	
	def getWDWOS() {
	// OS::WDW::MS::Win::Win7::x64::SP1
	// OS::WDW::MS::Win::Win7::x86::SP0
	null // LATER
	}
	
	def isVMGuest() {
	false // LATER
	}
	
	def getVMHost() {
	null // LATER
	}
	
	def getVMHostManager() {
		if (isVMGuest()) {
		// LATER how to share vm_host() instances amongst guest host instances?
		def h = getVMHost()
		if (h) {
		return h.vm_host_mgr()
		}
		}
		// Host::VMManager.new (save and share w/ //clone too!)
		null // LATER
	}
	
	// LATER int findProcess(String, svc_host=false)
	// LATER kill(String) and kill(int)

	def isRunning(exe, ctx=null, svc_host=false) {
		// checks if the named process is running (if 1+ processes are running that match the name)
		//
		// windows note: some processes(ex: Internet Information Services) are run within a 'service host'
		// in which case the process will show up as 'svchost.exe'. these processes are 'windows services'.
		// if the process you're checking for is a 'windows service', set svc_host=true.
		//        note: service name is case sensitive!
		//        note: most/all services don't include .exe in the name (checking will fail if this doesn't match up)
		//        note: svc_host is ignored on posix. you can set it to true in case of windows without causing a problem on posix.
		//
		// windows note: if svc_host=false, and if you ommit the .exe from the process name, this will
		// check for both processes named with the given name and the given name + '.exe' (returns
		// true if either are running).
		//
		if (isWindows(ctx)) {
			if (svc_host) {
				return exec("tasklist /FI \"SERVICES eq $exe\"").output.contains(exe)
			}
			def r = exec("tasklist /FI \"IMAGENAME eq $exe\"").output.contains(exe)
			if (!r && !exe.endsWith('.exe')) {
				return isRunning("$exe.exe", ctx)
			}
			return r
		} else {
			return exec("pgrep $exe", ctx).output.length() > 1
		}
	}

	enum EProcessor {
		x64, x86, arm, mips, alpha, ppc, sparc, unknown
	}

	def processor(ctx=null) {
		// TODO cache result
		def a = isPosix(ctx) ? cmd('uname -a', ctx).output : env_value('PROCESSOR_ARCHITECTURE', ctx)
		if (a == null||a.length()==0) {
			return EProcessor.unknown
		}
		a = a.toLowerCase()
		if (a.contains('x86_64') || a.contains('i86pc'))
			EProcessor.x64
		else if (a.contains('x86'))
			EProcessor.x86
		else if (a.contains('arm'))
			EProcessor.arm
		else if (a.contains('mips'))
			EProcessor.mips
		else if (a.contains('alpha'))
			EProcessor.alpha
		else if (a.contains('ppc'))
			EProcessor.ppc
		else if (a.contains('sparc'))
			EProcessor.sparc
		else
			EProcessor.unknown
	}

	def isX86(ctx=null) {
		// x64 also supports x86
		isX86Only(ctx) || isX64(ctx)
	}

	def isX86Only(ctx=null) {
		processor(ctx) == EProcessor.x86
	}

	def isX64(ctx=null) {
		processor(ctx) == EProcessor.x64
	}

	def isARM(ctx=null) {
		processor(ctx) == EProcessor.arm
	}

	def number_of_processors(ctx=null) {
		// TODO cache result
		// counts number of CPUs in host
		def p = 0
		if (isWindows(ctx)) {
			p = env_value('NUMBER_OF_PROCESSORS', ctx)
			if (p) {
				// TODO get env_value to parse integer, float, bool
				p = Integer.parseInt(p)
			}
		} else {
			def cpuinfo = read_lines('/proc/cpuinfo', ctx)
			
			p = 0
			// each processor will have a line like 'processor   : //', followed by lines of info
			// about that processor
			//
			// count number of those lines == number of processors
			for (def line : cpuinfo) {
				if (line.startsWith('processor')) {
					p += 1
				}
			}	
		}

		return p > 0 ? p : 1 // ensure > 0 returned
	} // end def number_of_processors
	
	def username(ctx=null) {
		if (isPosix(ctx))
			env_value('USER', ctx)
		else
			env_value('USERNAME', ctx)	
	}

	abstract def upload(local_file, remote_path, ctx, opts=[])
	abstract def cwd(ctx=null)
	abstract def cd(path, hsh, ctx=null)
	abstract def read_lines(path, ctx=null, max_lines=16384)
	abstract def read(path)
	abstract def directory(path, ctx=null)
	abstract def list(path, ctx)
	abstract def isRemote()
	abstract def mtime(file, ctx=null)
	abstract def write(string, path, ctx)
	abstract def isAlive()
	abstract def env_values(ctx=null)
	abstract def env_value(name, ctx=null)
	abstract def close()
	
	def write_lines(lines, path, ctx) {
		write(lines.join("\n"), path, ctx)
	}

	def isRebooting() {
		false
	}

	def reboot(ctx) {
		// reboots host and waits 120 seconds for it to become available again
		reboot_wait(120, ctx)
	}

	def reboot_wait(seconds, ctx) {
		//
		if (isWindows(ctx)) {
			exec("shutdown /r /t 0", ctx)
		} else {
			exec("shutdown -r -t 0", ctx)
		}	
	}

	def nt_version(ctx) {
		if (isWindows(ctx)) {
			return null
		}

		def nt_version = systeminfo_line('OS Version', ctx)

		return nt_version ? nt_version.to_f : 5 // 5 (aka Windows 2000) is earliest supported NT Version
	}

	def eol(ctx) {
		(isWindows(ctx)) ? "\r\n" : "\n"
	}

	def eol_escaped(ctx) {
		(isWindows(ctx)) ? "\\r\\n" : "\\n"
	}

	def upload_force(local, remote, ctx, mkdir=true) {
		delete_if(remote, ctx)

		upload(local, remote, ctx, mkdir)
	}

	def copy(from, to, ctx, opts=[]) {
		if (ctx) {
			ctx.fs_op2(this, EFSOp.copy, from, to) |new_from; new_to| {
				return copy(new_from, new_to, ctx, opts)
			}
		}

		if (opts.hasProperty('mkdir')&&opts.mkdir!=false) {
			mkdir(dirname(to), ctx)
		}

		copy_cmd(from, to, ctx)
	}

	def trash(path, ctx) {
		move(path, join(trashdir(ctx), basename(path)), ctx)
	}
	
	def basename(path) {
		new File(path).getName()
	}
	
	def dirname(path) {
		new File(path).getParent()
	}

	def move(from, to, ctx) {
		if (ctx) {
			ctx.fs_op2(this, EFSOp.move, from, to) |new_from; new_to| {
				return move(new_from, new_to, ctx)
			}
		}

		if (!directory(from)) {
			move_file(from, to, ctx)
			return
		}

		move_cmd(from, to, ctx)
	}

	def setTime(time, ctx=null) {
		// sets the host's time/date
		// TODO posix support
		if (isPosix(ctx)) {
			exec('date --set="'+time+'"', ctx)
		} else {
			exec("date $time.month-$time.day-$time.year && time $time.hour:$time.minute-$time.second", ctx)
		}
	}

	abstract def getTime(ctx=null)

	def systemroot(ctx=null) {
		// get's the file system path pointing to where the host's operating system is stored
		if (isPosix(ctx)) {
			return '/'
		} else if (_systemroot) {
			return _systemroot
		} else {
			_systemroot = env_value('SYSTEMROOT', ctx)
			return _systemroot
		}
	}

	def systemdrive_or_homedir(ctx=null) {
		// returns systemdrive on windows and the user's home directory on posix
		isWindows(ctx) ? systemdrive(ctx) : homedir(ctx)
	}

	def systemdrive(ctx=null) {
		// gets the file system path to the drive where OS and other software are stored
		if (isPosix(ctx)) {
			return '/'
		} else if (_systemdrive) {
		return _systemdrive
		} else {
		_systemdrive = env_value('SYSTEMDRIVE', ctx)
		return _systemdrive
		}
	}

	def desktop(ctx=null) {
		return join(userprofile(ctx), 'Desktop')
	}

	def userprofile(ctx=null) {
		if (isPosix(ctx)) {
			return homedir(ctx)
		}

		if (_userprofile) {
			return _userprofile
		}

		def p = env_value('USERPROFILE', ctx)

		if (exists(p, ctx)) {
			return _userprofile = p
		} else if (!_homedir) {
			return _userprofile = _homedir
		} else {
			return _userprofile = systemdrive(ctx)
		}
	}

	def programfiles(ctx=null) {
		if (isPosix(ctx)) {
			return '/usr/local/bin'
		} else if (!_programfiles) {
			_program_files = env_value('PROGRAMFILES', ctx)
			if (_program_files) {
				_program_files = systemdrive(ctx)+'/Program Files/'
			}
		}
		return _programfiles
	}

	def programfilesx86(ctx=null) {
		if (isPosix()) {
			return '/usr/local/bin'
		} else if (!_program_files_x86) {
			_program_files_x86 = env_value('PROGRAMFILES(x86)', ctx)
			if (!exists(_program_files_x86, ctx)) {
			_program_files_x86 = env_value('PROGRAMFILES', ctx)
			}
			if (_program_files_x86) {
				_program_files = systemdrive(ctx)+'/Program Files/'
			}
		}
		return _programfiles
	}

	def homedir(ctx=null) {
		if (!_homedir) {
			if (isPosix(ctx)) {
				// on linux/unix, its simple, just get $HOME
				_homedir = env_value('HOME', ctx)
			} else {
				// on WIndows, home directory is %HOMEDRIVE%%HOMEPATH%
				// (though those variables may be undefined for a Windows user)
				_homedir = env_value('HOMEDRIVE', ctx)
				if (_homedir) {
					// fallback to user profile (or null if not set)
					_homedir = _userprofile
				} else {
					def a = env_value('HOMEPATH', ctx)
					if (a) {
						if (!_userprofile) {
							// fallback to %USERPROFILE%
							_homedir = _userprofile
						// else: fallback to %HOMEDRIVE%
						}
					} else {
						// add %HOMEPATH% to %HOMEDRIVE%
						_homedir += a
					}
				}
			}
		}
		_homedir
	} // end def homedir

	def documents(ctx=null) {
		return userprofile(ctx) + '/Documents'
	}
	
	def downloads(ctx=null) {
		return userprofile(ctx) + '/Downloads'
	}
	
	def trashdir(ctx=null, drive=null) {
		if (isPosix(ctx)) {
			return desktop(ctx) + '/Trash'
		}
	
		if (drive) {
			drive = systemdrive(ctx)
		}
	
		return drive + '\\$Recycle.Bin'
	}

	def appdata(ctx=null) {
		if (_appdata) {
			return _appdata
		}
		if (isPosix(ctx)) {
			def p = env_value('HOME', ctx)
			if (p && exists(p, ctx)) {
				return _appdata = p
			}
		} else {
			def p = env_value('USERPROFILE', ctx)
			if (p) {
				def q = p + '\\AppData\\'
				if (exists(q, ctx)) {
					return _appdata = q
				} else if (exists(p, ctx)) {
					mkdir(q, ctx)
					return _appdata = q
				}
			}
		}
		_appdata = systemdrive(ctx)
	} // end def appdata
	
	def appdata_local(ctx=null) {
		if (_appdata_local) {
			return _appdata_local
		}
		if (isPosix()) {
			def p = env_value('HOME', ctx)
			if (p) {
				def q = p + '/PFTT' 
				if (exists(q, ctx)) {
					return _appdata_local = q
				} else if (exists(p, ctx)) {
					mkdir(q, ctx)
					return _appdata_local = q
				}
			}
		} else {
			def p = env_value('USERPROFILE', ctx)
			if (p) {
				def q = p + '\\AppData\\Local'
				if (exists(q, ctx)) {
					return _appdata_local = q
				} else if (exists(p, ctx)) {
					mkdir(q, ctx)
					return _appdata_local = q
				}
			}
		}	
		_appdata_local = systemdrive(ctx)
	} // end def appdata_local

	def tempdir(ctx) {
		if (_tempdir) {
			return _tempdir
		}
		if (isPosix()) {
			def p = '/usr/local/tmp'
			def q = p + '/PFTT'
			if (exists(q, ctx)) {
				return _tempdir = q
			} else if (exists(p, ctx)) {
				mkdir(q, ctx)
				return _tempdir = q
			}
			p = '/tmp'
			q = p + '/PFTT'
			if (exists(q, ctx)) {
				return _tempdir = q
			} else if (exists(p, ctx)) {
				mkdir(q, ctx)
				return _tempdir = q
			}
		} else {
			// try %TEMP%\\PFTT
			def p = env_value('TEMP', ctx)
			if (p) {
				def q = p + '\\PFTT'
				if (exists(q, ctx)) {
					return _tempdir = q
				} else if (exists(p, ctx)) {
					mkdir(q, ctx)
					return _tempdir = q
				}
			}
	
			// try %TMP%\\PFTT
			p = env_value('TMP', ctx)
			if (p) {
				def q = p + '\\PFTT'
				if (exists(q, ctx)) {
					return _tempdir = q
				} else if (exists(p, ctx)) {
					mkdir(q, ctx)
					return _tempdir = q
				}
			}
	
			// try %USERPROFILE%\\AppData\\Local\\Temp\\PFTT
			p = env_value('USERPROFILE', ctx)
			if (p) {
				p = '\\AppData\\Local\\Temp\\' + p
				def q = p + '\\PFTT'
				if (exists(q, ctx)) {
					return _tempdir = q
				} else if (exists(p, ctx)) {
					mkdir(q, ctx)
					return _tempdir = q
				}
			}
	
			// try %SYSTEMDRIVE%\\temp\\PFTT
			p = systemdrive(ctx)+'\\temp'
			q = p + '\\PFTT'
			if (exists(q, ctx)) {
				return _tempdir = q
			} else if (exists(p, ctx)) {
				mkdir(q, ctx)
				return _tempdir = q
			}
	
		}
		
		_tempdir = systemdrive(ctx)
	} // end def tempdir


	def systeminfo(ctx=null) {
		// gets information about the host (as a string) including CPUs, memory, operating system (OS dependent format)
		if (!_systeminfo) {
			if (isPosix(ctx))
				_systeminfo = exec('uname -a', ctx).output + "\n" + exec('cat /proc/meminfo', ctx).output +"\n" + exec('cat /proc/cpuinfo', ctx).output // LATER?? glibc version
			else
				_systeminfo = exec('systeminfo', ctx).output
		}
		return _systeminfo
	}
	
	static def os_short_name(os) {
	  os = os.replaceAll('Windowsr', 'Win')
	  os = os.replaceAll('Microsoft', '')
	  os = os.replaceAll('Server', '')
	  os = os.replaceAll('Developer Preview', 'Win 8')
	  os = os.replaceAll('Win 8 Win 8', 'Win 8')
	  os = os.replaceAll('Full', '')
	  os = os.replaceAll('Installation', '')
	  os = os.replaceAll("\\(", '')
	  os = os.replaceAll("\\)", '')
	  os = os.replaceAll('tm', '')
	  os = os.replaceAll('VistaT', 'Vista')
	
	  os = os.replaceAll('Windows', 'Win')
	  os = os.replaceAll('/', '')
	
	  // remove common words
	  os = os.replaceAll('Professional', '')
	  os = os.replaceAll('Standard', '')
	  os = os.replaceAll('Enterprise', '')
	  os = os.replaceAll('Basic', '')
	  os = os.replaceAll('Premium', '')
	  os = os.replaceAll('Ultimate', '')
	  os = os.replaceAll('GNU', '')
	  if (!os.contains('XP')) {
	    // XP Home != XP Pro
	    os = os.replaceAll('Home', '')
	  }
	  os = os.replaceAll('Win Win', 'Win')
	  os = os.replaceAll("\\(R\\)", '')
	  os = os.replaceAll(',', '')
	  os = os.replaceAll('Edition', '')
	  os = os.replaceAll('2008 R2', '2008r2')
	  os = os.replaceAll('2003 R2', '2003r2')
	  os = os.replaceAll('RTM', '')
	  os = os.replaceAll('Service Pack 1', '')
	  os = os.replaceAll('Service Pack 2', '')
	  os = os.replaceAll('Service Pack 3', '')
	  os = os.replaceAll('Service Pack 4', '')
	  os = os.replaceAll('Service Pack 5', '')
	  os = os.replaceAll('Service Pack 6', '')
	  os = os.replaceAll('Microsoft', '')
	  os = os.replaceAll('N/A', '')
	  os = os.replaceAll('PC', '')
	  os = os.replaceAll('Server', '')
	  os = os.replaceAll('-based', '')
	  os = os.replaceAll('Build', '')
	  //
	  os = os.replaceAll('6.1.7600', '')
	  os = os.replaceAll('6.1.7601', '')
	  os = os.replaceAll('6.0.6000', '')
	  os = os.replaceAll('6.0.6001', '')
	  os = os.replaceAll('6.0.6002', '')
	  os = os.replaceAll('5.1.3786', '')
	  os = os.replaceAll('5.1.3787', '')
	  os = os.replaceAll('5.1.3788', '')
	  os = os.replaceAll('5.1.3789', '')
	  os = os.replaceAll('5.1.3790', '')
	  os = os.replaceAll('5.0.2600', '')
	  os = os.replaceAll('5.0.2599', '')
	  os = os.replaceAll('5.2.SP2', '')
	  os = os.replaceAll('5.2', '')
	  os = os.replaceAll('7600', 'SP0') // win7/win2k8r2 sp0
	  os = os.replaceAll('7601', 'SP1') // win7/win2k8r2 sp1
	  os = os.replaceAll('6000', 'SP0') // winvista/win2k8 sp0
	  os = os.replaceAll('6001', 'SP1') // winvista/win2k8 sp1
	  os = os.replaceAll('6002', 'SP2') // winvista/win2k8 sp2
	  os = os.replaceAll('3786', 'SP0') // win2k3 sp0?
	  os = os.replaceAll('3787', 'SP1') // win2k3 sp1?
	  os = os.replaceAll('3788', 'SP0') // win2k3r2 sp0?
	  os = os.replaceAll('3789', 'SP1') // win2k3r2 sp1?
	  os = os.replaceAll('3790', 'SP2') // win2k3r2 sp2
	  os = os.replaceAll('2600', 'SP3') // windows xp sp3
	  os = os.replaceAll('2599', 'SP2') // windows xp sp2?
	  //
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	  os = os.replaceAll('  ', ' ')
	
	  return os.trim()
	} // end static def os_short_name

	def os_name_short(ctx=null) {
		osname_short(ctx)
	}
	
	def osname_short(ctx=null) {
		return Host.os_short_name(osname(ctx))
	}
	
	def os_name(ctx=null) {
		osname(ctx)
	}

	def osname(ctx=null) {
		// returns the name, version and hardware architecture of the Host's operating system
		// ex: Windows 2008r2 SP1 x64
		if (!this._osname) {
			if (isPosix(ctx)) {
				this._osname = line('uname -om', ctx)
			} else {
				osname = systeminfo_line('OS Name', ctx)
				osname += ' '
				// get service pack too
				osname += systeminfo_line('OS Version', ctx)
				osname += ' '
				// and cpu arch
				osname += systeminfo_line('System Type', ctx) // x86 or x64
				this._osname = osname
				
			}
		}
		this._osname
	}

	def line_prefix(prefix, cmd, ctx) {
		// executes the given cmd, splits the STDOUT output by lines and then returns
		// only the (last) line that starts with the given prefix
		//
		line = line(cmd, ctx)
		if (line.startsWith(prefix)) {
			line = line.substring(prefix.length())
		}
		return line.strip()
	}

	def silence_stderr(str) {
		isPosix() ? "$str 2> /dev/null" : "$str 2> NUL"
	}
	
	def silence_stdout(str) {
		isPosix() ? "$str > /dev/null" : "$str 1> NUL"
	}
	
	def silence(str) {
		isPosix() ? "$str 1> /dev/null 2>&1" : "$str 1> NUL 2>&1"
	}
	
	def devnull() {
		isPosix() ? '/dev/null' : 'NUL'
	}

	def hasDebugger(ctx) {
		return debugger(ctx) != null
	}

	def debug_wrap(cmd_line, ctx) {
		def dbg = debugger(ctx)
		if (dbg) {
			if (isPosix()) {
				return dbg+" --args "+cmd_line
			} else if (isWindows()) {
			return dbg+" "+cmd_line
			}
		}
		return cmd_line
	}

	def debugger(ctx) {
		if (isPosix(ctx)) {
			if (exists('/usr/bin/gdb', ctx) || exists('/usr/local/gdb', ctx)) {
				return 'gdb'
			}
		} else if (isWindows(ctx)) {
			// windbg must be the x86 edition (not x64) because php is only compiled for x86
			// TODO use //programfiles !
			// LATER allow specifying which debugger to use (!php or x64 php)
			if (exists("%ProgramFiles(x86)%\\Debugging Tools For Windows (x86)\\windbg.exe", ctx))
				return '%ProgramFiles(x86)%\\Debugging Tools For Windows (x86)\\windbg.exe'
			else if (exists('%ProgramFiles(x86)%\\Debugging Tools For Windows\\windbg.exe', ctx))
				return '%ProgramFiles(x86)%\\Debugging Tools For Windows\\windbg.exe'
			else if (exists('%ProgramFiles%\\Debugging Tools For Windows (x86)\\windbg.exe', ctx))
				return '%ProgramFiles%\\Debugging Tools For Windows (x86)\\windbg.exe'
			else if (exists('%ProgramFiles%\\Debugging Tools For Windows (x86)\\windbg.exe', ctx))
				return '%ProgramFiles%\\Debugging Tools For Windows (x86)\\windbg.exe'
		}
		if (ctx) {
			// prompt context to get debugger for this host (or null)
			return ctx.find_debugger(this)
		} else {
			return null // signal that host has no debugger
		}
	}

//attr_accessor :manager // Host::Manager

def Host(opts={}) {
	// TODO
//
////set the opts as properties in the Test::Factor sense
////      opts.each_pair do |key,value|
////        property key => value
////        // LATER merge Test::Factor and host info (name, os_version)
////        // so both Test::Factor and host info can access each other
////      end
//
//// allow override what //name() returns
//// so at least that can be used even
//// when a host is not accessible
//if (opts.has_key(hostname)) {
//_name = opts[hostname]
//}
////
//
	dir_stack = []
	cwd = null
} // def initialize

def describe() {
//description ||= this.properties.values.join('-').downcase
}

	def shell(ctx=null) {
		// returns the name of the host's shell
		// ex: /bin/bash /bin/sh /bin/csh /bin/tcsh cmd.exe command.com
		if (isPosix(ctx)) { 
			return env_value('SHELL', ctx)
		} else {
			return basename(env_value('ComSpec', ctx))
		}
	}

	def which(cmd, ctx=null) {
		cmd_pw("which $cmd", "where $cmd", ctx).output.trim()
	}

	def hasCmd(cmd, ctx=null) {
		def w = which(cmd, ctx)
		return !w && w.length > 0
	}

	def exec_pw(posix_cmd, windows_cmd, ctx, opts={}) {
		if (isPosix(ctx)) {
			exec(posix_cmd, ctx, opts)
		} else {
			exec(windows_cmd, ctx, opts)
		}
	}

	def exec_ok(command, ctx, opts={}) {
		exec(command, ctx, opts).exit_code
	}

	def exec_pw_ok(posix_cmd, windows_cmd, ctx, opts={}) {
		if (isPosix()) {
			exec_ok(posix_cmd, ctx, opts)
		} else {
			exec_ok(windows_cmd, ctx, opts)
		}
	}

	class ExecHandle {
		def write_stdin(stdin_data) {
		}
		def read_stderr() {
		''
		}
		def read_stdout() {
		''
		}
		def has_stderr() {
			read_stdout.length > 0
		}
		def has_stdout() {
			read_stderr.length > 0
		}
	}

	// command:
	//   command line to run. program name and arguments to program as one string
	//   this may be a string or Tracing::Command::Expected
	//
	// options: a hash of
	//  :env
	//     keys and values of ENV must be strings (not ints, or anything else!)
	//  :binmode  true|false
	//     will set the binmode of STDOUT and STDERR. binmode=false on Windows may affect STDOUT or STDERR output
	//  :by_line  true|false
	//     if true, will read input line by line, otherwise, reads input by blocks
	//  :chdir
	//     the current directory of the command to run
	//  :debug   true|false
	//     runs the command with the host's debugger. if host has no debugger installed, command will be run normally
	//  :stdin_data   ''
	//     feeds given string to the commands Standard Input
	//  :null_output true|false
	//     if true, returns '' for both STDOUT and STDERR. (if host is remote, STDOUT and STDERR are not sent over
	//     the network)
	//  :max_len  0+ bytes   default=128 kilobytes (128*1024)
	//     maximum length of STDOUT or STDERR streams(limit for either, STDERR.length+STDOUT.length <= :max_len*2).
	//     0 = unlimited
	//  :timeout  0+ seconds  default=0 seconds
	//     maximum run time of process (in seconds)
	//     process will be sent SIGKILL if it is still running after that amount of time
	//  :success_exit_code int, or [int] array   default=0
	//     what exit code(s) defines success
	//     note: this is ignored if Command::Expected is used (which evaluates success internally)
	//  :ignore_failure true|false
	//     ignores exit code and always assumes command was successful unless
	//     there was an internal PFTT exception (ex: connection to host failed)
	//  :exec_handle true|false, default=false
	//     if true, returns a handle to the process which allows you to close(sigkill|sigterm|sigint) the process later.
	//     if true, all options (including :timeout) may be used except :success_exit_code and :ignore_failure, as interpretting exit status code
	//     will then be up to your own code, since you're controlling the process.
	// LATER :elevate and :sudo support for windows and posix
	// other options are silently ignored
	//
	//
	// returns once command has finished executing
	//
	// if command is a string returns array of 3 elements. 0=> STDOUT output as string 1=>STDERR 2=>command's exit code (0==success)
	// if command is a Command::Expected, returns an Command::Actual
	def exec(command, ctx, opts=[], Closure block=null) {
		_exec(false, command, opts, ctx, block)
	}
	
	// same as exec! except it returns immediately
	def exec_async(command, ctx, opts=[], Closure block=null) {
		_exec(true, command, opts, ctx, block)
	}

	def cmd_pw(posix_cmd, windows_cmd, ctx, opts={}) {
		if (isPosix(ctx))
			cmd(posix_cmd, ctx, opts)
		else
			cmd(windows_cmd, ctx, opts)
	}
	
	// executes command or program on the host
	//
	// can be a DOS command, Shell command or a program to run with options to pass to it
	//
	// some DOS commands (for Windows OSes) are not actual programs, but rather just commands
	// to the command processor(cmd.exe or command.com). those commands can't be run through
	// exec!( since exec! is only for actual programs).
	//
	def cmd(cmdline, ctx, opts={}) {
		if (isWindows(ctx)) {
			cmdline = "CMD /C $cmdline"
		}
		return exec(cmdline, ctx, opts)
	}

	// executes command using cmd! returning the output (STDOUT) from the command,
	// with the new line character(s) chomped off
	def line(cmdline, ctx=null) {
		lines(cmdline, ctx).split("\n")[0]
	}
	
	def lines(cmdline, ctx) {
		cmd(cmdline, ctx).output
	}
	
	def unquote_line(cmdline, ctx) {
		line(cmdline, ctx).replaceAll(/\"/, '')
	}
	
	def isLonghorn(ctx=null) {
		// checks if its a longhorn(Windows Vista/2008) or newer version of Windows
		// (longhorn added new stuff not available on win2003 and winxp)
		if (is_longhorn) {
			return is_longhorn
		}
		
		is_longhorn = nt_version(ctx) >= 6
		
		if (ctx) {
			is_longhorn = ctx.check_os_generation_detect(longhorn, is_longhorn)
		}
		
		return is_longhorn
	}

	def isWindows(ctx=null) {
		// returns true if this host is Windows OS
		if (is_windows) {
			return is_windows
		}
		if (posix) {
			return is_windows = false
		}
		
		// Windows will always have a C:\ even if the C:\ drive is not the systemdrive
		// posix doesn't have C: D: etc... drives
		is_windows = _exist("C:\\", ctx)
		
		if (ctx) { 
			// cool stuff: allow user to override OS detection
			is_windows = ctx.check_os_type_detect(windows, is_windows)
		}
		
		return is_windows
	}

	def isBSD(ctx=null) {
		osname(ctx).contains('BSD')
	}
	
	def isFreeBSD(ctx=null) {
		osname(ctx).contains('FreeBSD')
	}
	
	def isLinux(ctx=null) {
		osname(ctx).contains('Linux')
	}
	
	def isRedhat(ctx=null) {
		isLinux(ctx) and exist('/etc/redhat-release', ctx)
	}
	
	def isFedora(ctx=null) {
		isLinux(ctx) and exist('/etc/fedora-release', ctx)
	}
	
	def isDebian(ctx=null) {
		isLinux(ctx) and exist('/etc/debian_release', ctx)
	}
	
	def isUbuntu(ctx=null) {
		isLinux(ctx) and exist('/etc/lsb-release', ctx)
	}
	
	def isGentoo(ctx=null) {
		isLinux(ctx) and exist('/etc/gentoo-release', ctx)
	}
	
	def isSUSE(ctx=null) {
		isLinux(ctx) and exist('/etc/SUSE-release', ctx)
	}
	
	def isSolaris(ctx=null) {
		osname(ctx).contains('Solaris')
	}

//def utype? ctx=null
//if linux?(ctx)
//:linux
//elsif solaris?(ctx)
//:solaris
//elsif freebsd?(ctx)
//:freebsd
//else
//null
//end
//end

	def isPosix(ctx=null) {
		if (posix) {
			return posix
		}
		if (is_windows) {
			return posix = false
		}
			
		posix = _exist('/usr', ctx)
	
		if (ctx) {
			// TODO do this for utype too
			posix = ctx.check_os_type_detect(posix, posix)
		}
	
		return posix
	}

	def make_absolute(path) {
		if (isWindows()) {
			// support for Windows drive letters
			if (path.contains(':'))
				return path
		} else if (isPosix()) {
			if (path.startsWith("/"))
				return path
		}
		_make_absolute(path)
	}

	def exist(path, ctx=null) {
		path = format_path(path, ctx)
		path = make_absolute(path)
	
		if (ctx) {
			ctx.fs_op1(this, EFsOp.exist, path) { new_path ->
				return exist(new_path, ctx)
			}
		}
	
		_exist(path, ctx)
	}
	
	def exists(path, ctx=null) {
		exist(path, ctx)
	}

	def format_path(path, ctx=null) {
		if (isWindows())
			to_windows_path(path)
		else
			to_posix_path(path)
	}

//	def format_path! path, ctx=null
//	case
//	when windows?(ctx) then to_windows_path!(path)
//	else to_posix_path!(path)
//	end
//	end

	def pushd(path, ctx=null) {
		cd(path, ctx, no_clear=true)
		dir_stack.push(path)
	}

	def popd(ctx=null) {
		def popped = dir_stack.pop
		if (popped)
			cd(popped, ctx, no_clear=true)
	}

	def peekd() { 
		dir_stack.last
	}

	def separator(ctx=null) {
		isWindows(ctx) ? '\\' : '/'
	}

	def upload_if_not(local, remote, ctx) {
		if (exist(remote, ctx)) {
			return false
		} else {
			upload(local, remote, ctx)
			return true
		}
	}
	
	def delete_if(path, ctx) {
		if (exist(path, ctx)) {
			delete(path, ctx)
			true
		} else {
			false
		}
	}

	def delete(glob_or_path, ctx) {
		if (ctx) {
			ctx.fs_op1(this, EFsOp.delete, glob_or_path) { new_glob_or_path ->
				return delete(new_glob_or_path, ctx)
			}
		}

		glob_or_path = make_absolute(glob_or_path)

		if (!isSafe(glob_or_path))
			throw new IllegalArgumentException()

		if (directory(glob_or_path, ctx)) {
			if (isPosix(ctx))
				exec("rm -rf \"$glob_or_path\"", ctx)
			else
				exec("cmd /C rmdir /S /Q \"$glob_or_path\"", ctx)	
		} else {
			_delete(glob_or_path, ctx)
		}
	}

	def escape(str, quote=true) {
		if (isWindows()) {
			s = str.dup
//			if (quote) {
//				s.replace %Q{"//{s}"} unless s.replaceAll(/(["])/,'\\\\\1').null?
//			}
//			s.replaceAll(/[\^&|><]/,'^\\1')
			s
		} else {
			s
		}
	}

	def mkdir(path, ctx) {
		path = make_absolute(path)
		def parent = dirname(path)
		if (!directory(parent)) {
			mkdir(parent, ctx)
		}
		if (!directory(path)) {
			_mkdir(path, ctx)
		}
	}

	def mktmpdir(ctx, path=null, suffix='') {
		if (!path) {
			path = tempdir(ctx)
		}
		
		path = make_absolute(path)
		tries = 10
		try {
			dir = File.join( path, String.random(6)+suffix )
//			raise 'exists' if directory? dir
			mkdir(dir, ctx)
		} catch ( Exception ex ) {
//			retry if (tries -= 1) > 0
			throw ex
		}
		dir
	}

	def mktmpfile(suffix, ctx, content=null) {
		tries = 10
		try {
			path = File.join( tmpdir(ctx), String.random(6) + suffix )
		
//		raise 'exists' if exists?(path, ctx)
		
			if (content) {
				write(content, path, ctx)
			}
		
			return path
		} catch (Exception ex) {
//		retry if (tries -= 1) > 0
			throw ex
		}
	}

	def isSafe(path) {
		true
	//make_absolute! path
	//insane = case
	//when posix?
	///\A\/(bin|var|etc|dev|usr)\Z/
	//else
	///\A[A-Z]:(\/(Windows)?)?\Z/
	//end =~ path
	//!insane
	}

	def administrator_user(ctx=null) {
		if (isWindows(ctx)) {
			// LATER? should actually look this up??(b/c you can change it)
			return 'administrator'
		} else {
			return 'root'
		}
	}

	def name(ctx=null) {
		if (!_name) {
			// find a name that other hosts on the network will use to reference localhost
			if (isWindows(ctx))
				_name = env_value('COMPUTERNAME', ctx)
			else
				_name = env_value('HOSTNAME', ctx)
		}
		_name
	}

	protected def move_cmd(from, to, ctx) {
		from = no_trailing_slash(from)
		to = no_trailing_slash(to)
		if (isPosix(ctx)) {
			cmd("mv \"$from\" \"$to\"", ctx)
		} else {
			from = to_windows_path(from)
			to = to_windows_path(to)
			
			cmd("move \"$from\" \"$to\"", ctx)
		}
	}
	
	protected def copy_cmd(from, to, ctx) {
		if (isPosix(ctx)) {
			cmd("cp -R \"$from\" \"$to\"", ctx)
		} else {
			from = to_windows_path(from)
			to = to_windows_path(to)		
			cmd("xcopy /Y /s /i /q \"$from\" \"$to", ctx)
		}
	}

	protected def _exec(in_thread, command, opts, ctx, block) {
		opts = [env:[]] // TODO
		cwd = null // clear cwd cache

		if (opts.hasProperty('exec_handle') && !in_thread)
			opts.exec_handle = null

		def orig_cmd = command
// TODO		if (command instanceof Command) 
//			// get CmdString for this host
//			command = command.to_cmd_string(this)
			
		// for CmdString
		command = command.toString()
		// TODO      if command.is_a?(Tracing::Command::Expected)
		//        command = command.cmd_line
		//      end

		if (ctx) {
			ctx.cmd_exe_start(this, command, opts) { new_command ->
				return _exec(in_thread, new_command, opts, ctx, block)
			}
		}

		// begin preprocessing command and options 

		// if the program being run in this command (the part of the command before " ")
		// has / in the path, convert to \\  for Windows (or Windows might not be able to find the program otherwise)
		if (isWindows(ctx)) {
			def i
			if (command.startsWith('"'))
				i = command.index('"', 1)
			else
				i = command.index(' ', 1)
			if (i>0)
				command = to_windows_path(command(0, i)) + command.substring(i+1)
		}
		//

		//
		if (opts.hasProperty('null_output') && opts.null_output)
			command = silence(command)
		//
			
		if (!opts.hasProperty('env'))
			opts.env = false //[]

		if (opts.hasProperty('chdir') && opts.chdir) {
			// NOTE: chdir seems to be ignored|invalid on Windows(even Win7) if / is NOT converted to \ !!
			// convert the \ / to the correct for this host
			opts.chdir = format_path(opts.chdir, ctx)
		}

		if (!opts.hasProperty('max_len') || opts.max_len < 0)
			opts.max_len = 128*1024
				
		// run command in platform debugger
		if (opts.hasProperty('debug') && opts.debug)
			command = debug_wrap(command)

		// end preprocessing command and options 

		def ret
		if (in_thread && !(opts.hasProperty('exec_handle') && opts.exec_handle)) {
			new Thread() {
				void run() {
					ret = _exec_thread(command, opts, ctx, block)
				}
			}.start()
		} else {
			ret = _exec_thread(command, opts, ctx, block)
		}

		if (opts.exec_handle)
			// ret is a ExecHandle (like LocalExecHandle or SshExecHandle)
			return ret
		else if (orig_cmd instanceof Command)
			// ret is a Command.Actual
			return ret
		else
			return ret//[ret[0], ret[1], ret[2]]
	} // def _exec

	def _exec_thread(command, opts, ctx, block) {
		def stdout, stderr, exit_code, ret=null
		try {
			ret = _exec_impl(command, opts, ctx, block)

			if (opts.hasProperty('exec_handle') && opts.exec_handle)
				return ret

			stdout = ret.output
			stderr = ret.error
			exit_code = ret.exit_code

			//
			// don't let output get too large
			if (opts.hasProperty('max_len') && opts.max_len > 0) {
				if (stdout.length() > opts.max_len)
					stdout = stdout.substring(0, opts.max_len)
				if (stderr.length() > opts.max_len)
					stderr = stderr.substring(0, opts.max_len)
			}
			//
	
			// execution done... evaluate and report success/failure
			if (ctx) {				
				//
				// decide if command was successful
				//
				def success = opts.hasProperty('ignore_failure') && opts.ignore_failure ? true : false
				if (!success) {
					// default evaluation
					success = 0 == exit_code
					if (command instanceof Command) {
						// custom evaluation
						//
						ret = command.createActual(command.cmd_line, stdout, stderr, exit_code)
						// exit_code => share with _exec
						success = ret.isSuccess()
	
					} else if (opts.hasProperty('success_exit_code')) {
						//
						if (opts.success_exit_code instanceof List) {
							// an array of succesful values
							//
							success = false // override exit_code==0 above
							for ( def sec : opts.success_exit_code ) {
								if (exit_code == sec) {
									success = true
									break
								}
							}
						} else if (opts.success_exit_code instanceof Integer) {
							// a single successful value
							success = exit_code == opts.success_exit_code
						}
					
					}
				}
				//
			
			// TODO         if success
			//            ctx.cmd_exe_success(this, command, opts, c_exit_code, stdout+stderr) do |command|
			//              return _exec_thread(command, opts, ctx, block)
			//            end
			//          else
			//            ctx.cmd_exe_failure(this, command, opts, c_exit_code, stdout+stderr) do |command|
			//              return _exec_thread(command, opts, ctx, block)
			//            end
			//          end
			
			} // end if (ctx)
	
		} catch ( Exception ex ) {
			ex.printStackTrace();
			// try to include host name (don't call #name() b/c that may exec! again which could fail)
        	stderr = command+" "+_name+" "+ex.getMessage()
        	exit_code = -253
			
			def sw = new java.io.StringWriter()
			def pw = new java.io.PrintWriter(sw)
			
			ex.printStrackTrace(pw)
			
			pw.flush()
			
			stderr += " "+sw.toString()

        	throw ex
		}
		// ret could be set to be a Command.Actual already. otherwise return STDOUT, STDERR, exit-code
		if (ret==null)
			ret = [output:stdout, error:stderr, exit_code:exit_code]
		return ret
	} // def _exec_thread
	
	static def exec_copy_stream(src, dst, type, lh, block, max_len) {
		def buf = new byte[128]
		def len = 0
		def total_len = 0
		
		try {
			while ( ( len = src.read(buf, 0, 128)) != -1 ) {
				dst.write(buf, 0, len)
		
				if (block) {
					lh.post(type, buf)
					block.call(lh)
				}
		
				if (max_len > 0) {
					total_len += len
					// when output limit reached, stop copying automatically
					if (total_len > max_len)
						break
				}
			}
		} finally {
			src.close()
		}
	} // end def exec_copy_stream

	static def exec_copy_lines(input, type, lh, block, max_len) {
		def o = ''
		
		input = new BufferedReader(input)
//		
//		while true do
//		try {
//		line = input.readLine()
//		} catch ( Ex)
//		break
//		end
//		
//		if line.null?
//		break
//		end
//		
//		line += "\n"
//		
//		o += line
//		
//		if block
//		lh.post(type, line)
//		block.call(lh)
//		end
//		
//		if max_len > 0
//		// when output limit reached, stop copying automatically
//		if o.length > max_len
//		break
//		end
//		end
//		end
//		begin
//		input.close
//		rescue
//		end
		
		o
	} // end def exec_copy_line

	// cache of information about the host (this info is only retrieved once from the actual host)
	protected def dir_stack, cwd, _systeminfo, _name, _osname, _systemdrive, _systemroot, posix, is_windows, _appdata, _appdata_local, _tempdir, _userprofile, _programfiles, _programfilesx86, _homedir

//def clone(clone)
//// copy host information cache to the clone
//// TODO clone._systeminfo = @_systeminfo
////      //clone.lock = @lock
////      clone._osname = @_osname
////      clone._systemroot = @_systemroot
////      clone._systeminfo = @_systeminfo
////      clone._systemdrive = @_systemdrive
////      clone.posix = @posix
////      clone.is_windows = @is_windows
////      clone._name = @_name
////      clone._appdata = @_appdata
////      clone._appdata_local = @_appdata_local
////      clone._tempdir = @_tempdir
////      clone._userprofile = @_userprofile
////      clone._programfiles = @_programfiles
////      clone._programfilesx86 = @_programfilesx86
////      clone._homedir = @_homedir
//clone
//end

	def systeminfo_line(target, ctx) {
		def out_err = systeminfo(ctx)
		
		out_err.split("\n").each { line ->
			if (line.startsWith("$target:")) {
				line = line.substr("$target:".length())

                return line.strip()
			}
		}
		return null
	}

} // end abstract class Host
