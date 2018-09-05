package com.mostc.pftt.scenario;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;

import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

public abstract class VFSRemoteFileSystemScenario extends RemoteFileSystemScenario {
	
	@Override
	public abstract boolean isDirectory(String string);

	@Override
	public abstract boolean exists(String string);
	
	@Override
	public abstract boolean createDirs(String path) throws IllegalStateException, IOException;
		
	@Override
	public abstract boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer);
	
	/** saves text in given file
	 * 
	 * @param filename
	 * @param text
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	@Override
	public abstract boolean saveTextFile(String path, String string) throws IllegalStateException, IOException;
	
	@Override
	public abstract boolean saveTextFile(String filename, String text, CharsetEncoder ce) throws IllegalStateException, IOException;

	@Override
	public abstract boolean delete(String file) throws IllegalStateException, IOException;
	
	@Override
	public boolean deleteElevated(String file) throws IllegalStateException, IOException {
		return delete(file);
	}
		
	@Override
	public abstract boolean copy(String src, String dst) throws IllegalStateException, Exception ;
	
	@Override
	public boolean copyElevated(String src, String dst) throws IllegalStateException, Exception {
		return copy(src, dst);
	}
	
	@Override
	public abstract boolean move(String src, String dst) throws IllegalStateException, Exception;
	
	@Override
	public boolean moveElevated(String src, String dst) throws IllegalStateException, Exception {
		return move(src, dst);
	}

	@Override
	public abstract String dirSeparator();
	
	@Override
	public abstract String readFileAsString(String path) throws IllegalStateException, FileNotFoundException, IOException;
	
	@Override
	public abstract boolean deleteChosenFiles(String dir, IFileChooser chr);

	@Override
	public abstract boolean saveFile(String stdin_file, byte[] stdin_post);
	
	@Override
	public abstract ByLineReader readFile(String file) throws FileNotFoundException, IOException;
	
	@Override
	public abstract ByLineReader readFile(String file, Charset cs) throws IllegalStateException, FileNotFoundException, IOException;
	
	@Override
	public abstract ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException;
	
	@Override
	public abstract String getContents(String file) throws IOException;
	
	@Override
	public abstract String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException;
	
	@Override
	public abstract String[] list(String path);
	
	@Override
	public abstract long getSize(String file);
	
	@Override
	public abstract long getMTime(String file);

	@Override
	public abstract boolean isOpen();
		
	@Override
	public String joinIntoOnePath(String ...parts) {
		if (parts==null||parts.length==0)
			return StringUtil.EMPTY;
		
		StringBuilder sb = new StringBuilder(Math.max(1024, parts[0].length()*2));
		sb.append(fixPath(parts[0]));
		for ( int i=1 ; i < parts.length ; i++ ) {
			if (parts[i]==null)
				continue;
			sb.append(dirSeparator());
			sb.append(fixPath(parts[i]));
		}
		return sb.toString();
	}
	
	@Override
	public String joinIntoOnePath(List<String> parts) {
		if (parts==null||parts.isEmpty())
			return StringUtil.EMPTY;
		
		StringBuilder sb = new StringBuilder(Math.max(1024, parts.get(0).length()*2));
		sb.append(fixPath(parts.get(0)));
		for ( int i=1 ; i < parts.size() ; i++ ) {
			sb.append(dirSeparator());
			sb.append(fixPath(parts.get(i)));
		}
		return sb.toString();
	}
		
	@Override
	public String joinMultiplePaths(String... paths) {
		return joinMultiplePaths(null, paths);
	}

	@Override
	public String joinMultiplePaths(List<String> paths, String... paths2) {
		StringBuilder sb = new StringBuilder();
		if (paths!=null&&paths.size()>0) {
			sb.append(fixPath(paths.get(0)));
			for ( int i=1 ; i < paths.size() ; i++ ) {
				sb.append(pathsSeparator());
				sb.append(fixPath(paths.get(i)));
			}
		}
		if (paths2!=null&&paths2.length>0) {
			sb.append(fixPath(paths2[0]));
			for ( int i=1 ; i < paths2.length ; i++ ) {
				sb.append(pathsSeparator());
				sb.append(fixPath(paths2[i]));
			}
		}
		return sb.toString();
	}
		
}
