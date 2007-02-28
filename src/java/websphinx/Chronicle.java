/*
 * WebSphinx web-crawling toolkit
 *
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

package websphinx;

import rcm.util.Timer;

/**
 * Run a crawler periodically.
 */
public class Chronicle extends Timer implements Runnable {
    Crawler crawler;
    int interval;
    boolean running = false;
    boolean triggered = false;

    /**
     * Make a Chronicle.
     * @param crawler Crawler to run periodically
     * @param interval Invocation interval, in seconds. Crawler is invoked
     * every interval seconds.  If the crawler is still running
     * when interval seconds have elapsed, it is aborted.
     *
     */
    public Chronicle (Crawler crawler, int interval) {
        this.crawler = crawler;
        this.interval = interval;
    }

    /**
     * Start chronicling.  Starts a background thread which
     * starts the crawler immediately, then re-runs the crawler
     * every interval seconds from now until stop() is called.
     */
    public void start () {
        if (running)
            return;

        running = true;
        set (interval * 1000, true);
        Thread thread = new Thread (this, crawler.getName ());
        thread.start ();
    }

    /**
     * Stop chronicling.  Also stops the crawler, if it's currently running.
     */
    public synchronized void stop () {
        if (!running)
            return;

        running = false;
        crawler.stop ();
        notify ();
        cancel ();
    }

    /**
     * Background thread that runs the crawler.  Clients shouldn't
     * call this.
     */
    public synchronized void run () {
        try {
            while (running) {
                crawler.run ();
                while (!triggered)
                    wait ();
                triggered = false;
            }
        } catch (InterruptedException e) {}
    }

    protected synchronized void alarm () {
        crawler.stop ();
        triggered = true;
        notify ();
    }

//#ifdef JDK1.1
    // FIX: allow crawler class name (starting up Workbench to configure it)
  public static void main (String[] args) throws Exception {
    java.io.ObjectInputStream in =
      new java.io.ObjectInputStream (new java.io.FileInputStream (args[0]));
    Crawler loadedCrawler = (Crawler)in.readObject ();
    in.close ();

    EventLog.monitor (loadedCrawler);

    Chronicle track = new Chronicle (loadedCrawler, Integer.parseInt (args[1]));
    track.start ();
  }
//#endif JDK1.1
}
