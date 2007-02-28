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

package rcm.awt;

import java.util.Hashtable;
import java.awt.Color;

public abstract class Colors {

    static Hashtable colors = new Hashtable ();
    static {
        colors.put ("black", Color.black);
        colors.put ("blue", Color.blue);
        colors.put ("cyan", Color.cyan);
        colors.put ("darkGray", Color.darkGray);
        colors.put ("gray", Color.gray);
        colors.put ("green", Color.green);
        colors.put ("lightGray", Color.lightGray);
        colors.put ("magenta", Color.magenta);
        colors.put ("orange", Color.orange);
        colors.put ("pink", Color.pink);
        colors.put ("red", Color.red);
        colors.put ("white", Color.white);
        colors.put ("yellow", Color.yellow);
    }

    public static Color parseColor (String name) {
        if (name == null)
            return null;

        Color c = (Color)colors.get (name);

        if (c != null)
            return c;
        else if (name.startsWith ("#") && name.length() == 7) {
            c = new Color (Integer.parseInt(name.substring (1, 3), 16),
                              Integer.parseInt(name.substring (3, 5), 16),
                              Integer.parseInt(name.substring (5, 7), 16));
            colors.put (name, c);
            return c;
        }
        else
            return null; // I give up
    }

}
