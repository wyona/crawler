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

import java.util.Vector;
import java.util.Enumeration;

/**
 * Base class for pattern matchers.
 */
public abstract class Pattern 
//#ifdef JDK1.1 
implements java.io.Serializable 
//#endif JDK1.1
{

    public abstract PatternMatcher match (Region region);

    public boolean found (Region region) {
        return match(region).hasMoreElements ();
    }

    public Region oneMatch (Region region) {
        return match(region).nextMatch ();
    }

    public Region[] allMatches (Region region) {
        Vector v = new Vector ();
        PatternMatcher enum = match (region);
        Region r;
        while ((r = enum.nextMatch ()) != null)
            v.addElement (r);
        
        Region[] regions = new Region[v.size ()];
        v.copyInto (regions);
        return regions;
    }

    public boolean found (String string) {
        return found (new Page (string));
    }

    public Region oneMatch (String string) {
        return oneMatch (new Page (string));
    }

    public Region[] allMatches (String string) {
        return allMatches (new Page (string));
    }
    
    public String[] getFieldNames () {
        return new String[0];
    }
    
    /**
     * Return a string representation of the pattern.
     */
    public abstract String toString ();

    public static final String groups = "Pattern.groups";

}

