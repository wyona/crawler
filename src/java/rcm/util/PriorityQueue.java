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

import java.util.Enumeration;
import java.util.Vector;

/**
 * Priority queue.  Objects stored in a priority queue must implement 
 * the Prioritized interface.
 * 
 */
public class PriorityQueue {

    private Vector q; // the queue of elements
    
    /**
     * Make an empty PriorityQueue.
     */
    public PriorityQueue () {
        q = new Vector ();
    }

    /**
     * Make an empty PriorityQueue with an initial capacity.
     * @param initialCapacity number of elements initially allocated in queue
     */
    public PriorityQueue (int initialCapacity) {
        q = new Vector (initialCapacity);
    }

    /**
     * Put an object on the queue.  Doesn't check for
     * duplicate puts.
     * @param x object to put on the queue 
     */
    public synchronized void put (Prioritized x) {
        int newSize = q.size()+1;
        q.setSize (newSize);
        float priorityX = x.getPriority ();

        int i, p;
        for (i=newSize-1, p = ((i+1)/2)-1; // i's parent
             i > 0 && getPriority (p) > priorityX; 
             i = p, p = ((i+1)/2)-1)
            q.setElementAt (q.elementAt (p), i);

        q.setElementAt (x, i);
    }

    /**
     * Get object with lowest priority from queue.
     * @return object with lowest priority, or null if queue is empty
     */
    public synchronized Object getMin () {
        return !empty() ? q.elementAt (0) : null;
    }

    /**
     * Get and delete the object with lowest priority.
     * @return object with lowest priority, or null if queue is empty
     */
    public synchronized Object deleteMin () {
        if (empty())
            return null;
        Object obj = q.elementAt (0);
        deleteElement (0);
        return obj;
    }

    /**
     * Delete an object from queue.  If object was inserted more than
     * once, this method deletes only one occurrence of it.
     * @param x object to delete
     * @return true if x was found and deleted, false if x not found in queue
     */
    public synchronized boolean delete (Prioritized x) {
        int i = q.indexOf (x);
        if (i == -1)
            return false;
        deleteElement (i);
        return true;
    }

    /**
     * Remove all objects from queue.
     */
    public synchronized void clear () {
        q.removeAllElements ();
    }

    
    /**
     * Enumerate the objects in the queue, in no particular order
     * @return enumeration of objects in queue
     */
    public synchronized Enumeration elements () {
        return q.elements ();
    }

    
    /**
     * Get number of objects in queue.
     * @return number of objects
     */
    public synchronized int size () {
        return q.size ();
    }

    
    /**
     * Test whether queue is empty.
     * @return true iff queue is empty.
     */
    public synchronized boolean empty () {
        return q.isEmpty ();
    }

    /**
     * Rebuild priority queuein case the priorities of its elements 
     * have changed since they were inserted.  If the priority of
     * any element changes, this method must be called to update
     * the priority queue.
     */
    public synchronized void update () {
        for (int i = (q.size()/2) - 1; i >= 0; --i)
            heapify (i);
    }

    final void deleteElement (int i) {
        int last = q.size()-1;
        q.setElementAt (q.elementAt (last), i);
        q.setElementAt (null, last);    // avoid holding extra reference
        q.setSize (last);
        heapify (i);
    }

    /* Establishes the heap property at i's descendents.
    */
    final void heapify (int i) {
        int max = q.size();
        while (i < max) {
            int r = 2*(i+1); // right child of i
            int l = r - 1;   // left child of i

            int smallest = i;
            float prioritySmallest = getPriority (i);
            float priorityR;

            if (r < max && (priorityR = getPriority (r)) < prioritySmallest) {
                smallest = r;
                prioritySmallest = priorityR;
            }
            if (l < max && getPriority (l) < prioritySmallest) {
                smallest = l;
            }

            if (smallest != i) {
                swap (i, smallest);
                i = smallest;
            }
            else
                break;
        }
    }

    /* Swap elements at positions i and j in the table.
    */
    final void swap (int i, int j) {
        Object tmp = q.elementAt (i);
        q.setElementAt (q.elementAt (j), i);
        q.setElementAt (tmp, j);
    }

    /* Return the priority of the element at position i.  For convenience,
       positions beyond the end of the table have infinite priority.
    */
    final float getPriority (int i) {
        return ((Prioritized)q.elementAt (i)).getPriority();
    }

    public static void main (String[] args) {
        PriorityQueue q = new PriorityQueue ();

        for (int i=0; i<args.length; ++i) {
            float f = Float.valueOf (args[i]).floatValue();
            q.put (new PQItem (f));
            System.out.println ("put (" + f + ")");
        }

        System.out.println ("getMin() = " + q.getMin());
        System.out.println ("empty() = " + q.empty());

        dump (q);

        if (q.size() > 0) {
            Enumeration enum = q.elements ();
            for (int j=0; j<q.size()/2; ++j)
                enum.nextElement();

            PQItem deletable = (PQItem)enum.nextElement();
            q.delete (deletable);
            System.out.println ("delete (" + deletable + ")");

            dump (q);
        }

        float last = Float.NEGATIVE_INFINITY;
        PQItem item;
        while ((item = (PQItem)q.deleteMin()) != null) {
            System.out.println ("deleteMin() = " + item);
            if (item.getPriority() < last)
                System.out.println ("ERROR! greater than last == " + last);
            last = item.getPriority ();
            dump (q);
        }
    }

    public static void dump (PriorityQueue q) {
        Enumeration enum = q.elements ();
        for (int j=0; enum.hasMoreElements(); ++j) {
            System.out.println ("elements()[" + (j+1) + "] = " + enum.nextElement());
        }
    }
}

// used for testing only (see main() above)
class PQItem implements Prioritized {
    float priority;

    public PQItem (float priority) {
        this.priority = priority;
    }

    public float getPriority () {
        return priority;
    }

    public String toString () {
        return String.valueOf (priority);
    }
}
