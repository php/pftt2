package com.mostc.pftt.host;

public class TempFileExecOutput extends ExecOutput {
	public String temp_file;
	
	public TempFileExecOutput() {
		
	}
	
	public TempFileExecOutput(String temp_file, ExecOutput eo) {
		this.charset = eo.charset;
		this.exit_code = eo.exit_code;
		this.output = eo.output;
		this.temp_file = temp_file;
	}

	public boolean cleanupIfSuccess(AHost host) {
		if (isSuccess()) {
			cleanup(host);
			return true;
		} else {
			return false;
		}
	}
	
	public void cleanup(AHost host) {
		try {
			host.delete(temp_file);
		} catch ( Exception ex ) {}
	}
}
