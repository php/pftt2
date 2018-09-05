package com.mostc.pftt.scenario;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;

import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.mostc.pftt.host.RemoteHost;

public abstract class MountedRemoteFileSystemScenario extends RemoteFileSystemScenario {
	protected final RemoteHost remote_host;
	
	public MountedRemoteFileSystemScenario(RemoteHost remote_host) {
		this.remote_host = remote_host;
	}
	
	@Override
	public boolean isUACRequiredForStart() {
		return true;
	}
	
	@Override
	public boolean isUACRequiredForSetup() {
		return true;
	}

	public RemoteHost getRemoteHost() {
		return remote_host;
	}

	@Override
	public boolean isDirectory(String path) {
		return remote_host.mIsDirectory(path);
	}

	@Override
	public boolean exists(String path) {
		return remote_host.mExists(path);
	}

	@Override
	public boolean createDirs(String path) throws IllegalStateException, IOException {
		return remote_host.mCreateDirs(path);
	}

	@Override
	public String getTempDir() {
		return remote_host.getTempDir();
	}

	@Override
	public boolean saveTextFile(String path, String string) throws IllegalStateException, IOException {
		return remote_host.mSaveTextFile(path, string);
	}

	@Override
	public boolean saveTextFile(String filename, String text, CharsetEncoder ce) throws IllegalStateException, IOException {
		return remote_host.mSaveTextFile(filename, text, ce);
	}

	@Override
	public boolean delete(String file) throws IllegalStateException, IOException {
		return remote_host.mDelete(file);
	}

	@Override
	public boolean deleteElevated(String file) throws IllegalStateException, IOException {
		return remote_host.mDeleteElevated(file);
	}

	@Override
	public boolean copy(String src, String dst) throws IllegalStateException, Exception {
		return remote_host.mCopy(src, dst);
	}

	@Override
	public boolean copyElevated(String src, String dst) throws IllegalStateException, Exception {
		return remote_host.mCopyElevated(src, dst);
	}

	@Override
	public boolean move(String src, String dst) throws IllegalStateException, Exception {
		return remote_host.mMove(src, dst);
	}

	@Override
	public boolean moveElevated(String src, String dst) throws IllegalStateException, Exception {
		return remote_host.mMoveElevated(src, dst);
	}

	@Override
	public String dirSeparator() {
		return remote_host.mDirSeparator();
	}
	
	@Override
	public String pathsSeparator() {
		return remote_host.mPathsSeparator();
	}

	@Override
	public boolean deleteChosenFiles(String dir, IFileChooser chr) {
		return remote_host.mDeleteChosenFiles(dir, chr);
	}

	@Override
	public String joinIntoOnePath(String... parts) {
		return remote_host.joinIntoOnePath(parts);
	}

	@Override
	public String joinIntoOnePath(List<String> parts) {
		return remote_host.joinIntoOnePath(parts);
	}

	@Override
	public boolean saveFile(String stdin_file, byte[] stdin_post) throws IllegalStateException, IOException {
		return remote_host.mSaveFile(stdin_file, stdin_post);
	}

	@Override
	public ByLineReader readFile(String file) throws FileNotFoundException, IOException {
		return remote_host.mReadFile(file);
	}

	@Override
	public ByLineReader readFile(String file, Charset cs) throws IllegalStateException, FileNotFoundException, IOException {
		return remote_host.mReadFile(file, cs);
	}

	@Override
	public ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException {
		return remote_host.mReadFileDetectCharset(file, cdd);
	}

	@Override
	public String getContents(String file) throws IOException {
		return remote_host.mGetContents(file);
	}

	@Override
	public String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException {
		return remote_host.mGetContentsDetectCharset(file, cdd);
	}

	@Override
	public boolean dirContainsExact(String path, String name) {
		return remote_host.mDirContainsExact(path, name);
	}

	@Override
	public boolean dirContainsFragment(String path, String name_fragment) {
		return remote_host.mDirContainsFragment(path, name_fragment);
	}

	@Override
	public String[] list(String path) {
		return remote_host.mList(path);
	}

	@Override
	public long getSize(String file) {
		return remote_host.mSize(file);
	}

	@Override
	public long getMTime(String file) {
		return remote_host.mMTime(file);
	}

	@Override
	public boolean isOpen() {
		return remote_host.isOpen();
	}

	@Override
	public String joinMultiplePaths(String... paths) {
		return remote_host.joinIntoMultiplePath(paths);
	}

	@Override
	public String joinMultiplePaths(List<String> paths, String... paths2) {
		return remote_host.joinIntoMultiplePath(paths, paths2);
	}

	@Override
	public String fixPath(String path) {
		return remote_host.fixPath(path);
	}
	
}

