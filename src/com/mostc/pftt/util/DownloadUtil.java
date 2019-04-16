package com.mostc.pftt.util;

import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.URL;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
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

import com.github.mattficken.io.IOUtil;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

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
		String local_file_zip = host.mCreateTempName("Download", ".zip");
		
		if(!downloadFile(cm, remote_url, local_file_zip))
		{
			return false;
		}
		
		// decompress local_file_zip
		try {
			host.mCreateDirs(local_dir);
			
			System.out.println("PFTT: release_get: decompressing "+local_file_zip+"...");
			
			return host.unzip(cm, local_file_zip, local_dir);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, DownloadUtil.class, "downloadAndUnzip", ex, "");
			return false;
		}
	} // end public static boolean downloadAndUnzip

	public static boolean downloadFile(ConsoleManager cm, String remote_url, String local_file_name) {
		try {
			cm.println(EPrintType.CLUE, DownloadUtil.class, "Downloading from ["+remote_url.toString()+"] as ["+local_file_name+"]");
			return downloadFile(cm, new URL(remote_url), local_file_name);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, DownloadUtil.class, "downloadFile", ex, "");
			return false;
		}
	}
	
	private static final int MAX_REDIRECT = 5;
	/**
	 * @param cm
	 * @param remote_url
	 * @param local_file_name
	 */
	public static boolean downloadFile(ConsoleManager cm, URL remote_url, String local_file_name)
	{
		return downloadFile(cm, remote_url, local_file_name, 0);
	}
	
	private static boolean downloadFile(ConsoleManager cm, URL remote_url, String local_file_name, int redirect) {
		if(redirect > MAX_REDIRECT)
		{
			cm.println(EPrintType.CANT_CONTINUE, DownloadUtil.class, "Exceeding maximal redirects" + MAX_REDIRECT + " for downloading [" + remote_url + "]");
			return false;
		}
		
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
			
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if( statusCode != 200)
			{
				switch(statusCode)
				{
				// Handling redirects
				case 301: // 301 Moved Permanently
				case 302: // 302 Found or originally temporary redirect
				case 303: // 303 see other 
				case 307: // 307 temporary redirect
				case 308: // 308 permanent redirect
					Header[] locations = response.getHeaders("Location");
					for(int i = 0; i < locations.length; i++)
					{
						String redirectedUrl = locations[i].getValue();
						cm.println(EPrintType.CLUE, DownloadUtil.class, "Redirecting from ["+remote_url.toString()+"] to ["+redirectedUrl+"], redirect count =" + redirect++);
						if(downloadFile(cm, new URL(redirectedUrl), local_file_name, redirect))
						{
							return true;
						}
					}
					break;					
				}
				cm.println(EPrintType.CANT_CONTINUE, DownloadUtil.class, "Error downloading file from [" + remote_url + "] with status " + statusLine.toString());
				return false;
			}
			
			File local_file = new File(local_file_name);
			// create parent directories if not exists
			local_file.getParentFile().mkdirs();
			
			FileOutputStream out_file = new FileOutputStream(local_file_name);
			
			IOUtil.copy(response.getEntity().getContent(), out_file, IOUtil.UNLIMITED);
			
			out_file.close();
			return true;
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, DownloadUtil.class, "downloadFile", ex, "error downloading file: "+remote_url);
			return false;
		} finally {
			if ( response == null || !connStrategy.keepAlive(response, context)) {
				try {
					conn.close();
				} catch ( Exception ex ) {
					cm.addGlobalException(EPrintType.CANT_CONTINUE, DownloadUtil.class, "downloadFile", ex, "");
				}
			}
		}
	}

} // end public class DownloadUtil
