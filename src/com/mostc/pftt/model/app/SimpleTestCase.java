package com.mostc.pftt.model.app;

public class SimpleTestCase extends AppUnitTestCase {
	public static final int MAX_TEST_TIME_SECONDS = 60;
	protected final String class_name;
	
	public SimpleTestCase(String class_name, String rel_filename, String abs_filename) {
		super(rel_filename, abs_filename);
		this.class_name = class_name;
	}

	@Override
	public String getName() {
		return class_name + "(" + rel_filename + ")";
	}

}
