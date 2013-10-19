package com.mostc.pftt.model.core;

import java.io.File;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.DebugPack;

public class PhpDebugPack extends DebugPack {
	protected final String path;
	
	protected PhpDebugPack(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}

	public static PhpDebugPack open(AHost host, String path) {
		path = new File(path).getAbsolutePath();
		if (StringUtil.endsWithIC(path, ".zip")) {
			// automatically decompress
			String zip_file = path;
			path = host.uniqueNameFromBase(AHost.removeFileExt(path));
				
			if (!host.unzip(zip_file, path))
				return null;
		}
		return new PhpDebugPack(path);
	}

} // end public class PhpDebugPack
