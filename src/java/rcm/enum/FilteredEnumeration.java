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

package rcm.enum;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Enumeration which transforms the elements of another enumeration.
 */
public abstract class FilteredEnumeration implements Enumeration {
    Enumeration e;
    Object o; // first object waiting to be returned, or null if none
    Vector v; // other objects yielded and waiting to be returned
    int i;    // next object to return from v

    public FilteredEnumeration (Enumeration e) {
        this.e = e;
    }

    public boolean hasMoreElements () { 
        next ();
        return o != null;
    }

    public Object nextElement () { 
        next ();
        if (o == null)
            throw new NoSuchElementException ();
        Object result = o;
        o = null;
        return result;
    }

    void next () {
        // check if yielded element is waiting to be returned 
        if (o != null)
            return;

        // check in v for other yielded elements
        if (v != null) {
            if (i < v.size ()) {
                o = v.elementAt (i);
                v.setElementAt (null, i);
                ++i;
            } else {
                v.setSize (0);
                i = 0;
            }
        }

        // transform elements until an element is yielded
        while (o == null && e.hasMoreElements ())
            transform (e.nextElement ());
    }

    public void yield (Object obj) {
        if (o == null)
            o = obj;
        else {
            if (v == null)
                v = new Vector ();
            v.addElement (obj);
        }
    }

    public abstract void transform (Object o);
}
