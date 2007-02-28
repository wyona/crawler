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

public abstract class Mem {

    public static long free () {
        return Runtime.getRuntime ().freeMemory ();
    }

    public static long used () {
        Runtime r = Runtime.getRuntime ();
        return r.totalMemory() - r.freeMemory ();
    }

    public static long total () {
        return Runtime.getRuntime ().totalMemory ();
    }

    public static String getReport () {
        return "Memory: used " + (used()/1000) + "KB, free "
            + (free()/1000) + "KB, total " + (total()/1000) + "KB";
    }

    public static void gc () {
        Runtime r = Runtime.getRuntime ();
        r.runFinalization ();
        r.gc ();
    }

    public static void dumpThreadInfo () {
        ThreadGroup g = Thread.currentThread().getThreadGroup ();
        Thread[] t = new Thread[g.activeCount ()];
        g.enumerate (t);
        System.err.println ("Active threads in " + g);
        for (int i=0; i<t.length; ++i)
            System.err.println (t[i]);
    }

}
