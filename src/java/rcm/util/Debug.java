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

import java.lang.reflect.*;
import java.io.PrintStream;

public abstract class Debug {
    public static final Debug VERBOSE = new Verbose ();
    public static final Debug QUIET = new Quiet ();
    public static final Debug NONE = new NoDebug ();

    public static Debug getDebugLevel (Class cls)
        throws NoSuchFieldException {
        try {
            Field fld = cls.getField ("debug");
            if (fld.getType () != Debug.class
                || !Modifier.isStatic (fld.getModifiers ()))
                throw new NoSuchFieldException ();
            return (Debug) fld.get (null);
        } catch (IllegalArgumentException e) {
            throw new NoSuchFieldException ();
        } catch (IllegalAccessException e) {
            throw new NoSuchFieldException ();
        } catch (SecurityException e) {
            throw new NoSuchFieldException ();
        }
    }

    public static void setDebugLevel (Class cls, Debug level) 
        throws NoSuchFieldException {
        try {
            Field fld = cls.getField ("debug");
            if (fld.getType () != Debug.class
                || !Modifier.isStatic (fld.getModifiers ()))
                throw new NoSuchFieldException ();
            fld.set (null, level);
        } catch (IllegalArgumentException e) {
            throw new NoSuchFieldException ();
        } catch (IllegalAccessException e) {
            throw new NoSuchFieldException ();
        } catch (SecurityException e) {
            throw new NoSuchFieldException ();
        }
    }

    public abstract boolean isEnabled ();
    public abstract void print (String message);
    public abstract void println (String message);
    public abstract void print (Object obj);
    public abstract void println (Object obj);
    public abstract void report (Throwable t);
    public abstract void printThreadInfo ();
    public abstract void printStackTrace ();
    public abstract void assertion (boolean f);

    public static class Verbose extends Debug { 
        protected PrintStream out;

        public Verbose () {
            this (System.err);
        }

        public Verbose (PrintStream out) {
            this.out = out;
        }

        public boolean isEnabled () {
            return true;
        }

        public void print (String message) {
            out.print (message);
            out.flush ();
        }

        public void println (String message) {
            out.println (message);
            out.flush ();
        }

        public void print (Object obj) {
            print (obj.toString ());
        }

        public void println (Object obj) {
            println (obj.toString ());
        }

        public void report (Throwable t) {
            t.printStackTrace (out);
            out.flush ();
        }

        public void printThreadInfo () {
            ThreadGroup g = Thread.currentThread().getThreadGroup ();
            Thread[] t = new Thread[g.activeCount ()];
            g.enumerate (t);
            out.println ("Active threads in " + g);
            for (int i=0; i<t.length; ++i)
                out.println (t[i]);
            out.flush ();
        }

        public void printStackTrace () {
            try {
                throw new Exception ();
            } catch (Exception e) {
                e.printStackTrace (out);
                out.flush ();
            }
        }

        public void assertion (boolean f) {
            if (!f)
                throw new RuntimeException ("assertion failure");
        }
    }

    public static class Quiet extends Verbose { 
        public Quiet () {
        }

        public Quiet (PrintStream out) {
            super (out);
        }

        public boolean isEnabled () {
            return false;
        }

        public void print (String message) {
        }
        public void println (String message) {
        }
        public void print (Object message) {
        }
        public void println (Object message) {
        }
        public void report (Throwable t) {
            t.printStackTrace (out);
            out.flush ();
        }
        public void printThreadInfo () {
        }
        public void printStackTrace () {
        }
        public void assertion (boolean f) {
            if (!f) {
                try {
                    throw new RuntimeException ("assertion failure");
                } catch (RuntimeException e) {
                    e.printStackTrace (out);
                    out.flush ();
                }
            }
        }
    }

    public static class NoDebug extends Debug { 
        public boolean isEnabled () {
            return false;
        }

        public void print (String message) {
        }
        public void println (String message) {
        }
        public void print (Object message) {
        }
        public void println (Object message) {
        }
        public void report (Throwable t) {
        }
        public void printThreadInfo () {
        }
        public void printStackTrace () {
        }
        public void assertion (boolean f) {
        }
    }

}
