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

/**
 * Binary search routines.
 */
public abstract class BinarySearch {
    public static Debug debug = Debug.QUIET;

    /**
     * Search a sorted array of integers.
     * @param array Array of integers
     * @param offset Starting offset of subarray to search
     * @param length Length of subarray to search
     * @param x Value to search for
     * @return largest index i in subarray (offset &lt;= i &lt;= offset+length)
     * such that all elements below i in the subarray are strictly less
     * than x.  If x is found in the subarray, then array[i] == x (and i is
     * the first occurence of x in the subarray).  If x is not found, 
     * then array[i] is where x should be inserted in the sort order.
     */
    public static int search (int[] array, int offset, int length, int x) {
        // handle 0-length subarray case right away
        if (length <= 0)
            return offset;

        int low = offset;
        int high = offset+length-1;
        // since length > 0, array[low] and array[high] are valid indices

        if (x <= array[low])
            return low;
        if (x > array[high])
            return high+1;
        
        while (low+1 < high) {
            // loop invariant: array[low] < x <= array[high],
            //                 offset <= low < high < offset+length
            int mid = (low + high)/2;
            if (x <= array[mid])
                high = mid;
            else
                low = mid;
        }
        // now we have array[low] < x <= array[high]
        //             && (low+1 == high || low == high)
        //  implies low+1 == high
        debug.assertion (low+1 == high);
        return high;
    }
}
