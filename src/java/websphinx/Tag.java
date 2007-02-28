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

import java.util.Enumeration;
import rcm.enum.ArrayEnumeration;

/**
 * Tag in an HTML page.
 */
public class Tag extends Region {

    String tagName;
    boolean startTag;
    String[] htmlAttributes;// HTML attributes on this tag (lower case and interned)
    Element element;

    /**
     * Make a Tag.
     * @param page Page containing tag
     * @param start Starting offset of tag in page
     * @param end Ending offset of tag
     * @param tagName Name of tag (like "p")
     * @param startTag true for start tags (like "&lt;p&gt;"), false for end tags ("&lt;/p&gt;")
     */
    public Tag (Page page, int start, int end, String tagName, boolean startTag) {
        super (page, start, end);
        this.tagName = tagName.toLowerCase ().intern ();
        this.startTag = startTag;
        this.htmlAttributes = null;
    }

    /**
     * Get tag name.
     * @return tag name (like "p"), in lower-case, String.intern()'ed form.
     */
    public String getTagName () {
        return tagName;
    }

    /**
     * Get element to which this tag is the start or end tag.
     * @return element, or null if tag has no element.
     */
    public Element getElement () {
        return element;
    }

    /**
     * Convert a String to a tag name.  Tag names are lower-case, intern()'ed
     * Strings.  Thus you can compare tag names with ==, as in: 
     * <CODE>getTagName() == Tag.IMG</CODE>.
     * @param name Name to convert (e.g., "P")
     * @return tag name (e.g. "p"), in lower-case, String.intern()'ed form.
     */
    public static String toTagName (String name) {
        return name.toLowerCase().intern ();
    }

    /**
     * Test if tag is a start tag.  Equivalent to !isEndTag().
     * @return true if and only if tag is a start tag (like "&lt;P&gt;")
     */
    public boolean isStartTag () {
        return startTag;
    }

    /**
     * Test if tag is an end tag.  Equivalent to !isStartTag().
     * @return true if and only if tag is a start tag (like "&lt;/P&gt;")
     */
    public boolean isEndTag () {
        return !startTag;
    }

    /**
     * Test if tag is a block-level tag.  Equivalent to !isFlowTag().
     * @return true if and only if tag is a block-level tag (like "&lt;P&gt;")
     */
    public boolean isBlockTag () {
        return HTMLParser.blocktag.containsKey (tagName);
    }

    /**
     * Test if tag is a flow-level tag.  Equivalent to !isBlockTag().
     * @return true if and only if tag is a block-level tag (like "&lt;A&gt;")
     */
    public boolean isFlowTag () {
        return !isBlockTag ();
    }

    /**
     * Test if tag belongs in the <HEAD> element.
     * @return true if and only if tag is a HEAD-level tag (like "&lt;TITLE&gt;")
     */
    public boolean isHeadTag () {
        return HTMLParser.headtag.containsKey (tagName);
    }

    /**
     * Test if tag belongs in the <BODY> element.
     * @return true if and only if tag is a BODY-level tag (like "&lt;A&gt;")
     */
    public boolean isBodyTag () {
        return !isHeadTag() 
                && tagName != HTML 
                && tagName != HEAD 
                && tagName != BODY;
    }

    /**
     * Convert a String to an HTML attribute name.  Attribute names are
     * lower-case, intern()'ed
     * Strings.  Thus you can compare attribute names with ==.
     * @param name Name to convert (e.g., "HREF")
     * @return tag name (e.g. "href"), in lower-case, String.intern()'ed form.
     */
    public static String toHTMLAttributeName (String name) {
        return name.toLowerCase ().intern ();
    }

    /**
     * Test if tag has an HTML attribute.
     * @param name Name of HTML attribute (e.g. "HREF").  Doesn't have to be
     * converted with toHTMLAttributeName(). 
     * @return true if tag has the attribute, false if not
     */
    public boolean hasHTMLAttribute (String name) {
        if (htmlAttributes == null)
            return false;
        name = toHTMLAttributeName (name);
        for (int i=0; i<htmlAttributes.length; ++i)
            if (htmlAttributes[i] == name)
                return true;
        return false;
    }

    /**
     * Get an HTML attribute's value.
     * @param name Name of HTML attribute (e.g. "HREF").  Doesn't have to be
     * converted with toHTMLAttributeName(). 
     * @return value of attribute if it exists, TRUE if the attribute exists but has no value, or null if tag lacks the attribute.
     */
    public String getHTMLAttribute (String name) {
        if (htmlAttributes == null)
            return null;
        name = toHTMLAttributeName (name);
        for (int i=0; i<htmlAttributes.length; ++i)
            if (htmlAttributes[i] == name)
                return getLabel (name);
        return null;
    }

    /**
     * Get an HTML attribute's value, with a default value if it doesn't exist.
     * @param name Name of HTML attribute (e.g. "HREF").  Doesn't have to be
     * converted with toHTMLAttributeName(). 
     * @param defaultValue default value to return if the attribute 
     * doesn't exist
     * @return value of attribute if it exists, TRUE if the attribute exists but has no value, or defaultValue if tag lacks the attribute.
     */
    public String getHTMLAttribute (String name, String defaultValue) {
        String val = getHTMLAttribute (name);
        return val != null ? val : defaultValue;
    }
    
    /**
     * Get number of HTML attributes on this tag.
     * @return number of HTML attributes
     */
    public int countHTMLAttributes () {
        return htmlAttributes != null ? htmlAttributes.length : 0;
    }

    /**
     * Get all the HTML attributes found on this tag.
     * @return array of name-value pairs, alternating between 
     * names and values.  Thus array[0] is a name, array[1] is a value,
     * array[2] is a name, etc.
     */
    public String[] getHTMLAttributes () {
        if (htmlAttributes == null)
            return new String[0];

        String[] result = new String[htmlAttributes.length * 2];
        for (int i=0, j=0; i<htmlAttributes.length; ++i) {
            String name = htmlAttributes[i];
            result[j++] = name;
            result[j++] = getLabel (name);
        }
        return result;
    }

    /**
     * Enumerate the HTML attributes found on this tag.
     * @return enumeration of the attribute names found on this tag.
     */
    public Enumeration enumerateHTMLAttributes () {
        return new ArrayEnumeration (htmlAttributes);
    }

    /**
     * Copy this tag, removing an HTML attribute.
     * @param name Name of HTML attribute (e.g. "HREF").  Doesn't have to be
     * converted with toHTMLAttributeName(). 
     * @return copy of this tag with named attribute removed.  The copy is 
     * a region of a fresh page containing only the tag. 
     */
    public Tag removeHTMLAttribute (String name) {
        return replaceHTMLAttribute (name, null);
    }
    
    /**
     * Copy this tag, setting an HTML attribute's value to TRUE.
     * @param name Name of HTML attribute (e.g. "HREF").  Doesn't have to be
     * converted with toHTMLAttributeName(). 
     * @return copy of this tag with named attribute set to TRUE.  The copy is 
     * a region of a fresh page containing only the tag. 
     */
    public Tag replaceHTMLAttribute (String name) {
        return replaceHTMLAttribute (name, TRUE);
    }
    
    /**
     * Copy this tag, setting an HTML attribute's value.
     * @param name Name of HTML attribute (e.g. "HREF").  Doesn't have to be
     * converted with toHTMLAttributeName(). 
     * @param value New value for the attribute
     * @return copy of this tag with named attribute set to value.  
     * The copy is 
     * a region of a fresh page containing only the tag. 
     */
    public Tag replaceHTMLAttribute (String name, String value) {
        name = toHTMLAttributeName (name);
        
        if (!startTag)
            return this; // illegal!
        
        StringBuffer newstr = new StringBuffer ();
        String[] newattrs = null;
        
        newstr.append ('<');
        newstr.append (tagName);

        boolean foundit = false;
        
        int len = htmlAttributes.length;
        for (int i=0; i < len; ++i) {
            String attrName = htmlAttributes[i];
            String attrVal;
            
            // FIX: entity-encode attrVal
            if (attrName == name) {
                newattrs = htmlAttributes;
                foundit = true;
                if (value == null)
                    continue;

                attrVal = value;
            }
            else
                attrVal = getLabel (attrName);
            
            newstr.append (' ');
            newstr.append (attrName);
            if (attrVal != TRUE) {
                newstr.append ('=');
                if (attrVal.indexOf ('"') == -1) {
                    newstr.append ('"');
                    newstr.append (attrVal);
                    newstr.append ('"');
                }
                else {
                    newstr.append ('\'');
                    newstr.append (attrVal);
                    newstr.append ('\'');
                }
            }
        }
        if (!foundit && value != null) {
            // add new attribute at end
            newstr.append (' ');
            newstr.append (name);
            if (value != name) {
                newstr.append ('=');
                if (value.indexOf ('"') == -1) {
                    newstr.append ('"');
                    newstr.append (value);
                    newstr.append ('"');
                }
                else {
                    newstr.append ('\'');
                    newstr.append (value);
                    newstr.append ('\'');
                }
            }

            // append name to list of attribute names
            newattrs = new String[len + 1];
            System.arraycopy (htmlAttributes, 0, newattrs, 0, len);
            newattrs[len] = name;
        }
        
        newstr.append ('>');

        Tag newTag = new Tag (new Page (newstr.toString()), 0, 
                              newstr.length(), tagName, startTag);
        newTag.names = names;
        newTag.htmlAttributes = newattrs;
        newTag.setLabel (name, value);
        
        return newTag;
    }

    /**
     * Commonly useful tag names.
     * Derived from <a href="http://www.sandia.gov/sci_compute/elements.html">HTML Elements</a> 
     * at Sandia National Labs. 
     */

    public static final String A = "a".intern();
    public static final String ABBREV = "abbrev".intern();
    public static final String ACRONYM = "acronym".intern();
    public static final String ADDRESS = "address".intern();    
    public static final String APPLET = "applet".intern();
    public static final String AREA = "area".intern();
    public static final String B = "b".intern();
    public static final String BASE = "base".intern();
    public static final String BASEFONT = "basefont".intern();
    public static final String BDO = "bdo".intern();
    public static final String BGSOUND = "bgsound".intern();
    public static final String BIG = "big".intern();
    public static final String BLINK = "blink".intern();
    public static final String BLOCKQUOTE = "blockquote".intern();
    public static final String BODY = "body".intern();
    public static final String BR = "br".intern();
    public static final String CAPTION = "caption".intern();
    public static final String CENTER = "center".intern();
    public static final String CITE = "cite".intern();
    public static final String CODE = "code".intern();
    public static final String COL = "col".intern();
    public static final String COLGROUP = "colgroup".intern();
    public static final String COMMENT = "!".intern();
    public static final String DD = "dd".intern();
    public static final String DEL = "del".intern();
    public static final String DFN = "dfn".intern();
    public static final String DIR = "dir".intern();
    public static final String DIV = "div".intern();
    public static final String DL = "dd".intern();    
    public static final String DT = "dt".intern();
    public static final String EM = "em".intern();
    public static final String EMBED = "embed".intern();
    public static final String FONT = "font".intern();
    public static final String FRAME = "frame".intern();
    public static final String FRAMESET = "frameset".intern();
    public static final String FORM = "form".intern();
    public static final String H1 = "h1".intern();
    public static final String H2 = "h2".intern();
    public static final String H3 = "h3".intern();
    public static final String H4 = "h4".intern();
    public static final String H5 = "h5".intern();
    public static final String H6 = "h6".intern();
    public static final String HEAD = "head".intern();
    public static final String HR = "hr".intern();
    public static final String HTML = "html".intern();
    public static final String I = "i".intern();
    public static final String IMG = "img".intern();
    public static final String INPUT = "input".intern();
    public static final String ISINDEX = "isindex".intern();
    public static final String KBD = "kbd".intern();
    public static final String LI = "li".intern();
    public static final String LINK = "link".intern();
    public static final String LISTING = "listing".intern();
    public static final String MAP = "map".intern();
    public static final String MARQUEE = "marquee".intern();
    public static final String MENU = "menu".intern();
    public static final String META = "meta".intern();
    public static final String NEXTID = "nextid".intern();
    public static final String NOBR = "nobr".intern();
    public static final String NOEMBED = "noembed".intern();
    public static final String NOFRAMES = "noframes".intern();
    public static final String OBJECT = "object".intern();
    public static final String OL = "ol".intern();
    public static final String OPTION = "option".intern();
    public static final String P = "p".intern();
    public static final String PARAM = "param".intern();
    public static final String PLAINTEXT = "plaintext".intern();
    public static final String PRE = "pre".intern();
    public static final String SAMP = "samp".intern();
    public static final String SCRIPT = "script".intern();
    public static final String SELECT = "select".intern();
    public static final String SMALL = "small".intern();
    public static final String SPACER = "spacer".intern();
    public static final String STRIKE = "strike".intern();
    public static final String STRONG = "strong".intern();
    public static final String STYLE = "style".intern();
    public static final String SUB = "sub".intern();
    public static final String SUP = "sup".intern();
    public static final String TABLE = "table".intern();
    public static final String TD = "td".intern();
    public static final String TEXTAREA = "textarea".intern();
    public static final String TH = "th".intern();
    public static final String TITLE = "title".intern();
    public static final String TR = "tr".intern();
    public static final String TT = "tt".intern();
    public static final String U = "u".intern();
    public static final String UL = "ul".intern();
    public static final String VAR = "var".intern();
    public static final String WBR = "wbr".intern();
    public static final String XMP = "xmp".intern();
    
    /**
     * Length of longest tag name.
     */
    public static int MAX_LENGTH = 10;      // longest tag name is BLOCKQUOTE
}
