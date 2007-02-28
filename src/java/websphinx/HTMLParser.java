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

import java.io.InputStream;
import java.io.IOException;
//#ifdef JDK1.1 
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.StringReader;
//#endif JDK1.1
/*#ifdef JDK1.0
import java.io.StringBufferInputStream;
#endif JDK1.0*/
import java.io.ByteArrayOutputStream;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Stack;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * HTML parser.  Parses an input stream or String and
 * converts it to a sequence of Tags and a tree of Elements.
 * HTMLParser is used by Page to parse pages.
 */
// FIX: make HTMLParser into an interface, and
// split this implementation into Tokenizer and TreeBuilder
public class HTMLParser {

    // parameter for HTML type detection.  
    // If the parser doesn't encounter at least one HTML tag
    // in the first VALID_HTML_PREFIX bytes of the stream, then parser 
    // concludes that the stream isn't HTML and stops parsing it.
    static final int VALID_HTML_PREFIX = 10000;

    int maxBytes = Integer.MAX_VALUE;

    /**
     * Make an HTMLParser.
     */
    public HTMLParser () {
    }

    /**
     * Parse a page as HTML.
     * @param page Page to parse
     */
    public void parse (Page page) throws IOException {
        tokenize (page);
        buildParseTree (page);
    }


    /*
     *  HTML tokenizer state machine
     */

    // state takes on one of the following values:
    private static final int START = 0;
    private static final int INWORD = 1;
    private static final int ENTITY = 2;
    private static final int LT = 4;
    private static final int BANG = 5;
    private static final int BANG_DASH = 6;
    private static final int CMT = 7;
    private static final int CMT_DASH = 8;
    private static final int CMT_DASHDASH = 9;
    private static final int DIRECTIVE = 10;
    private static final int STAG = 11;
    private static final int ETAG = 12;
    private static final int ATTR = 13;
    private static final int ATTRNAME = 14;
    private static final int EQ = 15;
    private static final int AFTEREQ = 16;
    private static final int ATTRVAL = 17;
    private static final int ATTRVAL_SQ = 18;
    private static final int ATTRVAL_DQ = 19;
    private static final int DONE = 20;
    private static final int ENTNUM = 21;
    private static final int ENTREF = 22;

    StringBuffer wordBuf = new StringBuffer ();
    StringBuffer tagName = new StringBuffer ();
    StringBuffer attrName = new StringBuffer ();
    StringBuffer attrVal = new StringBuffer ();
    Vector attrs = new Vector ();
    StringBuffer entity = new StringBuffer ();

    // FIX: should entities in attr names or values be expanded?
    private void tokenize (Page page) throws IOException {
        int state = START;

        String content = page.getContent ();
        int buflen = content.length ();
        int bufptr = 0;
        int bufbase = 0;

        // token list
        Vector tokens = new Vector();

        int wordStart = 0;
        int nWords = 0;

        Tag tag = null;
        int tagStart = 0;

        int entnum = 0;
        
        StringBuffer entityTargetBuf = null;
        int postEntityState = 0;

        boolean isHTML = "text/html".equals (page.getContentType ());

        while (bufptr < buflen) {
            if (!isHTML && bufptr >= VALID_HTML_PREFIX)
                // we didn't see any HTML tags in the first
                // VALID_HTML_PREFIX bytes,
                // so assume the document isn't HTML and stop parsing it.
                return;

            char c = content.charAt (bufptr);

            //System.err.println ("%% state == " + state + ", ptr == " + (bufbase+bufptr) + ", c == " + c);

            switch (state) {
                case START:
                    // after whitespace or tag
                    switch (c) {
                        case '<':
                            ++bufptr;
                            state = LT;
                            break;
                        case ' ':
                        case '\t':
                        case '\n':
                        case '\r':
                            ++bufptr;
                            break;
                        default:
                            wordBuf.setLength (0);
                            wordStart = bufbase+bufptr;
                            state = INWORD;
                            break;
                    }
                    break;

                case INWORD:
                    // Character data
                    switch (c) {
                        case '<':
                            tokens.addElement (new Text (page, wordStart, bufbase+bufptr, wordBuf.toString ()));
                            ++nWords;
                            state = START;
                            break;
                        case ' ':
                        case '\t':
                        case '\n':
                        case '\r':
                            tokens.addElement (new Text (page, wordStart, bufbase+bufptr, wordBuf.toString ()));
                            ++nWords;
                            state = START;
                            ++bufptr;
                            break;
                        case '&':
                            ++bufptr;
                            postEntityState = INWORD;
                            entityTargetBuf = wordBuf;
                            state = ENTITY;
                            break;
                        default:
                            wordBuf.append ((char)c);
                            ++bufptr;
                            // state == INWORD;
                            break;
                    }
                    break;

                //  Entities
                case ENTITY:
                    if (c == '#') {
                        ++bufptr;
                        entnum = 0;
                        state = ENTNUM;
                    }
                    else if ((c >= 'A' && c <= 'Z')
                             || (c >= 'a' && c <= 'z')) {
                        entity.setLength (0);
                        state = ENTREF;
                    }
                    else {
                        entityTargetBuf.append ('&');
                        state = postEntityState;
                    }
                    break;

                case ENTREF:
                    if (!Character.isLetterOrDigit(c)) {
                        Character ent = lookupEntityRef (entity.toString ());
                        if (ent != null) {
                            entityTargetBuf.append (ent.charValue());
                            if (c == ';')
                                ++bufptr;
                        }
                        else {
                            // unrecognized entity -- leave
                            // as-is
                            entityTargetBuf.append ('&');
                            entityTargetBuf.append (entity.toString ());
                        }                            
                        state = postEntityState;
                    }
                    else {
                        ++bufptr;
                        entity.append (c);
                        // state == ENTREF;
                    }
                    break;

                case ENTNUM:
                    if (c==';' || !Character.isDigit(c)) {
                        entityTargetBuf.append ((char) entnum);
                        if (c == ';')
                            ++bufptr;
                        state = postEntityState;
                    }
                    else {
                        entnum = 10*entnum + (c - '0');
                        ++bufptr;
                    }
                    break;

                case LT:
                    tagStart = bufbase+bufptr-1;
                    switch (c) {
                        case '/':
                            ++bufptr;
                            tagName.setLength (0);
                            state = ETAG;
                            break;
                        case '!':
                            ++bufptr;
                            state = BANG;
                            break;
                        default:
                            if (Character.isLetter (c)) {
                                tagName.setLength (0);
                                state = STAG;
                            }
                            else {
                                wordBuf.append ('<');
                                state = INWORD;
                            }
                            break;
                    }
                    break;

                // Comments and directives.
                // Implements the (broken, but easy) Netscape rule:
                // <!-- starts a comment, --> closes.
                // All other directives <!foo> are also returned as comments.
                case BANG:
                    if (c == '-') {
                        ++bufptr;
                        state = BANG_DASH;
                    }
                    else {
                        state = DIRECTIVE;
                    }
                    break;

                case BANG_DASH:
                    if (c == '-') {
                        ++bufptr;
                        state = CMT;
                    }
                    else {
                        state = DIRECTIVE;
                    }
                    break;

                case CMT:
                    if (c == '-') {
                        ++bufptr;
                        state = CMT_DASH;
                    }
                    else {
                        ++bufptr;
                    }
                    break;

                case CMT_DASH:
                    if (c == '-') {
                        ++bufptr;
                        state = CMT_DASHDASH;
                    }
                    else {
                        ++bufptr;
                        state = CMT;
                    }
                    break;

                case CMT_DASHDASH:
                    if (c == '>') {
                        ++bufptr;
                        tag = new Tag (page, tagStart, bufbase+bufptr, Tag.COMMENT, true);
                        tokens.addElement (tag);
                        state = START;
                    }
                    else if (c == '-') {
                        ++bufptr;
                        state = CMT_DASHDASH;
                    }
                    else {
                        ++bufptr;
                        state = CMT;
                    }
                    break;

                case DIRECTIVE:
                    if (c == '>') {
                        ++bufptr;
                        tag = new Tag (page, tagStart, bufbase+bufptr, Tag.COMMENT, true);
                        tokens.addElement (tag);
                        state = START;
                    }
                    else {
                        ++bufptr;
                    }
                    break;

                // Tags
                case STAG:
                    if (c == '>' || isWhitespace(c)) {
                        tag = new Tag (page, tagStart, bufbase+bufptr, // tag doesn't really end here
                                                                       // -- we'll fix it up when we actually see it
                                       tagName.toString (), true);
                        tokens.addElement (tag);
                        attrs.setSize (0);
                        state = ATTR;
                        isHTML = true;
                    }
                    else {
                        tagName.append (c);
                        ++bufptr;
                        // state == STAG;
                    }
                    break;

                case ETAG:
                    if (c == '>') {
                        ++bufptr;
                        tag = new Tag (page, tagStart, bufbase+bufptr, tagName.toString (), false);
                        tokens.addElement (tag);
                        state = START;
                    }
                    else {
                        tagName.append (c);
                        ++bufptr;
                        // state == ETAG
                    }
                    break;

                // Attributes
                case ATTR:
                    if (isWhitespace(c))
                        ++bufptr;
                    else if (c == '>') {
                        ++bufptr;
                        tag.end = bufbase+bufptr;
                        if (attrs.size() > 0) {
                            tag.htmlAttributes = new String[attrs.size()];
                            attrs.copyInto (tag.htmlAttributes);
                        }
                        state = START;
                    }
                    else {
                        attrName.setLength (0);
                        state = ATTRNAME;
                    }
                    break;

                case ATTRNAME:
                    if (c == '>' || c == '=' || isWhitespace(c)) {
                        state = EQ;
                    }
                    else {
                        attrName.append (c);
                        ++bufptr;
                        // state == ATTRNAME;
                    }
                    break;

                case EQ:
                    if (isWhitespace(c))
                        ++bufptr;
                    else if (c == '=') {
                        ++bufptr;
                        state = AFTEREQ;
                    }
                    else {
                        String name = Tag.toHTMLAttributeName (attrName.toString());
                        tag.setLabel (name);
                        attrs.addElement (name);
                        state = ATTR;
                    }
                    break;

                case AFTEREQ:
                    if (isWhitespace (c))
                        ++bufptr;
                    else
                        switch (c) {
                            case '>':
                            {
                                String name = Tag.toHTMLAttributeName (attrName.toString());
                                tag.setLabel (name);
                                attrs.addElement (name);
                                state = ATTR;
                                break;
                            }
                            case '\'':
                                ++bufptr;
                                attrVal.setLength (0);
                                state = ATTRVAL_SQ;
                                break;
                            case '"':
                                ++bufptr;
                                attrVal.setLength (0);
                                state = ATTRVAL_DQ;
                                break;
                            default:
                                attrVal.setLength (0);
                                state = ATTRVAL;
                                break;
                        }
                    break;

                case ATTRVAL:
                    if (c == '>' || isWhitespace(c)) {
                        String name = Tag.toHTMLAttributeName (attrName.toString());
                        tag.setLabel (name, attrVal.toString());
                        attrs.addElement (name);
                        state = ATTR;
                    }
                    else if (c == '&') {
                        ++bufptr;
                        postEntityState = ATTRVAL;
                        entityTargetBuf = attrVal;
                        state = ENTITY;
                    }
                    else {
                        ++bufptr;
                        attrVal.append (c);
                        // state == ATTRVAL;
                    }
                    break;

                case ATTRVAL_SQ:
                    if (c=='\'') {
                        ++bufptr;
                        String name = Tag.toHTMLAttributeName (attrName.toString());
                        tag.setLabel (name, attrVal.toString());
                        attrs.addElement (name);
                        state = ATTR;
                    }
                    else if (c == '&') {
                        ++bufptr;
                        postEntityState = ATTRVAL_SQ;
                        entityTargetBuf = attrVal;
                        state = ENTITY;
                    }
                    else {
                        ++bufptr;
                        attrVal.append (c);
                        // state == ATTRVAL_SQ;
                    }
                    break;

                case ATTRVAL_DQ:
                    if (c=='"') {
                        ++bufptr;
                        String name = Tag.toHTMLAttributeName (attrName.toString());
                        tag.setLabel (name, attrVal.toString());
                        attrs.addElement (name);
                        state = ATTR;
                    }
                    else if (c == '&') {
                        ++bufptr;
                        postEntityState = ATTRVAL_DQ;
                        entityTargetBuf = attrVal;
                        state = ENTITY;
                    }
                    else {
                        ++bufptr;
                        attrVal.append (c);
                        // state == ATTRVAL_DQ;
                    }
                    break;

                default:
                    throw new RuntimeException ("HtmlTokenizer entered illegal state " + state);
            }
        }

        // EOF
        switch (state) {
            case INWORD:
                // EOF terminated some text -- save the text
                tokens.addElement (new Text (page, wordStart, bufbase+bufptr, wordBuf.toString ()));
                ++nWords;
                break;

            default:
                // EOF in the middle of tags is illegal
                // don't try to recover
                break;
        }

        int nTotal = tokens.size ();

        page.tokens = new Region[nTotal];
        tokens.copyInto (page.tokens);

        page.words = new Text[nWords];
        int textnum = 0;
        page.tags = new Tag[nTotal - nWords];
        int tagnum = 0;

        for (int i=0; i < nTotal; ++i) {
            if (page.tokens[i] instanceof Tag)
                page.tags[tagnum++] = (Tag)page.tokens[i];
            else
                page.words[textnum++] = (Text)page.tokens[i];
        }
    }

    private static boolean isWhitespace (char c) {
//#ifdef JDK1.1 
        return Character.isWhitespace (c);
//#endif JDK1.1
/*#ifdef JDK1.0
        return Character.isSpace (c);
#endif JDK1.0*/
    }
        

    private static Hashtable entities = new Hashtable2()
          .add ("quot", new Character ((char)34))
          .add ("amp", new Character ((char)38))
          .add ("lt", new Character ((char)60))
          .add ("gt", new Character ((char)62))
          .add ("nbsp", new Character ((char)160))
          .add ("iexcl", new Character ((char)161))
          .add ("cent", new Character ((char)162))
          .add ("pound", new Character ((char)163))
          .add ("curren", new Character ((char)164))
          .add ("yen", new Character ((char)165))
          .add ("brvbar", new Character ((char)167))
          .add ("sect", new Character ((char)167))
          .add ("uml", new Character ((char)168))
          .add ("copy", new Character ((char)169))
          .add ("ordf", new Character ((char)170))
          .add ("laquo", new Character ((char)171))
          .add ("not", new Character ((char)172))
          .add ("shy", new Character ((char)173))
          .add ("reg", new Character ((char)174))
          .add ("macr", new Character ((char)175))
          .add ("deg", new Character ((char)176))
          .add ("plusmn", new Character ((char)177))
          .add ("sup2", new Character ((char)178))
          .add ("sup3", new Character ((char)179))
          .add ("acute", new Character ((char)180))
          .add ("micro", new Character ((char)181))
          .add ("para", new Character ((char)182))
          .add ("middot", new Character ((char)183))
          .add ("cedil", new Character ((char)184))
          .add ("sup1", new Character ((char)185))
          .add ("ordm", new Character ((char)186))
          .add ("raquo", new Character ((char)187))
          .add ("frac14", new Character ((char)188))
          .add ("frac12", new Character ((char)189))
          .add ("frac34", new Character ((char)190))
          .add ("iquest", new Character ((char)191))
          .add ("Agrave", new Character ((char)192))
          .add ("Aacute", new Character ((char)193))
          .add ("Acirc", new Character ((char)194))
          .add ("Atilde", new Character ((char)195))
          .add ("Auml", new Character ((char)196))
          .add ("Aring", new Character ((char)197))
          .add ("AElig", new Character ((char)198))
          .add ("Ccedil", new Character ((char)199))
          .add ("Egrave", new Character ((char)200))
          .add ("Eacute", new Character ((char)201))
          .add ("Ecirc", new Character ((char)202))
          .add ("Euml", new Character ((char)203))
          .add ("Igrave", new Character ((char)204))
          .add ("Iacute", new Character ((char)205))
          .add ("Icirc", new Character ((char)206))
          .add ("Iuml", new Character ((char)207))
          .add ("ETH", new Character ((char)208))
          .add ("Ntilde", new Character ((char)209))
          .add ("Ograve", new Character ((char)210))
          .add ("Oacute", new Character ((char)211))
          .add ("Ocirc", new Character ((char)212))
          .add ("Otilde", new Character ((char)213))
          .add ("Ouml", new Character ((char)214))
          .add ("times", new Character ((char)215))
          .add ("Oslash", new Character ((char)216))
          .add ("Ugrave", new Character ((char)217))
          .add ("Uacute", new Character ((char)218))
          .add ("Ucirc", new Character ((char)219))
          .add ("Uuml", new Character ((char)220))
          .add ("Yacute", new Character ((char)221))
          .add ("THORN", new Character ((char)222))
          .add ("szlig", new Character ((char)223))
          .add ("agrave", new Character ((char)224))
          .add ("aacute", new Character ((char)225))
          .add ("acirc", new Character ((char)226))
          .add ("atilde", new Character ((char)227))
          .add ("auml", new Character ((char)228))
          .add ("aring", new Character ((char)229))
          .add ("aelig", new Character ((char)230))
          .add ("ccedil", new Character ((char)231))
          .add ("egrave", new Character ((char)232))
          .add ("eacute", new Character ((char)233))
          .add ("ecirc", new Character ((char)234))
          .add ("euml", new Character ((char)235))
          .add ("igrave", new Character ((char)236))
          .add ("iacute", new Character ((char)237))
          .add ("icirc", new Character ((char)238))
          .add ("iuml", new Character ((char)239))
          .add ("eth", new Character ((char)240))
          .add ("ntilde", new Character ((char)241))
          .add ("ograve", new Character ((char)242))
          .add ("oacute", new Character ((char)243))
          .add ("ocirc", new Character ((char)244))
          .add ("otilde", new Character ((char)245))
          .add ("ouml", new Character ((char)246))
          .add ("divide", new Character ((char)247))
          .add ("oslash", new Character ((char)248))
          .add ("ugrave", new Character ((char)249))
          .add ("uacute", new Character ((char)250))
          .add ("ucirc", new Character ((char)251))
          .add ("uuml", new Character ((char)252))
          .add ("yacute", new Character ((char)253))
          .add ("thorn", new Character ((char)254))
          .add ("yuml", new Character ((char)255))
          ;

    private static Character lookupEntityRef (String name) {
        return (Character) entities.get (name);
    }

    /*
     *  Parser (constructs a canonical tree of elements)
     *
     */

    Vector vElements = new Vector ();
    Vector vLinks = new Vector ();

    StringBuffer text = new StringBuffer ();

    // elements with no content: e.g., IMG, BR, HR.  End tags for these elements
    // are simply ignored.
    private static Hashtable empty = new Hashtable2 ()
          .add (Tag.AREA)
          .add (Tag.BASE)
          .add (Tag.BASEFONT)
          .add (Tag.BGSOUND)
          .add (Tag.BR)
          .add (Tag.COL)
          .add (Tag.COLGROUP)
          .add (Tag.COMMENT) // actually <!-- ... -->
          .add (Tag.HR)
          .add (Tag.IMG)
          .add (Tag.INPUT)
          .add (Tag.ISINDEX)
          .add (Tag.LINK)
          .add (Tag.META)
          .add (Tag.NEXTID)
          .add (Tag.PARAM)
          .add (Tag.SPACER)
          .add (Tag.WBR)
          ;

    // elements that close <P> (correspond to "%block" entity in HTML 3.2 DTD)
    static Hashtable blocktag = new Hashtable2()
          .add (Tag.P)
          .add (Tag.UL)
          .add (Tag.OL)
          .add (Tag.DIR)
          .add (Tag.MENU)
          .add (Tag.PRE)
          .add (Tag.XMP)
          .add (Tag.LISTING)
          .add (Tag.DL)
          .add (Tag.DIV)
          .add (Tag.CENTER)
          .add (Tag.BLOCKQUOTE)
          .add (Tag.FORM)
          .add (Tag.ISINDEX)
          .add (Tag.HR)
          .add (Tag.TABLE)
          .add (Tag.H1)
          .add (Tag.H2)
          .add (Tag.H3)
          .add (Tag.H4)
          .add (Tag.H5)
          .add (Tag.H6)
          .add (Tag.ADDRESS)
          ;

    // maps elements which force closure to the elements that they close, e.g.,
    // LI maps to LI, DT maps to DD,DT, and all block-level tags map to P.
    private static Hashtable forcesClosed = new Hashtable2 ()
          .add (Tag.DD, new Hashtable2 () .add (Tag.DD) .add (Tag.DT))
          .add (Tag.DT, new Hashtable2 () .add (Tag.DD) .add (Tag.DT))
          .add (Tag.LI, new Hashtable2 () .add (Tag.LI))
          .add (Tag.OPTION, new Hashtable2 () .add (Tag.OPTION))
          .add (Tag.TR, new Hashtable2 () .add (Tag.TR))
          .add (Tag.TD, new Hashtable2 () .add (Tag.TD) .add (Tag.TH))
          .add (Tag.TH, new Hashtable2 () .add (Tag.TD) .add (Tag.TH))
          ;
    static {
        Hashtable p = new Hashtable2 () .add (Tag.P);
        Enumeration enum = blocktag.keys ();
        while (enum.hasMoreElements ())
            union (forcesClosed, enum.nextElement(), p);
    }

    // union of forcesClosed plus the tag's possible containers.  For instance,
    // LI maps to LI, OL, UL, MENU, DIR.  When a forcesClosed tag like LI is
    // encountered, the parser looks upward for the first context tag.
    // Having the tag's container element included in the search ensures that
    // LI in a nested list won't close its parent LI.
    static Hashtable context = new Hashtable2 ()
          .add (Tag.DD, new Hashtable2 () .add (Tag.DL))
          .add (Tag.DT, new Hashtable2 () .add (Tag.DL))
          .add (Tag.LI, new Hashtable2 () .add (Tag.OL) .add (Tag.UL) .add (Tag.MENU) .add (Tag.DIR))
          .add (Tag.OPTION, new Hashtable2 () .add (Tag.SELECT))
          .add (Tag.TR, new Hashtable2 () .add (Tag.TABLE))
          .add (Tag.TD, new Hashtable2 () .add (Tag.TR) .add (Tag.TABLE))
          .add (Tag.TH, new Hashtable2 () .add (Tag.TR) .add (Tag.TABLE))
          ;
    static {
        Enumeration enum = forcesClosed.keys ();
        while (enum.hasMoreElements ()) {
            Object tagname = enum.nextElement();
            union (context, tagname, (Hashtable)forcesClosed.get (tagname));
        }
    }

    // NIY: handle literal and semi-literal elements (XMP, LISTING, TEXTAREA, OPTION)
    // elements whose content should be treated as plain text
    static Hashtable literal = new Hashtable2()
            ;

    // maps link elements to their URL attribute (e.g., A maps to HREF)
    static Hashtable linktag = new Hashtable2 ()
            .add (Tag.A, "href")
            .add (Tag.AREA, "href")
            .add (Tag.APPLET, "code")
            .add (Tag.EMBED, "src")
            .add (Tag.FRAME, "src")
            .add (Tag.FORM, "action")
            .add (Tag.IMG, "src")
            .add (Tag.LINK, "href")
            .add (Tag.SCRIPT, "src")
            ;

    // elements whose text contents are crucial to the crawler
    static Hashtable savetext = new Hashtable2 ()
            .add (Tag.A)
            .add (Tag.TITLE);

    // elements found in <HEAD>
    static Hashtable headtag = new Hashtable2()
          .add (Tag.META)
          .add (Tag.TITLE)
          .add (Tag.BASE)
          .add (Tag.LINK)
          .add (Tag.ISINDEX)
          ;

    private static void union (Hashtable map, Object tagname, Hashtable tagset) {
        Hashtable2 currset = (Hashtable2)map.get (tagname);
        if (currset == null)
            map.put (tagname, tagset);
        else
            map.put (tagname, currset.union (tagset));
    }

    private void buildParseTree (Page page) {
        boolean keepText = false;

        elems.setSize (0);
        openPtr = 0;

        Region[] tokens = page.tokens;
        for (int t=0; t<tokens.length; ++t) {
            Region r = tokens[t];

            if (r instanceof Tag) {
                Tag tag = (Tag)r;
                String tagName = tag.getTagName();

                if (tag.isStartTag()) {
                    // start tag <X>

                    // check if <X> forces closure of an open element
                    if (forcesClosed.containsKey (tagName)) {
                        Element e = findOpenElement ((Hashtable)context.get (tagName));
                        if (e != null && ((Hashtable)forcesClosed.get (tagName)).containsKey (e.getTagName()))
                            close (e, tag.start);
                    }

                    // create the element and push it on the elems stack
                    Element e = makeElement (page.base, tag);
                    open (e);

                    if (empty.containsKey (tagName)) {
                        // element has no content
                        // close it off right now
                        close (e, tag.end);
                    }
                    else if (savetext.containsKey (tagName)) {
                        text.setLength (0);
                        keepText = true;
                    }

                    if (tagName == Tag.BASE) {
                        String href = tag.getHTMLAttribute ("href");
                        if (href != null) {
                            try {                         
                                page.base = new URL (page.base, new String (href.toCharArray())); // make copy to avoid reference to page content
                            } catch (MalformedURLException ex) {} // bad URL
                              catch (NullPointerException ex) {} // base == null
                        }
                    }
                }
                else {
                    // end tag </X>

                    // find matching start tag <X>
                    Element e = findOpenElement (tagName);
                    if (e != null) {
                        close (e, tag);

                        if (savetext.containsKey (tagName)) {
                            if (tagName == Tag.TITLE)
                                page.title = text.toString();
                            else if (e instanceof Link)
                                ((Link)e).setText (text.toString());
                            keepText = false;
                        }
                    }
                }

            }
            else { // r is a text token
                if (keepText) {
                    if (text.length() > 0)
                        text.append (' ');
                    text.append (r.toText());
                }
            }
        }

        // close any remaining open elements
        closeAll (page.end);

        // link together the top-level elements
        if (!elems.empty()) {
            int nElems = elems.size ();
            Element c = (Element)elems.elementAt (0);
            page.root = c;
            for (int j=1; j<nElems; ++j) {
                Element d = (Element)elems.elementAt (j);
                c.sibling = d;
                c = d;
            }
        }

        page.elements = new Element[vElements.size()];
        vElements.copyInto (page.elements);

        page.links = new Link[vLinks.size()];
        vLinks.copyInto (page.links);
    }

    private Element makeElement (URL base, Tag tag) {
        Element e = null;
        String tagName = tag.getTagName ();
        String hrefAttr = (String)linktag.get (tagName);
        String type;

        try {
            if (tagName == Tag.FORM) {
                e = new Form (tag, null, base);
                vLinks.addElement (e);
            }
            else if (tagName == Tag.INPUT 
                     && (type = tag.getHTMLAttribute ("type")) != null
                     && (type.equalsIgnoreCase ("submit") || type.equalsIgnoreCase ("image"))) {
                e = new FormButton (tag, null, currentForm);
                vLinks.addElement (e);
            }
            else if (hrefAttr != null && tag.hasHTMLAttribute (hrefAttr)) {
                e = new Link (tag, null, base);
                vLinks.addElement (e);
            }
        } catch (MalformedURLException f) {} // bad URL
          catch (NullPointerException ex) {} // base == null

        if (e == null)
            // just make an ordinary element
            e = new Element (tag, null);
            
        vElements.addElement (e);
        tag.element = e;
        return e;
    }

    // Stack management

    Stack elems = new Stack();
        // stack of Elements appearing before than the current element in
        // a preorder traversal, except that completely-visited subtrees
        // are represented by their root.
    int[] openElems = new int[20];
    int openPtr = 0;
        // stack of indices of open elements in elems

    Form currentForm;

    private void open (Element e) {
        if (openPtr > 0)
            e.parent = (Element)elems.elementAt (openElems[openPtr-1]);
        else
            e.parent = null;

        elems.push (e);
        if (e instanceof Form)
            currentForm = (Form)e;

        if (openPtr == openElems.length) {
            int[] newarr = new int[openElems.length + 10];
            System.arraycopy (openElems, 0, newarr, 0, openElems.length);
            openElems = newarr;
        }
        openElems[openPtr] = elems.size()-1;
        ++openPtr;
    }

    private Element findOpenElement (String tagname) {
        for (int i=openPtr-1; i >= 0; --i) {
            Element e = (Element)elems.elementAt (openElems[i]);
            if (tagname == e.getTagName ())
                return e;
        }
        return null;
    }

    private Element findOpenElement (Hashtable tags) {
        for (int i=openPtr-1; i >= 0; --i) {
            Element e = (Element)elems.elementAt (openElems[i]);
            if (tags.containsKey (e.getTagName ()))
                return e;
        }
        return null;
    }

    // NIY: stack up unclosed flow tags (like <B> and <A>) and reopen them
    // when the next element is opened
    private void close (Element elem, Tag tag) {
        elem.endTag = tag;
        tag.element = elem;
        close (elem, tag.start);
        elem.end = tag.end;
    }

    private void close (Element elem, int end) {
        int v;
        Element e;
        do {
            v = openElems[--openPtr];
            e = (Element)elems.elementAt (v);

            e.end = end;
            if (e instanceof Form)
                currentForm = null;

            int firstChild = v+1;
            int nElems = elems.size();
            if (firstChild < nElems) {
                Element c = (Element)elems.elementAt (firstChild);
                e.child = c;
                for (int j=firstChild+1; j<nElems; ++j) {
                    Element d = (Element)elems.elementAt (j);
                    c.sibling = d;
                    c = d;
                }
                elems.setSize (firstChild);
            }
            
        } while (e != elem);
    }

    private void closeAll (int end) {
        if (openPtr > 0)
            close ((Element)elems.elementAt (openElems[0]), end);
    }

    /*
     * Testing interface
     *
     */

    public static void main (String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println ("usage: HTMLParser <URL>");
            System.exit(-1);
        }

        Page page;
        if (args.length == 1)
            page = new Page (new Link(args[0]), new DownloadParameters (), new HTMLParser ());
        else
            page = new Page (new URL(args[0]), args[1], new HTMLParser ());

        /*
        long tm = System.currentTimeMillis();     //??dk
        HTMLParser tokenizer = new HTMLParser ();

        tm = System.currentTimeMillis() - tm;       //??dk
            System.err.println("[Parsed " + args[0] + " in " + tm + "ms]");
        */
 
        System.out.println ("Tokens: ------------------------------------------");
        Region[] tokens = page.tokens;
        for (int i=0; i<tokens.length; ++i) {
            System.out.println ("[" + tokens[i].getStart() + "," + tokens[i].getEnd() + "]" + tokens[i]);
        }

       System.out.println ("Tags: ------------------------------------------");
        Tag[] tags = page.tags;
        for (int i=0; i<tags.length; ++i) {
            Tag t = tags[i];
            System.out.print ((t.isStartTag() ? "start tag" : "end tag") + " " + t.getTagName ());

            Enumeration attrs = t.enumerateHTMLAttributes();
            String name, val;
            while (attrs.hasMoreElements()) {
                name = (String)attrs.nextElement();
                val = t.getHTMLAttribute (name);
                System.out.print (" " + name + "=\"" + val + "\"");
            }
            System.out.println ();
            System.out.println ("    " + t);
        }

        System.out.println ("Words: ------------------------------------------");
        Text[] words = page.words;
        for (int i=0; i<words.length; ++i) {
            System.out.println (words[i]);
        }

        System.out.println ("Elements: ------------------------------------------");
        printout (page.root, 0);

        System.out.println ("Links: ------------------------------------------");
        printout (page.getLinks (), 0);

    }

    private static String indentation (int indent) {
        StringBuffer s = new StringBuffer();
        for (int i=0; i<indent; ++i)
            s.append ("    ");
        return s.toString();
    }

    private static void printout (Element element, int indent) {
      for (Element e = element; e != null; e = e.getSibling ()) {
          Element c = e.getChild();

          System.out.println (indentation(indent) + e.getStartTag() + "[" + e.getStart() + "," + e.getEnd() + "]");
          if (c != null)
              printout (c, indent+1);
          if (e.getEndTag() != null)
              System.out.println (indentation(indent) + e.getEndTag());
      }
    }
    private static void printout (Link[] elements, int indent) {
        for (int i=0; i<elements.length; ++i) {
            Link e = elements[i];
            System.out.println (indentation(indent) + e.toDescription());
        }
    }
}

class Hashtable2 extends Hashtable {
    public Hashtable2 () {
    }

    public Hashtable2 add (Object key) {
        put (key, key);
        return this;
    }

    public Hashtable2 add (Object key, Object val) {
        put (key, val);
        return this;
    }

    public Hashtable2 union (Hashtable map) {
        Enumeration enum = map.keys ();
        while (enum.hasMoreElements ()) {
            Object key = enum.nextElement ();
            put (key, map.get (key));
        }

        return this;
    }
}
