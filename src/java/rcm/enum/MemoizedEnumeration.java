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
 * Enumeration which can be restarted from the beginning.
 */
public class MemoizedEnumeration implements RestartableEnumeration  {
    Vector v;  // elements which have already been returned 
    Enumeration e1; // enumeration of v
    Enumeration e2; // main enumeration 

    public MemoizedEnumeration (Enumeration e) {
        this.v = new Vector ();
        this.e2 = e;
    }

    public MemoizedEnumeration (Vector v) {
        this.v = (Vector) v.clone ();
        this.e1 = this.v.elements ();
    }

    public boolean hasMoreElements () {
        if (e1 != null) {
            if (e1.hasMoreElements ())
                return true;
            else
                e1 = null;
        }

        if (e2 != null) {
            if (e2.hasMoreElements ())
                return true;
            else
                e2 = null;
        } 

        return false;
    }

    public Object nextElement () {
        if (e1 != null)
            try {
                return e1.nextElement ();
            } catch (NoSuchElementException e) {
                e1 = null;
            }

        if (e2 != null)
            try {
                Object o = e2.nextElement ();
                v.addElement (o);
                return o;
            } catch (NoSuchElementException e) {
                e2 = null;
            }

        throw new NoSuchElementException ();
    }

    public void restart () {
        e1 = v.elements ();
    }
}
