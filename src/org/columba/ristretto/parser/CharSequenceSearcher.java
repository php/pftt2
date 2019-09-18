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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Implementation of the Boyer-Moore search algorithm for
 * CharSequences.
 * 
 * @author Timo Stich <tstich@users.sourceforge.net>
 */
public class CharSequenceSearcher {
	private int[] badCharacterSkip; 
	private int[] goodSuffixSkip; 
	
	private char[] pattern; 
	private int patternLength; 

	
	/**
	 * Constructs the CharSequenceSearcher.
	 * 
	 * @param pattern the pattern to search for.
	 */
	public CharSequenceSearcher(String pattern) {
		this(pattern.toCharArray());
	}
	
	/**
	 * Constructs the CharSequenceSearcher.
	 * 
	 * @param pattern the pattern to search for.
	 */
	public CharSequenceSearcher(char[] pattern) {
		badCharacterSkip = new int[256]; 

		setPattern(pattern);
	}

	/**
	 * Sets the pattern to search for. This also
	 * resets the matcher.
	 * 
	 * @param pattern the pattern to search for.
	 */
	public void setPattern(char[] pattern) {
		this.pattern = pattern;
		patternLength = pattern.length;
		goodSuffixSkip = new int[patternLength + 1]; 
		
		// init skip arrays
		initGoodSuffixSkipArray();
		initBadCharSkipArray();
	}

	private void initGoodSuffixSkipArray() {
		int i,j,p;
		int[] f = new int[patternLength + 1];

		j = patternLength + 1;
		f[patternLength] = j;

		for (i = patternLength; i > 0; i--) {
			while (j <= patternLength && pattern[i - 1] != pattern[j - 1]) {
				if (goodSuffixSkip[j] == 0) {
					goodSuffixSkip[j] = j - i;
				}
				j = f[j];
			}
			f[i - 1] = --j;
		}

		p = f[0];
		for (j = 0; j <= patternLength; ++j) {
			if (goodSuffixSkip[j] == 0) {
				goodSuffixSkip[j] = p;
			}
			if (j == p) {
				p = f[p];
			}
		}
	}

	private void initBadCharSkipArray() {
		Arrays.fill(badCharacterSkip, patternLength);

		for (int j = 0; j < patternLength - 1; j++) {
			badCharacterSkip[pattern[j]] = patternLength - j - 1;
		}
	}

	/**
	 * Searches the preciously set pattern in the given text. 
	 * The resulting list contains the positions of the matches
	 * as Integer.
	 * 
	 * @param text the text that is search in.
	 * @return a list of the positions of matches in the text.
	 */
	public List match(CharSequence text) {
		int i,j;
		int textLength = text.length();
		List result = new ArrayList();

		i = 0;
		while (i <= textLength - patternLength) {
			for (j = patternLength - 1; j >= 0
					&& pattern[j] == text.charAt(i + j); --j) {
			}

			if (j < 0) { 
				result.add(new Integer(i));

				i += goodSuffixSkip[0]; 
			} else { 
				i += Math.max(
						goodSuffixSkip[j + 1],
						badCharacterSkip[text.charAt(i + j)] - patternLength + j + 1);
			}
		}

		return result; 
	}
}