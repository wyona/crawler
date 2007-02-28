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

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLEncoder;

/**
 * &lt;FORM&gt; element in an HTML page.
 */
public class Form extends Link {

    /**
     * Make a LinkElement from a start tag and end tag and a base URL (for relative references).  
     * The tags must be on the same page.
     * @param startTag Start tag of element
     * @param endTag End tag of element
     * @param base Base URL used for relative references
     */
    public Form (Tag startTag, Tag endTag, URL base) throws MalformedURLException {
        super (startTag, endTag, base);
    }

    /**
     * Construct the URL for this form, from its start tag and a base URL (for relative references).
     * @param tag Start tag of form.
     * @param base Base URL used for relative references
     * @return URL to which the button points
     */
     protected URL urlFromHref (Tag tag, URL base) throws MalformedURLException {
        String href = tag.getHTMLAttribute ("action");
        if (href == null)
            // without an action attribute, the URL defaults to the base
            return base;
        return new URL (base, href);
    }

    /**
     * Get the method used to access this link.
     * @return GET or POST.
     */ 
    public int getMethod () {
        return getHTMLAttribute ("method", "GET").equalsIgnoreCase ("post")
           ? POST : GET;
    }

    /**
     * Construct the query that would be submitted if the form's SUBMIT button were pressed.
     * @return a URL representing the submitted form, or null if the form cannot be represented as a URL.
     */ 
    public URL makeQuery () {
        return makeQuery (null);
    }

    /**
     * Construct the query that would be submitted if the specified button were pressed.
     * @param button form button that triggers the submission.
     * @return a URL representing the submitted form, or null if the form cannot be represented as a URL.
     */ 
    public URL makeQuery (FormButton button) {
        StringBuffer querybuf = new StringBuffer ();
        makeQuery (getChild (), querybuf);
        
        if (button != null) {
            String type = button.getHTMLAttribute ("type", "");
            String name = button.getHTMLAttribute ("name", "");
            String value = button.getHTMLAttribute ("value", "");

            if (type.equalsIgnoreCase ("submit")) {
                passArgument (querybuf, name, value);
            }
            else if (type.equalsIgnoreCase ("image")) {
                // simulate an imagemap click
                passArgument (querybuf, name+".x", "0");
                passArgument (querybuf, name+".y", "0");
            }
        }
        
        String href = getURL().toExternalForm () + "?";
        if (querybuf.length() > 0)
            href += querybuf.toString().substring (1); // deletes '&' from front of querybuf
        
        try {
            return new URL (href);
        } catch (MalformedURLException e) {
            throw new RuntimeException ("internal error: " + e);
        }
    }

    // appends "&name=val&name=val..." to query
    // for all form fields found among elements and their children
    private void makeQuery (Element elem, StringBuffer query) {
      for (Element e = elem; e != null; e = e.getSibling ()) {
            String tagName = e.getTagName ();
            if (tagName == Tag.INPUT) {
                String type = e.getHTMLAttribute ("type", "text").toLowerCase ();

                if ( // always pass these fields
                    type.equals ("text")
                    || type.equals ("password") 
                    || type.equals ("hidden")

                    // pass these fields if checked
                    || ((type.equals ("checkbox") || type.equals ("radio"))
                        && e.hasHTMLAttribute ("checked"))) {
                    passArgument (query, 
                                  e.getHTMLAttribute ("name", ""), 
                                  e.getHTMLAttribute ("value", ""));
                }
            }
            else if (tagName == Tag.SELECT) {
                String name = e.getHTMLAttribute ("name", "");
                for (Element opt = e.getChild(); opt != null; opt = opt.getSibling()) {
                    if (opt.getTagName() == Tag.OPTION 
                        && opt.hasHTMLAttribute ("selected")) {
                        passArgument (query, name, opt.getHTMLAttribute ("value", ""));
                    }
                }
            }
            else if (tagName == Tag.TEXTAREA) {
                passArgument (query, e.getHTMLAttribute ("name", ""), e.toText ());                              
            }
            else { 
                makeQuery (e.getChild (), query);
            }
        }
    }
    
    private void passArgument (StringBuffer query, String name, String value) {
        query.append ('&');
        query.append (URLEncoder.encode(name)); // FIX: should name be encoded?
        query.append ('=');
        query.append (URLEncoder.encode(value));
    }
}

