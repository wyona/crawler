/*
 * Copyright (c) 1998-2002 Carnegie Mellon University.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package rcm.util;

import java.util.StringTokenizer;

/**
 * String utility routines.
 */
public abstract class Str {

    /**
     * Find first occurence of any of a set of characters.
     * @param subject String in which to search
     * @param chars Characters to search for
     * @return index of first occurence in subject of a character from chars,
     * or -1 if no match.
     */
    public static int indexOfAnyChar (String subject, String chars) {
        return indexOfAnyChar (subject, chars, 0);
    }

    /**
     * Find first occurence of any of a set of characters, starting
     * at a specified index.
     * @param subject String in which to search
     * @param chars Characters to search for
     * @param start Starting offset to search from
     * @return index of first occurence (after start) in subject of a character from chars,
     * or -1 if no match.
     */
    public static int indexOfAnyChar (String subject, String chars, int start) {
        for (int i=start; i<subject.length(); ++i)
            if (chars.indexOf (subject.charAt (i)) != -1)
                return i;
        return -1;
    }

    /**
     * Replace all occurences of a string.
     * @param subject String in which to search
     * @param original String to search for in subject
     * @param replacement String to substitute
     * @return subject with all occurences of original replaced by replacement
     */
    public static String replace (String subject, String original, String replacement) {
        StringBuffer output = new StringBuffer ();

        int p = 0;
        int i;
        while ((i = subject.indexOf (original, p)) != -1) {
            output.append (subject.substring (p, i));
            output.append (replacement);
            p = i + original.length();
        }
        if (p < subject.length ())
            output.append (subject.substring(p));
        return output.toString ();
    }

    /**
     * Escapes metacharacters in a string.
     * @param subject String in which metacharacters are to be escaped
     * @param escapeChar the escape character (e.g., \)
     * @param metachars the metacharacters that should be escaped
     * @return subject with escapeChar inserted before every character found in metachars
     */
    public static String escape (String subject, char escapeChar, String metachars) {
        return escape (subject, metachars, escapeChar, metachars);
    }

    /**
     * Escapes characters in a string.
     * @param subject String in which metacharacters are to be escaped
     * @param chars Characters that need to be escaped (e.g. "\b\t\r\n\\")
     * @param escapeChar the escape character (e.g., '\\')
     * @param metachars escape code letters corresponding to each letter in chars (e.g. "btrn\\")
     *    <B>Must have metachars.length () == chars.length().</B>
     * @return subject where every occurence of c in chars is replaced
     * by escapeChar followed the character corresponding to c in metachars.
     *
     */
    public static String escape (String subject, String chars, char escapeChar, String metachars) {
        StringBuffer output = new StringBuffer ();

        int p = 0;
        int i;
        while ((i = indexOfAnyChar (subject, chars, p)) != -1) {
            output.append (subject.substring (p, i));

            char c = subject.charAt (i); // character that needs escaping
            int k = chars.indexOf (c);
            char metac = metachars.charAt (k);   // its corresponding metachar
            output.append (escapeChar);
            output.append (metac);

            p = i + 1;
        }
        if (p < subject.length ())
            output.append (subject.substring(p));
        return output.toString ();
    }

    /**
     * Translate escape sequences (e.g. \r, \n) to characters.
     * @param subject String in which metacharacters are to be escaped
     * @param escapeChar the escape character (e.g., \)
     * @param metachars letters representing escape codes (typically "btrn\\")
     * @param chars characters corresponding to metachars (typically "\b\t\r\n\\").
     *    <B>Must have chars.length () == metachars.length().</B>
     * @param keepUntranslatedEscapes Controls behavior on unknown escape sequences
     * (see below).
     * @return subject where every escapeChar followed by c in metachars
     * is replaced by the character corresponding to c in chars.  If an escape
     * sequence is untranslatable (because escapeChar is followed by some character c
     * not in metachars), then the escapeChar is kept if keepUntranslatedEscapes is true,
     * otherwise the escapeChar is deleted. (The character c is always kept.)
     *
     */
    public static String unescape (String subject, char escapeChar, String metachars, String chars, boolean keepUntranslatedEscapes) {
        StringBuffer output = new StringBuffer ();

        int p = 0;
        int i;
        int len = subject.length ();
        while ((i = subject.indexOf (escapeChar, p)) != -1) {
            output.append (subject.substring (p, i));
            if (i + 1 == len)
                break;

            char metac = subject.charAt (i+1);  // metachar to replace
            int k = metachars.indexOf (metac);
            if (k == -1) {
                // untranslatable sequence
                if (keepUntranslatedEscapes)
                    output.append (escapeChar);
                output.append (metac);
            }
            else
                output.append (chars.charAt (k));   // its corresponding true char

            p = i + 2;    // skip over both escapeChar & metac
        }

        if (p < len)
            output.append (subject.substring(p));
        return output.toString ();
    }

    /**
     * Parse a number from a string. Finds the first recognizable base-10 number (integer or floating point)
     * in the string and returns it as a Number.  Uses American English conventions
     * (i.e., '.' as decimal point and ',' as thousands separator).
     * @param string String to parse
     * @return first recognizable number
     * @exception NumberFormatException if no recognizable number is found
     */
    private static final int INT = 0;
    private static final int FRAC = 1;
    private static final int EXP = 2;
    public static Number parseNumber (String s) throws NumberFormatException {
        int p = 0;
        for (int i=0; i<s.length(); ++i) {
            char c = s.charAt (i);
            if (Character.isDigit (c)) {
                int start = i;
                int end = ++i;
                int state = INT;

                if (start > 0 && s.charAt (start-1) == '.') {
                    --start;
                    state = FRAC;
                }
                if (start > 0 && s.charAt (start-1) == '-')
                    --start;

              foundEnd:
                while (i < s.length()) {
                    switch (s.charAt (i)) {
                      case '0': case '1': case '2': case '3': case '4':
                      case '5': case '6': case '7': case '8': case '9':
                        end = ++i;
                        break;
                      case '.':
                        if (state != INT)
                            break foundEnd;
                        state = FRAC;
                        ++i;
                        break;
                      case ',': // ignore commas
                        ++i;
                        break;
                      case 'e':
                      case 'E':
                        state = EXP;
                        ++i;
                        if (i < s.length() &&
                            ( (c = s.charAt (i)) == '+' || c == '-') )
                          ++i;
                        break;
                      default:
                        break foundEnd;
                    }
                }

                String num = s.substring (start, end);
                num = replace (num, ",", "");
                try {
                    if (state == INT)
                        return new Integer (num);
                    else
                        return new Float (num);
                } catch (NumberFormatException e) {
                    throw new RuntimeException ("internal error: " + e);
                }
            }
        }
        throw new NumberFormatException (s);
    }
/*
    For testing parseNumber

    public static void main (String[] args) {
      for (int i=0; i<args.length; ++i)
          System.out.println (parseNumber (args[i]));
  }
*/


    /**
     * Generate a string by concatenating n copies of another string.
     * @param s String to repeat
     * @param n number of times to repeat s
     * @return s concatenated with itself n times
     */
    public static String repeat (String s, int n) {
        StringBuffer out = new StringBuffer ();
        while (--n >= 0)
            out.append (s);
        return out.toString ();
    }

    /**
     * Compress whitespace.
     * @param s String to compress
     * @return string with leading and trailing whitespace removed, and
     * internal runs of whitespace replaced by a single space character
     */
    public static String compressWhitespace (String s) {
        StringBuffer output = new StringBuffer ();
        int p = 0;
        boolean inSpace = true;
        for (int i = 0, len = s.length (); i < len; ++i) {
            if (Character.isWhitespace (s.charAt (i))) {
                if (!inSpace) {
                    output.append (s.substring (p, i));
                    output.append (' ');
                    inSpace = true;
                }
            }
            else {
                if (inSpace) {
                    p = i;
                    inSpace = false;
                }
            }
        }
        if (!inSpace)
            output.append (s.substring (p));
        return output.toString ();
    }

    /**
     * Test if string contains only whitespace.
     * @param s String to test
     * @return true iff all characters in s satisfy Character.isWhitespace().
     * If s is empty, returns true.
     */
    public static boolean isWhitespace (String s) {
        for (int i = 0, n = s.length (); i < n; ++i)
            if (!Character.isWhitespace (s.charAt (i)))
                return false;
        return true;
    }

    /**
     * Concatenate an array of strings.
     * @param list Array of strings to concatenate
     * @param sep Separator to insert between each string
     * @return string consisting of list[0] + sep + list[1] + sep + ... + sep + list[list.length-1]
     */
    public static String join (String[] list, String sep) {
        StringBuffer result = new StringBuffer ();
        for (int i=0; i < list.length; ++i) {
            if (i > 0)
                result.append (sep);
            result.append (list[i]);
        }
        return result.toString ();
    }

    /**
     * Abbreviate a string.
     * @param s String to abbreviate
     * @param max Maximum length of returned string; must be at least 5
     * @returns s with linebreaks removed and enough characters removed from
     * the middle (replaced by "...") to make length &lt;= max
     */
    public static String abbreviate (String s, int max) {
        s = compressWhitespace (s);
        if (s.length() < max)
            return s;
        else {
            max = Math.max (max-3, 2);   // for "..."
            int half = max/2;
            return s.substring (0, half) + "..." + s.substring (s.length()-half);
        }
    }

    /**
     * Abbreviate a multi-line string.
     * @param s String to abbreviate
     * @param maxLines Max number of lines in returned string; must be at least 3
     * @param message Message to replace removed lines with; should end with
     * \n, but may be multiple lines.  Occurrences of %d are replaced with
     * the number of lines removed.
     * @returns s with enough whole lines removed from
     * the middle (replaced by message) to make its length in lines &lt;= max
     */
    public static String abbreviateLines (String s, int maxLines, String message) {
        int nLines = countLines (s);
        if (nLines < maxLines)
            return s;
        else {
            maxLines = Math.max (maxLines-1, 2);   // take out one line for "..."
            int half = maxLines/2;
            return s.substring (0, nthLine (s, half)) 
                + replace (message, "%d", String.valueOf (nLines - half*2))
                + s.substring (nthLine (s, -half));
        }
    }

    static int countLines (String s) {
        int n = 1;
        int i = -1;
        while ((i = s.indexOf ('\n', i+1)) != -1)
            ++n;
        return n;
    }
    static int nthLine (String s, int n) {
        if (n >= 0) {
            int i = -1;
            while (n > 0 && (i = s.indexOf ('\n', i+1)) != -1)
                --n;
            return i+1;
        } else {
            int i = s.length ();
            while (n < 0 && (i = s.lastIndexOf ('\n', i-1)) != -1)
                ++n;
            return i+1;
        }
    }

    /**
      * Split string around a substring match and return prefix.
      * @param s String to split
      * @param pat Substring to search for in s
      * @return Prefix of s ending just before the first occurrence
      * of pat.  If pat is not found in s, returns s itself.
      */
    public static String before (String s, String pat) {
        int i = s.indexOf (pat);
        return (i >= 0) ? s.substring(0, i) : s;
    }

    /**
      * Split string around a substring match and return suffix.
      * @param s String to split
      * @param pat Substring to search for in s
      * @return Suffix of s starting just after the first occurrence
      * of pat.  If pat is not found in s, returns "".
      */
    public static String after (String s, String pat) {
        int i = s.indexOf (pat);
        return (i >= 0) ? s.substring(i + pat.length ()) : "";
    }


    /**
      * Like String.startsWith, but case-insensitive.
      */
    public static boolean startsWithIgnoreCase (String s, String prefix) {
        int sLen = s.length ();
        int prefixLen = prefix.length ();
        return (sLen >= prefixLen
                && s.substring (0, prefixLen).equalsIgnoreCase (prefix));
    }

    /**
      * Like String.endsWith, but case-insensitive.
      */
    public static boolean endsWithIgnoreCase (String s, String suffix) {
        int sLen = s.length ();
        int suffixLen = suffix.length ();
        return (sLen >= suffixLen
                && s.substring (sLen - suffixLen).equalsIgnoreCase (suffix));
    }

    /**
      * Expands tabs to spaces.
      */
    public static String untabify (String s, int tabsize) {
        if (s.indexOf ('\t') == -1)
            return s; // no tabs, don't bother

        int col = 0;
        StringBuffer result = new StringBuffer ();
        for (StringTokenizer tokenizer = new StringTokenizer (s, "\t\r\n", true);
             tokenizer.hasMoreTokens (); ) {
            String tok = tokenizer.nextToken ();
            switch (tok.charAt (0)) {
            case '\t':
                {
                    int oldcol = col;
                    col = (col/tabsize + 1) * tabsize;
                    result.append (Str.repeat (" ", col - oldcol));
                }
                break;
            case '\r':
            case '\n':
                col = 0;
                result.append (tok);
                break;
            default:
                col += tok.length ();
                result.append (tok);
                break;
            }
        }

        return result.toString ();
    }

    /**
     * Reverse a string.
     * @param s String to reverse
     * @return string containing characters of s in reverse order
     */
    public static String reverse (String s) {
        StringBuffer t = new StringBuffer (s.length ());
        for (int i = s.length () - 1; i >= 0; --i)
            t.append (s.charAt(i));
        return t.toString ();
    }

    /**
     * Find longest common prefix of two strings.
     */
    public static String longestCommonPrefix (String s, String t) {
        return s.substring (0, longestCommonPrefixLength (s, t));
    }

    public static int longestCommonPrefixLength (String s, String t) {
        int m = Math.min (s.length (), t.length());
        for (int k = 0; k < m; ++k)
            if (s.charAt (k) != t.charAt (k))
                return k;
        return m;
    }

    /**
     * Find longest common suffix of two strings.
     */
    public static String longestCommonSuffix (String s, String t) {
        return s.substring (s.length () - longestCommonSuffixLength (s, t));
    }

    public static int longestCommonSuffixLength (String s, String t) {
        int i = s.length ()-1;
        int j = t.length ()-1;
        for (; i >= 0 && j >= 0; --i, --j)
            if (s.charAt (i) != t.charAt (j))
                return s.length () - (i+1);
        return s.length () - (i+1);
    }




    /**
     * Find longest common prefix of two strings, ignoring case.
     */
    public static String longestCommonPrefixIgnoreCase (String s, String t) {
        return s.substring (0, longestCommonPrefixLengthIgnoreCase (s, t));
    }

    public static int longestCommonPrefixLengthIgnoreCase (String s, String t) {
        int m = Math.min (s.length (), t.length());
        for (int k = 0; k < m; ++k)
            if (Character.toLowerCase (s.charAt (k)) != Character.toLowerCase (t.charAt (k)))
                return k;
        return m;
    }

    /**
     * Find longest common suffix of two strings, ignoring case.
     */
    public static String longestCommonSuffixIgnoreCase (String s, String t) {
        return s.substring (s.length () - longestCommonSuffixLengthIgnoreCase (s, t));
    }

    public static int longestCommonSuffixLengthIgnoreCase (String s, String t) {
        int i = s.length ()-1;
        int j = t.length ()-1;
        for (; i >= 0 && j >= 0; --i, --j)
            if (Character.toLowerCase (s.charAt (i)) != Character.toLowerCase (t.charAt (j)))
                return s.length () - (i+1);
        return s.length () - (i+1);
    }
}
