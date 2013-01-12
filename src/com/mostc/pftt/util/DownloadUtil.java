package com.mostc.pftt.util;

import java.io.FileOutputStream;
import java.net.Socket;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

public class DownloadUtil {

	public static boolean downloadAndUnzip(ConsoleManager cm, Host host, String remote_url, String local_dir) {
		try {
			return downloadAndUnzip(cm, host, new URL(remote_url), local_dir);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, DownloadUtil.class, "downloadAndUnzip", ex, "");
			return false;
		}
	}
	
	public static boolean downloadAndUnzip(ConsoleManager cm, Host host, URL remote_url, String local_dir) {
		String local_file_zip = host.mktempname("Download", ".zip");
		
		HttpParams params = new SyncBasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/ 20120405 Firefox/14.0.1");
		HttpProtocolParams.setUseExpectContinue(params, true);
		
		HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
		        // Required protocol interceptors
		        new RequestContent(),
		        new RequestTargetHost(),
		        // Recommended protocol interceptors
		        new RequestConnControl(),
		        new RequestUserAgent(),
		        new RequestExpectContinue()});
		
		HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
		
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(remote_url.getHost(), remote_url.getPort()==-1?80:remote_url.getPort());
		
		DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
		ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
		
		context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
		
		HttpResponse response = null;
		try {
			Socket socket = new Socket(http_host.getHostName(), http_host.getPort());
			conn.bind(socket, params);
			BasicHttpRequest request = new BasicHttpRequest("GET", remote_url.getPath());
			
			request.setParams(params);
			httpexecutor.preProcess(request, httpproc, context);
			response = httpexecutor.execute(request, conn, context);
			response.setParams(params);
			httpexecutor.postProcess(response, httpproc, context);
			
			FileOutputStream out_file = new FileOutputStream(local_file_zip);
			
			IOUtils.copy(response.getEntity().getContent(), out_file);
			
			out_file.close();
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, DownloadUtil.class, "downloadAndUnzip", ex, "error downloading file: "+remote_url);
			return false;
		} finally {
			if ( response == null || !connStrategy.keepAlive(response, context)) {
				try {
					conn.close();
				} catch ( Exception ex ) {
					cm.addGlobalException(EPrintType.CANT_CONTINUE, DownloadUtil.class, "downloadAndUnzip", ex, "");
				}
			}
		}
		
		// decompress local_file_zip
		try {
			host.mkdirs(local_dir);
			
			System.out.println("PFTT: release_get: decompressing "+local_file_zip+"...");
			
			// TODO c:\program files
			host.exec("\"C:\\Program Files\\7-Zip\\7z\" x "+local_file_zip, Host.FOUR_HOURS, local_dir).printOutputIfCrash(DownloadUtil.class.getSimpleName(), cm);
			
			return true;
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, DownloadUtil.class, "downloadAndUnzip", ex, "");
			return false;
		}
	} // end public static boolean downloadAndUnzip

} // end public class DownloadUtil
