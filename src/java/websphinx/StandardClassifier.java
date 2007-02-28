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

/**
 * Standard classifier, installed in every crawler by default.
 * <P>On the entire page, this classifier sets the following labels:
 * <UL>
 * <LI><B>root</B>: page is the root page of a Web site.  For instance,
 *     "http://www.digital.com/" and "http://www.digital.com/index.html" are both
 *     marked as root, but "http://www.digital.com/about" is not.
 * </UL>
 * <P>Also sets one or more of the following labels on every link:
 * <UL>
 * <LI><B>hyperlink</B>: link is a hyperlink (A, AREA, or FRAME tags) to another page on the Web (using http, file, ftp, or gopher protocols)
 * <LI><B>image</B>: link is an inline image (IMG).
 * <LI><B>form</B>: link is a form (FORM tag).  A form generally requires some parameters to use.
 * <LI><B>code</B>: link points to code (APPLET, EMBED, or SCRIPT).
 * <LI><B>remote</B>: link points to a different Web server.
 * <LI><B>local</B>: link points to the same Web server.
 * <LI><B>same-page</B>: link points to the same page (e.g., by an anchor reference like "#top")
 * <LI><B>sibling</B>: a local link that points to a page in the same directory (e.g. "sibling.html")
 * <LI><B>descendent</B>: a local link that points downwards in the directory structure (e.g., "deep/deeper/deepest.html")
 * <LI><B>ancestor</B>: a link that points upwards in the directory structure (e.g., "../..")
 * </UL>
 */
public class StandardClassifier implements Classifier  {

    /**
     * Make a StandardClassifier.
     */
    public StandardClassifier () {
    }

    /** 
     * Classify a page.
     * @param page Page to classify
     */
    // FIX: use regular expressions throughout this method
    public void classify (Page page) {
        Link origin = page.getOrigin ();
        String pageHost = origin.getHost ();
        int pagePort = origin.getPort ();
        String pagePath = origin.getFile();
        String pageFilename = origin.getFilename();

        URL base = page.getBase ();
        String baseHost = base.getHost ();
        int basePort = base.getPort ();
        String basePath = base.getFile ();

        if (pageFilename.equals ("") || pageFilename.startsWith ("index.htm"))
            page.setLabel ("root");

        // FIX: Link needs to resolve "foo/bar/.." and "foo/." to "foo" in order for this
        // stuff to work properly
        Link[] links = page.getLinks ();
        if (links != null) {
            for (int i=0; i<links.length; ++i) {
                Link link = links[i];
                
                if ((link.getHost().equals (pageHost)
                     && link.getPort() == pagePort)
                    || (link.getHost().equals (baseHost)
                        && link.getPort() == basePort)) {
                    link.setLabel ("local");
                    
                    String linkPath = link.getFile ();
                    
                    if (linkPath.equals (pagePath)
                        || linkPath.equals (basePath))
                        link.setLabel ("same-page");
                    else if (link.getDirectory ().equals (origin.getDirectory ()))
                        link.setLabel ("sibling");
                    else if (descendsFrom (linkPath, pagePath)
                             || descendsFrom (linkPath, basePath))
                        link.setLabel ("descendent");
                    else if (descendsFrom (pagePath, linkPath)
                             || descendsFrom (basePath, linkPath))
                        link.setLabel ("ancestor");
                    // NIY: child, parent
                }
                else
                    link.setLabel ("remote");

                // Link tag kinds: resource, form, hyperlink
                String tagName = link.getTagName();
                
                if (tagName == Tag.IMG)
                    link.setLabel ("image");
                else if (tagName == Tag.APPLET || tagName == Tag.EMBED || tagName == Tag.SCRIPT)
                    link.setLabel ("code");
                else if (tagName == Tag.FORM)
                    link.setLabel ("form");
                else if (tagName == Tag.A || tagName == Tag.AREA || tagName == Tag.FRAME) {
                    String protocol = link.getProtocol ();
                    
                    if ((protocol.equals ("http")
                         || protocol.equals ("ftp")
                         || protocol.equals ("file")
                         || protocol.equals ("gopher"))
                        && link.getMethod() == Link.GET)
                        link.setLabel ("hyperlink");
                }
            }
        }
    }

    private boolean descendsFrom (String path1, String path2) {
        return path1.startsWith (path2.endsWith ("/")
                                 ? path2
                                 : path2 + "/");
    }

    /**
     * Priority of this classifier.
     */
    public static final float priority = 0.0F;
    
    /**
     * Get priority of this classifier.
     * @return priority.
     */
    public float getPriority () {
        return priority;
    }
}
