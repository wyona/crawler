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
import java.net.URLConnection;
//#ifdef JDK1.1 
import java.net.HttpURLConnection;
//#endif JDK1.1
import java.io.IOException;
import java.io.InputStream;
import rcm.util.Str;

/**
 * A Web page.  Although a Page can represent any MIME type, it mainly
 * supports HTML pages, which are automatically parsed.  The parsing produces
 * a list of tags, a list of words, an HTML parse tree, and a list of links.
 */
public class Page extends Region {

    // typical page length, to optimize downloads
    static final int TYPICAL_LENGTH = 20240;

    // Permanent content
    Link origin;
    long lastModified = 0;
    long expiration = 0;
    String contentType;
    String contentEncoding;
    int responseCode = -1;
    String responseMessage = null;
    URL base;
    String title;
    Link[] links;

    int contentLock; 
        // If page was downloaded from Net, represents number of 
        //    callers who want to keep the content.
        // If page was created from a string, set to -1. 

    // Discardable content (thrown away when contentLock falls to 0)
    byte[] contentBytes;
    String content;
    Region[] tokens;
    Text[] words;
    Tag[] tags;
    Element[] elements;
    Element root;
    String canonicalTags;

    /**
     * Make a Page by downloading and parsing a Link.
     * @param link Link to download
     */
    public Page (Link link) throws IOException {
        this (link, DownloadParameters.NO_LIMITS, new HTMLParser ());
    }

    /**
     * Make a Page by downloading a Link.
     * @param link Link to download
     * @param dp Download parameters to use
     */
    public Page (Link link, DownloadParameters dp) throws IOException {
        this (link, dp, new HTMLParser ());
    }

    /**
     * Make a Page by downloading a Link.
     * @param link Link to download
     * @param parser HTML parser to use
     */
    public Page (Link link, DownloadParameters dp, HTMLParser parser) throws IOException {
        super (null, 0, 0);
        source = this;
        origin = link;
        base = getURL ();
        download (dp, parser);
        link.setPage (this);
    }

    /**
     * Make a Page from a URL and a string of HTML.
     * The created page has no originating link, so calls to getURL(), getProtocol(), etc. will fail.
     * @param url URL to use as a base for relative links on the page
     * @param html the HTML content of the page
     */
    public Page (URL url, String html) {
        this (url, html, new HTMLParser ());
    }

    /**
     * Make a Page from a URL and a string of HTML.
     * The created page has no originating link, so calls to getURL(), getProtocol(), etc. will fail.
     * @param url URL to use as a base for relative links on the page
     * @param html the HTML content of the page
     * @param parser HTML parser to use
     */
    public Page (URL url, String html, HTMLParser parser) {
        super (null, 0, html.length());
        source = this;
        base = url;
        this.content = html;
        this.contentBytes = html.getBytes ();
        contentLock = -1;
        parse (parser);
    }

    /**
     * Make a Page from a string of content.  The content is not parsed. 
     * The created page has no originating link, so calls to getURL(), getProtocol(), etc. will fail.
     * @param content HTML content of the page */
    public Page (String content) {
        super (null, 0, content.length());
        // FIX: don't think base==null will work
        source = this;
        this.content = content;
        this.contentBytes = content.getBytes ();
        contentLock = -1;
    }

    /**
     * Make a Page from a byte array of content.  The content is not parsed. 
     * The created page has no originating link, so calls to getURL(), getProtocol(), etc. will fail.
     * @param content byte content of the page */
    public Page (byte[] content) {
        super (null, 0, content.length);
        // FIX: don't think base==null will work
        source = this;
        this.contentBytes = new byte[content.length];
        System.arraycopy (content, 0, this.contentBytes, 0, content.length);
        this.content = new String (content);
        contentLock = -1;
    }

    //
    // Downloading
    //

    // This code generates SecurityExceptions in Netscape 4.0,
    // and it doesn't seem to be necessary anyway: redirects are followed
    // by Netscape and JDK by default, despite the fact that the JDK
    // docs claim that setFollowRedirects() defaults to false
    
    //static {
      //try {
      //  HttpURLConnection.setFollowRedirects (true);
      //} catch (Throwable t) { }
    //}

    /*
     * Download the page.  The downloaded page is parsed 
     * if its MIME type is HTML or unspecified.
     * @param parser HTML parser to use
     * @exception IOException if an error occurs in downloading the page
     */
    public void download (DownloadParameters dp, HTMLParser parser) throws IOException {
        URLConnection conn = 
            Access.getAccess ().openConnection (origin);
        
        // fetch and store final redirected URL and response headers
        InputStream in = conn.getInputStream ();
        base = conn.getURL ();
        lastModified = conn.getLastModified ();
        expiration = conn.getExpiration ();
        contentType = conn.getContentType ();
        contentEncoding = conn.getContentEncoding ();

//#ifdef JDK1.1 
        // get HTTP response codes
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection httpconn = (HttpURLConnection)conn;

            responseCode = httpconn.getResponseCode ();
            responseMessage = httpconn.getResponseMessage ();
            if (responseMessage == null)
                responseMessage = "unknown error";
            
            if (responseCode >= 300)
                // HTTP failure
                throw new IOException (responseCode + " " + responseMessage); 
        }
//#endif JDK1.1

//     System.err.println ("Original URL: " + origin.getURL());
//     System.err.println ("Final URL: " + conn.getURL());

        // download content
        int maxKB = dp.getMaxPageSize ();
        int maxBytes = (maxKB > 0) ? maxKB * 1024 : Integer.MAX_VALUE;
        int expectedLength = conn.getContentLength ();
        if (expectedLength > maxBytes)
            throw new IOException ("Page greater than " 
                                   + maxBytes + " bytes");
        if (expectedLength == -1)
            expectedLength = TYPICAL_LENGTH;
        byte[] buf = new byte[expectedLength];
        int n;
        int total = 0;

        while ((n = in.read (buf, total, buf.length - total)) != -1) {
            total += n;
            if (total > maxBytes)
                throw new IOException ("Page greater than " 
                                       + maxBytes + " bytes");
            if (total == buf.length) {
                // try to read one more character
                int c = in.read ();
                if (c == -1)
                    break; // EOF, we're done
                else {
                    // need more space in array.  Double the array, but don't make
                    // it bigger than maxBytes.
                    byte[] newbuf = new byte[Math.min (buf.length * 2, maxBytes)];
                    System.arraycopy (buf, 0, newbuf, 0, buf.length);
                    buf = newbuf;
                    buf[total++] = (byte) c;
                }
            }                    
        }
        in.close ();
        
        if (total != buf.length) {
            // resize the array to be precisely total bytes long
            byte[] newbuf = new byte[total];
            System.arraycopy (buf, 0, newbuf, 0, total);
            buf = newbuf;
        }
 
        contentBytes = buf;
        content = new String (buf);
        start = 0;
        end = total;
        contentLock = 1;

        //  parse the response
        if (contentType == null
            || contentType.startsWith ("text/html") 
            || contentType.startsWith ("content/unknown"))
            parse (parser);
    }

    void downloadSafely () {
      try {
          download (new DownloadParameters (), new HTMLParser ());
      } catch (Throwable e) {
      }
    }

    //
    // Parsing
    //

    /**
     * Parse the page.  Assumes the page has already been downloaded.
     * @param parser HTML parser to use
     * @exception RuntimeException if an error occurs in downloading the page
     */
    public void parse (HTMLParser parser) {
        if (!hasContent())
            downloadSafely ();
        try {
            parser.parse (this);
        } catch (IOException e) {
            throw new RuntimeException (e.toString());
        }
    }
    
    /**
     * Test whether page has been parsed.  Pages are parsed during 
     * download only if its MIME type is HTML or unspecified.
     * @return true if page was parsed, false if not
     */
    public boolean isParsed () {
        return tokens != null;
    }

    /**
     * Test whether page is HTML.
     * @return true if page is HTML.
     */
    public boolean isHTML () {
        return root != null;
    }

    /**
     * Test whether page is a GIF or JPEG image.
     * @return true if page is a GIF or JPEG image, false if not
     */
    public boolean isImage () {
        byte[] bytes = getContentBytes ();
        return startsWith (bytes, GIF_MAGIC) || startsWith (bytes, JPG_MAGIC);
    }

    private static final byte[] GIF_MAGIC = { 
        (byte) 'G', (byte)'I', (byte)'F', (byte)'8' 
    };
    private static final byte[] JPG_MAGIC = {
        (byte) 0377, (byte) 0330, (byte) 0377,
        (byte) 0340, (byte) 0, (byte) 020,
        (byte) 'J', (byte) 'F', (byte) 'I', (byte) 'F'
    };

    private boolean startsWith (byte[] bytes, byte[] prefix) {
        if (prefix.length > bytes.length)
            return false;
        for (int i = 0, n = prefix.length; i < n; ++i)
            if (bytes[i] != prefix[i])
                return false;
        return true;
    }

    //
    // Content management
    //

    /**
     * Lock the page's content (to prevent it from being discarded).
     * This method increments a lock counter, representing all the 
     * callers interested in preserving the content.  The lock
     * counter is set to 1 when the page is initially downloaded.
     */
    public void keepContent () {
        if (contentLock > 0)
            ++contentLock;
    }

    /**
     * Unlock the page's content (allowing it to be garbage-collected, to
     * save space during a Web crawl).  This method decrements a lock counter.
     * If the counter falls to
     * 0 (meaning no callers are interested in the content), 
     * the content is released.  At least the following
     * fields are discarded: content, tokens, tags, words, elements, and
     * root.  After the content has been discarded, calling getContent()
     * (or getTokens(), getTags(), etc.) will force the page to be downloaded
     * again.  Hopefully the download will come from the cache, however.
     * <P> Links are not considered part of the content, and are not subject to
     * discarding by this method.  Also, if the page was created from a string
     * (rather than by downloading), its content is not subject to discarding 
     * (since there would be no way to recover it). 
     */
    public void discardContent () {
        if (contentLock == 0)    // already discarded
            return;
            
        if (--contentLock > 0)   // somebody else still has a lock on the content
            return;
            
        if (origin == null)
            return;     // without an origin, we'd have no way to recover this page
            
        //System.err.println ("discarding content of " + toDescription());
        contentBytes = null;
        content = null;
        tokens = null;
        tags = null;
        words = null;
        elements = null;
        root = null;
        canonicalTags = null;

        // keep links, but isolate them from the element tree
        if (links != null) {
            for (int i=0; i<links.length; ++i) 
                if (links[i] instanceof Link)
                    ((Link)links[i]).discardContent ();
        }
        
        // FIX: debugging only: disconnect this page from its parent
        //origin.page = null;
        //origin = null;

        contentLock = 0;
    }

    /**
     * Test if page content is available.
     * @return true if content is downloaded and available, false if content has not been downloaded 
     * or has been discarded.
     */
    public final boolean hasContent () {
        return contentLock != 0;
    }

    //
    // Page accessors
    //

    /**
     * Get depth of page in crawl.
     * @return depth of page from root (depth of page is same as depth of its originating link)
     */
    public int getDepth () {
        return origin != null ? origin.getDepth () : 0;
    }
    
    /**
     * Get the Link that points to this page.
     * @return the Link object that was used to download this page.
     */ 
    public Link getOrigin () {
        return origin;
    }

    /**
     * Get the base URL, relative to which the page's links were interpreted.
     * The base URL defaults to the URL of the 
     * Link that was used to download the page.  If any redirects occur
     * while downloading the page, the final location becomes the new base
     * URL.  Lastly, if a <BASE> element is found in the page, that
     * becomes the new base URL.
     * @return the page's base URL.
     */ 
    public URL getBase () {
        return base;
    }

    /**
     * Get the URL.
     * @return the URL of the link that was used to download this page
     */ 
    public URL getURL () {
        return origin != null ? origin.getURL() : null;
    }

    /**
     * Get the title of the page.
     * @return the page's title, or null if the page hasn't been parsed.
     */
    public String getTitle () {
        return title;
    }

    /**
     * Get the content of the page as a String.  May not work properly for
     * binary data like images; use getContentBytes instead.
     * @return the String content of the page.
     */
    public String getContent () {
        if (!hasContent())
            downloadSafely ();
        return content;
    }

    /**
     * Get the content of the page as an array of bytes.
     * @return the content of the page in binary form.
     */
    public byte[] getContentBytes () {
        if (!hasContent())
            downloadSafely ();
        return contentBytes;
    }

    /**
     * Get the token sequence of the page.  Tokens are tags and whitespace-delimited text.
     * @return token regions in the page, or null if the page hasn't been downloaded or parsed.
     */
    public Region[] getTokens() {
        if (!hasContent ())
            downloadSafely ();
        return tokens;
    }

    /**
     * Get the tag sequence of the page.
     * @return tags in the page, or null if the page hasn't been downloaded or parsed.
     */
    public Tag[] getTags () {
        if (!hasContent ())
            downloadSafely ();
        return tags;
    }

    /**
     * Get the words in the page.  Words are whitespace- and tag-delimited text.
     * @return words in the page, or null if the page hasn't been downloaded or parsed.
     */
    public Text[] getWords () {
        if (!hasContent ())
            downloadSafely ();
        return words;
    }

    /**
     * Get the HTML elements in the page.  All elements in the page
     * are included in the list, in the order they would appear in
     * an inorder traversal of the HTML parse tree.
     * @return HTML elements in the page ordered by inorder, or null if the page
     * hasn't been downloaded or parsed.
     */
    public Element[] getElements () {
        if (!hasContent ())
            downloadSafely ();
        return elements;
    }
    
    /**
     * Get the root HTML element of the page.
     * @return first top-level HTML element in the page, or null 
     * if the page hasn't been downloaded or parsed.
     */
    public Element getRootElement () {
        if (!hasContent ())
            downloadSafely ();
        return root;
    }

    /**
     * Get the links found in the page.
     * @return links in the page, or null 
     * if the page hasn't been downloaded or parsed.
     */
    public Link[] getLinks() {
        return links;
    }

    /**
     * Convert the link's URL to a String
     * @return the URL represented as a string
     */
    public String toURL () {
        return origin != null ? origin.toURL () : null;
    }

    /**
     * Generate a human-readable description of the page.
     * @return a description of the link, in the form "title [url]".
     */
    public String toDescription () {
        return (title != null && title.length() > 0 ? title + " " : "") + "[" + getURL() + "]";
    }

    /**
     * Get page containing the region.
     * @return page containing the region
     */
    public String toString () {
        return getContent ();
    }

    /**
     * Get last-modified date of page.
     * @return the date when the page was last modified, or 0 if not known. 
     * The value is number of seconds since January 1, 1970 GMT
     */
    public long getLastModified () {
        return lastModified;
    }
    /**
     * Set last-modified date of page.
     * @param last the date when the page was last modified, or 0 if not known. 
     * The value is number of seconds since January 1, 1970 GMT
     */
    public void setLastModified (long last) {
        lastModified = last;
    }

    /**
     * Get expiration date of page.
     * @return the expiration date of the page, or 0 if not known. 
     * The value is number of seconds since January 1, 1970 GMT.
     */
    public long getExpiration () {
        return expiration;
    }
    /**
     * Set expiration date of page.
     * @param expire the expiration date of the page, or 0 if not known. 
     * The value is number of seconds since January 1, 1970 GMT.
     */
    public void setExpiration (long expire) {
        expiration = expire;
    }

    /**
     * Get MIME type of page.
     * @return the MIME type of page, such as "text/html", or null if not known. 
     */
    public String getContentType () {
        return contentType;
    }
    /**
     * Set MIME type of page.
     * @param type the MIME type of page, such as "text/html", or null if not known. 
     */
    public void setContentType (String type) {
        contentType = type;
    }

    /**
     * Get content encoding of page.
     * @return the encoding type of page, such as "base-64", or null if not known. 
     */
    public String getContentEncoding () {
        return contentEncoding;
    }
    /**
     * Set content encoding of page.
     * @param encoding the encoding type of page, such as "base-64", or null if not known. 
     */
    public void setContentEncoding (String encoding) {
        contentEncoding = encoding;
    }

    /**
     * Get response code returned by the Web server.  For list of
     * possible values, see java.net.HttpURLConnection.
     * @return response code, such as 200 (for OK) or 404 (not found).
     * Code is -1 if unknown.
     * @see java.net.HttpURLConnection
     */
    public int getResponseCode () {
        return responseCode;
    }

    /**
     * Get response message returned by the Web server.
     * @return response message, such as "OK" or "Not Found".  The response message is null if the page failed to be fetched or not known. 
     */
    public String getResponseMessage () {
        return responseMessage;
    }

    /**
     * Get raw content found in a region.
     * @param start starting offset of region
     * @param end ending offset of region
     * @return raw HTML contained in the region
     */
    public String substringContent (int start, int end) {
        return getContent ().substring (start, end);
    }

    /**
     * Get HTML found in a region.
     * @param start starting offset of region
     * @param end ending offset of region
     * @return representation of region as HTML
     */
    public String substringHTML (int start, int end) {
        String s = getContent ().substring (start, end);
        if (!isHTML ()) {
            s = Str.replace (s, "&", "&amp;");
            s = Str.replace (s, "<", "&lt;");
            s = Str.replace (s, ">", "&gt;");
            s = "<PRE>" + s + "</PRE>";
        }
        return s;
    }

    /**
     * Get tagless text found in a region.
     * Runs of whitespace and tags are reduced to a single space character.
     * @param start starting offset of region
     * @param end ending offset of region
     * @return tagless text contained in the region
     */
    public String substringText (int start, int end) {
        if (words == null)
            return ""; // page is not parsed

        // FIX: find some other mapping
        StringBuffer buf = new StringBuffer();
        for (int j = findStart (words, start); j<words.length; ++j) {
            if (words[j].end > end)
                break;
            else {
                if (buf.length() > 0)
                    buf.append (' ');
                buf.append (words[j].text);
            }
        }
        return buf.toString();             
    }

    /**
     * Get HTML tags found in a region.  Whitespace and text among the
     * tags are deleted.
     * @param start starting offset of region
     * @param end ending offset of region
     * @return tags contained in the region
     */
    public String substringTags (int start, int end) {
        if (tags == null)
            return ""; // page is not parsed

        // FIX: find some other mapping
        StringBuffer buf = new StringBuffer();
        for (int j = findStart (tags, start); j<tags.length; ++j) {
            if (tags[j].end > end)
                break;
            else {
                if (buf.length() > 0)
                    buf.append (' ');
                buf.append (getContent ().substring (tags[j].start, tags[j].end));
            }
        }
        return buf.toString();             
    }

    /**
     * Get canonicalized HTML tags found in a region.
     * A canonicalized tag looks like the following:
     * <PRE>
     * &lt;tagname#index attr=value attr=value attr=value ...&gt
     * <PRE>
     * where tagname and attr are all lowercase, index is the tag's
     * index in the page's tokens array.  Attributes are sorted in
     * increasing order by attribute name. Attributes without values
     * omit the entire "=value" portion.  Values are delimited by a 
     * space.  All occurences of &lt, &gt, space, and % characters 
     * in a value are URL-encoded (e.g., space is converted to %20).  
     * Thus the only occurences of these characters in the canonical 
     * tag are the tag delimiters.
     *
     * <P>For example, raw HTML that looks like:
     * <PRE>
     * &lt;IMG SRC="http://foo.com/map&lt;&gt;.gif" ISMAP&gt;Image&lt;/IMG&gt;
     * </PRE>
     * would be canonicalized to:
     * <PRE>
     * &lt;img ismap src=http://foo.com/map%3C%3E.gif&gt;&lt;/img&gt;
     * </PRE>
     * <P>
     * Comment and declaration tags (whose tag name is !) are omitted
     * from the canonicalization.
     *
     * @param start starting offset of region
     * @param end ending offset of region
     * @return canonicalized tags contained in the region
     */
    public String substringCanonicalTags (int start, int end) {
        if (tokens == null)
            return ""; // page is not parsed

        boolean all = (start == this.start && end == this.end);

        if (all && canonicalTags != null)
            return canonicalTags;

        // FIX: find some other mapping
        StringBuffer buf = new StringBuffer();
        for (int j = findStart (tokens, start); j<tokens.length; ++j) {
            if (tokens[j].end > end)
                break;
            else if (tokens[j] instanceof Tag)
                Tagexp.canonicalizeTag (buf, (Tag)tokens[j], j);
        }

        String result = buf.toString ();
        if (all)
            canonicalTags = result;
        return result;
    }

    public static void main (String[] args) throws Exception {
        int method = Link.GET;

        for (int i=0; i<args.length; ++i) {
            if (args[i].equals ("-post"))
                method = Link.POST;
            else if (args[i].equals ("-get"))
                method = Link.GET;
            else {
                Link link = method == Link.GET 
                             ? new Link (args[i]) 
                             : new Link (args[i]); // FIX: POST?
                try {
                    Page p = new Page (link);
                    System.out.write (p.getContentBytes ());
                } catch (IOException e) {
                    System.out.println (e);
                }
            }
        }
    }

}
