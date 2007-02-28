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

import rcm.util.Str;
import java.util.Enumeration;

/**
 * Tag pattern.  Tag patterns are regular expressions over
 * the alphabet of HTML tags.
 */
public class Tagexp extends Regexp {
    String stringRep;
    
    public Tagexp (String pattern) {
        super (toRegexp (pattern));
        stringRep = pattern;
    }
    
    public boolean equals (Object object) {
        if (! (object instanceof Tagexp))
            return false;
        Tagexp p = (Tagexp)object;
        return p.stringRep.equals (stringRep);
    }        
    
    public String toString () {
        return stringRep;
    }

    public PatternMatcher match (Region region) {
        return new TagexpMatcher (this, region);
    }

    static HTMLParser parser = new HTMLParser ();

    public static String toRegexp (String tagexp) {
        StringBuffer output = new StringBuffer ();

        // parse the tagexp as HTML
        Page page;
        //System.err.println ("Parsing: " + tagexp);
        synchronized (parser) {
            page = new Page (null, tagexp, parser);
        }

        // canonicalize the tags
        Region[] tokens = page.getTokens ();
        for (int i=0; i<tokens.length; ++i) {
            //System.err.println ("tok=" + tokens[i].toHTML());
            if (tokens[i] instanceof Tag)
                canonicalizeTagPattern (output, (Tag)tokens[i]);
            else
                translateText (output, tokens[i].toString ());
        }

        //System.err.println ("regexp=" + output);
        return output.toString ();
    }

    static void canonicalizeTag (StringBuffer output, Tag tag, int j) {
        String tagName = tag.getTagName ();
        if (tagName == Tag.COMMENT)
            return;  // don't put comments or decls in the canonicalization

        output.append ('<');
        if (tag.isEndTag ())
            output.append ('/');
        output.append (tagName);
        output.append ('#');
        output.append (String.valueOf (j));
        output.append ('#');

        if (tag.countHTMLAttributes () > 0) {
            String[] attrs = tag.getHTMLAttributes ();
            sortAttrs (attrs);

            for (int i=0; i<attrs.length; ) {
                String name = attrs[i++];
                String value = attrs[i++];

                output.append (' ');
                output.append (name);

                if (value != Region.TRUE) {
                    output.append ('=');
                    value = encodeAttrValue (value);
                    output.append (value);
                }

                output.append (' ');
            }
        }

        output.append ('>');
    }

    static void canonicalizeTagPattern (StringBuffer output, Tag tag) {
        String tagName = tag.getTagName ();
        if (tagName == Tag.COMMENT)
            return;  // don't put comments or decls in the canonicalization

        output.append ('<');
        if (tag.isEndTag ())
            output.append ('/');
        translatePattern (output, tagName, "#");
        output.append ('#');
        output.append ("\\d+");
        output.append ('#');

        output.append ("[^>]*");

        if (tag.countHTMLAttributes () > 0) {
            String[] attrs = tag.getHTMLAttributes ();
            sortAttrs (attrs);

            for (int i=0; i<attrs.length; ) {
                String name = attrs[i++];
                String value = attrs[i++];

                output.append (' ');
                translatePattern (output, name, "= >");

                if (value != Region.TRUE) {
                    output.append ('=');
                    value = encodeAttrValue (value);
                    translatePattern (output, value, " >");
                }

                output.append (' ');
                output.append ("[^>]*");
            }
        }

        output.append ('>');
    }

    static void sortAttrs (String[] attrs) {
        // simple insertion sort suffices (since attrs.length is
        // almost always less than 5
        for (int i=2; i<attrs.length; i+=2) {
            String name = attrs[i];
            String value = attrs[i+1];

            int j;
            for (j=i; j > 0 && attrs[j-2].compareTo (name) > 0; j-=2) {
                attrs[j] = attrs[j-2];
                attrs[j+1] = attrs[j-1];
            }

            attrs[j] = name;
            attrs[j+1] = value;
        }
    }

    static String encodeAttrValue (String value) {
        if (value.indexOf ('%') != -1)
            value = Str.replace (value, "%", "%25");
        if (value.indexOf (' ') != -1)
            value = Str.replace (value, " ", "%20");
        if (value.indexOf ('<') != -1)
            value = Str.replace (value, "<", "%3C");
        if (value.indexOf ('>') != -1)
            value = Str.replace (value, ">", "%3E");
        return value;
    }

    static String translatePattern (StringBuffer output, String s, String delimiters) {
        s = Wildcard.toRegexp (s);

        boolean inEscape = false;

        int len = s.length ();
        for (int i=0; i<len; ++i) {
            char c = s.charAt (i);
            if (inEscape) {
                output.append (c);
                inEscape = false;
            }
            else if (c == '\\') {
                output.append (c);
                inEscape = true;
            }
            else if (c == '.') {
                output.append ("[^");
                output.append (delimiters);
                output.append (']');
            }
            else {
                output.append (c);
            }
        }

        return output.toString ();
    }

    static void translateText (StringBuffer output, String s) {
        // NIY: (@<tag>) and (<tag>@)
        s = Str.replace (s, ".", "(?:<[^>]*>)");
        output.append (s);
    }

    public static void main (String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println ("usage: Tagexp <pattern> <source URL>*");
            return;
        }

        Pattern p = new Tagexp (args[0].replace ('_', ' ') );
        for (int i=1; i<args.length; ++i) {
            Page page = new Page (new Link (args[i]));
            //System.out.println (page.substringCanonicalTags (0, page.getEnd()));

            System.out.println ("-----------" + args[i]);
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

class TagexpMatcher extends PatternMatcher {
    Tagexp tagexp;
    Region source;
    org.apache.regexp.RE re;
    String canon;
    int pos;

    public TagexpMatcher (Tagexp tagexp, Region source) {
        this.tagexp = tagexp;
        this.source = source;
        this.re = new org.apache.regexp.RE (tagexp.pattern, 0);
        this.canon = 
            source.getSource().substringCanonicalTags (source.getStart(), 
                                                       source.getLength ());
        this.pos = 0;
    }

    protected Region findNext () {
        if (pos < canon.length () && re.match (canon, pos)) {
            pos = Math.max (pos+1, re.getParenEnd (0));

            Page page = source.getSource ();
            
            Region match = mapCanonical2Region (page, canon, 
                                                re.getParenStart (0), 
                                                re.getParenEnd (0));
            
            int n = re.getParenCount () - 1;
            Region[] groups = new Region[n];
            for (int i=0; i<n; ++i) {
                Region r = mapCanonical2Region (page, canon, 
                                                re.getParenStart (i+1), 
                                                re.getParenEnd (i+1));
                groups[i] = r;
                match.setField (tagexp.fields[i] != null 
                                ? tagexp.fields[i] 
                                : String.valueOf (i), r);
            }
            match.setFields (Pattern.groups, groups);
            return match;
        }
        else
            return null;
    }

    final static Region mapCanonical2Region (Page page, String canon, int start, int end) {
        // NIY: (@ and @)
        Region[] tokens = page.getTokens ();
        int ft, lt;

        if (start == end) {
            ft = prevTag (canon, start);
            lt = nextTag (canon, end);

            if (ft != -1)
                if (lt != -1)
                    return new Region (page, tokens[ft].getEnd(), tokens[lt].getStart());
                else
                    return new Region (page, tokens[ft].getEnd(), page.getEnd ());
            else
                if (lt != -1)
                    return new Region (page, page.getStart(), tokens[lt].getStart());
                else
                    return page;
        }
        else {
            ft = nextTag (canon, start);
            lt = prevTag (canon, end);

            Tag f = (Tag)tokens[ft];
            Tag l = (Tag)tokens[lt];
            Element e = f.getElement ();
            if (e != null && e.getStart() == f.getStart() && e.getEnd() == l.getEnd())
                return e;
            else if (ft == lt)
                return tokens[ft];
            else
                return tokens[ft].span (tokens[lt]);
        }
    }

    final static int nextTag (String canon, int p) {
        return indexOfTag (canon, canon.indexOf ('<', p));
    } 

    final static int prevTag (String canon, int p) {
        if (p == 0)
            return -1;
        return indexOfTag (canon, canon.lastIndexOf ('<', p-1));
    } 

    final static int indexOfTag (String canon, int p) {
        if (p == -1)
            return -1;
        int s = canon.indexOf ('#', p);
        if (s == -1)
            return -1;
        int e = canon.indexOf ('#', s+1);
        if (e == -1)
            return -1;
        return Integer.parseInt (canon.substring (s+1, e));
    }
}
