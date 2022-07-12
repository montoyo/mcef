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

/**
 * Provides a simple command line interface for the io.sigpipe.jbsdiff tools.
 *
 * @author malensek
 */
public class CLI {
    private static final String COMMAND_DIFF = "diff";
    private static final String COMMAND_PATCH = "patch";

    private CLI() { }

    /** Diff or patch with specified files.
     * Format is command oldFile newFile patchFile .
     * Command is either diff or patch */
    public static void main(String[] args) throws Exception {
        if ( args.length == 3 ) {
            args = withCommandGuess( args );
        }
        if (args.length < 4) {
            System.out.println("Not enough parameters!");
            printUsage();
        }

        String compression = System.getProperty("jbsdiff.compressor", "bzip2");
        compression = compression.toLowerCase();

        try {
            String command = args[0].toLowerCase();
            File oldFile = new File(args[1]);
            File newFile = new File(args[2]);
            File patchFile = new File(args[3]);

            if (COMMAND_DIFF.equals(command)) {
                FileUI.diff(oldFile, newFile, patchFile, compression);
            } else if (COMMAND_PATCH.equals(command)) {
                FileUI.patch(oldFile, newFile, patchFile);
            } else {
                printUsage();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** Shows the expected arguments for jbsdiff, with compression schemes. */
    public static void printUsage() {
        String usage = String.format("" +
                "Usage: command <oldfile> <newfile> <patchfile>%n%n" +

                "Commands:%n" +
                "    diff%n" +
                "    patch%n%n" +

                "Use the jbsdiff.compressor property to select a different " +
                "compression scheme:%n" +
                "    java -Djbsdiff.compressor=gz -jar jbsdiff-*.jar diff " +
                "a.bin b.bin patch.gz%n%n" +

                "Supported compression schemes: bzip2 (default), gz, pack200, xz.%n%n" +
                "The compression algorithm used will be detected automatically during %n" +
                "patch operations.  NOTE: algorithms other than bzip2 are incompatible %n" +
                "with the reference implementation of bsdiff!");

        System.out.println(usage);
        System.exit(1);
    }

    /** Attempts to fill in a forgotten command based on which file doesn't exist. */
    private static String[] withCommandGuess(String[] args) {
        if ( args.length != 3 ) {
            // caller provided too few to operate or enough arguments to avoid guessing
            return args;
        }
        if (COMMAND_DIFF.equals(args[0]) || COMMAND_PATCH.equals(args[0])) {
            // caller actually forgot a file, rather than the command
            return args;
        }
        File oldFile = new File(args[0]);
        File newFile = new File(args[1]);
        File patchFile = new File(args[2]);
        String chosenAction, command;
        String actionTemplate = "Guessing! %s %s & %s AS %s";
        if (oldFile.exists() && newFile.exists()
                && ! patchFile.exists()) {
            command = COMMAND_DIFF;
            chosenAction = String.format(actionTemplate,
                    command, oldFile, newFile, patchFile);
            System.out.println(chosenAction);
            args = insertCommand(command, args);
        } else if (oldFile.exists() && patchFile.exists()
                && ! newFile.exists()) {
            command = COMMAND_PATCH;
            chosenAction = String.format(actionTemplate,
                    command, oldFile, patchFile, newFile);
            System.out.println(chosenAction);
            args = insertCommand(command, args);
        }
        return args;
    }

    /** Prefixes an array with the desired command */
    private static String[] insertCommand(String command, String[] args) {
        String[] withCommand = new String[args.length +1];
        withCommand[0] = command;
        for (int i = 0; i < args.length; i++) {
            withCommand[i +1] = args[i];
        }
        return withCommand;
    }

}