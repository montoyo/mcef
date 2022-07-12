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
 * Represents a binary search result for a string of bytes, containing the
 * longest match found and the position in the sorted suffix array.
 *
 * @author malensek
 */
public class SearchResult {

    /** Number of matched bytes */
    private int length;

    /** Position of the result in the suffix array */
    private int position;

    public SearchResult(int length, int position) {
        this.length = length;
        this.position = position;
    }

    @Override
    public String toString() {
        return "length = " + length + ", position = " + position;
    }

    public int getLength() {
        return length;
    }

    public int getPosition() {
        return position;
    }
}
