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

/* $Id: CommandLineOptions.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.cli;

// java
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.UIManager;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.Version;
import org.apache.fop.accessibility.Accessibility;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.pdf.PDFAMode;
import org.apache.fop.pdf.PDFEncryptionManager;
import org.apache.fop.pdf.PDFEncryptionParams;
import org.apache.fop.pdf.PDFXMode;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.awt.AWTRenderer;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFSerializer;
import org.apache.fop.render.pdf.PDFConfigurationConstants;
import org.apache.fop.render.print.PageableRenderer;
import org.apache.fop.render.print.PagesMode;
import org.apache.fop.render.print.PrintRenderer;
import org.apache.fop.render.xml.XMLRenderer;
import org.xml.sax.SAXException;

/**
 * Options parses the commandline arguments
 */
@Slf4j
public class CommandLineOptions {

    /**
     * Used to indicate that only the result of the XSL transformation should be
     * output
     */
    public static final int RENDER_NONE = -1;

    /*
     * These following constants are used to describe the input (either .FO,
     * .XML/.XSL or intermediate format)
     */

    /** (input) not set */
    public static final int NOT_SET = 0;
    /** input: fo file */
    public static final int FO_INPUT = 1;
    /** input: xml+xsl file */
    public static final int XSLT_INPUT = 2;
    /** input: Area Tree XML file */
    public static final int AREATREE_INPUT = 3;
    /** input: Intermediate Format XML file */
    public static final int IF_INPUT = 4;
    /** input: Image file */
    public static final int IMAGE_INPUT = 5;

    /* show configuration information */
    private Boolean showConfiguration = Boolean.FALSE;
    /* for area tree XML output, only down to block area level */
    private Boolean suppressLowLevelAreas = Boolean.FALSE;
    /* user configuration file */
    private File userConfigFile = null;
    /* input fo file */
    private File fofile = null;
    /* xsltfile (xslt transformation as input) */
    private File xsltfile = null;
    /* xml file (xslt transformation as input) */
    private File xmlfile = null;
    /* area tree input file */
    private File areatreefile = null;
    /* intermediate format input file */
    private File iffile = null;
    /* area tree input file */
    private File imagefile = null;
    /* output file */
    private File outfile = null;
    /* input mode */
    private int inputmode = NOT_SET;
    /* output mode */
    private String outputmode = null;
    /* true if System.in (stdin) should be used for the input file */
    private boolean useStdIn = false;
    /* true if System.out (stdout) should be used for the output file */
    private boolean useStdOut = false;
    /* true if a catalog resolver should be used for entity and uri resolution */
    private boolean useCatalogResolver = false;
    /* rendering options (for the user agent) */
    private final Map renderingOptions = new java.util.HashMap();
    /* target resolution (for the user agent) */
    private int targetResolution = 0;
    /* control memory-conservation policy */
    private boolean conserveMemoryPolicy = false;

    private final FopFactory factory = FopFactory.newInstance();
    private FOUserAgent foUserAgent;

    private InputHandler inputHandler;

    private List xsltParams = null;

    private String mimicRenderer = null;

    /**
     * Construct a command line option object.
     */
    public CommandLineOptions() {
    }

    /**
     * Parses the command line arguments.
     *
     * @param args
     *            the command line arguments.
     * @throws FOPException
     *             for general errors
     * @throws IOException
     *             if the the configuration file could not be loaded
     * @return true if the processing can continue, false to abort
     */
    public boolean parse(final String[] args) throws FOPException, IOException {
        boolean optionsParsed = true;

        try {
            optionsParsed = parseOptions(args);
            if (optionsParsed) {
                if (this.showConfiguration == Boolean.TRUE) {
                    dumpConfiguration();
                }
                checkSettings();
                setUserConfig();

                // Factory config is set up, now we can create the user agent
                this.foUserAgent = this.factory.newFOUserAgent();
                this.foUserAgent.getRendererOptions().putAll(
                        this.renderingOptions);
                if (this.targetResolution != 0) {
                    this.foUserAgent.setTargetResolution(this.targetResolution);
                }
                addXSLTParameter("fop-output-format", getOutputFormat());
                addXSLTParameter("fop-version", Version.getVersion());
                this.foUserAgent
                        .setConserveMemoryPolicy(this.conserveMemoryPolicy);
            } else {
                return false;
            }
        } catch (final FOPException e) {
            printUsage(System.err);
            throw e;
        } catch (final java.io.FileNotFoundException e) {
            printUsage(System.err);
            throw e;
        }

        this.inputHandler = createInputHandler();

        if (MimeConstants.MIME_FOP_AWT_PREVIEW.equals(this.outputmode)) {
            // set the system look&feel for the preview dialog
            try {
                UIManager.setLookAndFeel(UIManager
                        .getSystemLookAndFeelClassName());
            } catch (final Exception e) {
                System.err.println("Couldn't set system look & feel!");
            }

            final AWTRenderer renderer = new AWTRenderer(true);
            renderer.setRenderable(this.inputHandler); // set before user agent!
            renderer.setUserAgent(this.foUserAgent);
            this.foUserAgent.setRendererOverride(renderer);
        } else if (MimeConstants.MIME_FOP_AREA_TREE.equals(this.outputmode)
                && this.mimicRenderer != null) {
            // render from FO to Intermediate Format
            final Renderer targetRenderer = this.foUserAgent
                    .getRendererFactory().createRenderer(this.foUserAgent,
                            this.mimicRenderer);
            final XMLRenderer xmlRenderer = new XMLRenderer();
            xmlRenderer.setUserAgent(this.foUserAgent);

            // Tell the XMLRenderer to mimic the target renderer
            xmlRenderer.mimicRenderer(targetRenderer);

            // Make sure the prepared XMLRenderer is used
            this.foUserAgent.setRendererOverride(xmlRenderer);
        } else if (MimeConstants.MIME_FOP_IF.equals(this.outputmode)
                && this.mimicRenderer != null) {
            // render from FO to Intermediate Format
            final IFSerializer serializer = new IFSerializer();
            serializer.setContext(new IFContext(this.foUserAgent));

            final IFDocumentHandler targetHandler = this.foUserAgent
                    .getRendererFactory().createDocumentHandler(
                            this.foUserAgent, this.mimicRenderer);
            serializer.mimicDocumentHandler(targetHandler);

            // Make sure the prepared serializer is used
            this.foUserAgent.setDocumentHandlerOverride(serializer);
        }
        return true;
    }

    /**
     * @return the InputHandler instance defined by the command-line options.
     */
    public InputHandler getInputHandler() {
        return this.inputHandler;
    }

    private void addXSLTParameter(final String name, final String value) {
        if (this.xsltParams == null) {
            this.xsltParams = new ArrayList<>();
        }
        this.xsltParams.add(name);
        this.xsltParams.add(value);
    }

    /**
     * Parses the command line arguments.
     *
     * @return true if processing can continue, false if it should stop (nothing
     *         to do)
     * @exception FOPException
     *                if there was an error in the format of the options
     */
    private boolean parseOptions(final String[] args) throws FOPException {
        // do not throw an exception for no args
        if (args.length == 0) {
            printVersion();
            printUsage(System.out);
            return false;
        }
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-x") || args[i].equals("--dump-config")) {
                this.showConfiguration = Boolean.TRUE;
            } else if (args[i].equals("-c")) {
                i = i + parseConfigurationOption(args, i);
            } else if (args[i].equals("-l")) {
                i = i + parseLanguageOption(args, i);
            } else if (args[i].equals("-s")) {
                this.suppressLowLevelAreas = Boolean.TRUE;
            } else if (args[i].equals("-r")) {
                this.factory.setStrictValidation(false);
            } else if (args[i].equals("-conserve")) {
                this.conserveMemoryPolicy = true;
            } else if (args[i].equals("-dpi")) {
                i = i + parseResolution(args, i);
            } else if (args[i].equals("-fo")) {
                i = i + parseFOInputOption(args, i);
            } else if (args[i].equals("-xsl")) {
                i = i + parseXSLInputOption(args, i);
            } else if (args[i].equals("-xml")) {
                i = i + parseXMLInputOption(args, i);
            } else if (args[i].equals("-atin")) {
                i = i + parseAreaTreeInputOption(args, i);
            } else if (args[i].equals("-ifin")) {
                i = i + parseIFInputOption(args, i);
            } else if (args[i].equals("-imagein")) {
                i = i + parseImageInputOption(args, i);
            } else if (args[i].equals("-awt")) {
                i = i + parseAWTOutputOption(args, i);
            } else if (args[i].equals("-pdf")) {
                i = i + parsePDFOutputOption(args, i, null);
            } else if (args[i].equals("-pdfa1b")) {
                i = i + parsePDFOutputOption(args, i, "PDF/A-1b");
            } else if (args[i].equals("-mif")) {
                i = i + parseMIFOutputOption(args, i);
            } else if (args[i].equals("-rtf")) {
                i = i + parseRTFOutputOption(args, i);
            } else if (args[i].equals("-tiff")) {
                i = i + parseTIFFOutputOption(args, i);
            } else if (args[i].equals("-png")) {
                i = i + parsePNGOutputOption(args, i);
            } else if (args[i].equals("-print")) {
                // show print help
                if (i + 1 < args.length) {
                    if (args[i + 1].equals("help")) {
                        printUsagePrintOutput();
                        return false;
                    }
                }
                i = i + parsePrintOutputOption(args, i);
            } else if (args[i].equals("-copies")) {
                i = i + parseCopiesOption(args, i);
            } else if (args[i].equals("-pcl")) {
                i = i + parsePCLOutputOption(args, i);
            } else if (args[i].equals("-ps")) {
                i = i + parsePostscriptOutputOption(args, i);
            } else if (args[i].equals("-txt")) {
                i = i + parseTextOutputOption(args, i);
            } else if (args[i].equals("-svg")) {
                i = i + parseSVGOutputOption(args, i);
            } else if (args[i].equals("-afp")) {
                i = i + parseAFPOutputOption(args, i);
            } else if (args[i].equals("-foout")) {
                i = i + parseFOOutputOption(args, i);
            } else if (args[i].equals("-out")) {
                i = i + parseCustomOutputOption(args, i);
            } else if (args[i].equals("-at")) {
                i = i + parseAreaTreeOption(args, i);
            } else if (args[i].equals("-if")) {
                i = i + parseIntermediateFormatOption(args, i);
            } else if (args[i].equals("-a")) {
                this.renderingOptions.put(Accessibility.ACCESSIBILITY,
                        Boolean.TRUE);
            } else if (args[i].equals("-v")) {
                /* Currently just print the version */
                printVersion();
            } else if (args[i].equals("-param")) {
                if (i + 2 < args.length) {
                    final String name = args[++i];
                    final String expression = args[++i];
                    addXSLTParameter(name, expression);
                } else {
                    throw new FOPException(
                            "invalid param usage: use -param <name> <value>");
                }
            } else if (args[i].equals("-catalog")) {
                this.useCatalogResolver = true;
            } else if (args[i].equals("-o")) {
                i = i + parsePDFOwnerPassword(args, i);
            } else if (args[i].equals("-u")) {
                i = i + parsePDFUserPassword(args, i);
            } else if (args[i].equals("-pdfprofile")) {
                i = i + parsePDFProfile(args, i);
            } else if (args[i].equals("-noprint")) {
                getPDFEncryptionParams().setAllowPrint(false);
            } else if (args[i].equals("-nocopy")) {
                getPDFEncryptionParams().setAllowCopyContent(false);
            } else if (args[i].equals("-noedit")) {
                getPDFEncryptionParams().setAllowEditContent(false);
            } else if (args[i].equals("-noannotations")) {
                getPDFEncryptionParams().setAllowEditAnnotations(false);
            } else if (args[i].equals("-version")) {
                printVersion();
                return false;
            } else if (!isOption(args[i])) {
                i = i + parseUnknownOption(args, i);
            } else {
                printUsage(System.err);
                System.exit(1);
            }
        }
        return true;
    } // end parseOptions

    private int parseConfigurationOption(final String[] args, final int i)
            throws FOPException {
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("if you use '-c', you must specify "
                    + "the name of the configuration file");
        } else {
            this.userConfigFile = new File(args[i + 1]);
            return 1;
        }
    }

    private int parseLanguageOption(final String[] args, final int i)
            throws FOPException {
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException(
                    "if you use '-l', you must specify a language");
        } else {
            Locale.setDefault(new Locale(args[i + 1], ""));
            return 1;
        }
    }

    private int parseResolution(final String[] args, final int i)
            throws FOPException {
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException(
                    "if you use '-dpi', you must specify a resolution (dots per inch)");
        } else {
            this.targetResolution = Integer.parseInt(args[i + 1]);
            return 1;
        }
    }

    private int parseFOInputOption(final String[] args, final int i)
            throws FOPException {
        setInputFormat(FO_INPUT);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException(
                    "you must specify the fo file for the '-fo' option");
        } else {
            final String filename = args[i + 1];
            if (isSystemInOutFile(filename)) {
                this.useStdIn = true;
            } else {
                this.fofile = new File(filename);
            }
            return 1;
        }
    }

    private int parseXSLInputOption(final String[] args, final int i)
            throws FOPException {
        setInputFormat(XSLT_INPUT);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the stylesheet "
                    + "file for the '-xsl' option");
        } else {
            this.xsltfile = new File(args[i + 1]);
            return 1;
        }
    }

    private int parseXMLInputOption(final String[] args, final int i)
            throws FOPException {
        setInputFormat(XSLT_INPUT);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the input file "
                    + "for the '-xml' option");
        } else {
            final String filename = args[i + 1];
            if (isSystemInOutFile(filename)) {
                this.useStdIn = true;
            } else {
                this.xmlfile = new File(filename);
            }
            return 1;
        }
    }

    private int parseAWTOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(MimeConstants.MIME_FOP_AWT_PREVIEW);
        return 0;
    }

    private int parsePDFOutputOption(final String[] args, final int i,
            final String pdfAMode) throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_PDF);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the PDF output file");
        } else {
            setOutputFile(args[i + 1]);
            if (pdfAMode != null) {
                if (this.renderingOptions.get("pdf-a-mode") != null) {
                    throw new FOPException("PDF/A mode already set");
                }
                this.renderingOptions.put("pdf-a-mode", pdfAMode);
            }
            return 1;
        }
    }

    private void setOutputFile(final String filename) {
        if (isSystemInOutFile(filename)) {
            this.useStdOut = true;
        } else {
            this.outfile = new File(filename);
        }
    }

    /**
     * Checks whether the given argument is the next option or the specification
     * of stdin/stdout.
     *
     * TODO this is very ad-hoc and should be better handled. Consider the
     * adoption of Apache Commons CLI.
     *
     * @param arg
     *            an argument
     * @return true if the argument is an option ("-something"), false otherwise
     */
    private boolean isOption(final String arg) {
        return arg.length() > 1 && arg.startsWith("-");
    }

    private boolean isSystemInOutFile(final String filename) {
        return "-".equals(filename);
    }

    private int parseMIFOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_MIF);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the MIF output file");
        } else {
            setOutputFile(args[i + 1]);
            return 1;
        }
    }

    private int parseRTFOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_RTF);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the RTF output file");
        } else {
            setOutputFile(args[i + 1]);
            return 1;
        }
    }

    private int parseTIFFOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_TIFF);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the TIFF output file");
        } else {
            setOutputFile(args[i + 1]);
            return 1;
        }
    }

    private int parsePNGOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_PNG);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the PNG output file");
        } else {
            setOutputFile(args[i + 1]);
            return 1;
        }
    }

    private int parsePrintOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(MimeConstants.MIME_FOP_PRINT);
        if (i + 1 <= args.length && args[i + 1].charAt(0) != '-') {
            final String arg = args[i + 1];
            final String[] parts = arg.split(",");
            for (final String s : parts) {
                if (s.matches("\\d+")) {
                    this.renderingOptions.put(PageableRenderer.START_PAGE,
                            (s));
                } else if (s.matches("\\d+-\\d+")) {
                    final String[] startend = s.split("-");
                    this.renderingOptions.put(PageableRenderer.START_PAGE,
                            (startend[0]));
                    this.renderingOptions.put(PageableRenderer.END_PAGE,
                            (startend[1]));
                } else {
                    final PagesMode mode = PagesMode.byName(s);
                    this.renderingOptions
                            .put(PageableRenderer.PAGES_MODE, mode);
                }
            }
            return 1;
        } else {
            return 0;
        }
    }

    private int parseCopiesOption(final String[] args, final int i)
            throws FOPException {
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the number of copies");
        } else {
            this.renderingOptions.put(PrintRenderer.COPIES, (
                    args[i + 1]));
            return 1;
        }
    }

    private int parsePCLOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_PCL);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the PDF output file");
        } else {
            setOutputFile(args[i + 1]);
            return 1;
        }
    }

    private int parsePostscriptOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_POSTSCRIPT);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException(
                    "you must specify the PostScript output file");
        } else {
            setOutputFile(args[i + 1]);
            return 1;
        }
    }

    private int parseTextOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_PLAIN_TEXT);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the text output file");
        } else {
            setOutputFile(args[i + 1]);
            return 1;
        }
    }

    private int parseSVGOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_SVG);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the SVG output file");
        } else {
            setOutputFile(args[i + 1]);
            return 1;
        }
    }

    private int parseAFPOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_AFP);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the AFP output file");
        } else {
            setOutputFile(args[i + 1]);
            return 1;
        }
    }

    private int parseFOOutputOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(org.apache.xmlgraphics.util.MimeConstants.MIME_XSL_FO);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the FO output file");
        } else {
            setOutputFile(args[i + 1]);
            return 1;
        }
    }

    private int parseCustomOutputOption(final String[] args, final int i)
            throws FOPException {
        String mime = null;
        if (i + 1 < args.length || args[i + 1].charAt(0) != '-') {
            mime = args[i + 1];
            if ("list".equals(mime)) {
                final String[] mimes = this.factory.getRendererFactory()
                        .listSupportedMimeTypes();
                log.info("Supported MIME types:");
                for (final String mime2 : mimes) {
                    log.info("  " + mime2);
                }
                System.exit(0);
            }
        }
        if (i + 2 >= args.length || isOption(args[i + 1])
                || isOption(args[i + 2])) {
            throw new FOPException(
                    "you must specify the output format and the output file");
        } else {
            setOutputMode(mime);
            setOutputFile(args[i + 2]);
            return 2;
        }
    }

    private int parseUnknownOption(final String[] args, final int i)
            throws FOPException {
        if (this.inputmode == NOT_SET) {
            this.inputmode = FO_INPUT;
            final String filename = args[i];
            if (isSystemInOutFile(filename)) {
                this.useStdIn = true;
            } else {
                this.fofile = new File(filename);
            }
        } else if (this.outputmode == null) {
            this.outputmode = org.apache.xmlgraphics.util.MimeConstants.MIME_PDF;
            setOutputFile(args[i]);
        } else {
            throw new FOPException("Don't know what to do with " + args[i]);
        }
        return 0;
    }

    private int parseAreaTreeOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(MimeConstants.MIME_FOP_AREA_TREE);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("you must specify the area-tree output file");
        } else if (i + 2 == args.length || isOption(args[i + 2])) {
            // only output file is specified
            setOutputFile(args[i + 1]);
            return 1;
        } else {
            // mimic format and output file have been specified
            this.mimicRenderer = args[i + 1];
            setOutputFile(args[i + 2]);
            return 2;
        }
    }

    private int parseIntermediateFormatOption(final String[] args, final int i)
            throws FOPException {
        setOutputMode(MimeConstants.MIME_FOP_IF);
        if (i + 1 == args.length || args[i + 1].charAt(0) == '-') {
            throw new FOPException(
                    "you must specify the intermediate format output file");
        } else if (i + 2 == args.length || args[i + 2].charAt(0) == '-') {
            // only output file is specified
            setOutputFile(args[i + 1]);
            return 1;
        } else {
            // mimic format and output file have been specified
            this.mimicRenderer = args[i + 1];
            setOutputFile(args[i + 2]);
            return 2;
        }
    }

    private int parseAreaTreeInputOption(final String[] args, final int i)
            throws FOPException {
        setInputFormat(AREATREE_INPUT);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException(
                    "you must specify the Area Tree file for the '-atin' option");
        } else {
            final String filename = args[i + 1];
            if (isSystemInOutFile(filename)) {
                this.useStdIn = true;
            } else {
                this.areatreefile = new File(filename);
            }
            return 1;
        }
    }

    private int parseIFInputOption(final String[] args, final int i)
            throws FOPException {
        setInputFormat(IF_INPUT);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException(
                    "you must specify the intermediate file for the '-ifin' option");
        } else {
            final String filename = args[i + 1];
            if (isSystemInOutFile(filename)) {
                this.useStdIn = true;
            } else {
                this.iffile = new File(filename);
            }
            return 1;
        }
    }

    private int parseImageInputOption(final String[] args, final int i)
            throws FOPException {
        setInputFormat(IMAGE_INPUT);
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException(
                    "you must specify the image file for the '-imagein' option");
        } else {
            final String filename = args[i + 1];
            if (isSystemInOutFile(filename)) {
                this.useStdIn = true;
            } else {
                this.imagefile = new File(filename);
            }
            return 1;
        }
    }

    private PDFEncryptionParams getPDFEncryptionParams() throws FOPException {
        PDFEncryptionParams params = (PDFEncryptionParams) this.renderingOptions
                .get(PDFConfigurationConstants.ENCRYPTION_PARAMS);
        if (params == null) {
            if (!PDFEncryptionManager.checkAvailableAlgorithms()) {
                throw new FOPException(
                        "PDF encryption requested but it is not available."
                                + " Please make sure MD5 and RC4 algorithms are available.");
            }
            params = new PDFEncryptionParams();
            this.renderingOptions.put(
                    PDFConfigurationConstants.ENCRYPTION_PARAMS, params);
        }
        return params;
    }

    private int parsePDFOwnerPassword(final String[] args, final int i)
            throws FOPException {
        if (i + 1 == args.length || isOption(args[i + 1])) {
            getPDFEncryptionParams().setOwnerPassword("");
            return 0;
        } else {
            getPDFEncryptionParams().setOwnerPassword(args[i + 1]);
            return 1;
        }
    }

    private int parsePDFUserPassword(final String[] args, final int i)
            throws FOPException {
        if (i + 1 == args.length || isOption(args[i + 1])) {
            getPDFEncryptionParams().setUserPassword("");
            return 0;
        } else {
            getPDFEncryptionParams().setUserPassword(args[i + 1]);
            return 1;
        }
    }

    private int parsePDFProfile(final String[] args, final int i)
            throws FOPException {
        if (i + 1 == args.length || isOption(args[i + 1])) {
            throw new FOPException("You must specify a PDF profile");
        } else {
            final String profile = args[i + 1];
            final PDFAMode pdfAMode = PDFAMode.valueOf(profile);
            if (pdfAMode != null && pdfAMode != PDFAMode.DISABLED) {
                if (this.renderingOptions.get("pdf-a-mode") != null) {
                    throw new FOPException("PDF/A mode already set");
                }
                this.renderingOptions.put("pdf-a-mode", pdfAMode.getName());
                return 1;
            } else {
                final PDFXMode pdfXMode = PDFXMode.valueOf(profile);
                if (pdfXMode != null && pdfXMode != PDFXMode.DISABLED) {
                    if (this.renderingOptions.get("pdf-x-mode") != null) {
                        throw new FOPException("PDF/X mode already set");
                    }
                    this.renderingOptions.put("pdf-x-mode", pdfXMode.getName());
                    return 1;
                }
            }
            throw new FOPException("Unsupported PDF profile: " + profile);
        }
    }

    private void setOutputMode(final String mime) throws FOPException {
        if (this.outputmode == null) {
            this.outputmode = mime;
        } else {
            throw new FOPException("you can only set one output method");
        }
    }

    private void setInputFormat(final int format) throws FOPException {
        if (this.inputmode == NOT_SET || this.inputmode == format) {
            this.inputmode = format;
        } else {
            throw new FOPException("Only one input mode can be specified!");
        }
    }

    /**
     * checks whether all necessary information has been given in a consistent
     * way
     */
    private void checkSettings() throws FOPException, FileNotFoundException {
        if (this.inputmode == NOT_SET) {
            throw new FOPException("No input file specified");
        }

        if (this.outputmode == null) {
            throw new FOPException("No output file specified");
        }

        if ((this.outputmode.equals(MimeConstants.MIME_FOP_AWT_PREVIEW) || this.outputmode
                .equals(MimeConstants.MIME_FOP_PRINT)) && this.outfile != null) {
            throw new FOPException("Output file may not be specified "
                    + "for AWT or PRINT output");
        }

        if (this.inputmode == XSLT_INPUT) {
            // check whether xml *and* xslt file have been set
            if (this.xmlfile == null && !this.useStdIn) {
                throw new FOPException(
                        "XML file must be specified for the transform mode");
            }
            if (this.xsltfile == null) {
                throw new FOPException(
                        "XSLT file must be specified for the transform mode");
            }

            // warning if fofile has been set in xslt mode
            if (this.fofile != null) {
                log.warn("Can't use fo file with transform mode! Ignoring.\n"
                        + "Your input is " + "\n xmlfile: "
                        + this.xmlfile.getAbsolutePath() + "\nxsltfile: "
                        + this.xsltfile.getAbsolutePath() + "\n  fofile: "
                        + this.fofile.getAbsolutePath());
            }
            if (this.xmlfile != null && !this.xmlfile.exists()) {
                throw new FileNotFoundException("Error: xml file "
                        + this.xmlfile.getAbsolutePath() + " not found ");
            }
            if (!this.xsltfile.exists()) {
                throw new FileNotFoundException("Error: xsl file "
                        + this.xsltfile.getAbsolutePath() + " not found ");
            }

        } else if (this.inputmode == FO_INPUT) {
            if (this.outputmode
                    .equals(org.apache.xmlgraphics.util.MimeConstants.MIME_XSL_FO)) {
                throw new FOPException(
                        "FO output mode is only available if you use -xml and -xsl");
            }
            if (this.fofile != null && !this.fofile.exists()) {
                throw new FileNotFoundException("Error: fo file "
                        + this.fofile.getAbsolutePath() + " not found ");
            }
        } else if (this.inputmode == AREATREE_INPUT) {
            if (this.outputmode
                    .equals(org.apache.xmlgraphics.util.MimeConstants.MIME_XSL_FO)) {
                throw new FOPException(
                        "FO output mode is only available if you use -xml and -xsl");
            } else if (this.outputmode.equals(MimeConstants.MIME_FOP_AREA_TREE)) {
                throw new FOPException(
                        "Area Tree Output is not available if Area Tree is used as input!");
            }
            if (this.areatreefile != null && !this.areatreefile.exists()) {
                throw new FileNotFoundException("Error: area tree file "
                        + this.areatreefile.getAbsolutePath() + " not found ");
            }
        } else if (this.inputmode == IF_INPUT) {
            if (this.outputmode
                    .equals(org.apache.xmlgraphics.util.MimeConstants.MIME_XSL_FO)) {
                throw new FOPException(
                        "FO output mode is only available if you use -xml and -xsl");
            } else if (this.outputmode.equals(MimeConstants.MIME_FOP_AREA_TREE)) {
                throw new FOPException(
                        "Area Tree Output is not available if Intermediate Format is used as input!");
            } else if (this.outputmode.equals(MimeConstants.MIME_FOP_IF)) {
                throw new FOPException(
                        "Intermediate Output is not available if Intermediate Format is used as input!");
            }
            if (this.iffile != null && !this.iffile.exists()) {
                throw new FileNotFoundException(
                        "Error: intermediate format file "
                                + this.iffile.getAbsolutePath() + " not found ");
            }
        } else if (this.inputmode == IMAGE_INPUT) {
            if (this.outputmode
                    .equals(org.apache.xmlgraphics.util.MimeConstants.MIME_XSL_FO)) {
                throw new FOPException(
                        "FO output mode is only available if you use -xml and -xsl");
            }
            if (this.imagefile != null && !this.imagefile.exists()) {
                throw new FileNotFoundException("Error: image file "
                        + this.imagefile.getAbsolutePath() + " not found ");
            }
        }
    } // end checkSettings

    /**
     * Sets the user configuration.
     *
     * @throws FOPException
     *             if creating the user configuration fails
     * @throws IOException
     */
    private void setUserConfig() throws FOPException, IOException {
        if (this.userConfigFile == null) {
            return;
        }
        try {
            this.factory.setUserConfig(this.userConfigFile);
        } catch (final SAXException e) {
            throw new FOPException(e);
        }
    }

    /**
     * @return the chosen output format (MIME type)
     * @throws FOPException
     *             for invalid output formats
     */
    protected String getOutputFormat() throws FOPException {
        if (this.outputmode == null) {
            throw new FOPException("Renderer has not been set!");
        }
        if (this.outputmode.equals(MimeConstants.MIME_FOP_AREA_TREE)) {
            this.renderingOptions.put("fineDetail", isCoarseAreaXml());
        }
        return this.outputmode;
    }

    /**
     * Create an InputHandler object based on command-line parameters
     *
     * @return a new InputHandler instance
     * @throws IllegalArgumentException
     *             if invalid/missing parameters
     */
    private InputHandler createInputHandler() {
        switch (this.inputmode) {
        case FO_INPUT:
            return new InputHandler(this.fofile);
        case AREATREE_INPUT:
            return new AreaTreeInputHandler(this.areatreefile);
        case IF_INPUT:
            return new IFInputHandler(this.iffile);
        case XSLT_INPUT:
            final InputHandler handler = new InputHandler(this.xmlfile,
                    this.xsltfile, this.xsltParams);
            if (this.useCatalogResolver) {
                handler.createCatalogResolver(this.foUserAgent);
            }
            return handler;
        case IMAGE_INPUT:
            return new ImageInputHandler(this.imagefile, this.xsltfile,
                    this.xsltParams);
        default:
            throw new IllegalArgumentException(
                    "Error creating InputHandler object.");
        }
    }

    /**
     * Get the FOUserAgent for this Command-Line run
     *
     * @return FOUserAgent instance
     */
    protected FOUserAgent getFOUserAgent() {
        return this.foUserAgent;
    }

    /**
     * Returns the XSL-FO file if set.
     *
     * @return the XSL-FO file, null if not set
     */
    public File getFOFile() {
        return this.fofile;
    }

    /**
     * Returns the input XML file if set.
     *
     * @return the input XML file, null if not set
     */
    public File getXMLFile() {
        return this.xmlfile;
    }

    /**
     * Returns the stylesheet to be used for transformation to XSL-FO.
     *
     * @return stylesheet
     */
    public File getXSLFile() {
        return this.xsltfile;
    }

    /**
     * Returns the output file
     *
     * @return the output file
     */
    public File getOutputFile() {
        return this.outfile;
    }

    /**
     * Returns the user configuration file to be used.
     *
     * @return the userconfig.xml file
     */
    public File getUserConfigFile() {
        return this.userConfigFile;
    }

    /**
     * Indicates whether the XML renderer should generate coarse area XML
     *
     * @return true if coarse area XML is desired
     */
    public Boolean isCoarseAreaXml() {
        return this.suppressLowLevelAreas;
    }

    /**
     * Indicates whether input comes from standard input (stdin).
     *
     * @return true if input comes from standard input (stdin)
     */
    public boolean isInputFromStdIn() {
        return this.useStdIn;
    }

    /**
     * Indicates whether output is sent to standard output (stdout).
     *
     * @return true if output is sent to standard output (stdout)
     */
    public boolean isOutputToStdOut() {
        return this.useStdOut;
    }

    /**
     * Returns the input file.
     *
     * @return either the fofile or the xmlfile
     */
    public File getInputFile() {
        switch (this.inputmode) {
        case FO_INPUT:
            return this.fofile;
        case XSLT_INPUT:
            return this.xmlfile;
        default:
            return this.fofile;
        }
    }

    private static void printVersion() {
        log.info("FOP Version " + Version.getVersion());
    }

    /**
     * Shows the command line syntax including a summary of all available
     * options and some examples.
     *
     * @param out
     *            the stream to which the message must be printed
     */
    public static void printUsage(final PrintStream out) {
        out.println("\nUSAGE\nfop [options] [-fo|-xml] infile [-xsl file] "
                + "[-awt|-pdf|-mif|-rtf|-tiff|-png|-pcl|-ps|-txt|-at [mime]|-print] <outfile>\n"
                + " [OPTIONS]  \n"
                + "  -version          print FOP version and exit\n"
                + "  -d                debug mode   \n"
                + "  -x                dump configuration settings  \n"
                + "  -q                quiet mode  \n"
                + "  -c cfg.xml        use additional configuration file cfg.xml\n"
                + "  -l lang           the language to use for user information \n"
                + "  -r                relaxed/less strict validation (where available)\n"
                + "  -dpi xxx          target resolution in dots per inch (dpi) where xxx is a number\n"
                + "  -s                for area tree XML, down to block areas only\n"
                + "  -v                run in verbose mode (currently simply print FOP version and continue)\n\n"
                + "  -o [password]     PDF file will be encrypted with option owner password\n"
                + "  -u [password]     PDF file will be encrypted with option user password\n"
                + "  -noprint          PDF file will be encrypted without printing permission\n"
                + "  -nocopy           PDF file will be encrypted without copy content permission\n"
                + "  -noedit           PDF file will be encrypted without edit content permission\n"
                + "  -noannotations    PDF file will be encrypted without edit annotation permission\n"
                + "  -a                enables accessibility features (Tagged PDF etc., default off)\n"
                + "  -pdfprofile prof  PDF file will be generated with the specified profile\n"
                + "                    (Examples for prof: PDF/A-1b or PDF/X-3:2003)\n\n"
                + "  -conserve         Enable memory-conservation policy (trades memory-consumption for disk I/O)\n"
                + "                    (Note: currently only influences whether the area tree is serialized.)\n\n"
                + " [INPUT]  \n"
                + "  infile            xsl:fo input file (the same as the next) \n"
                + "                    (use '-' for infile to pipe input from stdin)\n"
                + "  -fo  infile       xsl:fo input file  \n"
                + "  -xml infile       xml input file, must be used together with -xsl \n"
                + "  -atin infile      area tree input file \n"
                + "  -ifin infile      intermediate format input file \n"
                + "  -imagein infile   image input file (piping through stdin not supported)\n"
                + "  -xsl stylesheet   xslt stylesheet \n \n"
                + "  -param name value <value> to use for parameter <name> in xslt stylesheet\n"
                + "                    (repeat '-param name value' for each parameter)\n \n"
                + "  -catalog          use catalog resolver for input XML and XSLT files\n"
                + " [OUTPUT] \n"
                + "  outfile           input will be rendered as PDF into outfile\n"
                + "                    (use '-' for outfile to pipe output to stdout)\n"
                + "  -pdf outfile      input will be rendered as PDF (outfile req'd)\n"
                + "  -pdfa1b outfile   input will be rendered as PDF/A-1b compliant PDF\n"
                + "                    (outfile req'd, same as \"-pdf outfile -pdfprofile PDF/A-1b\")\n"
                + "  -awt              input will be displayed on screen \n"
                + "  -rtf outfile      input will be rendered as RTF (outfile req'd)\n"
                + "  -pcl outfile      input will be rendered as PCL (outfile req'd) \n"
                + "  -ps outfile       input will be rendered as PostScript (outfile req'd) \n"
                + "  -afp outfile      input will be rendered as AFP (outfile req'd)\n"
                + "  -tiff outfile     input will be rendered as TIFF (outfile req'd)\n"
                + "  -png outfile      input will be rendered as PNG (outfile req'd)\n"
                + "  -txt outfile      input will be rendered as plain text (outfile req'd) \n"
                + "  -at [mime] out    representation of area tree as XML (outfile req'd) \n"
                + "                    specify optional mime output to allow the AT to be converted\n"
                + "                    to final format later\n"
                + "  -if [mime] out    representation of document in intermediate format XML (outfile req'd)\n"
                + "                    specify optional mime output to allow the IF to be converted\n"
                + "                    to final format later\n"
                + "  -print            input file will be rendered and sent to the printer \n"
                + "                    see options with \"-print help\" \n"
                + "  -out mime outfile input will be rendered using the given MIME type\n"
                + "                    (outfile req'd) Example: \"-out application/pdf D:\\out.pdf\"\n"
                + "                    (Tip: \"-out list\" prints the list of supported MIME types)\n"
                // +
                // "  -mif outfile      input will be rendered as MIF (FrameMaker) (outfile req'd)\n"
                // +
                // "                    Experimental feature - requires additional fop-sandbox.jar.\n"
                + "  -svg outfile      input will be rendered as an SVG slides file (outfile req'd) \n"
                + "                    Experimental feature - requires additional fop-sandbox.jar.\n"
                + "\n"
                + "  -foout outfile    input will only be XSL transformed. The intermediate \n"
                + "                    XSL-FO file is saved and no rendering is performed. \n"
                + "                    (Only available if you use -xml and -xsl parameters)\n\n"
                + "\n"
                + " [Examples]\n"
                + "  fop foo.fo foo.pdf \n"
                + "  fop -fo foo.fo -pdf foo.pdf (does the same as the previous line)\n"
                + "  fop -xml foo.xml -xsl foo.xsl -pdf foo.pdf\n"
                + "  fop -xml foo.xml -xsl foo.xsl -foout foo.fo\n"
                + "  fop -xml - -xsl foo.xsl -pdf -\n"
                + "  fop foo.fo -mif foo.mif\n" + "  fop foo.fo -rtf foo.rtf\n"
                + "  fop foo.fo -print\n" + "  fop foo.fo -awt\n");
    }

    /**
     * shows the options for print output
     */
    private void printUsagePrintOutput() {
        System.err
                .println("USAGE: -print [from[-to][,even|odd]] [-copies numCopies]\n\n"
                        + "Example:\n"
                        + "all pages:                        fop infile.fo -print\n"
                        + "all pages with two copies:        fop infile.fo -print -copies 2\n"
                        + "all pages starting with page 7:   fop infile.fo -print 7\n"
                        + "pages 2 to 3:                     fop infile.fo -print 2-3\n"
                        + "only even page between 10 and 20: fop infile.fo -print 10-20,even\n");
    }

    /**
     * Outputs all commandline settings
     */
    private void dumpConfiguration() {
        log.info("Input mode: ");
        switch (this.inputmode) {
        case NOT_SET:
            log.info("not set");
            break;
        case FO_INPUT:
            log.info("FO ");
            if (isInputFromStdIn()) {
                log.info("fo input file: from stdin");
            } else {
                log.info("fo input file: " + this.fofile.toString());
            }
            break;
        case XSLT_INPUT:
            log.info("xslt transformation");
            if (isInputFromStdIn()) {
                log.info("xml input file: from stdin");
            } else {
                log.info("xml input file: " + this.xmlfile.toString());
            }
            log.info("xslt stylesheet: " + this.xsltfile.toString());
            break;
        case AREATREE_INPUT:
            log.info("AT ");
            if (isInputFromStdIn()) {
                log.info("area tree input file: from stdin");
            } else {
                log.info("area tree input file: "
                        + this.areatreefile.toString());
            }
            break;
        case IF_INPUT:
            log.info("IF ");
            if (isInputFromStdIn()) {
                log.info("intermediate input file: from stdin");
            } else {
                log.info("intermediate input file: " + this.iffile.toString());
            }
            break;
        case IMAGE_INPUT:
            log.info("Image ");
            if (isInputFromStdIn()) {
                log.info("image input file: from stdin");
            } else {
                log.info("image input file: " + this.imagefile.toString());
            }
            break;
        default:
            log.info("unknown input type");
        }
        log.info("Output mode: ");
        if (this.outputmode == null) {
            log.info("not set");
        } else if (MimeConstants.MIME_FOP_AWT_PREVIEW.equals(this.outputmode)) {
            log.info("awt on screen");
            if (this.outfile != null) {
                log.error("awt mode, but outfile is set:");
                log.error("out file: " + this.outfile.toString());
            }
        } else if (MimeConstants.MIME_FOP_PRINT.equals(this.outputmode)) {
            log.info("print directly");
            if (this.outfile != null) {
                log.error("print mode, but outfile is set:");
                log.error("out file: " + this.outfile.toString());
            }
        } else if (MimeConstants.MIME_FOP_AREA_TREE.equals(this.outputmode)) {
            log.info("area tree");
            if (this.mimicRenderer != null) {
                log.info("mimic renderer: " + this.mimicRenderer);
            }
            if (isOutputToStdOut()) {
                log.info("output file: to stdout");
            } else {
                log.info("output file: " + this.outfile.toString());
            }
        } else if (MimeConstants.MIME_FOP_IF.equals(this.outputmode)) {
            log.info("intermediate format");
            log.info("output file: " + this.outfile.toString());
        } else {
            log.info(this.outputmode);
            if (isOutputToStdOut()) {
                log.info("output file: to stdout");
            } else {
                log.info("output file: " + this.outfile.toString());
            }
        }

        log.info("OPTIONS");

        if (this.userConfigFile != null) {
            log.info("user configuration file: "
                    + this.userConfigFile.toString());
        } else {
            log.info("no user configuration file is used [default]");
        }
    }

}
