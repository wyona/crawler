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
import java.util.Vector;

/**
 * Offline mirror of a Web site.  Web pages written to
 * a mirror are stored as files on the local disk in a directory
 * structure mirroring their URLs.
 * <P>
 */
 
// FIX: discards ALL anchors (for some reason)
public class Mirror extends LinkTransformer {
    String root;
        // Root directory represented as file:/<dir>/
        
    Vector files = new Vector ();
        // collection of RewritableLinkTransformers, one for each
        // file in the mirror

    boolean needRewrite = false;

    String defaultFilename = "index.html";
        // name given to a directory URL (like http://foo.com/)
        // when it is saved to disk

    /**
     * Make a new Mirror.
     * @param directory Root directory (on local disk
     * relative to which the mirror pages are stored)
     */
    public Mirror (String directory) throws IOException {
        super ((HTMLTransformer)null);
        if (!directory.endsWith ("/"))
            directory += "/";
        File rootFile = new File (directory);
        if (!rootFile.isAbsolute ())
            rootFile = new File (rootFile.getAbsolutePath ());
        URL rootURL = Link.FileToURL (rootFile);
        root = rootURL.toExternalForm ();
    }
    
    /**
     * Get the filename used for directory URLs.
     * For example, if the default filename is "index.html",
     * then the remote URL "http://www.xxx.com/path/" would
     * map to the local pathname "www.xxx.com/path/index.html".
     * @return default filename.  Default is "index.html".
     */
    public String getDefaultFilename () {
        return defaultFilename;
    }

    /**
     * Set the filename used for directory URLs.
     * For example, if the default filename is "index.html",
     * then the remote URL "http://www.xxx.com/path/" would
     * map to the local pathname "www.xxx.com/path/index.html".
     * @param filename Default filename.
     */
    public synchronized void setDefaultFilename (String filename) {
        defaultFilename = filename;
    }

    /**
     * Get number of pages written to this mirror.
     * @return number of calls to writePage() on this mirror
     */
    public synchronized int getPageCount () {
        return files.size ();
    }

    public void write (Region region) throws IOException {
        throw new IOException ("write(Region) not supported by Mirror");
    }

    public void write (String string) throws IOException {
        throw new IOException ("write(String) not supported by Mirror");
    }

    /**
     * Write a page to the mirror. Stores the page on the local
     * disk, fixing up its links to point to the local
     * copies of any pages already stored to this mirror.
     * @param page Page to write
     */
    public synchronized void writePage (Page page) throws IOException {
        URL url = page.getURL ();        
        String local = toLocalFileURL (url);
        URL localURL = new URL (local);
        File localFile = Link.URLToFile (localURL);        

        File parent = new File (localFile.getParent ());
        if (parent != null)
            Access.getAccess ().makeDir (parent);

        MirrorTransformer out = new MirrorTransformer (this, localFile);
        out.setBase (localURL);
        out.setEmitBaseElement (getEmitBaseElement ());
        out.writePage (page);
        out.close ();
        
        needRewrite = !files.isEmpty ();
        files.addElement (out);
    }

    /**
     * Close the mirror.  Makes sure that links point to local versions of
     * pages wherever possible.
     */
    public synchronized void close () throws IOException {
        rewrite ();
    }

    /**
     * Rewrite the mirror to make local links consistent.
     */
    public synchronized void rewrite () throws IOException {
        if (needRewrite) {
            for (int i=0, n = files.size (); i < n; ++i) {
                RewritableLinkTransformer r = 
                    (RewritableLinkTransformer)files.elementAt (i);
                r.rewrite ();
            }
            needRewrite = false;
        }
    }
    
    // maps a remote URL to a local file URL ("<root>/<host>/<filename>")
    // resulting URL is never slash-terminated
    private String toLocalFileURL (URL remoteURL) {
        if (isMapped (remoteURL))
            return lookup (null, remoteURL);
            
        String remote = remoteURL.toExternalForm ();
        URL remoteDirURL = Link.getDirectoryURL (remoteURL);
        String remoteDir = remoteDirURL.toExternalForm();
        String remoteFile = (remote.length() > remoteDir.length()) ? encode (remote.substring (remoteDir.length())) : defaultFilename;
        String localDir = toLocalDirURL (remoteDirURL);
        String local = localDir + remoteFile;
        
        map (remoteURL, local);
        return local;
    }

    // Maps a remote directory URL (slash-terminated) to a local 
    // directory URL (slash-terminated)
    private String toLocalDirURL (URL remoteURL) {
        if (isMapped (remoteURL))
            return lookupDir (null, remoteURL);

        String remote = remoteURL.toExternalForm ();
        String local;
        URL remoteParentURL = Link.getParentURL (remoteURL);
        
        if (remoteParentURL.equals (remoteURL)) {
            // we've reached http://host/
            String host = remoteURL.getHost ();
            int port = remoteURL.getPort ();
            local = root
                    + encode ((port != -1) ? host + ":" + port : host)
                    + '/';
        }
        else {
            String remoteParent = remoteParentURL.toExternalForm();
            String remoteFile = encode (remote.substring (remoteParent.length(),
                                                          remote.length()-1));
            String localDir = toLocalDirURL (remoteParentURL);
            local = localDir + remoteFile + "/";
        }
            
        map (remoteURL, local);
        return local;
    }

    /**
     * Map a directory URL (of the form http://host/path/) to
     * a local directory.
     * @param url Directory URL.  Must end with a slash.
     * @param dir Local directory relative to which descendents of
     * url should be saved.
     */
    public synchronized void mapDir (URL url, String dir) throws MalformedURLException {
        if (!dir.endsWith ("/"))
            dir += "/";
        map (Link.getDirectoryURL (url),
             Link.FileToURL (new File (dir + defaultFilename))
             .toString());
    }
    
    /**
     * Lookup the local directory to which a remote directory
     * URL maps.
     * @param base local file URL to use as a base.  If non-null,
     * then the returned pathname is relative to this URL.  If
     * null, the returned pathname is an absolute URL (file:/path/).
     * @param url remote directory URL to look up. Must end in slash.
     */
    public String lookupDir (URL base, URL url) {
        String href = lookup (base, url);
        int lastSlash = href.lastIndexOf ('/');
        return href.substring(0, lastSlash+1);
    }

    private static String canonicalDir (String dir) {
        dir = dir.replace ('\\', '/');
        if (!dir.endsWith ("/"))
            dir += "/";
        if (!dir.startsWith ("/"))
            dir = "/" + dir;
        return dir;
    }
    
    private static String encode (String component) {
        char[] chars = component.toCharArray ();

        for (int i=0; i<chars.length; ++i)
            switch (chars[i]) {
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                case 'G': case 'H': case 'I': case 'J': case 'K': case 'L':
                case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
                case 'S': case 'T': case 'U': case 'V': case 'W': case 'X':
                case 'Y': case 'Z':

                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                case 'g': case 'h': case 'i': case 'j': case 'k': case 'l':
                case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
                case 's': case 't': case 'u': case 'v': case 'w': case 'x':
                case 'y': case 'z':

                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':

                case '.': case '-': case '_': case '~':

        	        break;

        	    default:
        	        chars[i] = '_';
        	        break;
        	}

        return new String (chars);
    }

    /*
     * Testing
     *
     */
    public static void main (String[] args) throws Exception {
        String directory = args[args.length-1];        
        Mirror out = new Mirror (directory);
        out.mapDir (new URL (args[0]), directory);
        for (int i=0; i<args.length-1; ++i) {
            Link link = new Link (args[i]);
            Page page = new Page (link);
            out.writePage (page);
        }
        out.close ();
    }

}

class MirrorTransformer extends RewritableLinkTransformer {
    Mirror mirror; // on the wall?
    
    public MirrorTransformer (Mirror mirror, File file) throws IOException {
        super (file.toString());
        this.mirror = mirror;
    }
    
    public String lookup (URL base, URL url) {
        return mirror.lookup (base, url);
    }
    
    public void map (URL remoteURL, String href) {
        mirror.map (remoteURL, href);
    }
    
    public void map (URL remoteURL, URL url) {
        mirror.map (remoteURL, url);
    }
    
    public boolean isMapped (URL url) {
        return mirror.isMapped (url);
    }
}
    
