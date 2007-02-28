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

/**
 * Enumeration which transforms the elements of another enumeration.
 */
public class ConcatEnumeration implements Enumeration {
    Enumeration[] e;
    int i = 0; // current enumeration in e

    public ConcatEnumeration (Enumeration e1, Enumeration e2) {
        this.e = new Enumeration[] { e1, e2 };
    }

    public ConcatEnumeration (Enumeration[] e) {
        this.e = e;
    }

    public boolean hasMoreElements () {
        while (i < e.length) {
            if (e[i].hasMoreElements ())
                return true;
            else
                ++i;
        }
        return false;
    }

    public Object nextElement () {
        while (i < e.length) {
            try {
                return e[i].nextElement ();
            } catch (NoSuchElementException e) {
                ++i;
            }
        }

        throw new NoSuchElementException ();
    }
}
