/*
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

package rcm.util;

import java.io.*;

public abstract class Exec {
    public static Debug debug = Debug.QUIET;

    public static Process exec (String[] cmdarray) throws IOException {
        return exec (cmdarray, null, null);
    }

    public static Process exec (String[] cmdarray, String[] envp) throws IOException {
        return exec (cmdarray, envp, null);
    }

    public static Process exec (String[] cmdarray, String[] envp, File directory) throws IOException {
	return 
	    isWindows ()
	    ? execWindows (cmdarray, envp, directory)
	    : execUnix (cmdarray, envp, directory);
    } 

    /*
     * Unix
     */

    static Process execUnix (String[] cmdarray, String[] envp, File directory) throws IOException {
        // instead of calling command directly, we'll call the shell to change
        // directory and set environment variables.

        // start constructing the sh command line.
        StringBuffer buf = new StringBuffer ();

        if (directory != null) {
            // change to directory
            buf.append ("cd '");
            buf.append (escapeQuote (directory.toString ()));
            buf.append ("'; ");
        }

        if (envp != null) {
            // set environment variables.  Quote the value (but not the name).
            for (int i = 0; i < envp.length; ++i) {
                String nameval = envp[i];
                int equals = nameval.indexOf ('=');
                if (equals == -1)
                    throw new IOException ("environment variable '" + nameval 
                                           + "' should have form NAME=VALUE");
                buf.append (nameval.substring (0, equals+1));
                buf.append ('\'');
                buf.append (escapeQuote (nameval.substring (equals+1)));
                buf.append ("\' ");
            }
        }
        
        // now that we have the directory and environment, run "which" 
        // to test if the command name is found somewhere in the path.
        // If "which" fails, throw an IOException.
        String cmdname = escapeQuote (cmdarray[0]); 
        Runtime rt = Runtime.getRuntime ();
        String[] sharray = new String[] { "sh", "-c", buf.toString () + " which \'" + cmdname + "\'" };
        Process which = rt.exec (sharray);
        try {
            which.waitFor ();
        } catch (InterruptedException e) {
            throw new IOException ("interrupted");
        }

        if (which.exitValue () != 0) 
            throw new IOException ("can't execute " + cmdname + ": bad command or filename"); 

        // finish in 
        buf.append ("exec \'");
        buf.append (cmdname);
        buf.append ("\' ");

        // quote each argument in the command
        for (int i = 1; i < cmdarray.length; ++i) {
            buf.append ('\'');
            buf.append (escapeQuote (cmdarray[i]));
            buf.append ("\' ");
        }

        debug.println ("executing " + buf);
        sharray[2] = buf.toString ();
        return rt.exec (sharray);
    }

    static String escapeQuote (String s) {
        // replace single quotes with a bit of magic (end-quote, escaped-quote, start-quote) 
        // that works in a single-quoted string in the Unix shell
        if (s.indexOf ('\'') != -1) {
            debug.println ("replacing single-quotes in " + s);
            s = Str.replace (s, "'", "'\\''");
            debug.println ("to get " + s);
        }
        return s;
    }

    /*
     * Windows
     */

     static boolean isWindows () {
        String os = System.getProperty ("os.name");
 	return (os != null && os.startsWith ("Windows"));
     }

     static boolean isJview () {
        String vendor = System.getProperty ("java.vendor");
 	return (vendor != null && vendor.startsWith ("Microsoft"));
     }

    static Process execWindows (String[] cmdarray, String[] envp, File directory) throws IOException {
	if (envp != null || directory != null) {
	    if (isJview ())
		// jview doesn't support JNI, so can't call putenv/chdir
		throw new IOException 
		    ("can't use Exec.exec() under Microsoft JVM");
	    
	    if (!linked) {
		try {
		    System.loadLibrary ("win32exec");
		    linked = true;
		} catch (LinkageError e) {
		    throw new IOException ("can't use Exec.exec(): "
					   + e.getMessage ());
		}
	    }
	    
	    if (envp != null) {
		for (int i = 0; i < envp.length; ++i)
		    putenv (envp[i]);
	    }
	    
	    if (directory != null)
		chdir (directory.toString ());
	}

        return Runtime.getRuntime ().exec (cmdarray);
    }

    static boolean linked = false; // true after System.loadLibrary() is called
    static native boolean putenv (String env);
    static native boolean chdir (String dir);
}
