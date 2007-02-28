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

import rcm.util.PriorityQueue;
import rcm.util.Timer;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
//#ifdef JDK1.1 
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
//#endif JDK1.1

/**
 * Web crawler.
 * <P>
 * To write a crawler, extend this class and override 
 * shouldVisit () and visit() to create your own crawler.
 * <P>
 * To use a crawler:
 * <OL>
 * <LI>Initialize the crawler by calling
 * setRoot() (or one of its variants) and setting other 
 * crawl parameters.
 * <LI>Register any classifiers you need with addClassifier().
 * <LI>Connect event listeners to monitor the crawler,
 *     such as websphinx.EventLog, websphinx.workbench.WebGraph,
 *     or websphinx.workbench.Statistics.
 * <LI>Call run() to start the crawler.
 * </OL>
 * A running crawler consists of a priority queue of 
 * Links waiting to be visited and a set of threads 
 * retrieving pages in parallel.  When a page is downloaded,
 * it is processed as follows:
 * <OL>
 * <LI><B>classify()</B>: The page is passed to the classify() method of 
 * every registered classifier, in increasing order of
 * their priority values.  Classifiers typically attach
 * informative labels to the page and its links, such as "homepage"
 * or "root page".
 * <LI><B>visit()</B>: The page is passed to the crawler's
 * visit() method for user-defined processing.
 * <LI><B>expand()</B>: The page is passed to the crawler's
 * expand() method to be expanded.  The default implementation
 * tests every unvisited hyperlink on the page with shouldVisit(), 
 * and puts
 * each link approved by shouldVisit() into the crawling queue.
 * </OL>
 * By default, when expanding the links of a page, the crawler 
 * only considers hyperlinks (not applets or inline images, for instance) that
 * point to Web pages (not mailto: links, for instance).  If you want
 * shouldVisit() to test every link on the page, use setLinkType(Crawler.ALL_LINKS).
 * 
 */

public class Crawler implements Runnable
//#ifdef JDK1.1 
, Serializable 
//#endif JDK1.1
{

    //#ifdef JDK1.1 
    private static final long serialVersionUID = -3757789861952010450L;
    //#endif JDK1.1

    /**
     * Specify WEB as the crawl domain to allow the crawler
     * to visit any page on the World Wide Web.
     */
    public static final String[] WEB = null;

    /**
     * Specify SERVER as the crawl domain to limit the crawler
     * to visit only pages on the same Web server (hostname
     * and port number) as the root link from which it started.
     */
    public static final String[] SERVER = {"local"};

    /**
     * Specify SUBTREE as the crawl domain to limit the crawler
     * to visit only pages which are descendants of the root link 
     * from which it started.
     */
    public static final String[] SUBTREE = {"sibling", "descendent"};


    /**
     * Specify HYPERLINKS as the link type to allow the crawler
     * to visit only hyperlinks (A, AREA, and FRAME tags which
     * point to http:, ftp:, file:, or gopher: URLs).
     */
    public static final String[] HYPERLINKS = {"hyperlink"};

    /**
     * Specify HYPERLINKS_AND_IMAGES as the link type to allow the crawler
     * to visit only hyperlinks and inline images.
     */
    public static final String[] HYPERLINKS_AND_IMAGES = {"hyperlink", "image"};

    /**
     * Specify ALL_LINKS as the link type to allow the crawler
     * to visit any kind of link
     */
    public static final String[] ALL_LINKS = null;
    
    // Crawler parameters
    private String name = getClass().getName();   // crawler's name
    private transient Link[] roots = null;
    private String[] rootHrefs = null;   // exists only when serializing crawler
    private String[] domain = WEB;
    private boolean synchronous = false;
    private boolean depthFirst = true;
    private String[] type = HYPERLINKS;
    private boolean ignoreVisitedLinks = true;
    private int maxDepth = 5;
    private DownloadParameters dp = new DownloadParameters ()
                                  .changeUserAgent (name);
    private Vector classifiers = new Vector ();
    private LinkPredicate linkPredicate;
    private PagePredicate pagePredicate;
    private Action action;
    
    // Transient state

    private transient Link[] crawledRoots = null;

    private transient int state = CrawlEvent.CLEARED;
    
    private transient Worm[] worms;
        // background threads

    private transient PriorityQueue fetchQueue; 
          // links waiting to be downloaded
    private transient PriorityQueue crawlQueue;
          // all links that have been expanded but not
          // processed (used only if crawler is in synchronous mode)

    private transient int numLinksTested;
        // number of links tested by shouldVisit()
    private transient int numPagesVisited;
        // number of pages passed to visit()
    private transient int numPagesLeft;
          // all links that have been expanded but not processed
          // == crawlQueue.size ()

    // FIX: convert to immutable linked lists
    private transient Vector crawlListeners;
        // list of CrawlListeners
    private transient Vector linkListeners;
        // list of LinkListeners

    private transient Hashtable visitedPages;
        // visited pages (a set of URLs)

    private transient RobotExclusion robotExclusion;
        // robot exclusion cache

    /**
     * Make a new Crawler.
     */
    public Crawler () {
        addClassifier (new StandardClassifier());
        init ();
    }

    /*
     * Initialize the transient fields of the crawler.
     */
    private void init () {
        state = CrawlEvent.CLEARED;
        
        numLinksTested = 0;
        numPagesVisited = 0;
        numPagesLeft = 0;
        
        worms = null;
        crawlQueue = new PriorityQueue();
        fetchQueue = new PriorityQueue();

        crawlListeners = new Vector ();
        linkListeners = new Vector ();

        visitedPages = new Hashtable ();
        robotExclusion = new RobotExclusion (getName ());
    }

    /*
     * Write a Crawler to an output stream.
     */       
//#ifdef JDK1.1 
    private void writeObject (ObjectOutputStream out) 
            throws IOException {
        if (roots != null) {
            rootHrefs = new String[roots.length];
            for (int i=0; i<roots.length; ++i)
                rootHrefs[i] = roots[i].getURL().toString();
        }
        else
            rootHrefs = null;

        out.defaultWriteObject ();

        rootHrefs = null;
    }
//#endif JDK1.1

    /*
     * Read a Crawler from an input stream.
     */
//#ifdef JDK1.1 
    private void readObject (ObjectInputStream in) 
           throws IOException, ClassNotFoundException {
        in.defaultReadObject ();

        if (rootHrefs != null) {
            roots = new Link [rootHrefs.length];
            for (int i=0; i<rootHrefs.length; ++i)
                roots[i] = new Link (rootHrefs[i]);
        }
        else
            roots = null;

        domain = useStandard (WEB, domain);
        domain = useStandard (SERVER, domain);
        domain = useStandard (SUBTREE, domain);

        type = useStandard (HYPERLINKS, type);
        type = useStandard (HYPERLINKS_AND_IMAGES, type);
        type = useStandard (ALL_LINKS, type);
                 
        init ();

        if (linkPredicate != null)
            linkPredicate.connected (this);
        if (pagePredicate != null)
            pagePredicate.connected (this);
        if (action != null)
            action.connected (this);        
    }

    private static String[] useStandard (String[] standard, String[] s) {
        if (s == null || standard == null || standard == s)
            return s;
        if (s.length != standard.length)
            return s;
        for (int i=0; i<s.length; ++i)
            if (!s[i].equals (standard[i]))
                return s;
        return standard;
    }
//#endif JDK1.1

    /**
     * Start crawling.  Returns either when the crawl is done, or 
     * when pause() or stop() is called.  Because this method implements the
     * java.lang.Runnable interface, a crawler can be run in the
     * background thread.
     */
    public void run () {
        crawledRoots = roots;

        if (state == CrawlEvent.STOPPED)
            clear ();
            
        if (state == CrawlEvent.CLEARED && crawledRoots != null) {
            // give each root a default priority based on its position in the array
            float priority = 0;
            float increment = 1.0f/crawledRoots.length;
            for (int i=0; i<crawledRoots.length; ++i) {
                crawledRoots[i].setPriority (priority);
                priority += increment;
            }
            submit (crawledRoots);
        }
            
        state = CrawlEvent.STARTED;
        sendCrawlEvent (state);
        
        synchronized (crawlQueue) {            
            Timer timer = new CrawlTimer (this);
            int timeout = dp.getCrawlTimeout();
            if (timeout > 0)
                timer.set (timeout*1000, false);

            int nWorms = Math.max (dp.getMaxThreads (), 1);
            worms = new Worm[nWorms];
            for (int i=0; i<nWorms; ++i) {
                worms[i] = new Worm (this, i);
                worms[i].start ();
            }

            try {
                while (state == CrawlEvent.STARTED) {
                    if (numPagesLeft == 0) {
                        // ran out of links to crawl
                        state = CrawlEvent.STOPPED;
                        sendCrawlEvent (state);
                    }
                    else if (synchronous) {
                        // Synchronous mode.
                        // Main thread calls process() on each link
                        // in crawlQueue, in priority order.
                        Link link = (Link)crawlQueue.getMin ();
                        if (link.getStatus () == LinkEvent.DOWNLOADED)
                            process (link);
                        else
                            crawlQueue.wait ();
                    }
                    else
                        // Asynchronous crawling.
                        // Main thread does nothing but wait, while
                        // background threads call process().
                        crawlQueue.wait ();
                }
            } catch (InterruptedException e) {}

            timer.cancel ();
                
            for (int i=0; i<worms.length; ++i)
                worms[i].die ();
            if (state == CrawlEvent.PAUSED) {
                // put partly-processed links back in fetchQueue
                synchronized (fetchQueue) {
                    for (int i=0; i<worms.length; ++i)
                        if (worms[i].link != null)
                            fetchQueue.put (worms[i].link);
                }
            }
            worms = null;
        }
    }

    /**
     * Initialize the crawler for a fresh crawl.  Clears the crawling queue
     * and sets all crawling statistics to 0.  Stops the crawler
     * if it is currently running.
     */
    public void clear () {
        stop ();
        numPagesVisited = 0;
        numLinksTested = 0;
        clearVisited ();
        if (crawledRoots != null)
            for (int i=0; i < crawledRoots.length; ++i)
                crawledRoots[i].disconnect ();
        crawledRoots = null;
        state = CrawlEvent.CLEARED;
        sendCrawlEvent (state);
    }

    /**
     * Pause the crawl in progress.  If the crawler is running, then
     * it finishes processing the current page, then returns.  The queues remain as-is,
     * so calling run() again will resume the crawl exactly where it left off.
     * pause() can be called from any thread.
     */
    public void pause () {
        if (state == CrawlEvent.STARTED) {
            synchronized (crawlQueue) {
                state = CrawlEvent.PAUSED;
                crawlQueue.notify ();
            }
            sendCrawlEvent (state);
        }
    }

    /**
     * Stop the crawl in progress.  If the crawler is running, then
     * it finishes processing the current page, then returns.
     * Empties the crawling queue.
     */
    public void stop () {
        if (state == CrawlEvent.STARTED || state == CrawlEvent.PAUSED) {
            synchronized (crawlQueue) {
                synchronized (fetchQueue) {
                    state = CrawlEvent.STOPPED;
                    fetchQueue.clear ();
                    crawlQueue.clear ();
                    numPagesLeft = 0;
                    crawlQueue.notify ();
                }
            }
            sendCrawlEvent (state);
        }
    }

    /*
     * Timeout the crawl in progress.  Used internally by
     * the CrawlTimer.
     */
    void timedOut () {
        if (state == CrawlEvent.STARTED) {
            synchronized (crawlQueue) {
                synchronized (fetchQueue) {
                    state = CrawlEvent.TIMED_OUT;
                    fetchQueue.clear ();
                    crawlQueue.clear ();
                    numPagesLeft = 0;
                    crawlQueue.notify ();
                }
            }
            sendCrawlEvent (state);
        }
    }

    
    /**
     * Get state of crawler.
     * @return one of CrawlEvent.STARTED, CrawlEvent.PAUSED, STOPPED, CLEARED.
     */
    public int getState () {
        return state;
    }

    /**
     * Callback for visiting a page.  Default version does nothing.
     *
     * @param page Page retrieved by the crawler
     */
    public void visit (Page page) {
    }

    /**
     * Callback for testing whether a link should be traversed.
     * Default version returns true for all links. Override this method
     * for more interesting behavior.
     *
     * @param l Link encountered by the crawler
     * @return true if link should be followed, false if it should be ignored.
     */
    public boolean shouldVisit (Link l) {
        return true;
    }

    /** 
     * Expand the crawl from a page.  The default implementation of this
     * method tests every link on the page using shouldVisit (), and 
     * submit()s the links that are approved.  A subclass may want to override
     * this method if it's inconvenient to consider the links individually 
     * with shouldVisit().
     * @param page Page to expand
     */
    public void expand (Page page) { 
        // examine each link on the page
        Link[] links = page.getLinks();

        if (links != null && links.length > 0) {
            // give each link a default priority based on its page
            // and position on page
            float priority = (depthFirst ? -numPagesVisited : numPagesVisited);
            float increment = 1.0f/links.length;

            for (int i=0;  i<links.length; ++i) {
                Link l = links[i];

                // set default download parameters
                l.setPriority (priority);
                priority += increment;
                l.setDownloadParameters (dp);

                ++numLinksTested;
                if (ignoreVisitedLinks && visited (l))
                    // FIX: use atomic test-and-set
                    // FIX: set l.page somehow?
                    sendLinkEvent (l, LinkEvent.ALREADY_VISITED);
                else if (!((type == null || l.hasAnyLabels (type))
                           && (domain == null || l.hasAnyLabels (domain))
                           && (linkPredicate == null || linkPredicate.shouldVisit (l))
                           && shouldVisit (l)))
                    sendLinkEvent (l, LinkEvent.SKIPPED);
                else if (page.getDepth() >= maxDepth)
                    sendLinkEvent (l, LinkEvent.TOO_DEEP);
                else
                    submit (l);
            }
        }
    }

    /*
     * Crawl statistics
     */

    /**
     * Get number of pages visited.
     * @return number of pages passed to visit() so far in this crawl
     */
    public int getPagesVisited() {
        return numPagesVisited;
    }
    /**
     * Get number of links tested.
     * @return number of links passed to shouldVisit() so far in this crawl
     */
    public int getLinksTested() {
        return numLinksTested;
    }
    /**
     * Get number of pages left to be visited.
     * @return number of links approved by shouldVisit() but not yet visited
     */
    public int getPagesLeft() {
        return numPagesLeft;
    }
    /**
     * Get number of threads currently working.
     * @return number of threads downloading pages
     */
    public int getActiveThreads () {
        Worm[] w = worms;
        
        if (w == null)
            return 0;
            
        int n = 0;
        for (int i=0; i<w.length; ++i)
            if (w[i] != null && w[i].link != null)
                ++n;                
        return n;
    }

    /*
     * Crawler parameters
     */

    /**
     * Get human-readable name of crawler.  Default value is the
     * class name, e.g., "Crawler".  Useful for identifying the crawler in a
     * user interface; also used as the default User-agent for identifying
     * the crawler to a remote Web server.  (The User-agent can be
     * changed independently of the crawler name with setDownloadParameters().)
     * @return human-readable name of crawler
     */
    public String getName () {
        return name;
    }
    /**
     * Set human-readable name of crawler.
     * @param name new name for crawler
     */
    public void setName (String name) {
        this.name = name;
    }

    /**
     * Convert the crawler to a String.
     * @return Human-readable name of crawler.
     */
    public String toString () {
        return getName ();
    }

    /**
     * Get starting points of crawl as an array of Link objects.
     * @return array of Links from which crawler will start its next crawl.
     */
    public Link[] getRoots () {
        if (roots == null)
            return new Link[0];
            
        Link[] result = new Link[roots.length];
        System.arraycopy (roots, 0, result, 0, roots.length);
        return result;
    }
    /**
     * Get roots of last crawl.  May differ from getRoots() 
     * if new roots have been set.
     * @return array of Links from which crawler started its last crawl,
     * or null if the crawler was cleared.
     */
    public Link[] getCrawledRoots () {
        if (crawledRoots == null)
            return null;
            
        Link[] result = new Link[crawledRoots.length];
        System.arraycopy (crawledRoots, 0, result, 0, crawledRoots.length);
        return result;
    }
    /**
     * Get starting points of crawl as a String of newline-delimited URLs.
     * @return URLs where crawler will start, separated by newlines.
     */
    public String getRootHrefs () {
        StringBuffer buf = new StringBuffer ();
        if (roots != null) {
            for (int i=0; i<roots.length; ++i) {
                if (buf.length() > 0)
                    buf.append ('\n');
                buf.append (roots[i].getURL().toExternalForm());
            }
        }
        return buf.toString ();
    }
    /**
     * Set starting points of crawl as a string of whitespace-delimited URLs.
     * @param hrefs URLs of starting point, separated by space, \t, or \n
     * @exception java.net.MalformedURLException if any of the URLs is invalid,
     *    leaving starting points unchanged
     */
    public void setRootHrefs (String hrefs) throws MalformedURLException {
        Vector v = new Vector ();
        StringTokenizer tok = new StringTokenizer (hrefs);        
        while (tok.hasMoreElements ())
            v.addElement (new Link (tok.nextToken()));
        roots = new Link[v.size()];
        v.copyInto (roots);
    }
    /**
     * Set starting point of crawl as a single Link.
     * @param link starting point
     */
    public void setRoot (Link link) {
        roots = new Link[1];
        roots[0] = link;
    }
    /**
     * Set starting points of crawl as an array of Links.
     * @param links starting points
     */
    public void setRoots (Link[] links) {
        roots = new Link[links.length];
        System.arraycopy (links, 0, roots, 0, links.length);
    }

    /**
     * Add a root to the existing set of roots.
     * @param link starting point to add
     */
    public void addRoot (Link link) {
        if (roots == null)
            setRoot (link);
        else {
            Link newroots[] = new Link[roots.length+1];
            System.arraycopy (roots, 0, newroots, 0, roots.length);
            newroots[newroots.length-1] = link;
            roots = newroots;
        }
    }

    /**
     * Get crawl domain.  Default value is WEB.
     * @return WEB, SERVER, or SUBTREE.
     */
    public String[] getDomain () {
        return domain;
    }
    /**
     * Set crawl domain.
     * @param domain one of WEB, SERVER, or SUBTREE.
     */
    public void setDomain (String[] domain) {
        this.domain = domain;
    }

    /**
     * Get legal link types to crawl.  Default value is HYPERLINKS.
     * @return HYPERLINKS, HYPERLINKS_AND_IMAGES, or ALL_LINKS.
     */
    public String[] getLinkType () {
        return type;
    }
    /**
     * Set legal link types to crawl.
     * @param domain one of HYPERLINKS, HYPERLINKS_AND_IMAGES, or ALL_LINKS.
     */
    public void setLinkType (String[] type) {
        this.type = type;
    }

    /**
     * Get depth-first search flag.  Default value is true.
     * @return true if search is depth-first, false if search is breadth-first.
     */
    public boolean getDepthFirst() {
        return depthFirst;
    }
    /**
     * Set depth-first search flag.  If neither depth-first nor breadth-first
     * is desired, then override shouldVisit() to set a custom priority on
     * each link.
     * @param useDFS true if search should be depth-first, false if search should be breadth-first.
     */
    public void setDepthFirst(boolean useDFS) {
        depthFirst = useDFS;
    }
    /**
     * Get synchronous flag.  Default value is false.
     * @return true if crawler must visit the pages in priority order; false if crawler can visit 
     * pages in any order.
     */
    public boolean getSynchronous() {
        return synchronous;
    }
    /**
     * Set ssynchronous flag.
     * @param f true if crawler must visit the pages in priority order; false if crawler can visit 
     * pages in any order.
     */
    public void setSynchronous(boolean f) {
        synchronous = f;
    }
    /**
     * Get ignore-visited-links flag.  Default value is true.
     * @return true if search skips links whose URLs have already been visited
     * (or queued for visiting).
     */
    public boolean getIgnoreVisitedLinks() {
        return ignoreVisitedLinks;
    }
    /**
     * Set ignore-visited-links flag.
     * @param f true if search skips links whose URLs have already been visited
     * (or queued for visiting).
     */
    public void setIgnoreVisitedLinks(boolean f) {
        ignoreVisitedLinks = f;
    }
    /**
     * Get maximum depth.  Default value is 5.
     * @return maximum depth of crawl, in hops from starting point.
     */
    public int getMaxDepth() {
        return maxDepth;
    }
    /**
     * Set maximum depth.
     * @param maxDepth maximum depth of crawl, in hops from starting point
     */
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }
    /**
     * Get download parameters (such as number of threads, timeouts, maximum
     * page size, etc.)
     */
    public DownloadParameters getDownloadParameters() {
        return dp;
    }
    /**
     * Set download parameters  (such as number of threads, timeouts, maximum
     * page size, etc.)
     * @param dp Download parameters
     */
    public void setDownloadParameters(DownloadParameters dp) {
        this.dp = dp;
    }

    /**
     * Set link predicate.  This is an alternative way to
     * specify the links to walk.  If the link predicate is
     * non-null, then only links that satisfy
     * the link predicate AND shouldVisit() are crawled.
     * @param pred Link predicate
     */
    public void setLinkPredicate (LinkPredicate pred) {
        if (pred == linkPredicate
            || (pred != null && pred.equals (linkPredicate)))
            return;
        if (linkPredicate != null)
            linkPredicate.disconnected (this);
        linkPredicate = pred;
        if (linkPredicate != null)
            linkPredicate.connected (this);
    }

    /**
     * Get link predicate.
     * @return current link predicate
     */
    public LinkPredicate getLinkPredicate () {
        return linkPredicate;
    }

    /**
     * Set page predicate.  This is a way to filter the pages
     * passed to visit().  If the page predicate is
     * non-null, then only pages that satisfy it are passed to visit().
     * @param pred Page predicate
     */
    public void setPagePredicate (PagePredicate pred) {
        if (pred == pagePredicate
            || (pred != null && pred.equals (pagePredicate)))
            return;
        if (pagePredicate != null)
            pagePredicate.disconnected (this);
        pagePredicate = pred;
        if (pagePredicate != null)
            pagePredicate.connected (this);
    }

    /**
     * Get page predicate.
     * @return current page predicate
     */
    public PagePredicate getPagePredicate () {
        return pagePredicate;
    }

    /**
     * Set the action.  This is an alternative way to specify
     * an action performed on every page.  If act is non-null,
     * then every page passed to visit() is also passed to this
     * action.
     * @param act Action
     */
    public void setAction (Action act) {
        if (act == action
            || (act != null && act.equals (action)))
            return;
        if (action != null)
            action.disconnected (this);
        action = act;
        if (action != null)
            action.connected (this);
    }

    /**
     * Get action.
     * @return current action
     */
    public Action getAction () {
        return action;
    }


    /*
     * Link queue management
     *
     */

    /**
     * Puts a link into the crawling queue.  If the crawler is running, the
     * link will eventually be retrieved and passed to visit().
     * @param link Link to put in queue
     */
    public void submit (Link link) {
        markVisited (link); // FIX: need atomic test-and-set of visited flag
        sendLinkEvent (link, LinkEvent.QUEUED);
        synchronized (crawlQueue) {
            synchronized (fetchQueue) {
                crawlQueue.put (link);
                ++numPagesLeft;
                fetchQueue.put (link);
                fetchQueue.notifyAll ();  // wake up worms
            }
        }
    }
    /**
     * Submit an array of Links for crawling.  If the crawler is running,
     * these links will eventually be retrieved and passed to visit().
     * @param links Links to put in queue
     */
    public void submit (Link[] links) {
        for (int i=0; i<links.length; ++i)
            submit (links[i]);
    }

    /**
     * Enumerate crawling queue.
     * @return an enumeration of Link objects which are waiting to be visited.
     */
    // FIX: enumerate in priority order
    public Enumeration enumerateQueue () {
        return crawlQueue.elements ();
    }

    /*
     * Classifiers
     *
     */

    /**
     * Adds a classifier to this crawler.  If the
     * classifier is already found in the set, does nothing.
     * @param c a classifier
     */
    public void addClassifier (Classifier c) {
        if (!classifiers.contains (c)) {
            float cpriority = c.getPriority ();
            
            for (int i=0; i<classifiers.size(); ++i) {
                Classifier d = (Classifier)classifiers.elementAt (i);
                if (cpriority < d.getPriority ()) {
                    classifiers.insertElementAt (c, i);
                    return;
                }
            }
            classifiers.addElement (c);
        }
    }

    /**
     * Removes a classifier from the set of classifiers.  
     * If c is not found in the set, does nothing.
     *
     * @param c a classifier
     */
    public void removeClassifier (Classifier c) {
        classifiers.removeElement (c);
    }

    /**
     * Clears the set of classifiers.
     */
    public void removeAllClassifiers () {
        classifiers.removeAllElements ();
    }

    /**
     * Enumerates the set of classifiers.
     *
     * @return An enumeration of the classifiers.
     */
    public Enumeration enumerateClassifiers () {
        return classifiers.elements();
    }

    /**
     * Get the set of classifiers
     *
     * @return An array containing the registered classifiers.
     */
    public Classifier[] getClassifiers () {
        Classifier[] c = new Classifier[classifiers.size()];
        classifiers.copyInto (c);
        return c;
    }

    /*
     * Event listeners
     *
     */

    /**
     * Adds a listener to the set of CrawlListeners for this crawler.
     * If the listener is already found in the set, does nothing.
     *
     * @param listen a listener
     */
    public void addCrawlListener (CrawlListener listen) {
        if (!crawlListeners.contains (listen))
            crawlListeners.addElement (listen);
    }

    /**
     * Removes a listener from the set of CrawlListeners.  If it is not found in the set,
     * does nothing.
     *
     * @param listen a listener
     */
    public void removeCrawlListener (CrawlListener listen) {
        crawlListeners.removeElement (listen);
    }

    /**
     * Adds a listener to the set of LinkListeners for this crawler.
     * If the listener is already found in the set, does nothing.
     *
     * @param listen a listener
     */
    public void addLinkListener (LinkListener listen) {
        if (!linkListeners.contains (listen))
            linkListeners.addElement (listen);
    }

    /**
     * Removes a listener from the set of LinkListeners.  If it is not found in the set,
     * does nothing.
     *
     * @param listen a listener
     */
    public void removeLinkListener (LinkListener listen) {
        linkListeners.removeElement (listen);
    }

    /**
     * Send a CrawlEvent to all CrawlListeners registered with this crawler.
     * @param id Event id
     */
    protected void sendCrawlEvent (int id) {
        CrawlEvent evt = new CrawlEvent (this, id);
        for (int j=0, len=crawlListeners.size(); j<len; ++j) {
            CrawlListener listen = (CrawlListener)crawlListeners.elementAt(j);
            switch (id) {
              case CrawlEvent.STARTED: 
                listen.started (evt);
                break;
              case CrawlEvent.STOPPED: 
                listen.stopped (evt);
                break;
              case CrawlEvent.CLEARED: 
                listen.cleared (evt);
                break;
              case CrawlEvent.TIMED_OUT: 
                listen.timedOut (evt);
                break;
              case CrawlEvent.PAUSED: 
                listen.paused (evt);
                break;
            }
        }
    }

    /**
     * Send a LinkEvent to all LinkListeners registered with this crawler.
     * @param l Link related to event
     * @param id Event id
     */
    protected void sendLinkEvent (Link l, int id) {
        LinkEvent evt = new LinkEvent (this, id, l);
        l.setStatus (id);
        for (int j=0, len=linkListeners.size(); j<len; ++j) {
            LinkListener listen = (LinkListener)linkListeners.elementAt(j);
            listen.crawled (evt);
        }
    }

    /**
     * Send an exceptional LinkEvent to all LinkListeners registered with this crawler.
     * @param l Link related to event
     * @param id Event id
     * @param exception Exception associated with event
     */
    protected void sendLinkEvent (Link l, int id, Throwable exception) {
        LinkEvent evt = new LinkEvent (this, id, l, exception);
        l.setStatus (id);
        l.setLabel ("exception", exception.toString ());
        for (int j=0, len=linkListeners.size(); j<len; ++j) {
            LinkListener listen = (LinkListener)linkListeners.elementAt(j);
            listen.crawled (evt);
        }
    }

    /*
     * Visited pages table
     *
     */

    /**
     * Test whether the page corresponding to a link has been visited
     * (or queued for visiting).
     * @param link  Link to test
     * @return true if link has been passed to walk() during this crawl
     */
    public boolean visited (Link link) {
        return visitedPages.containsKey (link.getPageURL().toString());
    }

    /**
     * Register that a link has been visited.
     * @param link  Link that has been visited
     */
    protected void markVisited (Link link) {
        visitedPages.put (link.getPageURL().toString(), this);
    }

    /**
     * Clear the set of visited links.
     */
    protected void clearVisited () {
        visitedPages.clear ();
    }

    /*
     * Fetch loop
     *
     */

    void fetch (Worm w) {
        Timer timer = new WormTimer (w);

        while (!w.dead) {
            //System.err.println (w + ": fetching a link");

            // pull the highest-priority link from the fetch queue
            synchronized (fetchQueue) {
                while (!w.dead
                       && (w.link = (Link)fetchQueue.deleteMin ()) == null) {
                    try {
                        fetchQueue.wait ();
                    } catch (InterruptedException e) {}
                }
            }

            if (w.dead)
                return;
                
            //System.err.println (w + ": processing " + w.link.toDescription());
            
            try {
                // download the link to get a page
                DownloadParameters dp;
                Page page;

                dp = w.link.getDownloadParameters();
                if (dp == null)
                    dp = this.dp;
                int timeout = dp.getDownloadTimeout();

                sendLinkEvent (w.link, LinkEvent.RETRIEVING);
                try {
                    
                    if (timeout > 0)
                        timer.set (timeout*1000, false);

                    if (dp.getObeyRobotExclusion() 
                        && robotExclusion.disallowed (w.link.getURL()))
                        throw new IOException ("disallowed by Robot Exclusion Standard (robots.txt)");

                    page = new Page (w.link, dp);
                    
                } finally {
                    timer.cancel ();
                }
                    
                if (w.dead)
                    return;
                    
                sendLinkEvent (w.link, LinkEvent.DOWNLOADED);

                if (synchronous) {
                    // Synchronous mode.
                    // Main thread will call process() when
                    // this link's turn arrives (in priority order).
                    // Wake up the main thread.
                    synchronized (crawlQueue) {
                        crawlQueue.notify ();
                    }
                }
                else {
                    // Asynchronous mode.
                    // Each worm calls process() on its link. 
                    process (w.link);
                }
                
                w.link = null;

                // loop around and fetch another link

            } catch (ThreadDeath e) {
                throw e;  // have to continue dying 
            } catch (Throwable e) {
                // Some other exception occurred, either during the page fetch
                // or in some user code.  Mark up the link with the error.
                if (w.dead)
                    return;
                    
                sendLinkEvent (w.link, LinkEvent.ERROR, e);
                synchronized (crawlQueue) {
                    crawlQueue.delete (w.link);
                    --numPagesLeft;
                    w.link = null;
                    crawlQueue.notify ();
                }
            }
        }
    }

    void process (Link link) {
        Page page = link.getPage ();

        // classify the page
        for (int j=0, len=classifiers.size(); j<len; ++j) {
            Classifier cl = (Classifier)classifiers.elementAt(j);
            cl.classify (page);
        }

        // invoke callbacks on the page
        ++numPagesVisited;
        if (pagePredicate == null || pagePredicate.shouldActOn (page)) {
            if (action != null)
                action.visit (page);
            visit (page);
        }
        expand (page);
        
        // send out the event
        sendLinkEvent (link, LinkEvent.VISITED);
        
        // discard link
        synchronized (crawlQueue) {
            crawlQueue.delete (link);
            --numPagesLeft;
            crawlQueue.notify ();
        }
    }

    void fetchTimedOut (Worm w, int interval) {
        if (w.dead)
            return;

        w.die ();
        sendLinkEvent (w.link, LinkEvent.ERROR, 
                       new IOException ("Timeout after " + interval + " seconds"));

        synchronized (crawlQueue) {
            crawlQueue.delete (w.link);
            --numPagesLeft;
            
            worms[w.i] = new Worm (this, w.i);
            worms[w.i].start ();
            
            crawlQueue.notify ();
        }
    }

//#ifdef JDK1.1
  // FIX: more error checking here
  public static void main (String[] args) throws Exception {
    java.io.ObjectInputStream in = 
      new java.io.ObjectInputStream (new java.io.FileInputStream (args[0]));
    Crawler loadedCrawler = (Crawler)in.readObject ();
    in.close ();

    EventLog.monitor (loadedCrawler).setOnlyNetworkEvents (false);
    loadedCrawler.run ();
  }
//#endif JDK1.1

}

/* Simple Thread subclass that invokes a crawler's fetch loop. */
class Worm extends Thread {
    Crawler crawler; // crawler in charge of this worm
    int i;           // index of this worm in crawler.worms[]
    Link link;       // link this worm is currently working on
    boolean dead = false; // true if this worm has been killed

    public Worm (Crawler crawler, int i) {
        super (crawler.getName() + " worm " + i);
        setDaemon (true);
        this.crawler = crawler;
        this.i = i;
    }

    public void run () {
        crawler.fetch (this);
    }
    
    public void die () {
        dead = true;
        stop ();
    }
        
}

class WormTimer extends Timer {
    Worm worm;

    public WormTimer (Worm worm) {
        this.worm = worm;
    }

    protected void alarm () {
        worm.crawler.fetchTimedOut (worm, getInterval()/1000);
    }
}

class CrawlTimer extends Timer {
    Crawler crawler;
    
    public CrawlTimer (Crawler crawler) {
        this.crawler = crawler;
    }
    
    protected void alarm () { 
        crawler.timedOut ();
    }        
}

