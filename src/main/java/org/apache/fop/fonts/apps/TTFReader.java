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

/* $Id: TTFReader.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fonts.apps;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.Version;
import org.apache.fop.fonts.FontUtil;
import org.apache.fop.fonts.truetype.FontFileReader;
import org.apache.fop.fonts.truetype.TTFCmapEntry;
import org.apache.fop.fonts.truetype.TTFFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A tool which reads TTF files and generates XML font metrics file for use in
 * FOP.
 */
@Slf4j
public class TTFReader extends AbstractFontReader {

    /** Used to detect incompatible versions of the generated XML files */
    public static final String METRICS_VERSION_ATTR = "metrics-version";
    /** Current version number for the metrics file */
    public static final int METRICS_VERSION = 2;

    /**
     * Main constructor.
     */
    public TTFReader() {
        super();
    }

    private static void displayUsage() {
        System.out.println("java " + TTFReader.class.getName()
                + " [options] fontfile.ttf xmlfile.xml");
        System.out.println();
        System.out.println("where options can be:");
        System.out.println("-d  Debug mode");
        System.out.println("-q  Quiet mode");
        System.out.println("-enc ansi");
        System.out
        .println("    With this option you create a WinAnsi encoded font.");
        System.out.println("    The default is to create a CID keyed font.");
        System.out
        .println("    If you're not going to use characters outside the");
        System.out
        .println("    pdfencoding range (almost the same as iso-8889-1)");
        System.out.println("    you can add this option.");
        System.out.println("-ttcname <fontname>");
        System.out
        .println("    If you're reading data from a TrueType Collection");
        System.out
        .println("    (.ttc file) you must specify which font from the");
        System.out
        .println("    collection you will read metrics from. If you read");
        System.out
        .println("    from a .ttc file without this option, the fontnames");
        System.out.println("    will be listed for you.");
        System.out.println(" -fn <fontname>");
        System.out
        .println("    default is to use the fontname in the .ttf file, but");
        System.out
        .println("    you can override that name to make sure that the");
        System.out
        .println("    embedded font is used (if you're embedding fonts)");
        System.out
        .println("    instead of installed fonts when viewing documents ");
        System.out.println("    with Acrobat Reader.");
    }

    /**
     * The main method for the TTFReader tool.
     *
     * @param args
     *            Command-line arguments: [options] fontfile.ttf xmlfile.xml
     *            where options can be: -fn <fontname> default is to use the
     *            fontname in the .ttf file, but you can override that name to
     *            make sure that the embedded font is used instead of installed
     *            fonts when viewing documents with Acrobat Reader. -cn
     *            <classname> default is to use the fontname -ef <path to the
     *            truetype fontfile> will add the possibility to embed the font.
     *            When running fop, fop will look for this file to embed it -er
     *            <path to truetype fontfile relative to
     *            org/apache/fop/render/pdf/fonts> you can also include the
     *            fontfile in the fop.jar file when building fop. You can use
     *            both -ef and -er. The file specified in -ef will be searched
     *            first, then the -er file.
     */
    public static void main(final String[] args) {
        String embFile = null;
        String embResource = null;
        String className = null;
        String fontName = null;
        String ttcName = null;
        boolean isCid = true;

        final Map options = new java.util.HashMap();
        final String[] arguments = parseArguments(options, args);

        final TTFReader app = new TTFReader();

        log.info("TTF Reader for Apache FOP " + Version.getVersion() + "\n");

        if (options.get("-enc") != null) {
            final String enc = (String) options.get("-enc");
            if ("ansi".equals(enc)) {
                isCid = false;
            }
        }

        if (options.get("-ttcname") != null) {
            ttcName = (String) options.get("-ttcname");
        }

        if (options.get("-ef") != null) {
            embFile = (String) options.get("-ef");
        }

        if (options.get("-er") != null) {
            embResource = (String) options.get("-er");
        }

        if (options.get("-fn") != null) {
            fontName = (String) options.get("-fn");
        }

        if (options.get("-cn") != null) {
            className = (String) options.get("-cn");
        }

        if (arguments.length != 2 || options.get("-h") != null
                || options.get("-help") != null
                || options.get("--help") != null) {
            displayUsage();
        } else {
            try {
                log.info("Parsing font...");
                final TTFFile ttf = app.loadTTF(arguments[0], ttcName);
                if (ttf != null) {
                    final org.w3c.dom.Document doc = app.constructFontXML(ttf,
                            fontName, className, embResource, embFile, isCid,
                            ttcName);

                    if (isCid) {
                        log.info("Creating CID encoded metrics...");
                    } else {
                        log.info("Creating WinAnsi encoded metrics...");
                    }

                    if (doc != null) {
                        app.writeFontXML(doc, arguments[1]);
                    }

                    if (ttf.isEmbeddable()) {
                        log.info("This font contains no embedding license restrictions.");
                    } else {
                        log.info("** Note: This font contains license retrictions for\n"
                                + "         embedding. This font shouldn't be embedded.");
                    }
                }
                log.info("");
                log.info("XML font metrics file successfully created.");
            } catch (final Exception e) {
                log.error("Error while building XML font metrics file.", e);
                System.exit(-1);
            }
        }
    }

    /**
     * Read a TTF file and returns it as an object.
     *
     * @param fileName
     *            The filename of the TTF file.
     * @param fontName
     *            The name of the font
     * @return The TTF as an object, null if the font is incompatible.
     * @throws IOException
     *             In case of an I/O problem
     */
    public TTFFile loadTTF(final String fileName, final String fontName)
            throws IOException {
        final TTFFile ttfFile = new TTFFile();
        log.info("Reading " + fileName + "...");

        final FontFileReader reader = new FontFileReader(fileName);
        final boolean supported = ttfFile.readFont(reader, fontName);
        if (!supported) {
            return null;
        }
        log.info("Font Family: " + ttfFile.getFamilyNames());
        if (ttfFile.isCFF()) {
            throw new UnsupportedOperationException(
                    "OpenType fonts with CFF data are not supported, yet");
        }
        return ttfFile;
    }

    /**
     * Generates the font metrics file from the TTF/TTC file.
     *
     * @param ttf
     *            The PFM file to generate the font metrics from.
     * @param fontName
     *            Name of the font
     * @param className
     *            Class name for the font
     * @param resource
     *            path to the font as embedded resource
     * @param file
     *            path to the font as file
     * @param isCid
     *            True if the font is CID encoded
     * @param ttcName
     *            Name of the TrueType Collection
     * @return The DOM document representing the font metrics file.
     */
    public org.w3c.dom.Document constructFontXML(final TTFFile ttf,
            final String fontName, final String className,
            final String resource, final String file, final boolean isCid,
            final String ttcName) {
        log.info("Creating xml font file...");

        Document doc;
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            doc = factory.newDocumentBuilder().newDocument();
        } catch (final javax.xml.parsers.ParserConfigurationException e) {
            log.error("Can't create DOM implementation", e);
            return null;
        }
        final Element root = doc.createElement("font-metrics");
        doc.appendChild(root);
        root.setAttribute(METRICS_VERSION_ATTR, String.valueOf(METRICS_VERSION));
        if (isCid) {
            root.setAttribute("type", "TYPE0");
        } else {
            root.setAttribute("type", "TRUETYPE");
        }

        Element el = doc.createElement("font-name");
        root.appendChild(el);

        // Note that the PostScript name usually is something like
        // "Perpetua-Bold", but the TrueType spec says that in the ttf file
        // it should be "Perpetua,Bold".

        final String s = FontUtil.stripWhiteSpace(ttf.getPostScriptName());

        if (fontName != null) {
            el.appendChild(doc.createTextNode(FontUtil
                    .stripWhiteSpace(fontName)));
        } else {
            el.appendChild(doc.createTextNode(s));
        }
        if (ttf.getFullName() != null) {
            el = doc.createElement("full-name");
            root.appendChild(el);
            el.appendChild(doc.createTextNode(ttf.getFullName()));
        }
        final Set familyNames = ttf.getFamilyNames();
        if (familyNames.size() > 0) {
            final String familyName = (String) familyNames.iterator().next();
            el = doc.createElement("family-name");
            root.appendChild(el);
            el.appendChild(doc.createTextNode(familyName));
        }

        el = doc.createElement("embed");
        root.appendChild(el);
        if (file != null && ttf.isEmbeddable()) {
            el.setAttribute("file", file);
        }
        if (resource != null && ttf.isEmbeddable()) {
            el.setAttribute("class", resource);
        }

        el = doc.createElement("cap-height");
        root.appendChild(el);
        el.appendChild(doc.createTextNode(String.valueOf(ttf.getCapHeight())));

        el = doc.createElement("x-height");
        root.appendChild(el);
        el.appendChild(doc.createTextNode(String.valueOf(ttf.getXHeight())));

        el = doc.createElement("ascender");
        root.appendChild(el);
        el.appendChild(doc.createTextNode(String.valueOf(ttf
                .getLowerCaseAscent())));

        el = doc.createElement("descender");
        root.appendChild(el);
        el.appendChild(doc.createTextNode(String.valueOf(ttf
                .getLowerCaseDescent())));

        final Element bbox = doc.createElement("bbox");
        root.appendChild(bbox);
        final int[] bb = ttf.getFontBBox();
        final String[] names = { "left", "bottom", "right", "top" };
        for (int i = 0; i < names.length; i++) {
            el = doc.createElement(names[i]);
            bbox.appendChild(el);
            el.appendChild(doc.createTextNode(String.valueOf(bb[i])));
        }

        el = doc.createElement("flags");
        root.appendChild(el);
        el.appendChild(doc.createTextNode(String.valueOf(ttf.getFlags())));

        el = doc.createElement("stemv");
        root.appendChild(el);
        el.appendChild(doc.createTextNode(ttf.getStemV()));

        el = doc.createElement("italicangle");
        root.appendChild(el);
        el.appendChild(doc.createTextNode(ttf.getItalicAngle()));

        if (ttcName != null) {
            el = doc.createElement("ttc-name");
            root.appendChild(el);
            el.appendChild(doc.createTextNode(ttcName));
        }

        el = doc.createElement("subtype");
        root.appendChild(el);

        // Fill in extras for CID keyed fonts
        if (isCid) {
            el.appendChild(doc.createTextNode("TYPE0"));

            generateDOM4MultiByteExtras(root, ttf, isCid);
        } else {
            // Fill in extras for singlebyte fonts
            el.appendChild(doc.createTextNode("TRUETYPE"));

            generateDOM4SingleByteExtras(root, ttf, isCid);
        }

        generateDOM4Kerning(root, ttf, isCid);

        return doc;
    }

    private void generateDOM4MultiByteExtras(final Element parent,
            final TTFFile ttf, final boolean isCid) {
        Element el;
        final Document doc = parent.getOwnerDocument();

        final Element mel = doc.createElement("multibyte-extras");
        parent.appendChild(mel);

        el = doc.createElement("cid-type");
        mel.appendChild(el);
        el.appendChild(doc.createTextNode("CIDFontType2"));

        el = doc.createElement("default-width");
        mel.appendChild(el);
        el.appendChild(doc.createTextNode("0"));

        el = doc.createElement("bfranges");
        mel.appendChild(el);
        final Iterator iter = ttf.getCMaps().listIterator();
        while (iter.hasNext()) {
            final TTFCmapEntry ce = (TTFCmapEntry) iter.next();
            final Element el2 = doc.createElement("bf");
            el.appendChild(el2);
            el2.setAttribute("us", String.valueOf(ce.getUnicodeStart()));
            el2.setAttribute("ue", String.valueOf(ce.getUnicodeEnd()));
            el2.setAttribute("gi", String.valueOf(ce.getGlyphStartIndex()));
        }

        el = doc.createElement("cid-widths");
        el.setAttribute("start-index", "0");
        mel.appendChild(el);

        final int[] wx = ttf.getWidths();
        for (final int element : wx) {
            final Element wxel = doc.createElement("wx");
            wxel.setAttribute("w", String.valueOf(element));
            el.appendChild(wxel);
        }
    }

    private void generateDOM4SingleByteExtras(final Element parent,
            final TTFFile ttf, final boolean isCid) {
        Element el;
        final Document doc = parent.getOwnerDocument();

        final Element sel = doc.createElement("singlebyte-extras");
        parent.appendChild(sel);

        el = doc.createElement("encoding");
        sel.appendChild(el);
        el.appendChild(doc.createTextNode(ttf.getCharSetName()));

        el = doc.createElement("first-char");
        sel.appendChild(el);
        el.appendChild(doc.createTextNode(String.valueOf(ttf.getFirstChar())));

        el = doc.createElement("last-char");
        sel.appendChild(el);
        el.appendChild(doc.createTextNode(String.valueOf(ttf.getLastChar())));

        final Element widths = doc.createElement("widths");
        sel.appendChild(widths);

        for (short i = ttf.getFirstChar(); i <= ttf.getLastChar(); i++) {
            el = doc.createElement("char");
            widths.appendChild(el);
            el.setAttribute("idx", String.valueOf(i));
            el.setAttribute("wdt", String.valueOf(ttf.getCharWidth(i)));
        }
    }

    private void generateDOM4Kerning(final Element parent, final TTFFile ttf,
            final boolean isCid) {
        Element el;
        final Document doc = parent.getOwnerDocument();

        // Get kerning
        Iterator iter;
        if (isCid) {
            iter = ttf.getKerning().keySet().iterator();
        } else {
            iter = ttf.getAnsiKerning().keySet().iterator();
        }

        while (iter.hasNext()) {
            final Integer kpx1 = (Integer) iter.next();

            el = doc.createElement("kerning");
            el.setAttribute("kpx1", kpx1.toString());
            parent.appendChild(el);
            Element el2 = null;

            Map h2;
            if (isCid) {
                h2 = ttf.getKerning().get(kpx1);
            } else {
                h2 = ttf.getAnsiKerning().get(kpx1);
            }

            final Iterator iter2 = h2.keySet().iterator();
            while (iter2.hasNext()) {
                final Integer kpx2 = (Integer) iter2.next();
                if (isCid || kpx2.intValue() < 256) {
                    el2 = doc.createElement("pair");
                    el2.setAttribute("kpx2", kpx2.toString());
                    final Integer val = (Integer) h2.get(kpx2);
                    el2.setAttribute("kern", val.toString());
                    el.appendChild(el2);
                }
            }
        }
    }

    /**
     * Bugzilla 40739, check that attr has a metrics-version attribute
     * compatible with ours.
     *
     * @param attr
     *            attributes read from the root element of a metrics XML file
     * @throws SAXException
     *             if incompatible
     */
    public static void checkMetricsVersion(final Attributes attr)
            throws SAXException {
        String err = null;
        final String str = attr.getValue(METRICS_VERSION_ATTR);
        if (str == null) {
            err = "Missing " + METRICS_VERSION_ATTR + " attribute";
        } else {
            int version = 0;
            try {
                version = Integer.parseInt(str);
                if (version < METRICS_VERSION) {
                    err = "Incompatible " + METRICS_VERSION_ATTR + " value ("
                            + version + ", should be " + METRICS_VERSION + ")";
                }
            } catch (final NumberFormatException e) {
                err = "Invalid " + METRICS_VERSION_ATTR + " attribute value ("
                        + str + ")";
            }
        }

        if (err != null) {
            throw new SAXException(err
                    + " - please regenerate the font metrics file with "
                    + "a more recent version of FOP.");
        }
    }

}
