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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Label;
import java.util.StringTokenizer;

// FIX: convert to RichString which supports font and color runs

public class MultiLineString {

    String[] lines;

    public MultiLineString (String string) {
        StringTokenizer tok = new StringTokenizer (string, "\n");
        lines = new String[tok.countTokens ()];
        for (int i=0; i<lines.length; ++i)
            lines[i] = tok.nextToken ();
    }

    public MultiLineString (String[] lines) {
        this.lines = lines;
    }

    public int countLines () {
        return lines.length;
    }

    public String getLineAt (int i) {
        return lines[i];
    }

    public int getWidth (FontMetrics fm) {
        int w = 0;
        for (int i=0; i<lines.length; ++i)
            w = Math.max (w, fm.stringWidth (lines[i]));
        return w;
    }

    public int getHeight (FontMetrics fm) {
        return fm.getHeight() * lines.length;
    }

    public void draw (Graphics g, int x, int y, int alignment) {
        FontMetrics fm = g.getFontMetrics ();
        
        y += fm.getAscent ();
        
        int width = alignment != Label.LEFT
            ? getWidth (fm)
            : 0; // don't need it if alignment is LEFT
        
        for (int i=0; i<lines.length; ++i) {
            int x1 = x;
            switch (alignment) {
                case Label.LEFT:
                    break;
                case Label.RIGHT:
                    x += width - fm.stringWidth (lines[i]);
                    break;
                case Label.CENTER:
                    x += (width - fm.stringWidth (lines[i]))/2;
                    break;
            }
                
            g.drawString (lines[i], x, y);
            y += fm.getHeight ();
        }
    }

}
