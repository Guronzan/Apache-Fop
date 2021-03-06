/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: SerializeHyphPattern.java 808166 2009-08-26 20:00:24Z spepping $ */

package org.apache.fop.hyphenation;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Serialize hyphenation patterns For all xml files in the source directory a
 * pattern file is built in the target directory This class may be called from
 * the ant build file in a java task
 */
@Slf4j
public class SerializeHyphPattern {

    private boolean errorDump = false;

    /**
     * Controls the amount of error information dumped.
     *
     * @param errorDump
     *            True if more error info should be provided
     */
    public void setErrorDump(final boolean errorDump) {
        this.errorDump = errorDump;
    }

    /**
     * Compile all xml files in sourceDir, and write output hyp files in
     * targetDir
     *
     * @param sourceDir
     *            Directory with pattern xml files
     * @param targetDir
     *            Directory to which compiled pattern hyp files should be
     *            written
     */
    public void serializeDir(final File sourceDir, final File targetDir) {
        final String extension = ".xml";
        final String[] sourceFiles = sourceDir.list(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(extension);
            }
        });
        for (final String sourceFile : sourceFiles) {
            final File infile = new File(sourceDir, sourceFile);
            final String outfilename = sourceFile.substring(0,
                    sourceFile.length() - extension.length())
                    + ".hyp";
            final File outfile = new File(targetDir, outfilename);
            serializeFile(infile, outfile);
        }
    }

    /*
     * checks whether input or output files exists or the latter is older than
     * input file and start build if necessary
     */
    private void serializeFile(final File infile, final File outfile) {
        boolean startProcess;
        startProcess = rebuild(infile, outfile);
        if (startProcess) {
            final HyphenationTree hTree = buildPatternFile(infile);
            // serialize class
            try {
                final ObjectOutputStream out = new ObjectOutputStream(
                        new java.io.BufferedOutputStream(
                                new java.io.FileOutputStream(outfile)));
                out.writeObject(hTree);
                out.close();
            } catch (final IOException ioe) {
                log.error("Can't write compiled pattern file: " + outfile, ioe);
            }
        }
    }

    /*
     * serializes pattern files
     */
    private HyphenationTree buildPatternFile(final File infile) {
        log.info("Processing " + infile);
        final HyphenationTree hTree = new HyphenationTree();
        try {
            hTree.loadPatterns(infile.toString());
            if (this.errorDump) {
                log.info("Stats: ");
                hTree.printStats();
            }
        } catch (final HyphenationException ex) {
            System.err.println("Can't load patterns from xml file " + infile
                    + " - Maybe hyphenation.dtd is missing?");
            if (this.errorDump) {
                System.err.println(ex.toString());
            }
        }
        return hTree;
    }

    /**
     * Checks for existence of output file and compares dates with input and
     * stylesheet file
     */
    private boolean rebuild(final File infile, final File outfile) {
        if (outfile.exists()) {
            // checks whether output file is older than input file
            if (outfile.lastModified() < infile.lastModified()) {
                return true;
            }
        } else {
            // if output file does not exist, start process
            return true;
        }
        return false;
    } // end rebuild

    /**
     * Entry point for ant java task
     *
     * @param args
     *            sourceDir, targetDir
     */
    public static void main(final String[] args) {
        final SerializeHyphPattern ser = new SerializeHyphPattern();
        ser.serializeDir(new File(args[0]), new File(args[1]));
    }

}
