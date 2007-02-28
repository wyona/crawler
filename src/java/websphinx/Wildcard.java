/*
 * WebSphinx web-crawling toolkit
 *
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

package websphinx;

/**
 * Wildcard pattern.  Wildcards are similar to sh-style file globbing.
 * A wildcard pattern is implicitly anchored, meaning that it must match the entire string.
 * The wildcard operators are:
 * <PRE>
 *    ? matches one arbitrary character
 *    * matches zero or more arbitrary characters
 *    [xyz] matches characters x or y or z
 *    {foo,bar,baz}   matches expressions foo or bar or baz
 *    ()  grouping to extract fields
 *    \ escape one of these special characters
 * </PRE>
 * Escape codes (like \n and \t) and Perl5 character classes (like \w and \s) may also be used.
 */
public class Wildcard extends Regexp {
    String stringRep;
    
    public Wildcard (String pattern) {
        super ("^" + toRegexp (pattern) + "$");
        stringRep = pattern;
    }

    public boolean equals (Object object) {
        if (! (object instanceof Wildcard))
            return false;
        Wildcard p = (Wildcard)object;
        return p.stringRep.equals (stringRep);
    }        
    
    public static String toRegexp (String wildcard) {
        String s = wildcard;

        int inAlternative = 0;
        int inSet = 0;
        boolean inEscape = false;

        StringBuffer output = new StringBuffer ();

        int len = s.length ();
        for (int i=0; i<len; ++i) {
            char c = s.charAt (i);
            if (inEscape) {
                output.append (c);
                inEscape = false;
            }
            else {
                switch (c) {
                  case '\\':
                    output.append (c);
                    inEscape = true;
                    break;
                  case '?':
                    output.append ('.');
                    break;
                  case '*':
                    output.append (".*");
                    break;
                  case '[':
                    output.append (c);
                    ++inSet;
                    break;
                  case ']':
                      // FIX: handle [] case properly
                    output.append (c);
                    --inSet;
                    break;
                  case '{':
                    output.append ("(?:");
                    ++inAlternative;
                    break;
                  case ',':
                    if (inAlternative > 0)
                        output.append ("|");
                    else
                        output.append (c);
                    break;
                  case '}':
                    output.append (")");
                    --inAlternative;
                    break;
                  case '^':
                    if (inSet > 0) {
                        output.append (c);
                    }
                    else {
                        output.append ('\\');
                        output.append (c);
                    }
                    break;
                  case '$':
                  case '.':
                  case '|':
                  case '+':
                    output.append ('\\');
                    output.append (c);
                    break;
                  default:
                    output.append (c);
                    break;
                }
            }
        }
        if (inEscape)
            output.append ('\\');

        return output.toString ();
    }

    public static String escape (String s) {
        return rcm.util.Str.escape (s, '\\', "\\?*{}()[]");
    }
    
    public String toString () {
        return stringRep;
    }
    
    public static void main (String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println ("usage: Wildcard <pattern> <string>*");
            return;
        }

        Pattern p = new Wildcard (args[0].replace ('_', ' ') );
        for (int i=1; i<args.length; ++i) {
            Region r = p.oneMatch (args[i]);
            System.out.println (args[i] + ": " + (r != null));
            if (r != null) {
                System.out.println ("  [" + r.getStart() + "," + r.getEnd() + "]" + r);
                Region[] groups = r.getFields ("websphinx.groups");
                if (groups != null)
                    for (int j=0; j<groups.length; ++j) {
                        Region s = groups[j];
                        System.out.println ("    "+"[" + s.getStart() + "," + s.getEnd() + "]" + s);
                    }
            }
        }
    }
}
