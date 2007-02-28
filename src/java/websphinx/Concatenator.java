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
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Hashtable;

/**
 * Transformer that concatenates multiple pages
 * into a single HTML page.
 * <P>
 * The entire set of pages is preceded by a "prolog"
 * and followed by an "epilog", which are constant
 * strings of HTML.  Each page is preceded
 * by a "header" and followed by a "footer".  Adjacent pages 
 * are separated by a "divider".
 * <P>
 * Concatenator performs the following
 * transformations on pages before appending them together:
 * <UL>
 * <LI> deletes elements that would conflict, including
 * &lt;HEADf&gt;, &lt;TITLEf&gt;, &lt;BODYf&gt;, &lt;HTMLf&gt,
 * &lt;STYLE&gt;, and &lt;FRAMES&gt;.
 * <LI> deletes &lt;BASEf&gt; or replaces it with a user-specified
 *      &lt;BASEf&gt;
 * <LI> changes links among the written pages into
 *      in-page references, of the form "#concatenator_N"
 * <LI> changes links to other pages into absolute references
 * </UL>
 *
 */
 
 // FIX: transform anchors
public class Concatenator extends RewritableLinkTransformer {
    
    boolean needRewrite = false;

    public static String defaultProlog = "<HTML><HEAD><TITLE>Concatenation</TITLE></HEAD><BODY>\n";
    public static String defaultHeader = "<TABLE WIDTH=\"100%\"><TR>\n"
                    +"<TD ALIGN=left><A NAME=\"%a\">%t [%u]</A>\n"
                    +"<TD ALIGN=right>Page %p</TABLE>\n";
    public static String defaultFooter = "";
    public static String defaultDivider = "\n<DIV STYLE=\"page-break-after: always;\"><HR></DIV>\n";
    public static String defaultEpilog = "\n</BODY></HTML>\n";

    String prolog = defaultProlog;
    String header = defaultHeader;
    String footer = defaultFooter;
    String divider = defaultDivider;
    String epilog = defaultEpilog;

    int nPages = 0;

    /**
     * Make a new Concatenator that writes to a file.
     * @param filename Filename to write concatenated pages to
     * @exception IOException if file cannot be opened
     */
    public Concatenator (String filename) throws IOException {
        super (makeDirs(filename));
    }
    
    private static String makeDirs (String filename) throws IOException {
        File file = new File (filename);
        File parent = new File (file.getParent ());
        if (parent != null)
            Access.getAccess ().makeDir (parent);
        return filename;
    }


    /**
     * Set the prolog.
     * @param prolog string of HTML that is emitted at the beginning
     * of the concatenation. Default value is: <BR>
     * <CODE>&lt;HTML&gt;&lt;HEAD&gt;&lt;TITLE&gt;Concatenation&lt;/TITLE&gt;&lt;/HEAD&gt;&lt;BODY&gt;\n</CODE>
     */
    public synchronized void setProlog (String prolog) {
        this.prolog = prolog;
    }
    /**
     * Get the prolog.
     * @return string of HTML that is emitted at the beginning
     * of the concatenation.
     */
    public String getProlog () {
        return prolog;
    }

     /**
     * Set the header.  The header can contain macro codes which
     * are replaced with attributes of the page about to be written:
     * <DL>
     * <DT>%t
     * <DD>title of the page
     * <DT>%u
     * <DD>URL of page
     * <DT>%a
     * <DD>anchor name of the page ("pageN", where N is the page number)
     * <DT>%p
     * <DD>page number (starting from 1)
     * </DL>
     * @param header string of HTML that is emitted before
     * each page. The default value is:<BR>
     * <CODE> &lt;TABLE WIDTH="100%"&gt;&lt;TR&gt;\n <BR>
     * &lt;TD ALIGN=left&gt;&lt;A NAME="%a"&gt;%t [%u]&lt;/A&gt;\n <BR>
     * &lt;TD ALIGN=right&gt;Page %p&lt;/TABLE&gt;\n</CODE>
     */
    public synchronized void setPageHeader (String header) {
        this.header = header;
    }
    /**
     * Get the header.
     * @return string of HTML that is emitted before
     * each page.
     */
    public String getPageHeader () {
        return header;
    }

    /**
     * Set the footer.  The footer can contain the same
     * macros as the header (%t, %u, %a, %p); see setPageHeader
     * for more details.
     * @param footer string of HTML that is emitted after
     * each page.
     */
    public synchronized void setPageFooter (String footer) {
        this.footer = footer;
    }
    /**
     * Get the footer.
     * @return string of HTML that is emitted after
     * each page.
     */
    public String getPageFooter () {
        return footer;
    }
    /**
     * Set the divider.
     * @param divider string of HTML that is emitted between
     * each pair of pages.
     */
    public synchronized void setDivider (String divider) {
        this.divider = divider;
    }
    /**
     * Get the divider.
     * @return string of HTML that is emitted between
     * each pair of pages.
    */
    public String getDivider () {
        return divider;
    }

    /**
     * Set the epilog.
     * @param epilog string of HTML that is emitted after
     * the entire concatenation.
     */
    public synchronized void setEpilog (String epilog) {
        this.epilog = epilog;
    }
    /**
     * Get the epilog.
     * @return string of HTML that is emitted after
     * the entire concatenation.
     */
    public String getEpilog () {
        return epilog;
    }

    /**
     * Get number of pages written to this mirror.
     * @return number of calls to writePage() on this mirror
     */
    public synchronized int getPageCount () {
        return nPages;
    }

    /**
     * Rewrite the concatenation.  Makes sure all the links
     * among concatenated pages have been fixed up.
     */
    public synchronized void rewrite () throws IOException {
        if (needRewrite) {
            super.rewrite ();
            needRewrite = false;
        }
    }

    /**
     * Close the concatenation.  Makes sure all the links
     * among concatenated pages have been fixed up and closes
     * the file.
     */
    public synchronized void close () throws IOException {
        if (nPages == 0)
            write (prolog);
        emit (epilog);
        rewrite ();
        super.close ();
    }

    /**
     * Write a page to the concatenation.
     * @param page Page to write
     */
    public synchronized void writePage (Page page) throws IOException {
        ++nPages;

        emit ((nPages == 1) ? prolog : divider);

        String title = page.getTitle ();
        URL url = page.getURL ();
        String urlString = url.toExternalForm ();
        String anchor = "page" + nPages;
        map (url, "#" + anchor);        

        emitTemplate (header, title, urlString, anchor, nPages);
        if (page.isImage () && page.getURL() != null)
            super.write ("<IMG SRC='" + page.getURL() + "'>");
        else if (page.isHTML())
            // it's HTML, can write it normally
            super.writePage (page);
        else
            super.write (page.toHTML());
        emitTemplate (footer, title, urlString, anchor, nPages);
        
        needRewrite = nPages > 1;
    }

    private void emitTemplate (String template,
                               String title, String url,
                               String anchor, int pages) throws IOException {
        if (template == null || template.length() == 0)
            return;
            
        template = Str.replace (template, "%t", title != null ? title : "");
        template = Str.replace (template, "%u", url != null ? url : "");
        template = Str.replace (template, "%a", anchor != null ? anchor : "");
        template = Str.replace (template, "%p", String.valueOf (pages));
        emit (template);
    }

    /**
     * Process an HTML element for concatenation.  Deletes 
     * tags that would
     * conflict with other pages (such as &lt;HEAD&gt;),
     * changes the URLs in Link elements, and deletes
     * or remaps the BASE element.
     * @param elem HTML element to process
     */
    protected void handleElement (Element elem) throws IOException {
        String name = elem.getTagName ();
        if (   name == Tag.TITLE
            || name == Tag.STYLE
            || name == Tag.BASE
            || name == Tag.ISINDEX
            || name == Tag.FRAMESET
            || name == Tag.FRAME) {
            // skip the entire element
        }
        else if (   name == Tag.HTML
                 || name == Tag.HEAD
                 || name == Tag.BODY
                 || name == Tag.NOFRAMES) {
            // skip only the start and end tags; preserve the content
            transformContents (elem);
        }
        else
            super.handleElement (elem);
    }
    
    /*
     * Testing
     *     
     *
     *
     */
    public static void main (String[] args) throws Exception {
        HTMLTransformer out = new Concatenator (args[args.length-1]);
        for (int i=0; i<args.length-1; ++i) {
            Link link = new Link (args[i]);
            Page page = new Page (link);
            out.writePage (page);
        }
        out.close ();
    }
}
    
