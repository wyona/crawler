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
public abstract class PairEnumeration implements Enumeration {
    Enumeration e1;
    Enumeration e2;

    PairEnumeration history;
    Vector e1History = new Vector (); // all objects returned by e1
    Vector e2History = new Vector (); // all objects returned by e2

    Object r; // last object returned by e1
    Enumeration e; // enumeration of e2's history
    boolean swapped = false;

    Object o; // first object yielded by transform ()
    Vector v; // other objects yielded by transform ()
    int i;    // next object to return from v

    int state = INIT;
    static final int INIT = 0;
    static final int RUNNING = 1;
    static final int DONE = 2;

    public PairEnumeration (Enumeration e1, Enumeration e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    public PairEnumeration (Enumeration e1, Enumeration e2, PairEnumeration history) {
        this.e1 = e1;
        this.e2 = e2;
        this.history = history;
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
        if (state == INIT) {
            if (history != null) {
                if (swapped == history.swapped) {
                    e1History = (Vector) history.e1History.clone ();
                    e2History = (Vector) history.e2History.clone ();
                } else {
                    e2History = (Vector) history.e1History.clone ();
                    e1History = (Vector) history.e2History.clone ();
                }
            }

            if (!e1.hasMoreElements ())
                swap ();

            if (e1.hasMoreElements ()) {
                r = e1.nextElement ();
                e = e2History.elements ();
                state = RUNNING;
            } else
                state = DONE;
        }

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

        // transform more pairs of elements until at least one 
        // output element is yielded
        while (o == null && state != DONE) {
            while (o == null && e.hasMoreElements ()) {
                Object s = e.nextElement ();
                if (swapped) 
                    transform (s, r);
                else
                    transform (r, s);
            }
            if (o != null)
                return;

            e1History.addElement (r);
            if (e2.hasMoreElements ())
                swap ();

            if (e1.hasMoreElements ()) {
                r = e1.nextElement ();
                e = e2History.elements ();
            } else
                state = DONE;
        }
    }

    void swap () {
        Enumeration te;
        Vector tv;

        te = e1;
        e1 = e2;
        e2 = te;

        tv = e1History;
        e1History = e2History;
        e2History = tv;

        swapped = !swapped;
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

    public abstract void transform (Object o1, Object o2);
}
