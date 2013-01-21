package com.mostc.pftt.scenario.app;

/** Semantic MediaWiki (SMW) is a free, open-source extension to MediaWiki – the wiki software that powers
 * Wikipedia – that lets you store and query data within the wiki's pages.
 * 
 * Semantic MediaWiki is also a full-fledged framework, in conjunction with many spinoff extensions, that 
 * can turn a wiki into a powerful and flexible “collaborative database”. All data created within SMW can
 * easily be published via the Semantic Web, allowing other systems to use this data seamlessly.
 * 
 * @see http://semantic-mediawiki.org/
 * 
 */

public class SemanticMediaWikiScenario extends MediaWikiScenario {
	
	@Override
	protected String getZipAppFileName() {
		return "SemanticMediaWiki1.8.zip";
	}
	
}
