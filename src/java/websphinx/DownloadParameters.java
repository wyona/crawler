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
 * Download parameters.  These parameters are limits on
 * how Page can download a Link.  A Crawler has a
 * default set of download parameters, but the defaults
 * can be overridden on individual links by calling
 * Link.setDownloadParameters().
 * <P>
 * DownloadParameters is an immutable class (like String).
 * "Changing" a parameter actually returns a new instance
 * of the class with only the specified parameter changed.
 */ 
public class DownloadParameters implements Cloneable
//#ifdef JDK1.1 
, java.io.Serializable 
//#endif JDK1.1
{
    private int maxThreads = 4;
        // number of background threads used by the crawler
    private int maxPageSize = 100;
        // maximum page size in kilobytes (-1 for no maximum)
    private int downloadTimeout = 60; 
        // timeout for a single page, in seconds (-1 for no timeout)
    private int crawlTimeout = -1;
        // timeout for entire crawl in seconds (-1 for no timeout)
    private boolean obeyRobotExclusion = false;
        // obey crawling rules in robots.txt

    // not implemented yet
//     private int maxRequestsPerServer = 2; 
//         // maximum number of simultaneous requests to a server (-1 for no maximum)
//     private int delay = 500;
//         // delay (in milliseconds) between starts of requests to same server (0 for no delay)

    private boolean interactive = true;
        // user is available to answer dialog boxes, e.g. for authentication
    private boolean useCaches = true;
        // use cached pages to satisfy requests wherever possible
    private String acceptedMIMETypes = null;
        // accept header for HTTP request, or null to use default
    private String userAgent = null;
        // User-Agent header for HTTP request, or null to use default


    public static final DownloadParameters DEFAULT = new DownloadParameters ();
    public static final DownloadParameters NO_LIMITS = 
        DEFAULT
        .changeMaxPageSize (-1)
        .changeDownloadTimeout (-1)
        .changeCrawlTimeout (-1)
        ;

    /**
     * Make a DownloadParameters object with default settigns.
     */
    public DownloadParameters () {
    }
    
    /**
     * Clone a DownloadParameters object.
     */
    public Object clone () {
        try {
            return super.clone ();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException ("Internal error: " + e);
        }
    }

    /**
     * Get maximum threads.
     * @return maximum number of background threads used by crawler.
     *   Default is 4.
     */
    public int getMaxThreads() {
        return maxThreads;
    }
    /**
     * Set maximum threads.
     * @param maxthreads maximum number of background threads used by crawler
     * @return new DownloadParameters object with the specified parameter changed.
     */
    public DownloadParameters changeMaxThreads(int maxthreads) {
        DownloadParameters dp = (DownloadParameters)clone();
        dp.maxThreads = maxthreads;
        return dp;
    }
    /**
     * Get maximum page size.  Pages larger than this limit are neither
     * downloaded nor parsed.
     * Default value is 100 (KB).  0 or negative values mean no limit.
     * @return maximum page size in kilobytes
     */
    public int getMaxPageSize() {
        return maxPageSize;
    }
    /**
     * Change maximum page size.  Pages larger than this limit are treated as
     * leaves in the crawl graph  -- neither downloaded nor parsed.
     * @param maxPageSize maximum page size in kilobytes
     * @return new DownloadParameters object with the specified parameter changed.
     */
    public DownloadParameters changeMaxPageSize(int maxPageSize) {
        DownloadParameters dp = (DownloadParameters)clone();
        dp.maxPageSize = maxPageSize;
        return dp;
    }
    /**
     * Get download timeout value.
     * @return length of time (in seconds) that crawler will wait for a page to download
     * before aborting it.
     * timeout. Default is 60 seconds.
     */
    public int getDownloadTimeout() {
        return downloadTimeout;
    }
    /**
     * Change download timeout value.
     * @param timeout length of time (in seconds) to wait for a page to download
     *     Use a negative value to turn off timeout.
     * @return new DownloadParameters object with the specified parameter changed.
     */
    public DownloadParameters changeDownloadTimeout(int timeout) {
        DownloadParameters dp = (DownloadParameters)clone();
        dp.downloadTimeout = timeout;
        return dp;
    }
    /**
     * Get timeout on entire crawl.
     * @return maximum length of time (in seconds) that crawler will run
     * before aborting.  Default is -1 (no limit).
     */
    public int getCrawlTimeout() {
        return crawlTimeout;
    }
    /**
     * Change timeout value.
     * @param timeout maximum length of time (in seconds) that crawler will run.
     *     Use a negative value to turn off timeout.
     * @return new DownloadParameters object with the specified parameter changed.
     */
    public DownloadParameters changeCrawlTimeout(int timeout) {
        DownloadParameters dp = (DownloadParameters)clone();
        dp.crawlTimeout = timeout;
        return dp;
    }
    /**
     * Get obey-robot-exclusion flag.  
     * @return true iff the
     * crawler checks robots.txt on the remote Web site
     * before downloading a page.  Default is false.
     */
    public boolean getObeyRobotExclusion() {
        return obeyRobotExclusion;
    }
    /**
     * Change obey-robot-exclusion flag.
     * @param f   If true, then the
     * crawler checks robots.txt on the remote Web site
     * before downloading a page.
     * @return new DownloadParameters object with the specified parameter changed.
     */
    public DownloadParameters changeObeyRobotExclusion(boolean f) {
        DownloadParameters dp = (DownloadParameters)clone();
        dp.obeyRobotExclusion = f;
        return dp;
    }
    /**
     * Get interactive flag.
     * @return true if a user is available to respond to
     * dialog boxes (for instance, to enter passwords for
     * authentication).  Default is true.
     */
    public boolean getInteractive() {
        return interactive;
    }
    /**
     * Change interactive flag.
     * @param f true if a user is available to respond
     * to dialog boxes
     * @return new DownloadParameters object with the specified parameter changed.
     */
    public DownloadParameters changeInteractive(boolean f) {
        DownloadParameters dp = (DownloadParameters)clone();
        dp.interactive = f;
        return dp;
    }
    /**
     * Get use-caches flag.
     * @return true if cached pages should be used whenever
     * possible
     */
    public boolean getUseCaches() {
        return useCaches;
    }
    /**
     * Change use-caches flag.
     * @param f true if cached pages should be used whenever possible
     * @return new DownloadParameters object with the specified parameter changed.
     */
    public DownloadParameters changeUseCaches(boolean f) {
        DownloadParameters dp = (DownloadParameters)clone();
        dp.useCaches = f;
        return dp;
    }
    /**
     * Get accepted MIME types.
     * @return list of MIME types that can be handled by 
     * the crawler (which are passed as the Accept header
     * in the HTTP request).
     * Default is null.
     */
    public String getAcceptedMIMETypes() {
        return acceptedMIMETypes;
    }
    /**
     * Change accepted MIME types.
     * @param types list of MIME types that can be handled
     * by the crawler.  Use null if the crawler can handle anything.
     * @return new DownloadParameters object with the specified parameter changed.
     */
    public DownloadParameters changeAcceptedMIMETypes(String types) {
        DownloadParameters dp = (DownloadParameters)clone();
        dp.acceptedMIMETypes = types;
        return dp;
    }
    /**
     * Get User-agent header used in HTTP requests.
     * @return user-agent field used in HTTP requests,
     * or null if the Java library's default user-agent
     * is used.  Default value is null (but for a Crawler,
     * the default DownloadParameters has the Crawler's
     * name as its default user-agent).
     */
    public String getUserAgent() {
        return userAgent;
    }
    /**
     * Change User-agent field used in HTTP requests.
     * @param userAgent user-agent field used in HTTP
     * requests.  Pass null to use the Java library's default
     * user-agent field.
     * @return new DownloadParameters object with the specified parameter changed.
     */
    public DownloadParameters changeUserAgent(String userAgent) {
        DownloadParameters dp = (DownloadParameters)clone();
        dp.userAgent = userAgent;
        return dp;
    }
}
