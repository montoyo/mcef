/*
Copyright (c) 2013, Colorado State University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

This software is provided by the copyright holders and contributors "as is" and
any express or implied warranties, including, but not limited to, the implied
warranties of merchantability and fitness for a particular purpose are
disclaimed. In no event shall the copyright holder or contributors be liable for
any direct, indirect, incidental, special, exemplary, or consequential damages
(including, but not limited to, procurement of substitute goods or services;
loss of use, data, or profits; or business interruption) however caused and on
any theory of liability, whether in contract, strict liability, or tort
(including negligence or otherwise) arising in any way out of the use of this
software, even if advised of the possibility of such damage.
*/

package io.sigpipe.jbsdiff.sort;

/**
 * Implements the suffix sorting and binary search algorithms found in bsdiff.
 *
 * @author malensek
 */
public class SuffixSort {

    private SuffixSort() { }

    public static void qsufsort(int[] I, int[] V, byte[] data) {
        int[] buckets = new int[256];
        int i, h, len;

        for (i=0; i < data.length; i++) {
            buckets[data[i] & 0xFF]++;
        }

        for(i = 1; i < 256; i++) {
            buckets[i] += buckets[i - 1];
        }

        for (i = 255; i > 0; i--) {
            buckets[i] = buckets[i - 1];
        }

        buckets[0] = 0;

        for (i = 0; i < data.length; i++) {
            I[++buckets[data[i] & 0xFF]] = i;
        }

        I[0] = data.length;

        for (i = 0; i < data.length; i++) {
            V[i] = buckets[data[i] & 0xFF];
        }

        V[data.length] = 0;

        for (i = 1; i < 256; i++) {
            if (buckets[i] == buckets[i - 1] + 1) {
                I[buckets[i]] = -1;
            }
        }

        I[0] = -1;

        for (h = 1; I[0] != -(data.length + 1); h += h) {
            len = 0;
            for (i = 0; i < data.length + 1; ) {
                if (I[i] < 0) {
                    len -= I[i];
                    i -= I[i];
                } else {
                    if (len != 0) {
                        I[i - len] =- len;
                    }

                    len = V[I[i]] + 1 - i;
                    split(I, V, i, len, h);
                    i += len;
                    len = 0;
                }
            }
            if(len != 0) {
                I[i - len] =- len;
            }
        }

        for(i = 0; i < data.length + 1; i++) {
            I[V[i]] = i;
        }
    }

    public static void split(int[] I, int[] V, int start, int len, int h) {
        int i, j, k, x, tmp, jj, kk;

        if (len < 16) {
            for (k = start; k < start + len; k += j) {
                j = 1;
                x = V[I[k] + h];

                for (i = 1; k + i < start + len; i++) {
                    if (V[I[k + i] + h] < x) {
                        x=V[I[k + i] + h];
                        j=0;
                    }
                    if (V[I[k + i] + h] == x) {
                        tmp = I[k + j];
                        I[k + j] = I[k + i];
                        I[k + i] = tmp;
                        j++;
                    }
                }
                for (i = 0; i < j; i++) {
                    V[I[k + i]] = k + j -1;
                }
                if (j == 1) {
                    I[k]= -1;
                }
            }
            return;
        }

        x = V[I[start + len / 2] + h];
        jj = 0;
        kk = 0;
        for (i = start; i < start + len; i++) {
            if (V[I[i] + h] < x) {
                jj++;
            }

            if(V[I[i] + h] == x) {
                kk++;
            }
        }
        jj += start;
        kk += jj;

        i = start;
        j = 0;
        k = 0;
        while (i < jj) {
            if (V[I[i] + h] < x) {
                i++;
            } else if (V[I[i] + h] == x) {
                tmp = I[i];
                I[i] = I[jj + j];
                I[jj + j] = tmp;
                j++;
            } else {
                tmp = I[i];
                I[i] = I[kk + k];
                I[kk + k] = tmp;
                k++;
            }
        }

        while (jj + j < kk) {
            if (V[I[jj + j] + h] == x) {
                j++;
            } else {
                tmp=I[jj + j];
                I[jj + j] = I[kk + k];
                I[kk + k] = tmp;
                k++;
            }
        }

        if (jj > start) {
            split(I, V, start, jj - start, h);
        }

        for (i = 0; i < kk - jj; i++) {
            V[I[jj + i]] = kk - 1;
        }

        if (jj == kk - 1) {
            I[jj]= -1;
        }

        if (start + len > kk) {
            split(I, V, kk, start + len - kk, h);
        }
    }

    /**
     * Starting from the beginning of two arrays, determines the amount of bytes
     * that are equal up until the first inequality.
     *
     * @param bytesA the first array to compare
     * @param offsetA index in the first array to start comparing at
     * @param bytesB the second array to compare
     * @param offsetB index in the second array to start comparing at
     *
     * @return the number of matching array entries
     */
    private static int matchLength(byte[] bytesA, int offsetA,
            byte[] bytesB, int offsetB) {

        int oldLimit = bytesA.length - offsetA;
        int newLimit = bytesB.length - offsetB;

        int i;
        for (i = 0; i < oldLimit && i < newLimit; ++i) {
            if (bytesA[i + offsetA] != bytesB[i + offsetB]) {
                break;
            }
        }

        return i;
    }

    public static SearchResult search(int[] I,
            byte[] oldBytes, int oldOffset,
            byte[] newBytes, int newOffset,
            int start, int end) {

        /* Are we done dividing the search space? */
        if (end - start < 2) {
            int x, y;
            x = matchLength(oldBytes, I[start], newBytes, newOffset);
            y = matchLength(oldBytes, I[end], newBytes, newOffset);

            if(x > y) {
                return new SearchResult(x, I[start]);
            } else {
                return new SearchResult(y, I[end]);
            }
        }

        /* Otherwise, find center pivot and compare */
        int center = start + (end - start) / 2;
        if (compareBytes(oldBytes, I[center], newBytes, newOffset) < 0) {
            return search(I, oldBytes, 0, newBytes, newOffset, center, end);
        } else {
            return search(I, oldBytes, 0, newBytes, newOffset, start, center);
        }
    }

    /**
     * Compare two byte arrays, similar to the C memcmp() function.  A value
     * greater than 0 indicates the first byte that does not match is greater in
     * the first array, whereas a value less than zero indicates the byte is
     * greater in the second array.
     *
     * Note that the output of this implementation probably does not exactly
     * match the output of memcmp() on all platforms, but satisfies the memcmp()
     * contract.
     *
     * @param bytesA first array
     * @param offsetA index in the first array to start comparing at
     * @param bytesB second array
     * @param offsetB index in the second array to start comparing at
     *
     * @return int indicating the relationship between the two arrays.
     */
    private static int compareBytes(byte[] bytesA, int offsetA,
            byte[] bytesB, int offsetB) {

        /* Only compare up until the end of the smallest array */
        int length = Math.min(bytesA.length - offsetA, bytesB.length - offsetB);

        int valA = 0, valB = 0;
        for (int i = 0; i < length; ++i) {
            valA = bytesA[i + offsetA] & 0xFF;
            valB = bytesB[i + offsetB] & 0xFF;

            if (valA != valB) {
                break;
            }
        }

        return valA - valB;
    }
}
