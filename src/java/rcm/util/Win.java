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

import java.awt.*;

public abstract class Win {
    public static void center (Window window, Component ref) {
        position (window, ref, 0.5, 0.5);
    }

    public static void position (Window frame, Component ref, 
                                 double xfrac, double yfrac) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = frame.getSize();
        Dimension refSize = (ref != null) 
            ? ref.getSize() 
            : screenSize;
        Point origin = (ref != null)
            ? ref.getLocationOnScreen () 
            : new Point (0, 0);

        int x = origin.x + relativePoint (xfrac, refSize.width, size.width);
        int y = origin.y + relativePoint (yfrac, refSize.height, size.height);

        // make sure frame is entirely on screen
        x = Math.max (0, Math.min (screenSize.width - size.width, x));
        y = Math.max (0, Math.min (screenSize.height - size.height, y));
        
        frame.setLocation (x, y);
    }

    static int relativePoint (double frac, int parentLength, int childLength) {
        if (frac < 0)
            return (int) (frac * childLength);
        else if (frac > 1)
            return (int) (parentLength + (frac - 2) * childLength);
        else
            return (int) (frac * (parentLength - childLength));
    }


    public static Frame findFrame (Component comp) {
        for (; comp!=null; comp = comp.getParent ())
            if (comp instanceof Frame) {
                return (Frame)comp;
            }
        return null;
    }

    public static Frame findFrameOrMakeFrame (Component parent) {
        return (parent != null) ? findFrame (parent) : new Frame ();
    }
        

}
