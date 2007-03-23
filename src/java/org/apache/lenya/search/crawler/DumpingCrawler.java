/*
 * Copyright 2006 Wyona
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.wyona.org/licenses/APACHE-LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.lenya.search.crawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;

import websphinx.Crawler;
import websphinx.DownloadParameters;
import websphinx.EventLog;
import websphinx.Link;
import websphinx.LinkTransformer;
import websphinx.Mirror;
import websphinx.Page;

/**
 * Crawler which creates a dump of a website.
 * In addition to the actual dump, it creates a .meta file which contains the mimetype 
 * and the encoding of the downloaded files.
 */
public class DumpingCrawler extends Crawler {

    private String[] crawlScopeURLs;

    private String dumpDir;
    private Mirror mirror;
    private int nofPages = 0;
    private int maxPages = 100;
    private PrintWriter meta;
    
    /**
     * Specify types of links which should be followed.
     * @see websphinx.StandardClassifier
     */
    private static final String[] LINK_TYPES = {"hyperlink", "image", "code", "header-link"};

    /**
     * @param crawlStartURL
     * @param crawlScopeURL
     * @param dumpDir
     * @throws FileNotFoundException
     */
    public DumpingCrawler(String crawlStartURL, String crawlScopeURL, String dumpDir) throws FileNotFoundException {
        this(crawlStartURL, makeArray(crawlScopeURL), dumpDir);
    }
    
    private static String[] makeArray(String str) {
        String[] array = new String[1];
        array[0] = str;
        return array;
    }
    
    /**
     * Creates a new SimpleCrawler.
     * @param crawlStartURL
     *          The URL where the crawl should start.
     * @param crawlScopeURLs
     *          Limits the scope of the crawl, only links which match any of the scope urls  
     *          will be followed. The first one must be a prefix of crawlStartURL.
     *          In most cases, the first crawlScopeURL is equal to or the parent of the crawlStartURL.
     *          If the crawlScopeURL does not end with a slash, a slash will be added.
     *          The crawlScopeURL also affects the path of the dumped files.
     *          Example:
     *          crawlStartURL: http://foo.com/do/re/mi.html
     *          crawlScopeURL: http://foo.com/do/
     *          -> the first file would be saved at:
     *                         /[dumpDir]/re/mi.html
     * @param dumpDir  
     *          The directory in the filesystem where the dumped files will be stored.
     *          Does not have to exist yet, it will be created by the crawler.
     * @throws FileNotFoundException 
     */
    public DumpingCrawler(String crawlStartURL, String[] crawlScopeURLs, String dumpDir) throws FileNotFoundException {
        try {
            this.setRoot(new Link(crawlStartURL));
        } catch (MalformedURLException e) {
            this.setRoot(null);
        }
        if (!crawlStartURL.startsWith(crawlScopeURLs[0])) {
            throw new IllegalArgumentException("crawlScopeURL [" + crawlScopeURLs[0] + 
                    "] must be a prefix of crawlStartURL [" + crawlStartURL + "]");
        }
        this.crawlScopeURLs = crawlScopeURLs;
        for (int i=0; i<this.crawlScopeURLs.length; i++) {
            if (!this.crawlScopeURLs[i].endsWith("/")) {
                this.crawlScopeURLs[i] = this.crawlScopeURLs[i] + "/";
            }
        }
        this.dumpDir = dumpDir;
        this.setSynchronous(true);
        if (this.crawlScopeURLs.length > 1) {
            this.setDomain(Crawler.WEB);
        } else {
            this.setDomain(Crawler.SERVER);
        }
        this.setLinkType(LINK_TYPES);
        try {
            this.mirror = new Mirror(this.dumpDir, this.crawlScopeURLs[0]);
        } catch (IOException e) {
            throw new RuntimeException("Could not create mirror with directory: " + this.dumpDir 
                    + ": " + e, e);
        }
        new File(dumpDir).mkdirs();
        this.meta = new PrintWriter(new FileOutputStream(this.dumpDir + File.separator + ".meta"));
    }

    /**
     * @see websphinx.Crawler#visit(websphinx.Page)
     */
    public void visit(Page page) {
        try {
            mirror.writePage(page);
            
            // write mimetype and encoding into meta file:
            File file = page.getLocalFile();
            if (file != null) {
                String path = file.getCanonicalPath();
                String rootPath = new File(this.dumpDir).getCanonicalPath();
                String relPath = path.substring(rootPath.length()+1);
                String output = relPath + "," + page.getMimeType();
                if (page.getContentEncoding() != null) {
                    output += "," + page.getContentEncoding();
                }
                if (page.getMimeType() != null) {
                    this.meta.println(output);
                }
            }
            page.discardContent();

        } catch (IOException e) {
            throw new RuntimeException("Could not save page: url=" + page.getURL() + ": " + e, e);
        }
    }
    
/*    public void visit(Page page) {
        String pageURL = page.getURL().toString();
        if (pageURL.startsWith(this.crawlScopeURL)) {
            File file = new File(this.dumpDir, pageURL.substring(this.crawlScopeURL.length()));
            //System.out.println("writing file: " + file.getAbsolutePath());
            try {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                LinkTransformer linkTransformer = new LinkTransformer(new FileOutputStream(file), 
                        page.getContentEncoding());
                linkTransformer.setBase(page.getBase());
                linkTransformer.writePage(page);
                linkTransformer.flush();
            } catch (IOException e) {
                throw new RuntimeException("Could not save page: url=" + page.getURL() + ", file=" 
                        + file + ": " + e, e);
            }
        }
    }
*/
    /**
     * @see websphinx.Crawler#shouldVisit(websphinx.Link)
     */
    public boolean shouldVisit(Link link) {
        for (int i=0; i<this.crawlScopeURLs.length; i++) {
            if (link.getURL().toString().startsWith(this.crawlScopeURLs[i]) && this.nofPages < this.maxPages) {
                this.nofPages ++;
                return super.shouldVisit(link);
            }
        }
        return false;
    }
    
    public void close() {
        try {
            mirror.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not close mirror: " + e, e);
        }
        this.meta.flush();
        this.meta.close();
    }
    
    public static void main(String[] args) throws Exception {
        String crawlStartURL = args[0];
        String[] crawlScopeURL = args[1].split(",");
        String dumpDir = args[2];
        int maxDepth = Integer.parseInt(args[3]);
        int maxPages = Integer.parseInt(args[4]);
        
        DumpingCrawler crawler = new DumpingCrawler(crawlStartURL, crawlScopeURL, dumpDir);
        
        crawler.setMaxDepth(maxDepth);
        crawler.setMaxPages(maxPages);
        
        EventLog eventLog = new EventLog(System.out);
        crawler.addCrawlListener(eventLog);
        crawler.addLinkListener(eventLog);
        
        crawler.run();
        crawler.close();
    }

    /**
     * Gets the maximum number of pages which will be downloaded.
     * This number includes also non-html files like images etc.  
     * Default is 100.
     * @return
     */
    public int getMaxPages() {
        return maxPages;
    }

    /**
     * Sets the maximum number of pages which will be downloaded. 
     * This number includes also non-html files like images etc.  
     * Default is 100.
     * @param maxPages
     */
    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }
}
