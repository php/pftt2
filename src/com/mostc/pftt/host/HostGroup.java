package com.mostc.pftt.host;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractTestPackRunner.TestPackRunnerThread;
import com.mostc.pftt.scenario.FileSystemScenario.IFileChooser;

/** Allows a group of Hosts to be used as a single host (in most cases).
 * 
 * @author Matt Ficken
 *
 */

public class HostGroup extends Host implements List<Host> {
	protected final List<Host> list;
	
	public HostGroup() {
		list = new LinkedList<Host>();
	}
	
	public HostGroup(int init_cap) {
		list = new ArrayList<Host>(init_cap);
	}
	
	public interface BooleanCollector {
		void collect(Host h, boolean v);
	}
	
	public interface StringCollector {
		void collect(Host h, String v);
	}
	
	public interface StringsCollector {
		void collect(Host h, String[] v);
	}
	
	public interface IntegerCollector {
		void collect(Host h, int v);
	}
	
	public interface LongCollector {
		void collect(Host h, long v);
	}
	
	@Override
	public boolean exec(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_post, Charset charset, String current_dir) throws IllegalStateException, Exception {
		for (Host h : this)
			if (!h.exec(cm, ctx_str, cmd, timeout_sec, env, stdin_post, charset, current_dir))
				return false;
		return !isEmpty();
	}
	public void exec(String cmd, int timeout_sec, BooleanCollector c) {
		exec(null, null, cmd, timeout_sec, c);
	}
	public void exec(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, BooleanCollector c) {
		exec(cm, ctx_str, cmd, timeout_sec, c);
	}
	public void exec(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_post, Charset charset, String current_dir, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.exec(cm, ctx_str, cmd, timeout_sec, env, stdin_post, charset, current_dir));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	
	@Override
	public boolean exec(ConsoleManager cm, String ctx_str, String commandline, int timeout, Map<String, String> env, byte[] stdin, Charset charset, String chdir, TestPackRunnerThread thread, int thread_slow_sec) throws Exception {
		for (Host h : this)
			if (!h.exec(cm, ctx_str, commandline, timeout, env, stdin, charset, chdir, thread, thread_slow_sec))
				return false;
		return !isEmpty();
	}
	
	@Override
	public boolean execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread test_thread, int slow_timeout_sec) throws Exception {
		for (Host h : this)
			if (!h.execElevated(cm, ctx_str, cmd, timeout_sec, env, stdin_data, charset, chdir, test_thread, slow_timeout_sec))
				return false;
		return !isEmpty();
	}
	public void execElevated(String cmd, int timeout_sec, BooleanCollector c) {
		execElevated(null, null, cmd, timeout_sec, c);
	}
	public void execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, BooleanCollector c) {
		execElevated(cm, ctx_str, cmd, timeout_sec, c);
	}
	public void execElevated(ConsoleManager cm, String ctx_str, String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String chdir, TestPackRunnerThread test_thread, int slow_timeout_sec, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.execElevated(cm, ctx_str, cmd, timeout_sec, env, stdin_data, charset, chdir, test_thread, slow_timeout_sec));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	
	@Override
	public boolean isClosed() {
		for (Host h : this) {
			if (!h.isClosed())
				return false;
		}
		return !isEmpty();
	}
	public void isClosed(BooleanCollector c) {
		for ( Host h : this )
			c.collect(h, h.isClosed());
	}
	public HostGroup getIsClosed() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isClosed())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotClosed() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isClosed())
				g.add(h);
		}
		return g;
	}
	@Override
	public boolean cmd(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String current_dir) throws IllegalStateException, Exception {
		for (Host h : this)
			if (!h.cmd(cmd, timeout_sec, env, stdin_data, charset, current_dir))
				return false;
		return !isEmpty();
	}
	public void cmd(String cmd, int timeout_sec, BooleanCollector c) {
		cmd(cmd, timeout_sec, c);
	}
	public void cmd(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String current_dir, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.cmd(cmd, timeout_sec, env, stdin_data, charset, current_dir));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public boolean cmdElevated(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String current_dir) throws IllegalStateException, Exception {
		for (Host h : this)
			if (!h.cmdElevated(cmd, timeout_sec, env, stdin_data, charset, current_dir))
				return false;
		return !isEmpty();
	}
	public void cmdElevated(String cmd, int timeout_sec, BooleanCollector c) {
		cmdElevated(cmd, timeout_sec, c);
	}
	public void cmdElevated(String cmd, int timeout_sec, Map<String, String> env, byte[] stdin_data, Charset charset, String current_dir, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.cmdElevated(cmd, timeout_sec, env, stdin_data, charset, current_dir));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public boolean mSaveTextFile(String path, String string) throws IllegalStateException, IOException {
		for (Host h : this) {
			if (!h.mSaveTextFile(path, string))
				return false;
		}
		return true;
	}
	public void saveTextFile(String path, String string, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.mSaveTextFile(path, string));
			} catch ( Throwable t ) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public boolean mSaveTextFile(String filename, String text, CharsetEncoder ce) throws IllegalStateException, IOException {
		for (Host h : this) {
			if (!h.mSaveTextFile(filename, text, ce))
				return false;
		}
		return true;
	}
	public void saveTextFile(String filename, String text, CharsetEncoder ce, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.mSaveTextFile(filename, text, ce));
			} catch ( Throwable t ) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public boolean mDelete(String file) throws IllegalStateException, IOException {
		for (Host h : this) {
			if (!h.mDelete(file))
				return false;
		}
		return true;
	}
	public void delete(String file, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.mDelete(file));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public boolean mDeleteElevated(String file) throws IllegalStateException, IOException {
		for (Host h : this) {
			if (!h.mDeleteElevated(file))
				return false;
		}
		return true;
	}
	public void deleteElevated(String file, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.mDeleteElevated(file));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public boolean mCopy(String src, String dst) throws IllegalStateException, Exception {
		for (Host h : this) {
			if (!h.mCopy(src, dst))
				return false;
		}
		return true;
	}
	public void copy(String src, String dst, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.mCopy(src, dst));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public boolean mCopyElevated(String src, String dst) throws IllegalStateException, Exception {
		for (Host h : this) {
			if (!h.mCopyElevated(src, dst))
				return false;
		}
		return true;
	}
	public void copyElevated(String src, String dst, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.mCopyElevated(src, dst));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public boolean mMove(String src, String dst) throws IllegalStateException, Exception {
		for (Host h : this) {
			if (!h.mMove(src, dst))
				return false;
		}
		return true;
	}
	public void move(String src, String dst, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.mMove(src, dst));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public boolean mMoveElevated(String src, String dst) throws IllegalStateException, Exception {
		for (Host h : this) {
			if (!h.mMoveElevated(src, dst))
				return false;
		}
		return true;
	}
	public void moveElevated(String src, String dst, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.mMoveElevated(src, dst));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public String mDirSeparator() {
		for (Host h : this)
			return h.mDirSeparator();
		return null;
	}
	public void dirSeperator(StringCollector c) {
		for (Host h : this)
			c.collect(h, h.mDirSeparator());
	}
	@Override
	public boolean isWindows() {
		for (Host h : this) {
			if (!h.isWindows())
				return false;
		}
		return !isEmpty();
	}
	public void isWindows(BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.isWindows());
	}
	public HostGroup getIsWindows() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isWindows())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotWindows() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isWindows())
				g.add(h);
		}
		return g;
	}
	@Override
	public boolean isVistaExact() {
		for (Host h : this) {
			if (!h.isVistaExact())
				return false;
		}
		return !isEmpty();
	}
	public void isVistaExact(BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.isVistaExact());
	}
	public HostGroup getIsVistaExact() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isVistaExact())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotVistaExact() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isVistaExact())
				g.add(h);
		}
		return g;
	}
	@Override
	public String getSystemDrive() {
		for (Host h : this)
			return h.getSystemDrive();
		return null;
	}
	public void getSystemDrive(StringCollector c) {
		for (Host h : this)
			c.collect(h, h.getSystemDrive());
	}
	@Override
	public boolean mIsDirectory(String dir) {
		for (Host h : this) {
			if (!h.mIsDirectory(dir))
				return false;
		}
		return !isEmpty();
	}
	public void isDirectory(String dir, BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.mIsDirectory(dir));
	}
	public HostGroup getIsDirectory(String dir) {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.mIsDirectory(dir))
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotDirectory(String dir) {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.mIsDirectory(dir))
				g.add(h);
		}
		return g;
	}
	@Override
	public String getTempDir() {
		for (Host h : this)
			return h.getTempDir();
		return null;
	}
	@Override
	public boolean mExists(String file) {
		for (Host h : this) {
			if (!h.mExists(file))
				return false;
		}
		return !isEmpty();
	}
	public void exists(String file, BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.mExists(file));
	}
	public HostGroup getExists(String file) {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.mExists(file))
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotExists(String file) {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.mExists(file))
				g.add(h);
		}
		return g;
	}
	@Override
	public boolean mCreateDirs(String path) throws IllegalStateException, IOException {
		for (Host h : this) {
			if (!h.mCreateDirs(path))
				return false;
		}
		return true;
	}
	public void mkdirs(String path, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.mCreateDirs(path));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public String mPathsSeparator() {
		for (Host h : this)
			return h.mPathsSeparator();
		return null;
	}
	public void pathsSeparator(StringCollector c) {
		for (Host h : this)
			c.collect(h, h.mPathsSeparator());
	}
	@Override
	public boolean isRemote() {
		for (Host h : this) {
			if (!h.isRemote())
				return false;
		}
		return !isEmpty();
	}
	public void isRemote(BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.isRemote());
	}
	public HostGroup getIsRemote() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isRemote())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotRemote() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isRemote())
				g.add(h);
		}
		return g;
	}
	@Override
	public boolean isOpen() {
		for (Host h : this) {
			if (!h.isOpen())
				return false;
		}
		return !isEmpty();
	}
	public void isOpen(BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.isOpen());
	}
	public HostGroup getIsOpen() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isOpen())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotOpen() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isOpen())
				g.add(h);
		}
		return g;
	}
	@Override
	public boolean upload(String local_file, String remote_file) throws IllegalStateException, IOException, Exception {
		for (Host h : this) {
			if (!h.upload(local_file, remote_file))
				return false;
		}
		return true;
	}
	public void upload(String local_file, String remote_file, BooleanCollector c) {
		for (Host h : this) {
			try {
				c.collect(h, h.upload(local_file, remote_file));
			} catch (Throwable t) {
				c.collect(h, false);
			}
		}
	}
	@Override
	public String getSystemRoot() {
		for (Host h : this)
			return h.getSystemRoot();
		return null;
	}
	public void getSystemRoot(StringCollector c) {
		for (Host h : this)
			c.collect(h, h.getSystemRoot());
	}
	@Override
	public boolean mDirContainsExact(String path, String name) {
		for (Host h : this) {
			if (!h.mDirContainsExact(path, name))
				return false;
		}
		return !isEmpty();
	}
	public void dirContainsExact(String path, String name, BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.mDirContainsExact(path, name));
	}
	public HostGroup getIsDirContainsExact(String path, String name) {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.mDirContainsExact(path, name))
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotDirContainsExact(String path, String name) {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.mDirContainsExact(path, name))
				g.add(h);
		}
		return g;
	}
	@Override
	public String[] mList(String path) {
		for (Host h : this)
			return h.mList(path);
		return null;
	}
	public void list(String path, StringsCollector c) {
		for (Host h : this)
			c.collect(h, h.mList(path));
	}
	@Override
	public boolean mDirContainsFragment(String path, String name) {
		for (Host h : this) {
			if (!h.mDirContainsFragment(path, name))
				return false;
		}
		return !isEmpty();
	}
	public void dirContainsFragment(String path, String name, BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.mDirContainsFragment(path, name));
	}
	public HostGroup getIsDirContainsFragment(String path, String name) {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.mDirContainsFragment(path, name))
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotDirContainsFragment(String path, String name) {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.mDirContainsFragment(path, name))
				g.add(h);
		}
		return g;
	}
	@Override
	public String getUsername() {
		for (Host h : this)
			return h.getUsername();
		return null;
	}
	public void getUsername(StringCollector c) {
		for (Host h : this)
			c.collect(h, h.getUsername());
	}
	@Override
	public String getHomeDir() {
		for (Host h : this)
			return h.getHomeDir();
		return null;
	}
	public void getHomeDir(StringCollector c) {
		for (Host h : this)
			c.collect(h, h.getHomeDir());
	}
	@Override
	public String getJobWorkDir() {
		for (Host h : this)
			return h.getJobWorkDir();
		return null;
	}
	public void getPhpSdkDir(StringCollector c) {
		for (Host h : this)
			c.collect(h, h.getJobWorkDir());
	}
	@Override
	public String getPfttDir() {
		for (Host h : this)
			return h.getPfttDir();
		return null;
	}
	public void getPfttDir(StringCollector c) {
		for (Host h : this)
			c.collect(h, h.getPfttDir());
	}
	@Override
	public boolean unzip(ConsoleManager cm, String zip_file, String app_dir) {
		for (Host h : this)
			if (!h.unzip(cm, zip_file, app_dir))
				return false;
		return !isEmpty();
	}
	@Override
	public boolean isVistaOrBefore() {
		for (Host h : this) {
			if (!h.isVistaOrBefore())
				return false;
		}
		return !isEmpty();
	}
	public void isVistaOrBefore(BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.isVistaOrBefore());
	}
	public HostGroup getIsVistaOrBefore() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isVistaOrBefore())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotVistaOrBefore() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isVistaOrBefore())
				g.add(h);
		}
		return g;
	}
	@Override
	public boolean isBeforeVista() {
		for (Host h : this) {
			if (!h.isBeforeVista())
				return false;
		}
		return !isEmpty();
	}
	public void isBeforeVista(BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.isBeforeVista());
	}
	public HostGroup getIsBeforeVista() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isBeforeVista())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotBeforeVista() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isBeforeVista())
				g.add(h);
		}
		return g;
	}
	@Override
	public boolean isVistaOrLater() {
		for (Host h : this) {
			if (!h.isVistaOrLater())
				return false;
		}
		return !isEmpty();
	}
	public void isVistaOrLater(BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.isVistaOrLater());
	}
	public HostGroup getIsVistaOrLater() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isVistaOrLater())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotVistaOrLater() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isVistaOrLater())
				g.add(h);
		}
		return g;
	}
	@Override
	public boolean isWin8Exact() {
		for (Host h : this) {
			if (!h.isWin8Exact())
				return false;
		}
		return !isEmpty();
	}
	public void isWin8Exact(BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.isWin8Exact());
	}
	public HostGroup getIsWin8Exact() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isWin8Exact())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotWin8Exact() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isWin8Exact())
				g.add(h);
		}
		return g;
	}
	@Override
	public boolean isWin8OrLater() {
		for (Host h : this) {
			if (!h.isWin8OrLater())
				return false;
		}
		return !isEmpty();
	}
	public void isWin8OrLater(BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.isWin8OrLater());
	}
	public HostGroup getIsWin8OrLater() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isWin8OrLater())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotWin8OrLater() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isWin8OrLater())
				g.add(h);
		}
		return g;
	}
	/** returns average amount of total physical memory for hosts in this group
	 * 
	 */
	@Override
	public long getTotalPhysicalMemoryK() {
		return getTotalPhysicalMemoryTotalK() / size();
	}
	public long getTotalPhysicalMemoryTotalK() {
		long total = 0;
		for (Host h : this)
			total += h.getTotalPhysicalMemoryK();
		return total;
	}
	public void getTotalPhysicalMemoryK(LongCollector c) {
		for (Host h : this)
			c.collect(h, h.getTotalPhysicalMemoryK());
	}
	@Override
	public long mSize(String file) {
		for (Host h : this)
			return h.mSize(file);
		return 0L;
	}
	public void getSize(String file, LongCollector c) {
		for (Host h : this)
			c.collect(h, h.mSize(file));
	}	
	@Override
	public long mMTime(String file) {
		for (Host h : this)
			return h.mMTime(file);
		return 0L;
	}
	public void getMTime(String file, LongCollector c) {
		for (Host h : this)
			c.collect(h, h.mMTime(file));
	}
	@Override
	public String joinMultiplePaths(String... paths) {
		for (Host h : this)
			return h.joinMultiplePaths(paths);
		return null;
	}
	@Override
	public String joinMultiplePaths(List<String> paths, String... paths2) {
		for (Host h : this)
			return h.joinMultiplePaths(paths, paths2);
		return null;
	}
	@Override
	public boolean isX64() {
		for (Host h : this) {
			if (!h.isX64())
				return false;
		}
		return !isEmpty();
	}
	public void isX64(BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.isX64());
	}
	public HostGroup getIsX64() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.isX64())
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotX64() {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.isX64())
				g.add(h);
		}
		return g;
	}
	@Override
	public boolean hasCmd(String cmd) {
		for (Host h : this) {
			if (!h.hasCmd(cmd))
				return false;
		}
		return !isEmpty();
	}
	public void hasCmd(String cmd, BooleanCollector c) {
		for (Host h : this)
			c.collect(h, h.hasCmd(cmd));
	}
	public HostGroup getHasCmd(String cmd) {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (h.hasCmd(cmd))
				g.add(h);
		}
		return g;
	}
	public HostGroup getNotHasCmd(String cmd) {
		HostGroup g = new HostGroup(size());
		for ( Host h : this ) {
			if (!h.hasCmd(cmd))
				g.add(h);
		}
		return g;
	}
	/** returns average number of CPUs for hosts in this group
	 * 
	 */
	@Override
	public int getCPUCount() {
		return getCPUCountTotal() / size();
	}
	public int getCPUCountTotal() {
		int total = 0;
		for (Host h : this)
			total += h.getCPUCount();
		return Math.max(total, size());
	}
	public void getCPUCount(IntegerCollector c) {
		for (Host h : this)
			c.collect(h, h.getCPUCount());
	}
	
	/* -- begin List proxy impl -- */
	@Override
	public boolean add(Host e) {
		return list.add(e);
	}
	@Override
	public void add(int index, Host element) {
		list.add(index, element);
	}
	@Override
	public boolean addAll(Collection<? extends Host> c) {
		return list.addAll(c);
	}
	@Override
	public boolean addAll(int index, Collection<? extends Host> c) {
		return list.addAll(index, c);
	}
	@Override
	public void clear() {
		list.clear();
	}
	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}
	@Override
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}
	@Override
	public Host get(int index) {
		return list.get(index);
	}
	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}
	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}
	@Override
	public Iterator<Host> iterator() {
		return list.iterator();
	}
	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}
	@Override
	public ListIterator<Host> listIterator() {
		return list.listIterator();
	}
	@Override
	public ListIterator<Host> listIterator(int index) {
		return list.listIterator(index);
	}
	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}
	@Override
	public Host remove(int index) {
		return list.remove(index);
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}
	@Override
	public Host set(int index, Host element) {
		return list.set(index, element);
	}
	@Override
	public int size() {
		return list.size();
	}
	@Override
	public List<Host> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}
	@Override
	public Object[] toArray() {
		return list.toArray();
	}
	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}
	@Override
	public int hashCode() {
		return list.hashCode();
	}
	@Override
	public String toString() {
		return list.toString();
	}
	@Override
	public boolean equals(Object o) {
		return o==this||list.equals(o);
	}
	@Override
	public boolean mDeleteIfExists(String string) {
		for ( Host h : this ) {
			if (h.mDeleteIfExists(string))
				return true;
		}
		return false;
	}
	@Override
	public boolean mDeleteIfExistsElevated(String string) {
		for ( Host h : this ) {
			if (h.mDeleteIfExistsElevated(string))
				return true;
		}
		return false;
	}
	@Override
	public String fixPath(String src_path) {
		for ( Host h : this ) {
			return h.fixPath(src_path);
		}
		return null;
	}
	@Override
	public String mCreateTempName(Class<?> class1, String string) {
		for ( Host h : this ) {
			return h.mCreateTempName(class1, string);
		}
		return null;
	}
	@Override
	public String mCreateTempName(Class<?> class1) {
		for ( Host h : this ) {
			return h.mCreateTempName(class1);
		}
		return null;
	}
	@Override
	public String mCreateTempName(String string, String string2) {
		for ( Host h : this ) {
			return h.mCreateTempName(string, string2);
		}
		return null;
	}
	@Override
	public String mCreateTempName(String temp_file_ctx) {
		for ( Host h : this ) {
			return h.mCreateTempName(temp_file_ctx);
		}
		return null;
	}
	@Override
	public boolean mDeleteFileExtension(String dir, String ext) {
		for ( Host h : this ) {
			if (h.mDeleteFileExtension(dir, ext))
				return true;
		}
		return false;
	}
	@Override
	public boolean mDeleteChosenFiles(String dir, IFileChooser chr) {
		for ( Host h : this ) {
			if (h.mDeleteChosenFiles(dir, chr))
				return true;
		}
		return false;
	}
	/* -- end List proxy impl -- */
	
	// synonym
	public int count() {
		return size();
	}
	
} // end public class HostGroup
