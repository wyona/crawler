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

/**
 * Button element in an HTML form -- for example, &lt;INPUT TYPE=submit&gt; or &lt;INPUT TYPE=image&gt;.
 * 
 * @author Rob Miller
 * @see Page
 * @see Link
 */
public class FormButton extends Link {

    Form form;

    /**
     * Make a LinkElement from a start tag and end tag and its containing form.
     * The tags and form must be on the same page.
     * @param startTag Start tag of button
     * @param endTag End tag of button (or null if none)
     * @param form Form containing this button
     */
    public FormButton (Tag startTag, Tag endTag, Form form) throws MalformedURLException {
        super (startTag, endTag, null);
        this.form = form;
        if (form == null)
            throw new MalformedURLException ();
    }

    /**
     * Get the URL.
     * @return the URL of the link
     */ 
    public URL getURL () {
        if (url == null)
            try {
                url = urlFromHref (getStartTag (), null);
            } catch (MalformedURLException e) {
                url = null;
            }

        return url;
    }

    /**
     * Get the form.
     * @return the form containing this button
     */ 
    public Form getForm () {
        return form;
    }

    /**
     * Get the method used to access this link.
     * @return GET or POST.
     */ 
    public int getMethod () {
        return form.getMethod ();
    }

    /**
     * Construct the URL for this button, from its start tag and a base URL (for relative references).
     * @param tag Start tag of button, such as &lt;INPUT TYPE=submit&gt;.
     * @param base Base URL used for relative references
     * @return URL to which the button points
     */
    protected URL urlFromHref (Tag tag, URL base) throws MalformedURLException {
        if (parent == null || form == null)
            // can't figure out URL until we're linked into an HTML element tree
            // containing our complete form
            return null;
        return form.makeQuery (this);
    }

}
