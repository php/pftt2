/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Ristretto Mail API.
 *
 * The Initial Developers of the Original Code are
 * Timo Stich and Frederik Dietz.
 * Portions created by the Initial Developers are Copyright (C) 2004
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.columba.ristretto.parser;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.columba.ristretto.io.Source;
import org.columba.ristretto.message.Header;
import org.columba.ristretto.message.LocalMimePart;
import org.columba.ristretto.message.MimeHeader;
import org.columba.ristretto.message.MimeType;

/**
 * Parser for the MIME structure of a RFC 2822 or RFC 2045 compliant message.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class BodyParser {

	private static final Pattern headerEndPattern = Pattern.compile("\r\n\r\n");

	private static final Pattern lineEndPattern = Pattern
			.compile("\r?\n|\r\n?");

	private BodyParser() {
	}

	/**
	 * Positions the {@link Source}after the header of the message. This is
	 * necessary when invoking the BodyParser on a complete message source
	 * without use of the {@link HeaderParser}.
	 * 
	 * @param source
	 * @throws IOException
	 */
	public static void skipHeader(Source source) throws IOException {
		Matcher matcher = headerEndPattern.matcher(source);
		if (matcher.find()) {
			source.seek(matcher.end());
		}
	}

	/**
	 * Parse the MIME structure of a message from a {@link Source}. The Source
	 * is parsed from the actual position of the Source and is positioned after
	 * the MIME structure when parsing is finished.
	 * <p>
	 * <b>Note: </b> Normally the BodyParser is called <it>after </it> the
	 * {@link HeaderParser}. When using without the HeaderParser the Source
	 * must be positioned correctly!
	 * 
	 * @see #skipHeader(Source)
	 * 
	 * @param header
	 *            the header of this part which contains information about the
	 *            content type.
	 * @param message
	 *            the source of the message which is parsed. The source will be
	 *            positioned after the MIME structure when parsing has finished.
	 * @return the parsed LocalMimePart
	 * @throws IOException
	 * @throws ParserException
	 */
	public static LocalMimePart parseMimePart(MimeHeader header, Source message)
			throws IOException, ParserException {

		boolean endBoundaryFound = false;

		MimeType type = header.getMimeType();
		// If normal part just return it
		if (!type.getType().equals("multipart")) {
			return new LocalMimePart(header, message.fromActualPosition());
		}

		// Search for the boundaries and call this method recursive on every
		// found part
		LocalMimePart multipart = new LocalMimePart(header, message
				.fromActualPosition());
		String boundary = header.getContentParameter("boundary");
		if (boundary == null)
			throw new ParserException(
					"Content-Type is multipart, but no boundary specified!");

		CharSequenceSearcher boundarySearcher = new CharSequenceSearcher("--"
				+ boundary);
		List foundBoundaries = boundarySearcher.match(message);

		// filter invalid boundaries that are not followed by \r, \n or -
		Iterator it = foundBoundaries.iterator();
		while (it.hasNext()) {
			char suffix = message.charAt(((Integer) it.next()).intValue()
					+ boundary.length() + 2);
			if (suffix != '\r' && suffix != '\n' && suffix != '-') {
				it.remove();
			}
		}

		// Process the found boundaries
		if (foundBoundaries.size() == 0) {
			throw new ParserException("No startboundary found: " + boundary,
					message);
		}

		it = foundBoundaries.iterator();

		int boundaryStart, boundaryEnd;

		int start = ((Integer) it.next()).intValue() + boundary.length() + 2;

		//cope with suffixed (--)\r?\n
		switch (message.charAt(start)) {
		case '\r': {
			start++;
			if (message.charAt(start) == '\n') {
				start++;
			}
			break;
		}

		case '\n': {
			start++;
			break;
		}
		}

		// For every found start boundary
		while (it.hasNext()) {
			boundaryStart = ((Integer) it.next()).intValue();
			boundaryEnd = boundaryStart + boundary.length() + 2; // 2 = "--"

			//cope with prefixed \r? and \n
			switch (message.charAt(boundaryStart - 1)) {
			case '\n': {
				boundaryStart--;
				if (message.charAt(boundaryStart) == '\n') {
					boundaryStart--;
				}
				break;
			}

			case '\r': {
				boundaryStart--;
			}
			}

			//cope with suffixed (--)\r?\n
			switch (message.charAt(boundaryEnd)) {
			case '-': {
				// this is the last boundary we do not need
				// the exact end position
				boundaryEnd += 4;
				endBoundaryFound = true;
				break;
			}

			case '\r': {
				boundaryEnd++;
				if (message.length() > boundaryEnd
						&& message.charAt(boundaryEnd) == '\n') {
					boundaryEnd++;
				}
				break;
			}

			case '\n': {
				boundaryEnd++;
				break;
			}

			}

			//Subsource that includes the mimepart
			Source subSource = message.subSource(start, boundaryStart);

			//Parse the Header of the subpart
			Header subHeader = HeaderParser.parse(subSource);

			//And parse the body of subpart
			LocalMimePart subPart = BodyParser.parseMimePart(new MimeHeader(
					subHeader), subSource);

			//Set the sources and add it to the mimeparttree
			subPart.setSource(subSource);
			multipart.addChild(subPart);
			start = boundaryEnd;
		}

		int end;
		if (!endBoundaryFound) {
			// Handle a missing end boundary with taking everything till the end
			end = message.length();

			//Subsource that includes the mimepart
			Source subSource = message.subSource(start, end);

			//Parse the Header of the subpart
			Header subHeader = HeaderParser.parse(subSource);

			//And parse the body of subpart
			LocalMimePart subPart = BodyParser.parseMimePart(new MimeHeader(
					subHeader), subSource);

			//Set the sources and add it to the mimeparttree
			subPart.setSource(subSource);
			multipart.addChild(subPart);
		}

		return multipart;
	}

}