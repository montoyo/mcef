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

package io.sigpipe.jbsdiff.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import io.sigpipe.jbsdiff.DefaultDiffSettings;
import io.sigpipe.jbsdiff.Diff;
import io.sigpipe.jbsdiff.DiffSettings;
import io.sigpipe.jbsdiff.InvalidHeaderException;
import io.sigpipe.jbsdiff.Patch;

/**
 * Provides an interface for working with bsdiff files on disk.
 *
 * @author malensek
 */
public class FileUI {
	
	private FileUI() {};

    public static void diff(File oldFile, File newFile, File patchFile)
    throws CompressorException, InvalidHeaderException, IOException {
        diff(oldFile, newFile, patchFile, CompressorStreamFactory.BZIP2);
    }

    public static void diff(File oldFile, File newFile, File patchFile,
                            String compression)
    throws CompressorException, InvalidHeaderException, IOException {
        FileInputStream oldIn = new FileInputStream(oldFile);
        byte[] oldBytes = new byte[(int) oldFile.length()];
        oldIn.read(oldBytes);
        oldIn.close();

        FileInputStream newIn = new FileInputStream(newFile);
        byte[] newBytes = new byte[(int) newFile.length()];
        newIn.read(newBytes);
        newIn.close();

        FileOutputStream out = new FileOutputStream(patchFile);
        DiffSettings settings = new DefaultDiffSettings(compression);
        Diff.diff(oldBytes, newBytes, out, settings);
        out.close();
    }

    public static void patch(File oldFile, File newFile, File patchFile)
    throws CompressorException, InvalidHeaderException, IOException {
        Patch.patch(oldFile, newFile, patchFile);
    }
}
