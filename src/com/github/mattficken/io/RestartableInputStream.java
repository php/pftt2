package com.github.mattficken.io;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface RestartableInputStream {
	InputStream openInputStream() throws FileNotFoundException;
}
