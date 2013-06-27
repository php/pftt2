package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** 
 * 
 * @author Matt Ficken
 *
 */

public abstract class FileSystemScenario extends AbstractSerialScenario {
	
	public static FileSystemScenario getFileSystemScenario(ScenarioSet scenario_set) {
		return scenario_set.getScenario(FileSystemScenario.class, DEFAULT_FILESYSTEM_SCENARIO);
	}
	
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return FileSystemScenario.class;
	}
	
	public interface ITestPackStorageDir extends IScenarioSetup {
		/** called once test pack is copied/installed to storage dir.
		 * 
		 * returns FALSE if shouldn't proceed with using test-pack on storage dir
		 * 
		 * @param cm
		 * @param host
		 * @return
		 */
		boolean notifyTestPackInstalled(ConsoleManager cm, AHost local_host);
		
		/** returns local path to access storage directory.
		 * 
		 * this will be a drive that the storage dir is mounted on (if remote) or the actual location of the storage dir (if local)
		 * 
		 * @param local_host
		 * @return
		 */
		String getLocalPath(AHost local_host);
		
		/** returns remote path to access storage directory on the remote host.
		 * 
		 * for local file systems this will be the same as #getLocalPath.
		 * 
		 * for remote file systems, this is the path on the remote host (which will be different
		 * than local path #getLocalPath)
		 * 
		 * @param local_host
		 * @return
		 */
		String getRemotePath(AHost local_host);
		
		/** disposes of this StorageDirectory if its empty (if it has no files or folders)
		 * 
		 * 
		 * @see #disposeForce
		 * @param cm
		 * @param host
		 * @param active_test_pack
		 * @return
		 */
		boolean closeIfEmpty(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack);
		
		/** disposes of the directory, deleting all files, shares, namespaces, etc... that
		 * back this StorageDirectory
		 * 
		 * @see #disposeIfEmpty
		 * @param cm
		 * @param local_host
		 * @param active_test_pack 
		 * @return
		 */
		boolean closeForce(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack);
	} // end public interface ITestPackStorageDir
	
	public static abstract class AbstractTestPackStorageDir extends SimpleScenarioSetup implements ITestPackStorageDir {
		@Override
		public void close(ConsoleManager cm) {
			
		}
	}
	
	public abstract ITestPackStorageDir setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set);
	
	/** checks if -phpt-in-place console option can be ignored or not
	 * 
	 * @return FALSE - ignore -phpt-in-place, TRUE to follow it (if present)
	 */
	public abstract boolean allowPhptInPlace();
	
} // end public abstract class AbstractFileSystemScenario
