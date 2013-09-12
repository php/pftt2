
import com.mostc.pftt.scenario.Scenario;

/** this file is automatically loaded if no configuration file is specified.
 * (if one or more configuration files are specified, this file is not loaded at all)
 * 
 */

def scenarios() {
	Arrays.asList(Scenario.getAllDefaultScenarios())
}
