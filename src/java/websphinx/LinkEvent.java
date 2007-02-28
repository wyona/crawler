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
 * Link event.  A LinkEvent is issued when the crawler
 * starts or stops retrieving a link, and when it makes
 * a decision about a link.
 */
public class LinkEvent 
{
    Crawler crawler;
    int id;
    Link link;
    Throwable exception;

    /**
     * No event occured on this link yet. Never delivered in a LinkEvent,
     * but may be returned by link.getStatus().
     */
    public static final int NONE = 0;
    
    /**
     * Link was rejected by shouldVisit()
     */
    public static final int SKIPPED = 1;

    /**
     * Link has already been visited during the crawl, so it was skipped.
     */
    public static final int ALREADY_VISITED = 2;

    /**
     * Link was accepted by walk() but exceeds the maximum depth from the start set.
     */
    public static final int TOO_DEEP = 3;

    /**
     * Link was accepted by walk() and is waiting to be downloaded
     */
    public static final int QUEUED = 4;

    /**
     * Link is being retrieved
     */
    public static final int RETRIEVING = 5;

    /**
     * An error occurred in retrieving the page.
     * The error can be obtained from getException().
     */
    public static final int ERROR = 6;

    /**
     * Link has been retrieved
     */
    public static final int DOWNLOADED = 7;

    /**
     * Link has been thoroughly processed by crawler
     */
    public static final int VISITED = 8;

    /**
     * Map from id code (RETRIEVING) to name ("retrieving")
     */
    public static final String[] eventName = {
        "none",
        "skipped",
        "already visited",
        "too deep",
        "queued",
        "retrieving",
        "error",
        "downloaded",
        "visited"
    };

    /**
     * Make a LinkEvent.
     * @param crawler Crawler that generated this event
     * @param id event code, like LinkEvent.RETRIEVING
     * @param link Link on which this event occurred
     */
    public LinkEvent (Crawler crawler, int id, Link link) {
        this.crawler = crawler;
        this.id = id;
        this.link = link;
    }

    /**
     * Make a LinkEvent for an error.
     * @param crawler Crawler that generated this event
     * @param id Event code, usually ERROR
     * @param link Link on which this event occurred
     * @param exception Throwable 
     */
    public LinkEvent (Crawler crawler, int id, Link link, Throwable exception) {
        this.crawler = crawler;
        this.id = id;
        this.link = link;
        this.exception = exception;
    }

    /**
     * Get crawler that generated the event
     * @return crawler
     */
    public Crawler getCrawler () { 
        return crawler;
    }

    /**
     * Get event id
     * @return id
     */
    public int getID () { return id; }

    /**
     * Get event name (string equivalent to its ID)
     * @return id
     */
    public String getName () { return eventName[id]; }

    /**
     * Get link to which this event occurred.
     * @return link
     */
    public Link getLink () { return link; }

    /**
     * Get exception related to this event.  Valid when ID == ERROR.
     * @return exception object
     */
    public Throwable getException () { return exception; }

    /**
     * Convert this event to a String describing it.
     */
    public String toString () {
        String result;
        if (id == ERROR)
            result = exception.toString();
        else
            result = eventName[id];
        result += " " + link.toDescription ();
        return result;
    }
}
