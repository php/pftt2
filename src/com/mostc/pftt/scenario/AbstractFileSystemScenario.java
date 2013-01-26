package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
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
	public Class<?> getSerialKey() {
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
		boolean notifyTestPackInstalled(ConsoleManager cm, Host local_host);
		
		/** returns local path to access storage directory.
		 * 
		 * this will be a drive that the storage dir is mounted on (if remote) or the actual location of the storage dir (if local)
		 * 
		 * @param local_host
		 * @return
		 */
		String getLocalPath(Host local_host);
		
		/** returns TRUE if storage dir deleted
		 * 
		 * @param cm
		 * @param host
		 * @return
		 */
		boolean delete(ConsoleManager cm, Host local_host);
	} // end public interface ITestPackStorageDir
	
	public abstract ITestPackStorageDir createStorageDir(ConsoleManager cm, Host host);
	
	/** checks if -phpt-in-place console option can be ignored or not
	 * 
	 * @return FALSE - ignore -phpt-in-place, TRUE to follow it (if present)
	 */
	public abstract boolean allowPhptInPlace();
	
} // end public abstract class AbstractFileSystemScenario
