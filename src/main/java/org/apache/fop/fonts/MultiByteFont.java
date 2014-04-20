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

/* $Id: MultiByteFont.java 907265 2010-02-06 18:19:31Z jeremias $ */

package org.apache.fop.fonts;

//Java
import java.text.DecimalFormat;
import java.util.Map;

/**
 * Generic MultiByte (CID) font
 */
public class MultiByteFont extends CIDFont {

    private static int uniqueCounter = -1;

    private String ttcName = null;
    private final String encoding = "Identity-H";

    private int defaultWidth = 0;
    private CIDFontType cidType = CIDFontType.CIDTYPE2;

    private String namePrefix = null; // Quasi unique prefix

    private final CIDSubset subset = new CIDSubset();

    /** A map from Unicode indices to glyph indices */
    private BFEntry[] bfentries = null;

    /**
     * Default constructor
     */
    public MultiByteFont() {
        // Make sure that the 3 first glyphs are included
        this.subset.setupFirstThreeGlyphs();

        // Create a quasiunique prefix for fontname
        synchronized (this.getClass()) {
            uniqueCounter++;
            if (uniqueCounter > 99999 || uniqueCounter < 0) {
                uniqueCounter = 0; // We need maximum 5 character then we start
                                   // again
            }
        }
        final DecimalFormat counterFormat = new DecimalFormat("00000");
        final String cntString = counterFormat.format(uniqueCounter);

        // Subset prefix as described in chapter 5.5.3 of PDF 1.4
        final StringBuilder sb = new StringBuilder("E");
        for (int i = 0, c = cntString.length(); i < c; ++i) {
            // translate numbers to uppercase characters
            sb.append((char) (cntString.charAt(i) + (65 - 48)));
        }
        sb.append("+");
        this.namePrefix = sb.toString();

        setFontType(FontType.TYPE0);
    }

    /** {@inheritDoc} */
    @Override
    public int getDefaultWidth() {
        return this.defaultWidth;
    }

    /** {@inheritDoc} */
    @Override
    public String getRegistry() {
        return "Adobe";
    }

    /** {@inheritDoc} */
    @Override
    public String getOrdering() {
        return "UCS";
    }

    /** {@inheritDoc} */
    @Override
    public int getSupplement() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public CIDFontType getCIDType() {
        return this.cidType;
    }

    /**
     * Sets the CIDType.
     * 
     * @param cidType
     *            The cidType to set
     */
    public void setCIDType(final CIDFontType cidType) {
        this.cidType = cidType;
    }

    private String getPrefixedFontName() {
        return this.namePrefix + FontUtil.stripWhiteSpace(super.getFontName());
    }

    /** {@inheritDoc} */
    @Override
    public String getEmbedFontName() {
        if (isEmbeddable()) {
            return getPrefixedFontName();
        } else {
            return super.getFontName();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmbeddable() {
        return !(getEmbedFileName() == null && getEmbedResourceName() == null);
    }

    /** {@inheritDoc} */
    @Override
    public CIDSubset getCIDSubset() {
        return this.subset;
    }

    /** {@inheritDoc} */
    @Override
    public String getEncodingName() {
        return this.encoding;
    }

    /** {@inheritDoc} */
    @Override
    public int getWidth(final int i, final int size) {
        if (isEmbeddable()) {
            final int glyphIndex = this.subset.getGlyphIndexForSubsetIndex(i);
            return size * this.width[glyphIndex];
        } else {
            return size * this.width[i];
        }
    }

    /** {@inheritDoc} */
    @Override
    public int[] getWidths() {
        final int[] arr = new int[this.width.length];
        System.arraycopy(this.width, 0, arr, 0, this.width.length);
        return arr;
    }

    /**
     * Returns the glyph index for a Unicode character. The method returns 0 if
     * there's no such glyph in the character map.
     * 
     * @param c
     *            the Unicode character index
     * @return the glyph index (or 0 if the glyph is not available)
     */
    private int findGlyphIndex(final char c) {
        final int idx = c;
        int retIdx = SingleByteEncoding.NOT_FOUND_CODE_POINT;

        for (int i = 0; i < this.bfentries.length && retIdx == 0; ++i) {
            if (this.bfentries[i].getUnicodeStart() <= idx
                    && this.bfentries[i].getUnicodeEnd() >= idx) {

                retIdx = this.bfentries[i].getGlyphStartIndex() + idx
                        - this.bfentries[i].getUnicodeStart();
            }
        }
        return retIdx;
    }

    /** {@inheritDoc} */
    @Override
    public char mapChar(final char c) {
        notifyMapOperation();
        int glyphIndex = findGlyphIndex(c);
        if (glyphIndex == SingleByteEncoding.NOT_FOUND_CODE_POINT) {
            warnMissingGlyph(c);
            glyphIndex = findGlyphIndex(Typeface.NOT_FOUND);
        }
        if (isEmbeddable()) {
            glyphIndex = this.subset.mapSubsetChar(glyphIndex, c);
        }
        return (char) glyphIndex;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChar(final char c) {
        return findGlyphIndex(c) != SingleByteEncoding.NOT_FOUND_CODE_POINT;
    }

    /**
     * Sets the array of BFEntry instances which constitutes the Unicode to
     * glyph index map for a font. ("BF" means "base font")
     * 
     * @param entries
     *            the Unicode to glyph index map
     */
    public void setBFEntries(final BFEntry[] entries) {
        this.bfentries = entries;
    }

    /**
     * Sets the defaultWidth.
     * 
     * @param defaultWidth
     *            The defaultWidth to set
     */
    public void setDefaultWidth(final int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    /**
     * Returns the TrueType Collection Name.
     * 
     * @return the TrueType Collection Name
     */
    public String getTTCName() {
        return this.ttcName;
    }

    /**
     * Sets the the TrueType Collection Name.
     * 
     * @param ttcName
     *            the TrueType Collection Name
     */
    public void setTTCName(final String ttcName) {
        this.ttcName = ttcName;
    }

    /**
     * Sets the width array.
     * 
     * @param wds
     *            array of widths.
     */
    public void setWidthArray(final int[] wds) {
        this.width = wds;
    }

    /**
     * Returns a Map of used Glyphs.
     * 
     * @return Map Map of used Glyphs
     */
    public Map getUsedGlyphs() {
        return this.subset.getSubsetGlyphs();
    }

    /** {@inheritDoc} */
    public char[] getCharsUsed() {
        if (!isEmbeddable()) {
            return null;
        }
        return this.subset.getSubsetChars();
    }
}
