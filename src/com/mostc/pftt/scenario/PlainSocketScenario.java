package com.mostc.pftt.scenario;

/** placeholder scenario to indicate that SSLSocketScenario is not being used
 * 
 * @see SSLSocketScenario
 * @author Matt Ficken
 *
 */

public class PlainSocketScenario extends AbstractSocketScenario {

	@Override
	public String getName() {
		return "Plain-Socket";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

}
