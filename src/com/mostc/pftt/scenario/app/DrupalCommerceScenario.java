package com.mostc.pftt.scenario.app;

import com.mostc.pftt.scenario.app.DrupalScenario;

/** Drupal Commerce is used to build eCommerce websites and applications of all sizes. At 
 * its core it is lean and mean, enforcing strict development standards and leveraging the
 * greatest features of Drupal 7 and major modules like Views and Rules for maximum
 * flexibility.
 * 
 * @see http://drupal.org/project/commerce
 * 
 */

public class DrupalCommerceScenario extends DrupalScenario {

	@Override
	protected String getZipAppFileName() {
		return "drupal-commerce-7.x-1.4.zip";
	}
	
}
