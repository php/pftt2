package com.mostc.pftt.model.core;

import com.mostc.pftt.host.AHost;

public class PhpBuildInfo {
	protected final EBuildBranch build_branch;
	protected final String version_revision;
	protected final EBuildSourceType build_source_type;
	protected final EBuildType build_type;
	protected final ECompiler compiler;
	protected final ECPUArch cpu_arch;
	protected final EOS os;
	
	public PhpBuildInfo(EBuildBranch build_branch, String version_revision, EBuildType build_type, ECompiler compiler, ECPUArch cpu_arch, EOS os) {
		this(build_branch, version_revision, build_type, compiler, cpu_arch, EBuildSourceType.UNKNOWN, os);
	}
	
	public PhpBuildInfo(EBuildBranch build_branch, String version_revision, EBuildType build_type, ECompiler compiler, ECPUArch cpu_arch, EBuildSourceType build_source_type, EOS os) {
		this.build_branch = build_branch;
		this.version_revision = version_revision;
		this.build_source_type = build_source_type;
		this.build_type = build_type;
		this.compiler = compiler;
		this.cpu_arch = cpu_arch;
		this.os = os;
	}

	@Override
	public String toString() {
		return build_branch + "-" + toStringWithoutBuildBranch();
	}
	
	public String toStringWithoutBuildBranch() {
		StringBuilder sb = new StringBuilder(16);
		sb.append(version_revision);
		sb.append('-');
		sb.append(build_type);
		if (os!=EOS.WIN32) {
			// XXX argument specify if this should always be included
			//      -for cases where system is handling both linux and windows php build
			// XXX should compare to local host OS
			sb.append('-');
			sb.append(os);
		}
		sb.append('-');
		sb.append(cpu_arch);
		sb.append('-');
		sb.append(compiler);
		if (build_source_type!=null && build_source_type!=EBuildSourceType.WINDOWS_DOT_PHP_DOT_NET) {
			sb.append('-');
			sb.append(build_source_type);
		}
		return sb.toString();
	}
	
	private static boolean eq(Object a, Object b) {
		return (a==null&&b==null)||(a!=null&&b!=null&&a.equals(b));
	}
	
	public boolean equals(PhpBuildInfo o) {
		return eq(this.build_branch, o.build_branch) &&
				eq(this.version_revision, o.version_revision) &&
				eq(this.build_source_type, o.build_source_type) &&
				eq(this.build_type, o.build_type) &&
				eq(this.compiler, o.compiler) &&
				eq(this.cpu_arch, o.cpu_arch) &&
				eq(this.os, o.os);
	}
	
	private static int hc(Object o) {
		return o==null?0:o.hashCode();
	}
	
	@Override
	public int hashCode() {
		return hc(this.build_branch) &
				hc(this.version_revision) &
				hc(this.build_source_type) &
				hc(this.build_type) &
				hc(this.compiler) &
				hc(this.cpu_arch) &
				hc(this.os);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this)
			return true;
		else if (o instanceof PhpBuildInfo)
			return equals((PhpBuildInfo)o);
		else
			return false;
	}
	
	public EBuildBranch getBuildBranch() {
		return build_branch;
	}
	
	public String getVersionRevision() {
		return version_revision;
	}
	
	public EBuildSourceType getBuildSourceType() {
		return build_source_type;
	}
	
	public EBuildType getBuildType() {
		return build_type;
	}
	
	public ECompiler getCompiler() {
		return compiler;
	}
	
	public ECPUArch getCPUArch() {
		return cpu_arch;
	}
	
	public EOS getOS() {
		return os;
	}
	
	public boolean isSupported(AHost host) {
		if (os==EOS.WIN32 && host.isWindows() || os==EOS.LINUX && !host.isWindows()) {
			if (cpu_arch==ECPUArch.X64)
				return host.isX64();
			else
				return true;
		}
		return false;
	}
	
} // end public class PhpBuildInfo
