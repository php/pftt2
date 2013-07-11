package com.mostc.pftt.results;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

public interface ISerializer {
	public void serial(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException;
}
