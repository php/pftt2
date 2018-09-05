package com.mostc.pftt.puts;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;

public class Uploader {
	enum EFTPSSL {
		NO_SSL,
		REQUIRE_SSL,
		DETECT_SSL
	}
	static FTPClient configureClient(FTPSClient ftps) {
		ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
		return ftps;
	}
	public static void main(String[] args) throws IOException {
		EFTPSSL use_ssl = EFTPSSL.NO_SSL;
		FTPClient ftp;
		switch(use_ssl) {
		case REQUIRE_SSL:
			ftp = configureClient(new FTPSClient(true));
			break;
		case DETECT_SSL:
			ftp = configureClient(new FTPSClient(false));
			break;
		default:
			ftp = new FTPClient();
		}
		
		ftp.connect("131.107.220.66", 21);
		ftp.login("pftt", "1nter0pLAb!!");
		ftp.setFileType(FTP.BINARY_FILE_TYPE);
		ftp.mkd("/PFTT-Results/PHP_5_3/pathname");
		ftp.storeFile("/PFTT-Results/PHP_5_3/pathname/test.txt", new ByteArrayInputStream("test2".getBytes()));
		ftp.logout();
		ftp.disconnect();
	}
}
