package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.results.ConsoleManager;

/** 
 * 
 * @author Matt Ficken
 *
 */

public abstract class AbstractFileSystemScenario extends AbstractSerialScenario {
	
	public static AbstractFileSystemScenario getFileSystemScenario(ScenarioSet scenario_set) {
		return scenario_set.getScenario(AbstractFileSystemScenario.class, DEFAULT_FILESYSTEM_SCENARIO);
	}
	
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return AbstractFileSystemScenario.class;
	}
	
	public interface ITestPackStorageDir {
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
		boolean disposeIfEmpty(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack);
		
		/** disposes of the directory, deleting all files, shares, namespaces, etc... that
		 * back this StorageDirectory
		 * 
		 * @see #disposeIfEmpty
		 * @param cm
		 * @param local_host
		 * @param active_test_pack 
		 * @return
		 */
		boolean disposeForce(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack);
	} // end public interface ITestPackStorageDir
	
	public abstract ITestPackStorageDir createStorageDir(ConsoleManager cm, AHost host);
	
	/** checks if -phpt-in-place console option can be ignored or not
	 * 
	 * @return FALSE - ignore -phpt-in-place, TRUE to follow it (if present)
	 */
	public abstract boolean allowPhptInPlace();
	
} // end public abstract class AbstractFileSystemScenario
