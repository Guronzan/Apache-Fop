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

/* $Id: TTFFontLoader.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.fonts.truetype;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.fop.fonts.BFEntry;
import org.apache.fop.fonts.CIDFontType;
import org.apache.fop.fonts.EncodingMode;
import org.apache.fop.fonts.FontLoader;
import org.apache.fop.fonts.FontResolver;
import org.apache.fop.fonts.FontType;
import org.apache.fop.fonts.MultiByteFont;
import org.apache.fop.fonts.NamedCharacter;
import org.apache.fop.fonts.SingleByteFont;
import org.apache.xmlgraphics.fonts.Glyphs;

/**
 * Loads a TrueType font into memory directly from the original font file.
 */
public class TTFFontLoader extends FontLoader {

    private MultiByteFont multiFont;
    private SingleByteFont singleFont;
    private final String subFontName;
    private EncodingMode encodingMode;

    /**
     * Default constructor
     *
     * @param fontFileURI
     *            the URI representing the font file
     * @param resolver
     *            the FontResolver for font URI resolution
     */
    public TTFFontLoader(final String fontFileURI, final FontResolver resolver) {
        this(fontFileURI, null, true, EncodingMode.AUTO, true, resolver);
    }

    /**
     * Additional constructor for TrueType Collections.
     *
     * @param fontFileURI
     *            the URI representing the font file
     * @param subFontName
     *            the sub-fontname of a font in a TrueType Collection (or null
     *            for normal TrueType fonts)
     * @param embedded
     *            indicates whether the font is embedded or referenced
     * @param encodingMode
     *            the requested encoding mode
     * @param useKerning
     *            true to enable loading kerning info if available, false to
     *            disable
     * @param resolver
     *            the FontResolver for font URI resolution
     */
    public TTFFontLoader(final String fontFileURI, final String subFontName,
            final boolean embedded, final EncodingMode encodingMode,
            final boolean useKerning, final FontResolver resolver) {
        super(fontFileURI, embedded, true, resolver);
        this.subFontName = subFontName;
        this.encodingMode = encodingMode;
        if (this.encodingMode == EncodingMode.AUTO) {
            this.encodingMode = EncodingMode.CID; // Default to CID mode for
            // TrueType
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void read() throws IOException {
        read(this.subFontName);
    }

    /**
     * Reads a TrueType font.
     *
     * @param ttcFontName
     *            the TrueType sub-font name of TrueType Collection (may be null
     *            for normal TrueType fonts)
     * @throws IOException
     *             if an I/O error occurs
     */
    private void read(final String ttcFontName) throws IOException {
        final InputStream in = openFontUri(this.resolver, this.fontFileURI);
        try {
            final TTFFile ttf = new TTFFile();
            final FontFileReader reader = new FontFileReader(in);
            final boolean supported = ttf.readFont(reader, ttcFontName);
            if (!supported) {
                throw new IOException("TrueType font is not supported: "
                        + this.fontFileURI);
            }
            buildFont(ttf, ttcFontName);
            this.loaded = true;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void buildFont(final TTFFile ttf, final String ttcFontName) {
        if (ttf.isCFF()) {
            throw new UnsupportedOperationException(
                    "OpenType fonts with CFF data are not supported, yet");
        }

        boolean isCid = this.embedded;
        if (this.encodingMode == EncodingMode.SINGLE_BYTE) {
            isCid = false;
        }

        if (isCid) {
            this.multiFont = new MultiByteFont();
            this.returnFont = this.multiFont;
            this.multiFont.setTTCName(ttcFontName);
        } else {
            this.singleFont = new SingleByteFont();
            this.returnFont = this.singleFont;
        }
        this.returnFont.setResolver(this.resolver);

        this.returnFont.setFontName(ttf.getPostScriptName());
        this.returnFont.setFullName(ttf.getFullName());
        this.returnFont.setFamilyNames(ttf.getFamilyNames());
        this.returnFont.setFontSubFamilyName(ttf.getSubFamilyName());
        this.returnFont.setCapHeight(ttf.getCapHeight());
        this.returnFont.setXHeight(ttf.getXHeight());
        this.returnFont.setAscender(ttf.getLowerCaseAscent());
        this.returnFont.setDescender(ttf.getLowerCaseDescent());
        this.returnFont.setFontBBox(ttf.getFontBBox());
        this.returnFont.setFlags(ttf.getFlags());
        this.returnFont.setStemV(Integer.parseInt(ttf.getStemV())); // not used
        // for TTF
        this.returnFont.setItalicAngle(Integer.parseInt(ttf.getItalicAngle()));
        this.returnFont.setMissingWidth(0);
        this.returnFont.setWeight(ttf.getWeightClass());

        if (isCid) {
            this.multiFont.setCIDType(CIDFontType.CIDTYPE2);
            final int[] wx = ttf.getWidths();
            this.multiFont.setWidthArray(wx);
            final List entries = ttf.getCMaps();
            final BFEntry[] bfentries = new BFEntry[entries.size()];
            int pos = 0;
            final Iterator<TTFCmapEntry> iter = ttf.getCMaps().listIterator();
            while (iter.hasNext()) {
                final TTFCmapEntry ce = iter.next();
                bfentries[pos] = new BFEntry(ce.getUnicodeStart(),
                        ce.getUnicodeEnd(), ce.getGlyphStartIndex());
                pos++;
            }
            this.multiFont.setBFEntries(bfentries);
        } else {
            this.singleFont.setFontType(FontType.TRUETYPE);
            this.singleFont.setEncoding(ttf.getCharSetName());
            this.returnFont.setFirstChar(ttf.getFirstChar());
            this.returnFont.setLastChar(ttf.getLastChar());
            copyWidthsSingleByte(ttf);
        }

        if (this.useKerning) {
            copyKerning(ttf, isCid);
        }
        if (this.embedded && ttf.isEmbeddable()) {
            this.returnFont.setEmbedFileName(this.fontFileURI);
        }
    }

    private void copyWidthsSingleByte(final TTFFile ttf) {
        final int[] wx = ttf.getWidths();
        for (int i = this.singleFont.getFirstChar(); i <= this.singleFont
                .getLastChar(); i++) {
            this.singleFont.setWidth(i, ttf.getCharWidth(i));
        }
        final Iterator<TTFCmapEntry> iter = ttf.getCMaps().listIterator();
        while (iter.hasNext()) {
            final TTFCmapEntry ce = iter.next();
            if (ce.getUnicodeStart() < 0xFFFE) {
                for (char u = (char) ce.getUnicodeStart(); u <= ce
                        .getUnicodeEnd(); u++) {
                    final int codePoint = this.singleFont.getEncoding()
                            .mapChar(u);
                    if (codePoint <= 0) {
                        final String unicode = Character.toString(u);
                        final String charName = Glyphs.stringToGlyph(unicode);
                        if (charName.length() > 0) {
                            final NamedCharacter nc = new NamedCharacter(
                                    charName, unicode);
                            final int glyphIndex = ce.getGlyphStartIndex() + u
                                    - ce.getUnicodeStart();
                            this.singleFont.addUnencodedCharacter(nc,
                                    wx[glyphIndex]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Copy kerning information.
     */
    private void copyKerning(final TTFFile ttf, final boolean isCid) {

        // Get kerning
        Iterator<Integer> iter;
        if (isCid) {
            iter = ttf.getKerning().keySet().iterator();
        } else {
            iter = ttf.getAnsiKerning().keySet().iterator();
        }

        while (iter.hasNext()) {
            final Integer kpx1 = iter.next();

            Map h2;
            if (isCid) {
                h2 = ttf.getKerning().get(kpx1);
            } else {
                h2 = ttf.getAnsiKerning().get(kpx1);
            }
            this.returnFont.putKerningEntry(kpx1, h2);
        }
    }
}
