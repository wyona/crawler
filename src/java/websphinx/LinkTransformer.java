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

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Hashtable;

/**
 * Transformer that remaps URLs in links.
 * <P>
 * The default LinkTransformer simply converts all links
 * to absolute URLs.  Other common effects are easy to
 * achieve:
 * <UL>
 * <LI>To make all links relative to a base URL, use 
 * setBase() to set a base URL.
 * <LI>To replace certain URLs with different ones, 
 * use map() to set up the mappings.
 * </UL>
 * The default LinkTransformer strips out &lt;BASE&gt;
 * elements.  Instead, it can output a &lt;BASE&gt;
 * element with a user-specified URL.  Use setBase() to set
 * the URL and setEmitBaseElement() to indicate that it
 * should be emitted.
 */
public class LinkTransformer extends HTMLTransformer {
    protected Hashtable map;
    protected URL base = null;
    boolean emitBaseElement = false;
    
    boolean needToEmitBase = false;

    /**
     * Make a LinkTransformer writing to a file.
     * @param filename Filename to write to
     */
    public LinkTransformer (String filename) throws IOException {
        super (filename);
    }

    /**
     * Make a LinkTransformer that writes pages to a
     * file.
     * @param filename Name of file to receive HTML output
     * @param seekable True if file should be opened for random access
     */
    public LinkTransformer (String filename, boolean seekable) throws IOException {
        super (filename, seekable);
    }
    
    /**
     * Make a LinkTransformer writing to a stream.
     * @param out stream to write to
     */
    public LinkTransformer (OutputStream out) {
        super (out);
    }

    /**
     * Make a LinkTransformer writing to another HTMLTransformer
     * @param next next transformer in filter chain
     */
    public LinkTransformer (HTMLTransformer next) {
        super (next);
    }

    /**
     * Get the base URL used by the LinkTransformer.
     * A transformed link's URL is written out relative
     * to this URL.  For instance, if the base URL is
     * http://www.yahoo.com/Entertainment/, then a link 
     * URL http://www.yahoo.com/News/Current/
     * would be written out as ../News/Current/.
     * @return base URL, or null if no base URL is set.  Default is null.
     */
    public URL getBase () {
        return base;
    }

    /**
     * Set the base URL used by the LinkTransformer.
     * A transformed link's URL is written out relative
     * to this URL.  For instance, if the base URL is
     * http://www.yahoo.com/Entertainment/, then a link 
     * URL http://www.yahoo.com/News/Current/
     * would be written out as ../News/Current/.
     * @param base base URL, or null if no base URL should be used.
     */
    public synchronized void setBase (URL base) {
        this.base = base;
    }

    /**
     * Test whether the LinkTransformer should emit a 
     * &lt;BASE&gt; element pointing to the base URL.
     * @return true if a &lt;BASE&gt; element should be
     * emitted with each page.
     */
    public boolean getEmitBaseElement () {
        return emitBaseElement;
    }

    /**
     * Set whether the LinkTransformer should emit a 
     * &lt;BASE&gt; element pointing to the base URL.
     * @param emitBase true if a &lt;BASE&gt; element should be
     * emitted with each page.
     */
    public synchronized void setEmitBaseElement (boolean emitBase) {
        emitBaseElement = emitBase;
    }

    /**
     * Look up the href for a URL, taking any mapping
     * into account.
     * @param base base URL (or null if an absolute URL is desired)
     * @param url URL of interest
     * @return relative href for url from base
     */
    public String lookup (URL base, URL url) {
        if (map != null) {
            Object obj = map.get (url);
            if (obj instanceof URL)
                return base != null
                    ? Link.relativeTo (base, (URL)obj)
                    : obj.toString ();
            else if (obj instanceof String)
                return base != null
                    ? Link.relativeTo (base, (String)obj)
                    : obj.toString ();
        }

        return base != null
            ? Link.relativeTo (base, url)
            : url.toString ();
    }

    /**
     * Map a URL to an href.  For example, Concatenator
     * uses this call to map page URLs to their corresponding
     * anchors in the concatenation.
     * @param url URL of interest
     * @param href href which should be returned by lookup (null, url)
     */
    public synchronized void map (URL url, String href) {
        if (map == null)
            map = new Hashtable ();
        map.put (url, href);
    }

    /**
     * Map a URL to a new URL.  For example, Mirror
     * uses this call to map remote URLs to their corresponding
     * local URLs.
     * @param url URL of interest
     * @param newURL URL which should be returned by lookup (null, url)
     */
    public synchronized void map (URL url, URL newURL) {
        if (map == null)
            map = new Hashtable ();
        map.put (url, newURL);
    }

    /**
     * Test whether a URL is mapped.
     * @param url URL of interest
     * @return true if map () was called to remap url
     */
    public boolean isMapped (URL url) {
        return map != null && map.containsKey (url);
    }

    /**
     * Write a page through the transformer.  If 
     * getEmitBaseElement() is true and getBase() is
     * non-null, then the transformer
     * outputs a &lt;BASE&gt; element either inside the
     * page's &lt;HEAD&gt; element (if present) or before
     * the first tag that belongs in &lt;BODY&gt;.
     * @param page Page to write
     */
    public synchronized void writePage (Page page) throws IOException {
        needToEmitBase = emitBaseElement && base != null;
        super.writePage (page);
        needToEmitBase = false;
    }    

    /**
     * Handle an element written through the transformer.
     * Remaps attributes that contain URLs.
     * @param elem Element to transform
     */
    protected void handleElement (Element elem) throws IOException {
        Tag tag = elem.getStartTag ();
        String tagName = elem.getTagName ();

        if (needToEmitBase && tag.isBodyTag ()) {
            emit ("<BASE HREF=\"" + base.toString () + "\">");
            needToEmitBase = false;
        }

        if (elem instanceof Link)
            handleLink ((Link)elem);
        else if (tagName == Tag.BASE)
            handleBase (elem);
        else if (needToEmitBase && tagName == Tag.HEAD) {
            // put BASE at the end of HEAD, if we don't find it earlier
            emit (elem.getStartTag ());
            transformContents (elem);
            if (needToEmitBase) {
                emit ("<BASE HREF=\"" + base.toString () + "\">");
                needToEmitBase = false;
            }
            if (elem.getEndTag () != null)
                emit (elem.getEndTag ());
        }
        else
            super.handleElement (elem);
    }
    
    /**
     * Handle a Link's transformation.
     * Default implementation replaces the link's URL
     * with lookup(URL).
     * @param link Link to transform
     */
    protected void handleLink (Link link) throws IOException {
        emit ( link.replaceHref (lookup (base, link.getURL())) );
        transformContents (link);
        if (link.getEndTag () != null)
           emit (link.getEndTag ());
    }

    /**
     * Handle the BASE element.
     * Default implementation removes if if EmitBaseElement
     * is false, or changes its URL to Base if EmitBaseElement
     * is true.
     * @param elem BASE element to transform
     */
    protected void handleBase (Element elem) throws IOException {
        Tag tag = elem.getStartTag ();
        if (needToEmitBase) {
            emit (tag.replaceHTMLAttribute ("href", base.toString()));
            needToEmitBase = false;
        }
        else if (tag.hasHTMLAttribute ("href")
                && tag.countHTMLAttributes () > 1)
            // tag has other attributes that we want to preserve
            emit (tag.removeHTMLAttribute ("href"));
        // otherwise skip the BASE element
    }
    
    /*
     * Testing
     *
    public static void main (String[] args) throws Exception {
        OutputStream out = (args.length >= 2)
            ? (OutputStream)new java.io.FileOutputStream (args[1])
            : (OutputStream)System.out;
        HTMLTransformer unparser = new LinkTransformer (out);

        Link link = new Link (args[0]);
        Page page = new Page (link);

        unparser.write (page);
        unparser.close ();
    }
     */

}
