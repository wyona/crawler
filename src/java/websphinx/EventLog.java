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

import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Date;
//#ifdef JDK1.1
import java.io.PrintWriter;
//#endif JDK1.1
/*#ifdef JDK1.0
import java.io.PrintStream;
#endif JDK1.0*/

/**
 * Crawling monitor that writes messages to standard output or a file.
 * Acts as both a CrawlListener (monitoring start and end of the crawl)
 * and as a LinkListener (monitoring page retrieval).
 */
public class EventLog implements CrawlListener, LinkListener {

//#ifdef JDK1.1
    PrintWriter stream;
//#endif JDK1.1
/*#ifdef JDK1.0
    PrintStream stream;
#endif JDK1.0*/
    boolean onlyNetworkEvents = true;

    /**
     * Make a EventLog that writes to standard output.
     */
    public EventLog () {
        this (System.out);
    }

    /**
     * Make a EventLog that writes to a stream.
     */
    public EventLog (OutputStream out) {
/*#ifdef JDK1.0
        stream = new PrintStream (out, true);
#endif JDK1.0*/
//#ifdef JDK1.1
        stream = new PrintWriter (out, true);
//#endif JDK1.1
    }

    /**
     * Make a EventLog that writes to a file.  The file is overwritten.
     * @param filename File to which crawling event messages are written
     */
    public EventLog (String filename) throws IOException {
/*#ifdef JDK1.0
        stream = new PrintStream (Access.getAccess ().writeFile (new File(filename), false));
#endif JDK1.0*/
//#ifdef JDK1.1
        stream = new PrintWriter (Access.getAccess ().writeFile (new File(filename), false));
//#endif JDK1.1
    }

    /**
     * Set whether logger prints only network-related LinkEvents.
     * If true, then the logger only prints LinkEvents where
     * LinkEvent.isNetworkEvent() returns true.  If false,
     * then the logger prints all LinkEvents.  Default is true.
     * @param flag true iff only network LinkEvents should be logged
     */
    public void setOnlyNetworkEvents (boolean flag) {
        onlyNetworkEvents = flag;
    }
    /**
     * Test whether logger prints only network-related LinkEvents.
     * If true, then the logger only prints LinkEvents where
     * LinkEvent.isNetworkEvent() returns true.  If false,
     * then the logger prints all LinkEvents.  Default is true.
     * @return true iff only network LinkEvents are logged
     */
    public boolean getOnlyNetworkEvents () {
        return onlyNetworkEvents;
    }

    /**
     * Notify that the crawler started.
     */
    public void started (CrawlEvent event) {
        stream.println (new Date() + ": *** started " + event.getCrawler());
    }

    /**
     * Notify that the crawler has stopped.
     */
    public void stopped (CrawlEvent event) {
        stream.println (new Date() + ": *** finished " + event.getCrawler());
    }

    /**
     * Notify that the crawler's state was cleared.
     */
    public void cleared (CrawlEvent event) {
        stream.println (new Date() + ": *** cleared " + event.getCrawler());
    }

    /**
     * Notify that the crawler timed out.
     */
    public void timedOut (CrawlEvent event) {
        stream.println (new Date() + ": *** timed out " + event.getCrawler());
    }

    /**
     * Notify that the crawler paused.
     */
    public void paused (CrawlEvent event) {
        stream.println (new Date() + ": *** paused " + event.getCrawler());
    }

    /**
     * Notify that a link event occured.
     */
    public void crawled (LinkEvent event) {
        switch (event.getID()) {
          case LinkEvent.RETRIEVING:
          case LinkEvent.DOWNLOADED:
          case LinkEvent.VISITED:
          case LinkEvent.ERROR:
            break;
          default:
            if (onlyNetworkEvents)
                return;
            break;
        }
        stream.println (new Date () + ": "
                        + event);

        Throwable exc = event.getException();
        if (exc != null && ! (exc instanceof IOException))
            exc.printStackTrace (stream);
    }

    /**
     * Create a EventLog that prints to standard error and attach it to a crawler.
     * This is a convenience method.
     * @param crawler Crawler to be monitored
     */
    public static EventLog monitor (Crawler crawler) {
        EventLog logger = new EventLog (System.err);
        crawler.addCrawlListener (logger);
        crawler.addLinkListener (logger);
        return logger;
    }
}
