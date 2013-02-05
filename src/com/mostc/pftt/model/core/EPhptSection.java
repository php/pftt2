package com.mostc.pftt.model.core;

import com.github.mattficken.io.StringUtil;

/** All valid sections a PHPT may contain.
 * 
 * Contains all the official documented sections and de-facto undocumented sections used by a few actual PHPTs.
 * 
 * @see http://qa.php.net/phpt_details.php
 * @author Matt Ficken
 *
 */

public enum EPhptSection {
	TEST {
		@Override
		public boolean validate(PhptTestCase test) {
			return test.containsSection(TEST);
		}
	},
	DESCRIPTION {
		@Override
		public String prepareSection(boolean keep_all, String text) {
			return keep_all?text:StringUtil.EMPTY;
		}
	},
	CREDITS {
		@Override
		public String prepareSection(boolean keep_all, String text) {
			return keep_all?text:StringUtil.EMPTY;
		}
	},
	/** not in documentation, de-facto */
	FAIL {
		@Override
		public String prepareSection(boolean keep_all, String text) {
			return keep_all?text:StringUtil.EMPTY;
		}
	},
	/** not in documentation, de-facto */
	CREDIT {
		@Override
		public String prepareSection(boolean keep_all, String text) {
			return keep_all?text:StringUtil.EMPTY;
		}
	},
	/** not in documentation, de-facto */
	UEXPECTF {
		@Override
		public String prepareSection(boolean keep_all, String text) {
			return keep_all?text:StringUtil.EMPTY;
		}
	},
	/** not in documentation, de-facto */
	DONE {
		@Override
		public String prepareSection(boolean keep_all, String text) {
			return keep_all?text:StringUtil.EMPTY;
		}
	},
	/** not in documentation, de-facto */
	COMMENT {
		@Override
		public String prepareSection(boolean keep_all, String text) {
			return keep_all?text:StringUtil.EMPTY;
		}
	},
	SKIPIF,
	REQUEST,
	POST {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(PUT, POST_RAW, GZIP_POST, DEFLATE_POST);
		}
	},
	PUT {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(POST, POST_RAW, GZIP_POST, DEFLATE_POST);
		}
	},
	POST_RAW {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(POST, PUT, GZIP_POST, DEFLATE_POST);
		}
	},
	GZIP_POST {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(POST, PUT, POST_RAW, DEFLATE_POST);
		}
	},
	DEFLATE_POST {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(POST, PUT, POST_RAW, GZIP_POST);
		}
	},
	GET,
	COOKIE,
	STDIN,
	INI,
	ARGS,
	ENV,
	FILE {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(FILEEOF, FILE_EXTERNAL, REDIRECTTEST);
		}
	},
	FILEEOF {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(FILE, FILE_EXTERNAL, REDIRECTTEST);
		}
	},
	FILE_EXTERNAL {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(FILE, FILEEOF, REDIRECTTEST);
		}
	}, 
	REDIRECTTEST {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(FILE, FILEEOF, FILE_EXTERNAL);
		}
	},
	HEADERS,
	CGI {
		@Override
		public String prepareSection(boolean keep_all, String text) {
			return keep_all?text:StringUtil.EMPTY;
		}
	},
	XFAIL {
		@Override
		public String prepareSection(boolean keep_all, String text) {
			return keep_all?text:StringUtil.EMPTY;
		}
	},
	EXPECTHEADERS,
	EXPECT {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(EXPECTF, EXPECTREGEX);
		}
		@Override
		public String prepareSection(boolean keep_all, String content) {
			return StringUtil.normalizeLineEnding(content);
		}
	},
	EXPECTF {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(EXPECT, EXPECTREGEX);
		}
		@Override
		public String prepareSection(boolean keep_all, String content) {
			return StringUtil.normalizeLineEnding(content);
		}
	},
	EXPECTREGEX {
		@Override
		public boolean validate(PhptTestCase test) {
			return !test.containsAnySection(EXPECT, EXPECTF);
		}
		@Override
		public String prepareSection(boolean keep_all, String content) {
			return StringUtil.normalizeLineEnding(content);
		}
	},
	CLEAN;
	
	/** prepares a section when it is read by PhptTestCase#load
	 * 
	 * Usually does either:
	 * -normalizes line endings @see StringUtil#normalizeLineEnding
	 * -replaces sections that aren't needed(ex: description) with an empty string (saves memory)
	 * 
	 * @param keep_all - default false, if true, don't replace a section with an empty string
	 * @param content
	 * @return
	 */
	public String prepareSection(boolean keep_all, String content) {
		return content;
	}
	
	public boolean validate(PhptTestCase test) {
		return true;
	}
	
} // end public static enum EPHPTSection
