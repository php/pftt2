package com.mostc.pftt.model.core;

import java.io.File;
import java.io.Writer;

import com.mostc.pftt.host.AHost;

public class PhpBuildInfo {
	protected EBuildBranch build_branch;
	protected String version_revision;
	protected EBuildSourceType build_source_type;
	protected EBuildType build_type;
	protected ECompiler compiler;
	protected ECPUArch cpu_arch;
	protected EOSType os_type; 
	 
	static PhpBuildInfo open(File file) {
		def root = new XmlSlurper().parse(file);
		for ( def e : root.depthFirst().findAll { it.name() == 'build_info' } ) {
			def bi = new PhpBuildInfo()
			bi.build_branch = EBuildBranch.valueOf(e['@build_branch'].text())
			bi.version_revision = e['@version_revision'].text()
			bi.build_source_type = EBuildSourceType.valueOf(e['@build_source_type'].text())
			bi.build_type = EBuildType.valueOf(e['@build_type'].text())
			bi.compiler = ECompiler.valueOf(e['@compiler'].text())
			bi.cpu_arch = ECPUArch.valueOf(e['@cpu_arch'].text())
			// TODO bi.os_type = EOSType.valueOf(e['@os_type'].text())
			return bi
		}
		return null
	}
	
	static void write(PhpBuildInfo bi, Writer w) {
		def xml = new groovy.xml.MarkupBuilder(w)
		xml.build_info(
				'build_branch': bi.build_branch,
				'version_revision': bi.version_revision,
				'build_source_type': bi.build_source_type,
				'build_type': bi.build_type,
				'compiler': bi.compiler,
				'cpu_arch': bi.cpu_arch,
				'os_type': bi.os_type
			)
	}
	
	private PhpBuildInfo() {
		
	}
	
	public PhpBuildInfo(EBuildBranch build_branch, String version_revision, EBuildType build_type, ECompiler compiler, ECPUArch cpu_arch, EOSType os) {
		this(build_branch, version_revision, build_type, compiler, cpu_arch, EBuildSourceType.UNKNOWN, os);
	}
	
	public PhpBuildInfo(EBuildBranch build_branch, String version_revision, EBuildType build_type, ECompiler compiler, ECPUArch cpu_arch, EBuildSourceType build_source_type, EOSType os) {
		this.build_branch = build_branch;
		this.version_revision = version_revision;
		this.build_source_type = build_source_type;
		this.build_type = build_type;
		this.compiler = compiler;
		this.cpu_arch = cpu_arch;
		this.os_type = os_type;
	}

	@Override
	public String toString() {
		return build_branch.toString() + "-" + toStringWithoutBuildBranch();
	}
	
	public String toStringWithoutBuildBranch() {
		StringBuilder sb = new StringBuilder(16);
		sb.append(version_revision);
		sb.append('-');
		sb.append(build_type);
		if (os_type!=EOSType.WIN32 && os_type!=null) {
			// XXX argument specify if this should always be included
			//      -for cases where system is handling both linux and windows php build
			// XXX should compare to local host OS
			sb.append('-');
			sb.append(os_type);
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
				eq(this.os_type, o.os_type);
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
				hc(this.os_type);
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
	
	public boolean isX64() {
		return cpu_arch == ECPUArch.X64;
	}
	
	public boolean isX86() {
		return cpu_arch == ECPUArch.X86;
	}
	
	public EOSType getOSType() {
		return os_type;
	}
	
	public boolean isSupported(AHost host) {
		if (os_type==EOSType.WIN32 && host.isWindows() || os_type==EOSType.LINUX && !host.isWindows()) {
			if (cpu_arch==ECPUArch.X64)
				return host.isX64();
			else
				return true;
		}
		return false;
	}
	
} // end public class PhpBuildInfo
