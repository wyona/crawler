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

import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;
//#ifdef JDK1.1
import java.io.ObjectInputStream;
//#endif JDK1.1

public class Regexp extends Pattern {

    String stringRep;
    transient org.apache.regexp.REProgram pattern;
    transient String[] fields;

    public Regexp (String pattern) {
        stringRep = pattern;
        init ();
    }
    
    public boolean equals (Object object) {
        if (! (object instanceof Regexp))
            return false;
        Regexp p = (Regexp)object;
        return p.stringRep.equals (stringRep);
    }        
    
    //#ifdef JDK1.1
    private void readObject (ObjectInputStream in) 
           throws IOException, ClassNotFoundException {
        in.defaultReadObject ();
        init ();
    }
    //#endif JDK1.1

    
    private void init () {
        try {
            this.pattern = new org.apache.regexp.RECompiler ().compile (translateFields (stringRep));
        } catch (org.apache.regexp.RESyntaxException e) {
            throw new RuntimeException ("syntax error in pattern: " 
                                        + e.getMessage ());
        }
    }
    
    public String[] getFieldNames () {
        return fields;
    }
    
    public String toString () {
        return stringRep;
    }

    public PatternMatcher match (Region region) {
        return new RegexpMatcher (this, region);
    }

    public static String escape (String s) {
        return rcm.util.Str.escape (s, '\\', "\\^.$|()[]*+?{}");
    }

    String translateFields (String s) {
        Vector vfields = new Vector ();
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

                  case '(':
                    output.append (c);
                    if (s.startsWith ("?{", i+1)) {
                        int start = i+3;
                        int end = s.indexOf ('}', start);
                        vfields.addElement (s.substring (start, end));
                        i = end;
                    }
                    else if (!s.startsWith ("?", i+1))
                        vfields.addElement (String.valueOf (vfields.size()));
                    break;

                  default:
                    output.append (c);
                    break;
                }
            }
        }

        fields = new String[vfields.size()];
        vfields.copyInto (fields);
        return output.toString ();
    }
    
    public static void main (String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println ("usage: Regexp <pattern> <source URL>*");
            return;
        }

        Pattern p = new Regexp (args[0].replace ('_', ' ') );
        for (int i=1; i<args.length; ++i) {
            Page page = new Page (new Link (args[i]));
            System.out.println ("--------------------" + args[i]);
            PatternMatcher m = p.match (page);
            for (Region r = m.nextMatch(); r != null; r = m.nextMatch()) {
                System.out.println ("[" + r.getStart() + "," + r.getEnd() + "]" + r);
                Enumeration enum = r.enumerateObjectLabels ();
                while (enum.hasMoreElements ()) {
                    String lbl = (String)enum.nextElement ();
                    Object object = r.getObjectLabel (lbl);
                    if (object instanceof Region) {
                        Region s = (Region)object;
                        System.out.println ("    "+lbl+"=[" + s.getStart() + "," + s.getEnd() + "]" + s);
                    }
                }
            }
        }
    }
}

class RegexpMatcher extends PatternMatcher {
    Regexp regexp;
    Region source;
    org.apache.regexp.RE re;
    String content;
    int pos;

    public RegexpMatcher (Regexp regexp, Region source) {
        this.regexp = regexp;
        this.source = source;
        this.re = new org.apache.regexp.RE (regexp.pattern, 0);
        this.content = source.toString ();
        this.pos = 0;
    }

    protected Region findNext () {
        if (pos < content.length () && re.match (content, pos)) {
            pos = Math.max (pos+1, re.getParenEnd (0));
 
            Page page = source.getSource ();
            int base = source.getStart ();
            Region match = new Region (page, 
                                       base + re.getParenStart (0),
                                       base + re.getParenEnd (0));
            
            int n = re.getParenCount () - 1;
            Region[] groups = new Region[n];
            for (int i=0; i<n; ++i) {
                Region r = new Region (page, 
                                       base + re.getParenStart (i+1),
                                       base + re.getParenEnd (i+1));
                groups[i] = r;
                match.setField (regexp.fields[i], r);
            }
            match.setFields (Pattern.groups, groups);
            return match;
        }
        else {
            pos = content.length ();
            return null;
        }
    }
}
