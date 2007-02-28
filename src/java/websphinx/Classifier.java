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
 * Classifier interface.  A classifier is a helper object that annotates
 * pages and links with labels (using Page.setLabel() and Link.setLabel()).
 * When a page is retrieved by a crawler, it is passed to the classify()
 * method of every Classifier registered with the crawler.  Here are some
 * typical uses for classifiers:
 * <UL>
 * <LI> classifying links into categories like child or parent (see
 *  websphinx.StandardClassifier);
 * <LI> classifying pages into categories like biology or computers;
 * <LI> recognizing and parsing pages formatted in a particular style, such as
 *      AltaVista, Yahoo, or latex2html (e.g., the search engine classifiers
 *      in websphinx.searchengine)
 * <LI>
 * </UL>
 */
public interface Classifier 
//#ifdef JDK1.1 
extends java.io.Serializable 
//#endif JDK1.1
{
    /** 
     * Classify a page.  Typically, the classifier calls page.setLabel() and
     * page.setField() to mark up the page.  The classifier may also look
     * through the page's links and call link.setLabel() to mark them up.
     * @param page Page to classify
     */
    public abstract void classify (Page page);
    
    /** 
     * Get priority of this classifier.  Lower priorities execute first.
     * A classifier should also define a public constant <CODE>priority</CODE>
     * so that classifiers that depend on it can compute their 
     * priorities statically.  For example, if your classifier
     * depends on FooClassifier and BarClassifier, you might set your
     * priority as:
     * <PRE>
     * public static final float priority = Math.max (FooClassifier, BarClassifier) + 1;
     * public float getPriority () { return priority; }
     * </PRE>
     * 
     * @return priority of this classifier
     */
    public float getPriority ();    
}
