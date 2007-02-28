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

import java.util.*;
import java.io.*;

public class Config extends Properties {
    File file;
    IOException lastException;

    public Config (String fileBaseName) {
        this (fileBaseName, null);
    }

    public Config (File file) {
        this (file, null);
    }

    public Config (String fileBaseName, Config defaults) {
        this (new File (getHomeDirectory (), fileBaseName),
              defaults);
    }

    public Config (File file, Config defaults) {
        super (defaults);

        this.file = file;
        try {
            FileInputStream in = 
                new FileInputStream (file);
            load (in);
            in.close ();
        } catch (IOException e) {
            lastException = e;
        }
    }

    public IOException getLastException () {
        return lastException;
    }

//     public String getProperty (String key, String defaultValue) {
//         String val = super.getProperty (key, defaultValue);
//         if (val != null && val == defaultValue)
//             put (key, defaultValue);
//         return val;
//     }

    public void save () {
        try {
            FileOutputStream out = new FileOutputStream (file);
            save (out, "");
            out.close ();
            lastException = null;
        } catch (IOException e) {
            lastException = e;
        }
    }

    public int countKeysStartingWith (String prefix) {
        int n = 0;
        for (Enumeration e = propertyNames (); e.hasMoreElements (); ) {
            String name = (String)e.nextElement ();
            if (name.startsWith (prefix))
                ++n;
        }
        return n;
    }

    public void removeAllKeysStartingWith (String prefix) {
        Vector keysToDelete = new Vector ();
        for (Enumeration e = propertyNames (); e.hasMoreElements (); ) {
            String name = (String)e.nextElement ();
            if (name.startsWith (prefix))
                keysToDelete.addElement (name);
        }
        
        for (Enumeration e = keysToDelete.elements ();
             e.hasMoreElements (); )
            remove (e.nextElement ());
    }
    
    public static File getHomeDirectory () {
        String homedir;
        if ((homedir = System.getProperty ("user.home")) == null
            && (homedir = System.getProperty ("user.dir")) == null)
            homedir = ".";
        
        return new File (homedir);
    }
}
