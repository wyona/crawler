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

/**
 * Crawling event.  CrawlEvents are broadcast when the
 * crawler starts, stops, or clears its state.
 */
public class CrawlEvent
{
    private Crawler crawler;
    private int id;

    /**
     * Crawler started.
     */
    public static final int STARTED = 0;
    
    /**
     * Crawler ran out of links to crawl
     */
    public static final int STOPPED = 1;

    /**
     * Crawler's state was cleared.
     */
    public static final int CLEARED = 2;

    /**
     * Crawler timeout expired.
     */
    public static final int TIMED_OUT = 3;

    /**
     * Crawler was paused.
     */
    public static final int PAUSED = 4;

    /**
     * Make a CrawlEvent.
     * @param crawler Crawler that generated this event
     * @param id event id (one of STARTED, STOPPED, etc.)
     */
    public CrawlEvent (Crawler crawler, int id) {
        this.crawler = crawler;
        this.id = id;
    }

    /**
     * Get crawler that generated the event
     * @return crawler
     */
    public Crawler getCrawler () { 
        return crawler;
    }

    /**
     * Get event id.
     * @return one of STARTED, STOPPED, CLEARED, TIMED_OUT,
     * or PAUSED.
     */
    public int getID () { return id; }

}
