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

import java.awt.*;

public class BorderPanel extends Panel {

    int left, top, bottom, right;

    public BorderPanel (Insets insets) {
        this.left = insets.left;
        this.top = insets.top;
        this.bottom = insets.bottom;
        this.right = insets.right;
    }

    public BorderPanel (int left, int top, int bottom, int right) {
        this.left = left;
        this.top = top;
        this.bottom = bottom;
        this.right = right;
    }


    public void layout () {
        Dimension d = getSize ();

        int x = left;
        int y = top;
        int w = d.width - left - right;
        int h = d.height - top - bottom;

        Component[] comps = getComponents ();
        for (int i=0; i<comps.length; ++i)
            comps[i].setBounds (x, y, w, h);
    }

    public Dimension getPreferredSize () {
        Dimension max = new Dimension (0, 0);

        Component[] comps = getComponents ();
        for (int i=0; i<comps.length; ++i) {
            Dimension d = comps[i].getPreferredSize ();
            max.width = Math.max (d.width, max.width);
            max.height = Math.max (d.height, max.height);
        }

        max.width += left+right;
        max.height += top+bottom;
        return max;
    }

    public Dimension getMinimumSize () {
        Dimension max = new Dimension (0, 0);

        Component[] comps = getComponents ();
        for (int i=0; i<comps.length; ++i) {
            Dimension d = comps[i].getMinimumSize ();
            max.width = Math.max (d.width, max.width);
            max.height = Math.max (d.height, max.height);
        }

        max.width += left+right;
        max.height += top+bottom;
        return max;
    }

    public static Panel wrap (Component comp, Insets insets) {
        Panel p = new BorderPanel (insets);
        p.add (comp);
        return p;
    }

    public static Panel wrap (Component comp, int left, int top, int bottom, int right) {
        Panel p = new BorderPanel (left, top, bottom, right);
        p.add (comp);
        return p;
    }

    public static Panel wrap (Component comp, int horiz, int vert) {
        Panel p = new BorderPanel (horiz, vert, horiz, vert);
        p.add (comp);
        return p;
    }

    public static Panel wrap (Component comp, int all) {
        Panel p = new BorderPanel (all, all, all, all);
        p.add (comp);
        return p;
    }
}
