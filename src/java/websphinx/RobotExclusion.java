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
import java.util.Hashtable;
import java.io.PushbackInputStream;
import java.io.BufferedInputStream;
import java.util.Vector;

public class RobotExclusion {

    String myUserAgent;
        // user agent (name) of this crawler, all lower-case
    Hashtable entries = new Hashtable ();
        // maps from a website ("host:port") to String[]

    /**
     * Make a RobotExclusion object.
     * @param userAgent name of the robot using this object, as shown
     *  in the User-Agent header fields of its HTTP requests.  Use
     *  null for anonymous robots.
     */
    public RobotExclusion (String userAgent) {
        myUserAgent = userAgent.toLowerCase ();
    }

    /**
     * Check whether a URL is disallowed by robots.txt.
     * @param url URL to test
     * @return true if url's Web site denies robot access to the url
     */
    public boolean disallowed (URL url) {
        if (!url.getProtocol().startsWith ("http"))
            // only HTTP URLs are protected by robots.txt
            return false;

        String website = getWebSite (url);
        String[] rules = (String[])entries.get (website);

        if (rules == null) {
            rules = getRobotsTxt (website, myUserAgent);
            entries.put (website, rules);
        }

        String path = url.getFile ();
        for (int i=0; i<rules.length; ++i) {
            if (path.startsWith (rules[i])) {
                //System.err.println ("disallowed by rule " + rules[i]);
                return true;
            }
            //System.err.println ("allowed by rule " + rules[i]);
        }
        return false;
    }

    /**
     * Clear the cache of robots.txt entries.
     */
    public void clear () {
        entries.clear ();
    }

    /*
     * Implementation
     *
     */

    String getWebSite (URL url) {
        String hostname = url.getHost ();
        int port = url.getPort ();
        return port != -1 ? hostname + ":" + port : hostname;
    }

    Vector rulebuf = new Vector ();
    String[] getRobotsTxt (String website, String userAgent) {
        try {
            URL robotstxtURL = new URL ("http://" + website + "/robots.txt");
            URLConnection uc = Access.getAccess ().openConnection (robotstxtURL);
            PushbackInputStream in = new PushbackInputStream (new BufferedInputStream (uc.getInputStream ()));

            rulebuf.setSize (0);

            boolean relevant = false, specific = false;
            String lastFieldName = null;
            while (readField (in)) {
                //System.err.println (fieldName + ":" + fieldValue);

                if (fieldName == null) { // end of record
                    if (specific)
                        break; // while loop
                    relevant = false;
                }
                else if (fieldName.equals ("user-agent")) {
                    if (lastFieldName != null && lastFieldName.equals ("disallow")) {
                        // end of record
                        if (specific)
                            break; // while loop
                        relevant = false;
                    }

                    if (userAgent != null && userAgent.indexOf (fieldValue.toLowerCase()) != -1) {
                        relevant = true;
                        specific = true;
                        rulebuf.setSize (0);
                    }
                    else if (fieldValue.equals ("*")) {
                        relevant = true;
                        rulebuf.setSize (0);
                    }
                }
                else if (relevant && fieldName.equals ("disallow")) {
                    rulebuf.addElement (fieldValue);
                }
                else { // end of record
                    if (specific)
                        break; // while loop
                    relevant = false;
                }
                lastFieldName = fieldName;
            }

            in.close ();

            String[] rules = new String[rulebuf.size ()];
            rulebuf.copyInto (rules);
            return rules;
        } catch (Exception e) {
            // debugging only
            // System.err.println ("RobotExclusion: error while retrieving " + website + "/robots.txt:");
            // e.printStackTrace ();
            return new String[0];
        }
    }

    String fieldName, fieldValue;
    static final int MAX_LINE_LENGTH = 1024;
    StringBuffer linebuf = new StringBuffer ();

    // Reads one line from the input stream, parsing it into
    // fieldName and fieldValue.  Field name is lower case;
    // whitespace is stripped at both ends of name and value.
    // e.g., User-agent: Webcrawler
    // is parsed into fieldName="user-agent" and fieldValue="Webcrawler".
    // Field-less lines are parsed as fieldName=null and fieldValue=null.
    // Returns true if a line was read, false on end-of-file.
    boolean readField (PushbackInputStream in) throws Exception {
        fieldName = null;
        fieldValue = null;
        linebuf.setLength (0);

        int c;
        int n = 0;
        boolean saw_eoln = false;
        while (true) {
            c = in.read ();
            if (c == -1)
                break;
            else if (c == '\r' || c == '\n')
                saw_eoln = true;
            else if (saw_eoln) {
                in.unread (c);
                break;
            }
            else {
                linebuf.append ((char)c);
            }

            ++n;
            if (n == MAX_LINE_LENGTH)
                break;
        }

        //System.err.println (linebuf);

        if (n == 0)
            return false;

        // extract fields from line and return
        String line = linebuf.toString ();
        int colon = line.indexOf (':');
        if (colon == -1) {
            fieldName = null;
            fieldValue = null;
        }
        else {
            fieldName = line.substring (0, colon).trim ().toLowerCase ();
            fieldValue = line.substring (colon+1).trim ();
        }
        return true;
    }

    public static void main (String argv[]) throws Exception {
        RobotExclusion robot = new RobotExclusion (argv[0]);

        for (int i=1; i<argv.length; ++i) {
            System.out.println (argv[i] + ": "
                    + (!robot.disallowed (new URL (argv[i])) ? "OK" : "disallowed"));
        }
        System.in.read ();
    }
}
