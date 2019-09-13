package com.mostc.pftt.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Collection;

import javax.net.ssl.SSLSocket;

import org.columba.ristretto.auth.AuthenticationException;
import org.columba.ristretto.composer.MimeTreeRenderer;
import org.columba.ristretto.io.CharSequenceSource;
import org.columba.ristretto.message.Address;
import org.columba.ristretto.message.BasicHeader;
import org.columba.ristretto.message.Header;
import org.columba.ristretto.message.LocalMimePart;
import org.columba.ristretto.message.MimeHeader;
import org.columba.ristretto.message.MimeType;
import org.columba.ristretto.smtp.SMTPException;
import org.columba.ristretto.smtp.SMTPProtocol;
import org.columba.ristretto.ssl.RistrettoSSLSocketFactory;

import com.github.mattficken.io.ArrayUtil;

/** Utility functions for emailing messages
 *
 * @see #sendHTMLMessage
 * @see #sendTextMessage
 * @see #connect
 * @see #ensureAuthenticated
 * @author Matt Ficken
 *
 */

public final class EMailUtil {

	public static void main(String[] args) throws Exception {
		SMTPProtocol smtp = connect("smtp.gmail.com", 465, Address.parse("tomattficken@gmail.com"), ESMTPSSL.EXPLICIT_SSL, ESMTPAuthMethod.PLAIN, "tomattficken@gmail.com", "".toCharArray());
		System.err.println(smtp);
		System.err.println(smtp.getState());
		sendHTMLMessage(smtp, Address.parse("tomattficken@gmail.com"), ArrayUtil.toList(Address.parse("v-mafick@microsoft.com")), "subject", "<html><body><h1>html_msg_Str</h1></body></html>");
	}
	
	public static void sendTextMessage(SMTPProtocol smtp, Address from, Collection<Address> to, String subject, String text_msg_str) throws IOException, SMTPException, Exception {
		sendTextAndHTMLMessage(smtp, from, to, subject, null, text_msg_str);
	}
	
	/** sends an HTML email message, including a plain-text copy of the message
	 * 
	 * @see #connect
	 * @see #ensureAuthenticated
	 * @param smtp
	 * @param from
	 * @param to
	 * @param subject
	 * @param html_msg_str
	 * @throws IOException
	 * @throws SMTPException
	 * @throws Exception
	 */
	public static void sendHTMLMessage(SMTPProtocol smtp, Address from, Collection<Address> to, String subject, String html_msg_str) throws IOException, SMTPException, Exception {
		sendTextAndHTMLMessage(smtp, from, to, subject, html_msg_str, removeHTMLTags(html_msg_str));
	}
	
	public static String removeHTMLTags(String html_msg_str) {
		return html_msg_str.replaceAll("\\<.*?\\>", "");
	}
	
	public static void sendTextAndHTMLMessage(SMTPProtocol smtp, Address from, Collection<Address> to, String subject, String html_msg_str, String text_msg_str) throws IOException, SMTPException, Exception {
		sendTextAndHTMLMessage(smtp, from, to, toInputStream(from, to, subject, html_msg_str, text_msg_str));
	}
	
	public static void sendTextAndHTMLMessage(SMTPProtocol smtp, Address from, Collection<Address> to, InputStream message_src) throws IOException, SMTPException {
		smtp.mail(from);

		for ( Address t : to )
			smtp.rcpt(t);
		
		smtp.data(message_src);
	}
	
	public static InputStream toInputStream(Address from, Collection<Address> to, String subject, String html_msg_str, String text_msg_str) throws Exception {
		Header header = new Header();
		BasicHeader basic_header = new BasicHeader(header);
		basic_header.setFrom(from);
		basic_header.setTo((Address[])to.toArray(new Address[to.size()]));
		basic_header.setSubject(subject, Charset.forName("ISO-8859-1"));
		MimeHeader mime_header = new MimeHeader(header);
		mime_header.set("Mime-Version", "1.0");
		// multipart/alternative content type tells mail client that it can
		// choose either the text or html message format (doesn't need to show both; without this it will)
		mime_header.setMimeType(new MimeType("multipart", "alternative"));
		LocalMimePart root = new LocalMimePart(mime_header);
		
		if (text_msg_str!=null) {
			LocalMimePart text_part = new LocalMimePart(new MimeHeader());
			text_part.getHeader().setMimeType(new MimeType("text", "plain"));
			text_part.setBody(new CharSequenceSource(text_msg_str));
			root.addChild(text_part);
		}
		
		if (html_msg_str!=null) {
			LocalMimePart html_part = new LocalMimePart(new MimeHeader());
			html_part.getHeader().setMimeType(new MimeType("text", "html"));
			html_part.setBody(new CharSequenceSource(html_msg_str));
			root.addChild(html_part);
		}
		
		return MimeTreeRenderer.getInstance().renderMimePart(root);
	} // end public static InputStream toInputStream
	
	public static enum ESMTPSSL {
		EXPLICIT_SSL,
		IMPLICIT_SSL,
		NO_SSL
	}
	
	public static enum ESMTPAuthMethod {
		NONE,
		PLAIN,
		LOGIN,
		NTLM,
		DIGEST_MD5 {
			@Override
			public String toString() {
				return "DIGEST-MD5";
			}
		};
	}
	
	public static SMTPProtocol connect(String host, Address from, ESMTPSSL use_ssl, ESMTPAuthMethod auth_method, String username, char[] password) throws IOException, SMTPException {
		return connect(host, 25, from, use_ssl, auth_method, username, password);
	}
	
	public static SMTPProtocol connect(String host, int port, Address from, ESMTPSSL use_ssl, ESMTPAuthMethod auth_method, String username, char[] password) throws IOException, SMTPException {
		SMTPProtocol smtp = new SMTPProtocol(host, port);
		ensureAuthenticated(smtp, port, from, use_ssl, auth_method, username, password);
		return smtp;
	}
	 
	public static void ensureAuthenticated(SMTPProtocol smtp, int port, Address from, ESMTPSSL use_ssl, ESMTPAuthMethod auth_method, String username, char[] password) throws IOException, SMTPException {
		boolean authenticated = (auth_method == ESMTPAuthMethod.NONE);

		if (smtp.getState() == SMTPProtocol.NOT_CONNECTED) {
			if (use_ssl==ESMTPSSL.EXPLICIT_SSL) {
				SSLSocket ssl_sock = (SSLSocket) RistrettoSSLSocketFactory.getInstance().createSocket(smtp.getHostName(), port);
				
				ssl_sock.startHandshake();
				
				smtp.createStreams(ssl_sock);
			} else {
				smtp.openPort();
			}
			
			smtp.helo(InetAddress.getLocalHost());

			if (use_ssl==ESMTPSSL.IMPLICIT_SSL) {
				smtp.startTLS();
				// send HELO after STARTTLS see RFC3207 Section 4.2
				smtp.helo(InetAddress.getLocalHost());
			}
		}

		if (!authenticated) {
			try {
				smtp.auth(auth_method.toString(), username, password);
				authenticated = true;
			} catch (AuthenticationException e) {
				if (e.getCause() instanceof SMTPException) {
					int error_code = ((SMTPException) e.getCause()).getCode();

					if( error_code == 504 ) {
						return;
					}
				}
				throw (SMTPException) e.getCause();

			}
		} // end if

	} // end public static void ensureAuthenticated
	
	private EMailUtil() {}
	
} // end public final class EMailUtil
