package com.mostc.pftt.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.DefaultClientConnection;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class ManagedClientConnectionAdapter implements ManagedClientConnection {
	private final ClientConnectionManager manager;
    private final ClientConnectionOperator operator;
    private final DefaultClientConnection connection;
    private volatile boolean reusable;
    private volatile long duration;

    public ManagedClientConnectionAdapter(
            final PhptClientConnectionManager manager,
            final ClientConnectionOperator operator,
            final DefaultClientConnection connection) {
        super();
        if (manager == null) {
            throw new IllegalArgumentException("Connection manager may not be null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Connection operator may not be null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("HTTP pool entry may not be null");
        }
        this.manager = manager;
        this.operator = operator;
        this.connection = connection;
        this.reusable = false;
        this.duration = Long.MAX_VALUE;
    }

    public ClientConnectionManager getManager() {
        return this.manager;
    }

    public void close() throws IOException {
    	connection.close();
    }

    public void shutdown() throws IOException {
    	connection.shutdown();
    }

    public boolean isOpen() {
        OperatedClientConnection conn = connection;
        if (conn != null) {
            return conn.isOpen();
        } else {
            return false;
        }
    }

    public boolean isStale() {
        OperatedClientConnection conn = connection;
        if (conn != null) {
            return conn.isStale();
        } else {
            return true;
        }
    }

    public void setSocketTimeout(int timeout) {
        OperatedClientConnection conn = connection;
        conn.setSocketTimeout(timeout);
    }

    public int getSocketTimeout() {
        OperatedClientConnection conn = connection;
        return conn.getSocketTimeout();
    }

    public HttpConnectionMetrics getMetrics() {
        OperatedClientConnection conn = connection;
        return conn.getMetrics();
    }

    public void flush() throws IOException {
        OperatedClientConnection conn = connection;
        conn.flush();
    }

    public boolean isResponseAvailable(int timeout) throws IOException {
        OperatedClientConnection conn = connection;
        return conn.isResponseAvailable(timeout);
    }

    public void receiveResponseEntity(
            final HttpResponse response) throws HttpException, IOException {
        OperatedClientConnection conn = connection;
        conn.receiveResponseEntity(response);
    }

    public HttpResponse receiveResponseHeader() throws HttpException, IOException {
        OperatedClientConnection conn = connection;
        return conn.receiveResponseHeader();
    }

    public void sendRequestEntity(
            final HttpEntityEnclosingRequest request) throws HttpException, IOException {
        OperatedClientConnection conn = connection;
        conn.sendRequestEntity(request);
    }

    public void sendRequestHeader(
            final HttpRequest request) throws HttpException, IOException {
        OperatedClientConnection conn = connection;
        conn.sendRequestHeader(request);
    }

    public InetAddress getLocalAddress() {
        OperatedClientConnection conn = connection;
        return conn.getLocalAddress();
    }

    public int getLocalPort() {
        OperatedClientConnection conn = connection;
        return conn.getLocalPort();
    }

    public InetAddress getRemoteAddress() {
        OperatedClientConnection conn = connection;
        return conn.getRemoteAddress();
    }

    public int getRemotePort() {
        OperatedClientConnection conn = connection;
        return conn.getRemotePort();
    }

    public boolean isSecure() {
        OperatedClientConnection conn = connection;
        return conn.isSecure();
    }

    public SSLSession getSSLSession() {
        OperatedClientConnection conn = connection;
        SSLSession result = null;
        Socket sock = conn.getSocket();
        if (sock instanceof SSLSocket) {
            result = ((SSLSocket)sock).getSession();
        }
        return result;
    }

    public Object getAttribute(final String id) {
        OperatedClientConnection conn = connection;
        if (conn instanceof HttpContext) {
            return ((HttpContext) conn).getAttribute(id);
        } else {
            return null;
        }
    }

    public Object removeAttribute(final String id) {
        OperatedClientConnection conn = connection;
        if (conn instanceof HttpContext) {
            return ((HttpContext) conn).removeAttribute(id);
        } else {
            return null;
        }
    }

    public void setAttribute(final String id, final Object obj) {
        OperatedClientConnection conn = connection;
        if (conn instanceof HttpContext) {
            ((HttpContext) conn).setAttribute(id, obj);
        }
    }

    public HttpRoute getRoute() {
    	return null;
    }

    public void open(final HttpRoute route, final HttpContext context, final HttpParams params) throws IOException {
        OperatedClientConnection conn = connection;

        HttpHost proxy  = route.getProxyHost();
        this.operator.openConnection(
                conn,
                (proxy != null) ? proxy : route.getTargetHost(),
                route.getLocalAddress(),
                context, params);
    }

    public void tunnelTarget(boolean secure, final HttpParams params) throws IOException {
        // N/A
    }

    public void tunnelProxy(final HttpHost next, boolean secure, final HttpParams params) throws IOException {
    	// N/A
    }

    public void layerProtocol(final HttpContext context, final HttpParams params) throws IOException {
    	// only if SSL is used
    }

    public Object getState() {
    	return null;
    }

    public void setState(final Object state) {
    }

    public void markReusable() {
        this.reusable = true;
    }

    public void unmarkReusable() {
        this.reusable = false;
    }

    public boolean isMarkedReusable() {
        return this.reusable;
    }

    public void setIdleDuration(long duration, TimeUnit unit) {
        if(duration > 0) {
            this.duration = unit.toMillis(duration);
        } else {
            this.duration = -1;
        }
    }

    public void releaseConnection() {
    	manager.releaseConnection(this, this.duration, TimeUnit.MILLISECONDS);
    }

    public void abortConnection() throws IOException {
    	connection.shutdown();
    }
	
} // end public class ManagedClientConnectionAdapter
