package com.mostc.pftt.scenario;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.mattficken.io.ArrayUtil;
import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.NoCharsetByLineReader;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.EPrintType;

// https://manage.windowsazure.com
// http://portal.azure.com/
//

// https://github.com/projectkudu/kudu/wiki/REST-API#vfs
// Azure hosted deployment of: https://github.com/c9/vfs-http-adapter
//       NodeJS based?
//
// FTP will actually be slower if client doesn't support multiple simulateanous file uploads
// additionally VFS supports uploading a ZIP file, including the compression and decompression time this will be much faster than uploading lots of small files
//
// http://ostc-pftt01.azurewebsites.net/
// 
//
// https://ostc-pftt01.scm.azurewebsites.net
// https://ostc-pftt01.scm.azurewebsites.net/DebugConsole
// https://ostc-pftt01.scm.azurewebsites.net/api/vfs/<path>
//
// can't write to d:\
// can't files write to d:\home (homedir)
// can create dirs in homedir
// web root is d:\home\site\wwwroot
//
// remote vfs path = /site/  => d:\home\site
//
//
// Username: $ostc-pftt01
// Password: uHv4fwCeohk5RmQQ2cBrlw8p4mF5XvLAh2kQ08sshr7KsRiErBTKuy6owvnp
// 
// get from the 'Publish Profile' from azure
// @see https://github.com/projectkudu/kudu/wiki/Deployment-credentials
//

// Set Deployment user/password and copy exactly here
//   -may prefix username with $ ... probably removing $

public class AzureKuduVFSScenario extends VFSRemoteFileSystemScenario {
	DefaultHttpClient httpclient;
	String web_site_name;
	
	public AzureKuduVFSScenario() {
		web_site_name = //"matt-php-website";//
		"ostc-pftt01"; // TODO temp
		
        httpclient = new DefaultHttpClient(new PoolingClientConnectionManager());
        
        httpclient.getCredentialsProvider().setCredentials(
        		new AuthScope(web_site_name+".scm.azurewebsites.net", AuthScope.ANY_PORT),
        		new UsernamePasswordCredentials("ostc-pftt01", "8KwHqv98jjXgawMTmfhlsfunMq1h0B4CrTYqtxubZTsEi7w0wl5swHuASnyM")
        	);
	}
	
	public void close() {
		httpclient.getConnectionManager().shutdown();
	}
	
	@Override
	public boolean createDirs(String path) throws ClientProtocolException, IOException {
		// ensure path ends with /
		if (!path.endsWith("/"))
			path += "/";
		doPut(wwwrootURL(path), null, 0, null, null, true);
		return true;
	}
	
	@Override
	public boolean saveTextFile(String path, String content) throws ClientProtocolException, IOException {
		return saveFile(path, content.getBytes());
	}
	
	@Override
	public boolean saveFile(String path, byte[] content) {
		// ensure path does not end with / (Directory)
		if (path.endsWith("/"))
			path = path.substring(0, path.length()-1);
		try {
			doPut(wwwrootURL(path), null, 0, null, content, true);
			return true;
		} catch (ClientProtocolException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (IOException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		}
		return false;
	}
	
	/**
	 * 
	 * BN: Kudu seems to have problems with large zip files (~80MB) with lots of files (~60000)
	 * 		seems to stop decompression after ~15000-~20000 files (crash or safety feature?)
	 * 
	 * @param path
	 * @param local_zip_file
	 * @return
	 */
	public boolean putZip(String path, File local_zip_file) {
		if (true)
			return true; // TODO temp
		path = "/site/wwwroot/";//+local_zip_file.getName(); // TODO temp
		
		// ensure path does not end with / (Directory)
		if (path.endsWith("/"))
			path = path.substring(0, path.length()-1);
		try {
			// ensure destination dir exists
			// TODO temp createDirs(dirname(path));
			
			// TODO note don't send double // ?
			
			// @see https://github.com/projectkudu/kudu/wiki/REST-API
			
			String url = "https://"+web_site_name+".scm.azurewebsites.net/api/zip/site/wwwroot/";//+local_zip_file.getName();//FileSystemScenario.basename(path);
			
			//doPut(url, new BufferedInputStream(new FileInputStream(local_zip_file)), local_zip_file.length(), ContentType.create("application/zip"), null, true);
			
			byte[] bytes = IOUtil.toBytes(new FileInputStream(local_zip_file), (int) local_zip_file.length());
			
			doPut(url, null, 0, null, bytes, false);
			
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
		/*} catch (ClientProtocolException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (IOException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);*/
		}
		return false;
	}
	
	protected JSONArray doGetList(String path) throws ClientProtocolException, JSONException, IOException {
		// ensure path ends with /
		if (!path.endsWith("/"))
			path += "/";
		
		String str = doGetString(wwwrootURL(path));
		if (!StringUtil.startsWithIgnoreWhitespace(str, '['))
			return new JSONArray(); // empty dir? does not exist?
				
		return new JSONArray(str);
		
		// [{"name":"dir1","size":0,"mtime":"2015-03-11T06:23:18.2995535+00:00","mime":"inode/directory","href":"https://ostc-pftt01.scm.azurewebsites.net/api/vfs/site/wwwroot/dir1/","path":"D:\\home\\site\\wwwroot\\dir1"},{"name":"hostingstart.html","size":202392,"mtime":"2015-03-06T20:43:59.9415701+00:00","mime":"text/html","href":"https://ostc-pftt01.scm.azurewebsites.net/api/vfs/site/wwwroot/hostingstart.html","path":"D:\\home\\site\\wwwroot\\hostingstart.html"},{"name":"test_file.php","size":29,"mtime":"2015-03-11T05:23:41.562511+00:00","mime":"application/octet-stream","href":"https://ostc-pftt01.scm.azurewebsites.net/api/vfs/site/wwwroot/test_file.php","path":"D:\\home\\site\\wwwroot\\test_file.php"},{"name":"test_file2.php","size":40,"mtime":"2015-03-11T05:51:49.406879+00:00","mime":"application/octet-stream","href":"https://ostc-pftt01.scm.azurewebsites.net/api/vfs/site/wwwroot/test_file2.php","path":"D:\\home\\site\\wwwroot\\test_file2.php"},{"name":"test_file2.txt","size":40,"mtime":"2015-03-11T06:09:58.9862092+00:00","mime":"text/plain","href":"https://ostc-pftt01.scm.azurewebsites.net/api/vfs/site/wwwroot/test_file2.txt","path":"D:\\home\\site\\wwwroot\\test_file2.txt"}]
		
	}
	
	protected JSONObject doGetListFile(String file) throws ClientProtocolException, JSONException, IOException {
		String name = basename(file);
		JSONArray json = doGetList(dirname(file));
		
		for ( int i=0 ; i < json.length() ; i++ ) {
			if (json.getJSONObject(i).getString("name").equals(name)) {
				return json.getJSONObject(i);
			}
		}
		return null;
	}
	
	@Override
	public long getSize(String file) {
		try {
			return doGetListFile(file).getInt("size");
		} catch (IOException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (JSONException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		}
		return 0L;
	}

	@Override
	public long getMTime(String file) {
		try {
			return new Date(doGetListFile(file).getInt("mtime")).getTime();
		} catch (IOException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (JSONException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		}
		return 0L;
	}
	
	@Override
	public boolean dirContainsExact(String path, String name) {
		try {
			JSONArray json = doGetList(path);
			
			for ( int i=0 ; i < json.length() ; i++ ) {
				if (json.getJSONObject(i).getString("name").equals(name))
					return true;
			}
		} catch (IOException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (JSONException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		}
		return false;
	}

	@Override
	public boolean dirContainsFragment(String path, String name_fragment) {
		try {
			JSONArray json = doGetList(path);
			
			for ( int i=0 ; i < json.length() ; i++ ) {
				if (json.getJSONObject(i).getString("name").contains(name_fragment))
					return true;
			}
		} catch (IOException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (JSONException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		}
		return false;
	}
	
	@Override
	public String[] list(String path) {
		try {
			JSONArray list = doGetList(path);
			
			ArrayList<String> out = new ArrayList<String>(list.length());
			
			for ( int i=0 ; i < list.length() ; i++ ) {
				out.add(list.getJSONObject(i).getString("name"));
			}
			
			return ArrayUtil.toArray(out);
		} catch (IOException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (JSONException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		}
		return new String[]{};
	}
	
	@Override
	public boolean delete(String path) throws ClientProtocolException, IOException {
		doDelete(wwwrootURL(path));
		return true;
	}
	
	@Override
	public String getTempDir() {
		// the management URL for these files is https://...net/api/vfs/site/wwwroot/temp/<files and dirs>
		// the public URL for these files is http://...net/temp/<files and dirs>
		//
		// this assumes that all paths will pass through #wwwrootURL to add the /site/wwwroot/ etc... before
		// passing it to Kudu
		//
		// 
		return "/temp/";
	}
	
	protected String wwwrootURL(String path) {
		// can't write files to /
		// can create dirs in /
		return vfsURL("/site/wwwroot/" + path);
	}
	
	protected String vfsURL(String path) {
		return "https://"+web_site_name+".scm.azurewebsites.net/api/vfs/"+FileSystemScenario.toUnixPath(path);
	}
	
	protected void doPut(String url, InputStream is_content, long is_content_length, ContentType content_type, byte[] bytes_content, boolean ok_overwrite) throws ClientProtocolException, IOException {
		// NOTE: will create all the dirs in path if they don't exist already
		HttpPut request = new HttpPut(url);
		if (ok_overwrite) {
			// if file exists and this header isn't included, will get a 412 error and file will NOT be overwritten
			request.setHeader("If-Match", "*");
		}
        
        if (is_content!=null) {
        	request.setEntity(new InputStreamEntity(is_content, is_content_length, content_type));
        } else if (bytes_content!=null) {
        	request.setEntity(new ByteArrayEntity(bytes_content, content_type));
        }
        
        System.out.println("executing request" + request.getRequestLine());
        HttpResponse response = httpclient.execute(request);
        System.out.println("318");
        HttpEntity entity = response.getEntity();
        System.out.println("320");
        EntityUtils.consumeQuietly(entity);
        System.out.println("322");
        //System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        if (entity != null) {
            //System.out.println("Response content length: " + entity.getContentLength());
        	//if (response.getStatusLine().getStatusCode()==400) {
        		//new Exception().printStackTrace();
        		//System.exit(0);
        	//System.out.println(IOUtil.toString(entity.getContent(), 2000));
        	//}
        }
        //EntityUtils.consume(entity);
        request.releaseConnection();
	}
	
	protected void doDelete(String url) throws ClientProtocolException, IOException {
		HttpDelete request = new HttpDelete(url);
		// must include this header to delete a file
        request.setHeader("If-Match", "*");
                
        //System.out.println("executing request" + request.getRequestLine());
        HttpResponse response = httpclient.execute(request);
        HttpEntity entity = response.getEntity();
        
        EntityUtils.consumeQuietly(entity);

        //System.out.println("----------------------------------------");
        //System.out.println(response.getStatusLine());
        if (entity != null) {
            //System.out.println("Response content length: " + entity.getContentLength());
            //System.out.println(IOUtil.toString(entity.getContent(), 2000));
        	//if (response.getStatusLine().getStatusCode()==400) {
        		//new Exception().printStackTrace();
        		//System.exit(0);
        	//}
        }
        //EntityUtils.consume(entity);
        request.releaseConnection();
	}
	
	protected String doGetString(String url) throws ClientProtocolException, IOException {
		HttpGet request = null;
		String str;
		try {
			request = new HttpGet(url);
			                
	        //System.out.println("executing request" + request.getRequestLine());
	        HttpResponse response = httpclient.execute(request);
	        HttpEntity entity = response.getEntity();
	        
	        str = IOUtil.toString(entity.getContent(), IOUtil.HALF_MEGABYTE);	        
		} finally {
			if (request!=null)
				request.releaseConnection();
		}
        return str;
	}
	
	public static void main(String[] args) throws Exception {
		AzureKuduVFSScenario s = new AzureKuduVFSScenario();
		
		s.putZip("bin", new File("c:\\php-sdk\\php-test-pack-5.4.38.zip"));
		
		/*
		s.putZip("remote_test_pack_dir", new File("c:\\php-sdk\\php-test-pack-5.4.38.zip"));
		
		System.out.println(StringUtil.toString(s.list("/")));
		s.delete("/test_file2.php");
		System.out.println(StringUtil.toString(s.list("/")));
		s.saveTextFile("/test_file2.php", "<?php echo \"Hello World!\"; phpinfo(); ?>");
		s.createDirs("dir1");
		System.out.println(StringUtil.toString(s.list("/")));
		s.delete("dir1");
		System.out.println(StringUtil.toString(s.list("/")));
		*/
    }

	@Override
	public boolean isDirectory(String path) {
		try {
			return doGetList(path)!=null;
		} catch (ClientProtocolException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (JSONException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (IOException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		}
		return false;
	}

	@Override
	public boolean exists(String path) {
		try {
			return path.endsWith("/") ? doGetList(path)!=null : doGetList(dirname(path))!=null;
		} catch (ClientProtocolException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (JSONException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		} catch (IOException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
		}
		return false;
	}

	@Override
	public boolean saveTextFile(String filename, String text, CharsetEncoder ce) throws IllegalStateException, IOException {
		return saveTextFile(filename, text); // TODO
	}

	@Override
	public boolean deleteIfExists(String path) {
		try {
			return delete(path);
		} catch (IOException ex) {
			ConsoleManagerUtil.printStackTrace(AzureKuduVFSScenario.class, ex);
			return false;
		}
	}

	@Override
	public boolean copy(String src, String dst) throws IllegalStateException, Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean move(String src, String dst) throws IllegalStateException, Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String pathsSeparator() {
		// Important: this is used in generating the include path, etc... for PhpIni files uploaded to Azure Websites
		//            which is just a Windows VM ... use the same value as for Windows
		return ";"; 
	}
	
	@Override
	public String dirSeparator() {
		// Important: same reason use the Windows \
		//            #vfsURL will fix the path
		return "\\";
	}
	
	public class KuduTestPackStorageDir extends AbstractTestPackStorageDir {
		
		@Override
		public String getLocalPath(AHost local_host) {
			// Critical
			return "C:\\inetpub\\wwwroot";// TODO temp D:\\HOME\\SITE\\WWWROOT";//D:\\Home\\Site\\WWWROOT\\TESTDIR1";
		}

		@Override
		public String getRemotePath(AHost local_host) {
			// Critical
			return "/";//D:\\Home\\Site\\WWWROOT\\TESTDIR1";
		}

		@Override
		public String getNameWithVersionInfo() {
			return getName();
		}

		@Override
		public String getName() {
			return "Azure-Kudu-Storage";
		}

		@Override
		public boolean isRunning() {
			return true;
		}

		@Override
		public void close(ConsoleManager cm) {
		}

		@Override
		public boolean notifyTestPackInstalled(ConsoleManager cm, AHost local_host) {
			return true;
		}

		@Override
		public boolean closeIfEmpty(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
			close(cm);
			return true;
		}

		@Override
		public boolean closeForce(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
			close(cm);
			return true;
		}
		
	}
	
	@Override
	public ITestPackStorageDir setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return new KuduTestPackStorageDir();
	}	

	@Override
	public boolean deleteChosenFiles(String dir, IFileChooser chr) {
		return false; // TODO
	}

	@Override
	public ByLineReader readFile(String file) throws FileNotFoundException, IOException {
		HttpGet request = null;
		try {
			request = new HttpGet(file);
				                
			HttpResponse response = httpclient.execute(request);
			HttpEntity entity = response.getEntity();
			
			ByteArrayInputStream bin = new ByteArrayInputStream(IOUtil.toBytes(entity.getContent(), IOUtil.HALF_MEGABYTE));
			
			return new NoCharsetByLineReader(bin);
		} finally {
			if (request!=null)
				request.releaseConnection();
		}
	}

	@Override
	public ByLineReader readFile(String file, Charset cs) throws IllegalStateException, FileNotFoundException, IOException {
		return readFile(file); // TODO temp
	}

	@Override
	public ByLineReader readFileDetectCharset(String file, CharsetDeciderDecoder cdd) throws FileNotFoundException, IOException {
		return readFile(file); // TODO temp
	}

	@Override
	public String getContents(String file) throws IOException {
		HttpGet request = null;
		NoCharsetByLineReader reader = null;
		try {
			request = new HttpGet(file);
				                
		    HttpResponse response = httpclient.execute(request);
		    HttpEntity entity = response.getEntity();
		        
		    reader = new NoCharsetByLineReader(entity.getContent());
			return IOUtil.toString(reader, IOUtil.HALF_MEGABYTE);
		} finally {
			if (reader!=null)
				reader.close();
			if (request!=null)
				request.releaseConnection();
		}
	}
	
	@Override
	public String readFileAsString(String path) throws IllegalStateException, FileNotFoundException, IOException {
		//return IOUtil.toString(readFile(path), IOUtil.ONE_MEGABYTE);
		// TODO temp azure - different between #getContents and #readFileAsString ?? 
		return getContents(path);
	}

	@Override
	public String getContentsDetectCharset(String file, CharsetDeciderDecoder cdd) throws IOException {
		return getContents(file); // TODO
	}
	
	@Override
	public String fixPath(String path) {
		return FileSystemScenario.toUnixPath(path);
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public String getName() {
		return "Azure-Kudu-VFS";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		/* TODO if (!(scenario_set.getScenario(SAPIScenario.class) instanceof AzureWebsitesScenario)) {
			cm.println(EPrintType.CLUE, AzureKuduVFSScenario.class, "This filesystem is only useable with Azure Web Apps");
			return false;
		}*/
		return true;
	}
	
}

