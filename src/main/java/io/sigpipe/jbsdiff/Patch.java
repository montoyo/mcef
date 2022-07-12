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

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class provides functionality for using an old file and a patch to
 * generate a new file using the bsdiff patching algorithm.
 *
 * @author malensek
 */
public class Patch {

    private Patch() { }

    /**
     * Using an old file and its accompanying patch, this method generates a new
     * (updated) file and writes it to an {@link OutputStream}.
     *
     * @param old    the original ('old') state of the binary
     * @param patch  a binary patch file to apply to the old state
     * @param out    an {@link OutputStream} to write the patched binary to
     *
     * @throws CompressorException when a compression error occurs.
     * @throws InvalidHeaderException when the bsdiff header is malformed or not
     *     present.
     * @throws IOException when an I/O error occurs
     */
    public static void patch(byte[] old, byte[] patch, OutputStream out)
            throws CompressorException, InvalidHeaderException, IOException {
        /* Read bsdiff header */
        InputStream headerIn = new ByteArrayInputStream(patch);
        Header header = new Header(headerIn);
        headerIn.close();

        /* Set up InputStreams for reading different regions of the patch */
        InputStream controlIn, dataIn, extraIn;
        controlIn = new ByteArrayInputStream(patch);
        dataIn = new ByteArrayInputStream(patch);
        extraIn = new ByteArrayInputStream(patch);

        try {
            /* Seek to the correct offsets in each stream */
            controlIn.skip(Header.HEADER_SIZE);
            dataIn.skip(Header.HEADER_SIZE + header.getControlLength());
            extraIn.skip(Header.HEADER_SIZE + header.getControlLength() +
                    header.getDiffLength());

            /* Set up compressed streams */
            CompressorStreamFactory compressor = new CompressorStreamFactory();
            controlIn = compressor.createCompressorInputStream(controlIn);
            dataIn = compressor.createCompressorInputStream(dataIn);
            extraIn = compressor.createCompressorInputStream(extraIn);

            /* Start patching */
            int newPointer = 0, oldPointer = 0;
            byte[] output = new byte[header.getOutputLength()];
            while (newPointer < output.length) {

                ControlBlock control = new ControlBlock(controlIn);

                /* Read diff string */
                read(dataIn, output, newPointer, control.getDiffLength());

                /* Add old data to diff string */
                for (int i = 0; i < control.getDiffLength(); ++i) {
                    if ((oldPointer + i >= 0) && oldPointer + i < old.length) {
                        output[newPointer + i] += old[oldPointer + i];
                    }
                }

                newPointer += control.getDiffLength();
                oldPointer += control.getDiffLength();

                /* Copy the extra string to the output */
                read(extraIn, output, newPointer, control.getExtraLength());

                newPointer += control.getExtraLength();
                oldPointer += control.getSeekLength();
            }

            out.write(output);

        } finally {
            controlIn.close();
            dataIn.close();
            extraIn.close();
        }
    }

    public static void patch(File oldFile, File newFile, File patchFile)
            throws CompressorException, InvalidHeaderException, IOException {
        /* Read bsdiff header */
        InputStream headerIn = new FileInputStream(patchFile);
        Header header = new Header(headerIn);
        headerIn.close();

        /* Set up InputStreams for reading different regions of the patch */
        InputStream controlIn, dataIn, extraIn;
        controlIn = new BufferedInputStream(new FileInputStream(patchFile));
        dataIn = new BufferedInputStream(new FileInputStream(patchFile));
        extraIn = new BufferedInputStream(new FileInputStream(patchFile));

        try {
            /* Seek to the correct offsets in each stream */
            controlIn.skip(Header.HEADER_SIZE);
            dataIn.skip(Header.HEADER_SIZE + header.getControlLength());
            extraIn.skip(Header.HEADER_SIZE + header.getControlLength() +
                    header.getDiffLength());

            /* Set up compressed streams */
            CompressorStreamFactory compressor = new CompressorStreamFactory();
            controlIn = compressor.createCompressorInputStream(controlIn);
            dataIn = compressor.createCompressorInputStream(dataIn);
            extraIn = compressor.createCompressorInputStream(extraIn);

            FileInputStream oldStream = new FileInputStream(oldFile);
            byte[] old = new byte[(int) oldFile.length()];
            oldStream.read(old);
            oldStream.close();

            OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile));

            /* Start patching */
            int newPointer = 0, oldPointer = 0;
            int outputLength = header.getOutputLength();
            while (newPointer < outputLength) {

                ControlBlock control = new ControlBlock(controlIn);

                /* Read diff string */
                int diffLength = control.getDiffLength();
                int extraLength = control.getExtraLength();
                byte[] output = new byte[diffLength + extraLength];
                read(dataIn, output, 0, diffLength);

                /* Add old data to diff string */
                for (int i = 0; i < diffLength; ++i) {
                    if ((oldPointer + i >= 0) && oldPointer + i < old.length) {
                        output[i] += old[oldPointer + i];
                    }
                }

                newPointer += diffLength;
                oldPointer += diffLength;

                /* Copy the extra string to the output */
                read(extraIn, output, diffLength, extraLength);
                out.write(output);

                newPointer += extraLength;
                oldPointer += control.getSeekLength();
            }

            out.close();

        } finally {
            controlIn.close();
            dataIn.close();
            extraIn.close();
        }
    }

    /**
     * Reads data from an InputStream, and throws an {@link IOException} if
     * fewer bytes were read than requested.  Since the lengths of data in a
     * bsdiff patch are explicitly encoded in the control blocks, reading less
     * than expected is an unrecoverable error.
     *
     * @param in   InputStream to read from
     * @param dest byte array to read data into
     * @param off  offset in dest to write data at
     * @param len  length of the read
     *
     * @throws IOException when fewer bytes were read than requested
     */
    private static void read(InputStream in, byte[] dest, int off, int len)
            throws IOException {
        if (len == 0) {
            /* We don't need to do anything */
            return;
        }

        int read = in.read(dest, off, len);
        if (read < len) {
            throw new IOException("Corrupt patch; bytes expected = " + len +
                    " bytes read = " + read);
        }
    }
}
