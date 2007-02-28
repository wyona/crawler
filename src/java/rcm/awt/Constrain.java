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

public abstract class Constrain {

    public static void add (Container container, Component comp, Object constraints) {
        container.add (comp);
        GridBagLayout layout = (GridBagLayout)container.getLayout ();
        GridBagConstraints c = (GridBagConstraints)constraints;
        layout.setConstraints (comp, c);
    }        

    public static GridBagConstraints labelLike (int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        return c;
    }
        
    public static GridBagConstraints labelLike (int x, int y, int w) {
        GridBagConstraints c = labelLike (x, y);
        c.gridwidth = w;
        return c;
    }
        
    public static GridBagConstraints fieldLike (int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    public static GridBagConstraints fieldLike (int x, int y, int w) {
        GridBagConstraints c = fieldLike (x, y);
        c.gridwidth = w;
        return c;
    }

    public static GridBagConstraints areaLike (int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        return c;
    }

    public static GridBagConstraints areaLike (int x, int y, int w) {
        GridBagConstraints c = areaLike (x, y);
        c.gridwidth = w;
        return c;
    }

    public static GridBagConstraints rightJustify (GridBagConstraints c) {
        c.anchor = GridBagConstraints.NORTHEAST;
        return c;
    }

    public static GridBagConstraints centered (GridBagConstraints c) {
        c.anchor = GridBagConstraints.CENTER;
        return c;
    }

    public static GridBagConstraints anchor (GridBagConstraints c,
                                               int anchor) {
        c.anchor = anchor;
        return c;
    }

    public static Panel makeConstrainedPanel () {
        Panel panel = new Panel ();
        panel.setLayout (new GridBagLayout ());
        return panel;        
    }

    public static Panel makeConstrainedPanel (int w, int h) {
        Panel panel = makeConstrainedPanel ();
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = h;
        c.gridwidth = w;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.VERTICAL;
        add (panel, new Panel(), c);
        
        c = new GridBagConstraints();
        c.gridx = w;
        c.gridy = 0;
        c.gridheight = h;
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        add (panel, new Panel(), c);
        
        return panel;
    }

}
