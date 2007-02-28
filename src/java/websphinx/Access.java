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

import java.net.*;
import java.io.*;
import java.util.Vector;

public class Access {
    private File tempDir;
    private Vector temps = new Vector ();

    public Access () {
        String tempDirName;
        
        try {
            tempDirName = System.getProperty ("websphinx.temp.directory");
        } catch (SecurityException e) {
            tempDirName = null;
        }

        if (tempDirName == null) {
            String os = System.getProperty("os.name");
            tempDirName = (os.startsWith ("Windows"))
                            ? "c:\\temp\\"
                            : "/tmp/";
        }
        
        if (!(tempDirName.endsWith ("/") ||
              tempDirName.endsWith (File.separator)))
            tempDirName += "/";
        
        tempDir = new File (tempDirName);
    }

    public URLConnection openConnection (URL url) throws IOException {
        URLConnection conn = url.openConnection ();
        conn.connect ();
        return conn;
    }

    public URLConnection openConnection (Link link) throws IOException {
        // get the URL
        int method = link.getMethod();
        URL url;
        switch (method) {
            case Link.GET:
                url = link.getPageURL();
                break;
            case Link.POST:
                url = link.getServiceURL();
                break;
            default:
                throw new IOException ("Unknown HTTP method " + link.getMethod());
        }

        // open a connection to the URL
        URLConnection conn = url.openConnection ();

        // set up request headers
        DownloadParameters dp = link.getDownloadParameters ();
        if (dp != null) {
            conn.setAllowUserInteraction (dp.getInteractive ());
            conn.setUseCaches (dp.getUseCaches ());

            String userAgent = dp.getUserAgent ();
            if (userAgent != null)
                conn.setRequestProperty ("User-Agent", userAgent);

            String types = dp.getAcceptedMIMETypes ();
            if (types != null)
                conn.setRequestProperty ("accept", types);
        }

        // submit the query if it's a POST (GET queries are encoded in the URL)
        if (method == Link.POST) {
//#ifdef JDK1.1 
            if (conn instanceof HttpURLConnection)
                ((HttpURLConnection)conn).setRequestMethod ("POST");
//#endif JDK1.1
            
            String query = link.getQuery ();
            if (query.startsWith ("?"))
                query = query.substring (1);

            conn.setDoOutput (true);
            conn.setRequestProperty ("Content-type",
                                     "application/x-www-form-urlencoded");
            conn.setRequestProperty ("Content-length", String.valueOf(query.length()));

            // commence request
//#ifdef JDK1.1 
            PrintStream out = new PrintStream (conn.getOutputStream ());
//#endif JDK1.1
/*#ifdef JDK1.0
            PrintStream out = new PrintStream (conn.getOutputStream ());
#endif JDK1.0*/
            out.print (query);
            out.flush ();
        }

        conn.connect ();
        return conn;
    }

  public InputStream readFile (File file) throws IOException {
    return new FileInputStream (file);
  }

  public OutputStream writeFile (File file, boolean append) throws IOException {
    //#ifdef JDK1.1
    return new FileOutputStream (file.toString(), append);
    //#endif JDK1.1
    /*#ifdef JDK1.0
    if (append)
        throw new IOException ("Can't append to files under JDK 1.0");
    else
        return new FileOutputStream (file.toString());
    #endif JDK1.0*/        
  }

    public RandomAccessFile readWriteFile (File file) throws IOException {
        return new RandomAccessFile (file, "rw");
    }
    
    public void makeDir (File file) throws IOException {
        file.mkdirs ();
    }

    public File getTemporaryDirectory () {
        return tempDir;
    }

    public File makeTemporaryFile (String basename, String extension) {
        File dir = getTemporaryDirectory ();
        File f;
        synchronized (temps) {
            do
                f = new File (dir,
                              basename
                              + String.valueOf ((int)(Math.random() * 999999))
                              + extension);
            while (temps.contains (f) || f.exists());

            temps.addElement (f);
        }
        return f;
    }

    public void deleteAllTempFiles () {
        synchronized (temps) {
            for (int i=0; i<temps.size(); ++i) {
                File f = (File)temps.elementAt(i);
                f.delete ();
            }
            temps.setSize (0);
        }
    }

  /*
   * Global access object
   *
   */

    private static Access theAccess = new Access ();

    public static Access getAccess () {
        return theAccess;
    }

    public static void setAccess (Access access) {
        theAccess = access;
    }

}
