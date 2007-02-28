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

/**
 * String which compares case-insensitively with other strings.
 * Especially useful as a case-insensitive hashtable key.
 */
public class CaselessString {
    String string;

    public CaselessString (String string) {
        this.string = string;
    }

    public boolean equals (Object obj) {
        if (obj instanceof String
            || obj instanceof CaselessString)
            return string.equalsIgnoreCase (obj.toString ());
        else
            return false;
    }

    public int hashCode() {
    	int hash = 0;
    	int len = string.length ();

    	if (len < 16) {
    	    // use all characters
     	    for (int i = 0; i < len; ++i)
         		hash = (hash * 37) + Character.toUpperCase (string.charAt (i));
     	} else {
     	    int skip = len / 8; // sample every 8th char
     	    for (int i = 0; i < len; i += skip)
         		hash = (hash * 39) + Character.toUpperCase (string.charAt (i));
     	}

    	return hash;
    }

    public String toString () {
        return string;
    }
}
