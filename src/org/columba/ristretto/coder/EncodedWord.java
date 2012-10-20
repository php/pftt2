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
package org.columba.ristretto.coder;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of EncodedWord en- and decoding methods.
 * <br>
 * <b>RFC(s):</b> 2047
 *
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class EncodedWord {

    /** JDK 1.4+ logging framework logger, used for logging. */
    private static final Logger LOG = Logger.getLogger("org.columba.ristretto.coder");


	/**
	 * QuotedPritntable Encoding. Default.
	 */
	public static final int QUOTED_PRINTABLE = 0;

	/**
	 *Base64 Encoding. Should be used to encode 16bit charsets
	 */
	public static final int BASE64 = 1;


	// finds a encoded word wich if of the form
	// =?charset?encoding(b/g)?encoded text part?=
	private static final Pattern encodedWordPattern =
		Pattern.compile("=\\?([^?]+)\\?([bBqQ])\\?([^?]+)\\?=");

	
	// filters whitespaces
	private static final Pattern spacePattern = Pattern.compile("\\s*");

	// tokenizes a string into words
	private static final Pattern wordTokenizerPattern =
		Pattern.compile("\\b([^\\s]+[\\s]*)");

	private static final Pattern whitespacePattern = Pattern.compile(" ");

	/**
	 * Decodes a string that contains EncodedWords.
	 * 
	 * @param input a string containing EncodedWords
	 * @return the decoded string
	 */
	public static StringBuffer decode(CharSequence input) {
		StringBuffer result = new StringBuffer(input.length());
		int lastMatchEnd = 0;
		Matcher matcher = encodedWordPattern.matcher(input);
		Charset charset;
		char type;
		String encodedPart;

		while (matcher.find()) {
			CharSequence inbetween =
				input.subSequence(lastMatchEnd, matcher.start());
			if (!spacePattern.matcher(inbetween).matches()) {
				result.append(inbetween);
			}


			try {
				charset = Charset.forName(matcher.group(1));
			} catch ( UnsupportedCharsetException e ) {
				charset = Charset.forName(System.getProperty("file.encoding"));
			} 
			type = matcher.group(2).toLowerCase().charAt(0);
			encodedPart = matcher.group(3);

			if (type == 'q') {
				encodedPart = encodedPart.replace('_', ' ');
				// _ are WS and must be converted before normal decoding
				result.append(QuotedPrintable.decode(encodedPart, charset));
			} else {
				result.append(charset.decode(Base64.decode(encodedPart)));
			}

			lastMatchEnd = matcher.end();
		}

		result.append(input.subSequence(lastMatchEnd, input.length()));

		return result;
	}

	/**
	 * Takes a text in form of a CharSequence encoded in the given charset (e.g. ISO-8859-1)
	 * and makes it US-ASCII compatible and RFC822 compatible for the use as e.g. subject
	 * with special characters.
	 * <br>
	 * This algorithm tries to achieve several goals when decoding:
	 * <li>never encode a single character but try to encode whole words</li>
	 * <li>if two words must be encoded and there a no more than 10 characters
	 * 	inbetween, encode everything in one single encoded word</li>
	 * <li>an encoded word must never be longer than 76 characters in total</li>
	 * <li>ensure that no encodedWord is in a line-wrap (RFC822 advices to no have more than 78
	 *   characters in a headerline)</li>
	 * 
	 * @param input	the headerline 
	 * @param charset the used charset (e.g. ISO-8859-1)
	 * @param type the encoding to be used
	 * @return input encoded in EncodedWords
	 */
	public static StringBuffer encode(
		CharSequence input,
		Charset charset,
		int type) {
		StringBuffer result = new StringBuffer(input.length());
		LinkedList words = new LinkedList();
		String encodedWordPrototype;
		if (type == QUOTED_PRINTABLE) {
			encodedWordPrototype = "=?" + charset.displayName() + "?q?";
		} else {
			encodedWordPrototype = "=?" + charset.displayName() + "?b?";
		}
		int maxLength = 75 - (encodedWordPrototype.length() + 2);

		// First find words which need to be encoded
		Matcher matcher = wordTokenizerPattern.matcher(input);
		while (matcher.find()) {
			String word = matcher.group(1);
			for (int i = 0; i < word.length(); i++) {
				if (word.charAt(i) > 127) {
					words.add(new int[] { matcher.start(), matcher.end()});
					break;
				}
			}
		}

		// No need to create encodedWords
		if (words.size() == 0) {
			return result.append(input);
		}

		// Second group them together if possible (see goals above)
		Iterator it = words.iterator();
		int[] last = (int[]) it.next();
		while (it.hasNext()) {
			int[] act = (int[]) it.next();
			if ((last[1] - last[0] + act[1] - act[0] <= maxLength)
				&& (act[0] - last[1] < 10)) {
				it.remove();
				last[1] = act[1];
			} else {
				last = act;
			}
		}

		// Create encodedWords
		it = words.iterator();
		int lastWordEnd = 0;
		while (it.hasNext()) {
			int[] act = (int[]) it.next();

			// create encoded part
			CharSequence rawWord = input.subSequence(act[0], act[1]);
			CharSequence encodedPart;
			if (type == QUOTED_PRINTABLE) {
				// Replace <space> with _
				Matcher wsMatcher = whitespacePattern.matcher(rawWord);
				rawWord = wsMatcher.replaceAll("_");

				encodedPart = QuotedPrintable.encode(rawWord, charset);
			} else {
				encodedPart =
					Base64.encode(charset.encode(CharBuffer.wrap(rawWord)));
			}

			// append encodedWord(s)
			result.append(input.subSequence(lastWordEnd, act[0]));
			result.append(encodedWordPrototype);
			result.append(encodedPart);
			result.append("?=");

			lastWordEnd = act[1];
		}
		result.append(input.subSequence(lastWordEnd, input.length()));

		return result;
	}


}
