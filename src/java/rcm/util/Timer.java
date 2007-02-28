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

import java.util.Vector;

public class Timer {

    int interval;
    boolean periodic;
    boolean isExpired = false;

    static TimerManager manager = new TimerManager ();
    long deadline;
    Timer next, prev;

    public Timer () {
    }

    public void set (int msecDelay, boolean periodic) {
        interval = msecDelay;
        this.periodic = periodic;
        isExpired = false;
        if (!manager.isAlive ()) {
            System.err.println ("TimerManager: restarting");
            manager = new TimerManager ();
        }
        manager.register (this, System.currentTimeMillis () + msecDelay);
    }

    public int getInterval () {
        return interval;
    }

    public boolean getPeriodic () {
        return periodic;
    }

    public void cancel () {
        manager.delete (this);
    }

    protected void alarm () {
    }

    public boolean expired () {
        return isExpired;
    }
        
    /*
    public static void main (String[] args) {
        for (int i=0; i<args.length; ++i) {
            boolean periodic = (args[i].charAt (0) == 'p');
            if (periodic) args[i] = args[i].substring (1);
            new TestTimer (args[i], Integer.parseInt (args[i]), periodic);
        }
        while (true) Thread.yield ();
    }
    */
}

class TimerManager extends Thread {
    Timer first, last;

    /*
    static ThreadGroup rootThreadGroup;
    static {
        rootThreadGroup = Thread.currentThread().getThreadGroup();
        while (rootThreadGroup.getParent() != null)
            rootThreadGroup = rootThreadGroup.getParent();
    }
    */

    public TimerManager () {
        super (/* rootThreadGroup, */ "Timer Manager");
        setDaemon (true);
        start ();
    }

    public synchronized void register (Timer t, long deadline) {
        t.deadline = deadline;
        delete (t);  // just in case it's already registered

        //System.err.println ("TimerManager: set " + t + " to go off at " + deadline);
      insertion: 
        {
            for (Timer u = first; u != null; u = u.next) {
                if (t.deadline < u.deadline) {
                    if (u.prev != null)
                        u.prev.next = t;
                    else
                        first = t;
                    t.prev = u.prev;
                    t.next = u;
                    u.prev = t;
                    break insertion;
                }
            }
            if (last != null) {
                last.next = t;
                t.prev = last;
                t.next = null;
                last = t;
            } else {
                first = last = t;
            }
        }

        //System.err.println ("TimerManager: waking up background thread");
        notifyAll ();
    }

    public synchronized void delete (Timer t) {
        if (t.next != null)
            t.next.prev = t.prev;
        if (t.prev != null)
            t.prev.next = t.next;
        if (t == last)
            last = t.prev;
        if (t == first)
            first = t.next;
        t.next = null;
        t.prev = null;
    }

    static final int FOREVER = 60000;  // wake up at least every 60 seconds

    public synchronized void run () {
        while (true) {
            try {
                //System.err.println ("TimerManager: awake");
                if (first == null) {
                    //System.err.println ("TimerManager: waiting forever");
                    wait (FOREVER);
                    //System.err.println ("TimerManager: woke up");
                }
                else {
                    Timer t = first;
                    long now = System.currentTimeMillis ();
                    if (t.deadline <= now) {
                        // System.err.println ("TimerManager: timer " + t + " just went off at " + now);
                        try {
                            t.isExpired = true;
                            t.alarm ();
                        } catch (Throwable e) {
                            if (e instanceof ThreadDeath)
                                throw (ThreadDeath)e;
                            else
                                e.printStackTrace ();
                        }
                        if (t.periodic) {
                            register (t, now + t.interval);
                        }
                        else {
                            delete (t);
                        }
                    }
                    else {
                        //System.err.println ("TimerManager: waiting for " + (t.deadline - now) + " msec");
                        wait (t.deadline - now);
                        //System.err.println ("TimerManager: woke up");
                    }
                }
            } catch (InterruptedException e) {}
        }
    }
}

/*
class TestTimer extends Timer {
    String message;

    public TestTimer (String message, int millisec, boolean periodic) {
        this.message = message;
        set (millisec, periodic);
    }

    public void alarm () {
        System.out.println (message);
    }
}
*/
