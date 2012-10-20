/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.text;

import java.nio.charset.Charset;

import com.github.mattficken.io.CharsetRec;
import com.github.mattficken.io.ERecognizedLanguage;


/**
 * This class matches UTF-16 and UTF-32, both big- and little-endian. The
 * BOM will be used if it is present.
 */
public abstract class CharsetRecog_Unicode extends CharsetRecognizer {

    /* (non-Javadoc)
     * @see com.ibm.icu.text.CharsetRecognizer#getName()
     */
    public abstract String getName();

    /* (non-Javadoc)
     * @see com.ibm.icu.text.CharsetRecognizer#match(com.ibm.icu.text.CharsetDetector)
     */
    abstract int match(CharsetDetector det);
    
    public static class CharsetRecog_UTF_16_BE extends CharsetRecog_Unicode
    {
        public String getName()
        {
            return "UTF-16BE";
        }
        
        @Override
        public void match(byte[] input, int len, CharsetRec cr) {
        	cr.cs = CharsetRec.UTF16_BE;
        	
            if (len>=2 && ((input[0] & 0xFF) == 0xFE && (input[1] & 0xFF) == 0xFF)) {
            	cr.confidence = 100;
            	cr.lang = getLang();
            	return;
            }
            
            // TODO: Do some statistics to check for unsigned UTF-16BE
            cr.confidence = 0;
            cr.lang = null;
        }
        
        int match(CharsetDetector det)
        {
            byte[] input = det.fRawInput;
            
            if (input.length>=2 && ((input[0] & 0xFF) == 0xFE && (input[1] & 0xFF) == 0xFF)) {
                return 100;
            }
            
            // TODO: Do some statistics to check for unsigned UTF-16BE
            return 0;
        }

		@Override
		public ERecognizedLanguage getLang() {
			return ERecognizedLanguage.ANY;
		}
    }
    
    public static class CharsetRecog_UTF_16_LE extends CharsetRecog_Unicode
    {
        public String getName()
        {
            return "UTF-16LE";
        }
        @Override
		public ERecognizedLanguage getLang() {
			return ERecognizedLanguage.ANY;
		}
        int match(CharsetDetector det)
        {
            byte[] input = det.fRawInput;
            
            if (input.length >= 2 && ((input[0] & 0xFF) == 0xFF && (input[1] & 0xFF) == 0xFE))
            {
               // An LE BOM is present.
               if (input.length>=4 && input[2] == 0x00 && input[3] == 0x00) {
                   // It is probably UTF-32 LE, not UTF-16
                   return 0;
               }
               return 100;
            }        
            
            // TODO: Do some statistics to check for unsigned UTF-16LE
            return 0;
        }
        
        @Override
        public void match(byte[] input, int len, CharsetRec cr) {
        	cr.cs = CharsetRec.UTF16_LE;
            if (len >= 2 && ((input[0] & 0xFF) == 0xFF && (input[1] & 0xFF) == 0xFE))
            {
               // An LE BOM is present.
               if (len>=4 && input[2] == 0x00 && input[3] == 0x00) {
                   // It is probably UTF-32 LE, not UTF-16
            	   cr.confidence = 0;
                   return;
               }
               cr.confidence = 100;
               cr.lang = getLang();
               return;
            }        
            
            // TODO: Do some statistics to check for unsigned UTF-16LE
            cr.confidence = 0;
            cr.lang = null;
        }
    }
    
    public static abstract class CharsetRecog_UTF_32 extends CharsetRecog_Unicode
    {
        abstract int getChar(byte[] input, int index);
        
        public abstract String getName();
        
        int match(CharsetDetector det)
        {
            byte[] input   = det.fRawInput;
            int limit      = (det.fRawLength / 4) * 4;
            int numValid   = 0;
            int numInvalid = 0;
            boolean hasBOM = false;
            int confidence = 0;
            
            if (limit==0) {
                return 0;
            }
            if (getChar(input, 0) == 0x0000FEFF) {
                hasBOM = true;
            }
            
            for(int i = 0; i < limit; i += 4) {
                int ch = getChar(input, i);
                
                if (ch < 0 || ch >= 0x10FFFF || (ch >= 0xD800 && ch <= 0xDFFF)) {
                    numInvalid += 1;
                } else {
                    numValid += 1;
                }
            }
            
            
            // Cook up some sort of confidence score, based on presence of a BOM
            //    and the existence of valid and/or invalid multi-byte sequences.
            if (hasBOM && numInvalid==0) {
                confidence = 100;
            } else if (hasBOM && numValid > numInvalid*10) {
                confidence = 80;
            } else if (numValid > 3 && numInvalid == 0) {
                confidence = 100;            
            } else if (numValid > 0 && numInvalid == 0) {
                confidence = 80;
            } else if (numValid > numInvalid*10) {
                // Probably corrupt UTF-32BE data.  Valid sequences aren't likely by chance.
                confidence = 25;
            }
            
            return confidence;
        }
        
        protected abstract Charset getCharset();
        
        @Override
        public void match(byte[] input, int len, CharsetRec cr) {
            int limit      = (len / 4) * 4;
            int numValid   = 0;
            int numInvalid = 0;
            boolean hasBOM = false;
            int confidence = 0;
            
            cr.cs = getCharset();
            
            if (limit==0) {
            	cr.confidence = 0;
            	return;
            }
            if (getChar(input, 0) == 0x0000FEFF) {
                hasBOM = true;
            }
            
            for(int i = 0; i < limit; i += 4) {
                int ch = getChar(input, i);
                
                if (ch < 0 || ch >= 0x10FFFF || (ch >= 0xD800 && ch <= 0xDFFF)) {
                    numInvalid += 1;
                } else {
                    numValid += 1;
                }
            }
            
            
            // Cook up some sort of confidence score, based on presence of a BOM
            //    and the existence of valid and/or invalid multi-byte sequences.
            if (hasBOM && numInvalid==0) {
                confidence = 100;
            } else if (hasBOM && numValid > numInvalid*10) {
                confidence = 80;
            } else if (numValid > 3 && numInvalid == 0) {
                confidence = 100;            
            } else if (numValid > 0 && numInvalid == 0) {
                confidence = 80;
            } else if (numValid > numInvalid*10) {
                // Probably corrupt UTF-32BE data.  Valid sequences aren't likely by chance.
                confidence = 25;
            }
            
            cr.confidence = confidence;
            cr.lang = getLang();
        }
    }
    
    public static class CharsetRecog_UTF_32_BE extends CharsetRecog_UTF_32
    {
    	@Override
		public ERecognizedLanguage getLang() {
		return ERecognizedLanguage.ANY;
	}
        int getChar(byte[] input, int index)
        {
            return (input[index + 0] & 0xFF) << 24 | (input[index + 1] & 0xFF) << 16 |
                   (input[index + 2] & 0xFF) <<  8 | (input[index + 3] & 0xFF);
        }
        
        public String getName()
        {
            return "UTF-32BE";
        }
        
        @Override
        protected Charset getCharset() {
        	return CharsetRec.UTF32_BE;
        }
    }

    
    public static class CharsetRecog_UTF_32_LE extends CharsetRecog_UTF_32
    {
    	@Override
		public ERecognizedLanguage getLang() {
			return ERecognizedLanguage.ANY;
		}
    	@Override
    	protected Charset getCharset() {
    		return CharsetRec.UTF32_LE;
    	}
        int getChar(byte[] input, int index)
        {
            return (input[index + 3] & 0xFF) << 24 | (input[index + 2] & 0xFF) << 16 |
                   (input[index + 1] & 0xFF) <<  8 | (input[index + 0] & 0xFF);
        }
        
        public String getName()
        {
            return "UTF-32LE";
        }
    }
}
