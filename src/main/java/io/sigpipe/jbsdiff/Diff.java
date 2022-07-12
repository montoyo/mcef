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

package io.sigpipe.jbsdiff;

import io.sigpipe.jbsdiff.sort.SearchResult;
import io.sigpipe.jbsdiff.sort.SuffixSort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

/**
 * This class provides functionality for generating bsdiff patches from two
 * source files (an old and new file).  Using the differences between the old
 * and new files, a bsdiff patch can be applied to an old file to generate a
 * copy of the new file.
 *
 * @author malensek
 */
public class Diff {

    private Diff() { }

    /**
     * Using two different versions of a file, generate a bsdiff patch that can
     * be applied to the old file to create the new file.  Uses the default
     * bzip2 compression algorithm.
     *
     * @param oldBytes    The original ('old') state of the file/binary.
     * @param newBytes    New state of the file/binary that will be compared
     *                        to create a patch file
     * @param out         An {@link OutputStream} to write the patch file to
     *
     * @throws CompressorException when a compression error occurs.
     * @throws InvalidHeaderException when the bsdiff header is malformed or not
     *     present.
     * @throws IOException when an error occurs writing the bsdiff control
     *     blocks.
     */
    public static void diff(byte[] oldBytes, byte[] newBytes, OutputStream out)
            throws CompressorException, InvalidHeaderException, IOException {
        diff(oldBytes, newBytes, out, new DefaultDiffSettings());
    }

    /**
     * Using two different versions of a file, generate a bsdiff patch that can
     * be applied to the old file to create the new file.
     *
     * @param oldBytes    The original ('old') state of the file/binary.
     * @param newBytes    New state of the file/binary that will be compared
     *                        to create a patch file
     * @param out         An {@link OutputStream} to write the patch file to
     * @param settings    A {@link DiffSettings} implementation, which defines
     *                        the compression and suffix sort algorithms to
     *                        create the patch with.
     *
     * @throws CompressorException when a compression error occurs.
     * @throws InvalidHeaderException when the bsdiff header is malformed or not
     *     present.
     * @throws IOException when an error occurs writing the bsdiff control
     *     blocks.
     */
    public static void diff(byte[] oldBytes, byte[] newBytes, OutputStream out,
                            DiffSettings settings)
            throws CompressorException, InvalidHeaderException, IOException {
        CompressorStreamFactory compressor = new CompressorStreamFactory();
        String compression = settings.getCompression();

        int[] I = settings.sort(oldBytes);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        OutputStream patchOut =
                compressor.createCompressorOutputStream(compression, byteOut);

        SearchResult result = null;
        int scan = 0, len = 0, position = 0;
        int lastScan = 0, lastPos = 0, lastOffset = 0;
        int oldScore = 0, scsc = 0;
        int s, Sf, lenf, Sb, lenb;
        int overlap, Ss, lens;

        byte[] db = new byte[newBytes.length + 1];
        byte[] eb = new byte[newBytes.length + 1];
        int dblen = 0, eblen = 0;

        while (scan < newBytes.length) {
            oldScore = 0;

            for (scsc = scan += len; scan < newBytes.length; scan++) {
                result = SuffixSort.search(I,
                        oldBytes, 0,
                        newBytes, scan,
                        0, oldBytes.length);
                len = result.getLength();
                position = result.getPosition();

                for (; scsc < scan + len; scsc++) {
                    if ((scsc + lastOffset < oldBytes.length) &&
                            (oldBytes[scsc + lastOffset] == newBytes[scsc]))
                        oldScore++;
                }

                if (((len == oldScore) && (len != 0)) || (len > oldScore + 8)) {
                    break;
                }

                if ((scan + lastOffset < oldBytes.length) &&
                        (oldBytes[scan + lastOffset] == newBytes[scan]))
                    oldScore--;
            }

            if ((len != oldScore) || (scan == newBytes.length)) {
                s = 0;
                Sf = 0;
                lenf = 0;
                for (int i = 0; (lastScan + i < scan) &&
                        (lastPos + i < oldBytes.length); ) {
                    if (oldBytes[lastPos + i] == newBytes[lastScan + i]) {
                        s++;
                    }

                    i++;
                    if (s * 2 - i > Sf * 2 - lenf) {
                        Sf = s;
                        lenf = i;
                    }
                }

                lenb = 0;
                if (scan < newBytes.length) {
                    s = 0;
                    Sb = 0;
                    for (int i = 1; (scan >= lastScan + i) &&
                            (position >= i); i++) {
                        if (oldBytes[position - i] ==
                                newBytes[scan - i]) {
                            s++;
                        }
                        if (s * 2 - i > Sb * 2 - lenb) {
                            Sb = s;
                            lenb = i;
                        }
                    }
                }

                if (lastScan + lenf > scan - lenb) {
                    overlap = (lastScan + lenf) - (scan - lenb);
                    s = 0;
                    Ss = 0;
                    lens = 0;
                    for (int i = 0; i < overlap; i++) {
                        if (newBytes[lastScan + lenf - overlap + i] ==
                                oldBytes[lastPos + lenf - overlap + i]) {
                            s++;
                        }
                        if (newBytes[scan - lenb + i] ==
                                oldBytes[position - lenb + i]) {
                            s--;
                        }
                        if (s > Ss) {
                            Ss = s;
                            lens = i + 1;
                        }
                    }
                    lenf += lens - overlap;
                    lenb -= lens;
                }

                for (int i = 0; i < lenf; i++) {
                    db[dblen + i] |= (newBytes[lastScan + i] -
                            oldBytes[lastPos + i]);
                }

                for (int i = 0; i < (scan - lenb) - (lastScan + lenf); i++) {
                    eb[eblen + i] = newBytes[lastScan + lenf + i];
                }

                dblen += lenf;
                eblen += (scan - lenb) - (lastScan + lenf);

                ControlBlock control = new ControlBlock();
                control.setDiffLength(lenf);
                control.setExtraLength((scan - lenb) - (lastScan + lenf));
                control.setSeekLength((position - lenb) -
                        (lastPos + lenf));
                control.write(patchOut);

                lastScan = scan - lenb;
                lastPos = position - lenb;
                lastOffset = position - scan;
            }
        }

        /* Done writing control blocks */
        patchOut.close();

        Header header = new Header();
        header.setControlLength(byteOut.size());

        patchOut =
                compressor.createCompressorOutputStream(compression, byteOut);
        patchOut.write(db);
        patchOut.close();
        header.setDiffLength(byteOut.size() - header.getControlLength());

        patchOut =
                compressor.createCompressorOutputStream(compression, byteOut);
        patchOut.write(eb);
        patchOut.close();

        header.setOutputLength(newBytes.length);

        header.write(out);
        out.write(byteOut.toByteArray());
    }
}
