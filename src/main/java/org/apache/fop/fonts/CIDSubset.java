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

/* $Id: CIDSubset.java 828516 2009-10-22 09:16:37Z jeremias $ */

package org.apache.fop.fonts;

import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.fop.util.CharUtilities;

//Naming:
//glyph index: original index of the glyph in the non-subset font (!= unicode index)
//character selector: index into a set of glyphs. For subset CID fonts, this starts at 0. For
//  non-subset fonts, this is the same as the glyph index.
//Unicode index: The Unicode codepoint of a character.
//Glyph name: the Adobe glyph name (as found in Glyphs.java)

/**
 * Keeps track of the glyphs used in a document. This information is later used
 * to build a subset of a font.
 */
public class CIDSubset {

    /**
     * usedGlyphs contains orginal, new glyph index (glyph index -> char
     * selector)
     */
    private final Map<Integer, Integer> usedGlyphs = new java.util.HashMap<>();

    /**
     * usedGlyphsIndex contains new glyph, original index (char selector ->
     * glyph index)
     */
    private final Map<Integer, Integer> usedGlyphsIndex = new java.util.HashMap<>();
    private int usedGlyphsCount = 0;

    /**
     * usedCharsIndex contains new glyph, original char (char selector ->
     * Unicode)
     */
    private final Map<Integer, Character> usedCharsIndex = new java.util.HashMap<>();

    public CIDSubset() {
    }

    /**
     * Adds the initial 3 glyphs which are the same for all CID subsets.
     */
    public void setupFirstThreeGlyphs() {
        // Make sure that the 3 first glyphs are included
        this.usedGlyphs.put(0, 0);
        this.usedGlyphsIndex.put(0, 0);
        this.usedGlyphsCount++;
        this.usedGlyphs.put(1, 1);
        this.usedGlyphsIndex.put(1, 1);
        this.usedGlyphsCount++;
        this.usedGlyphs.put(2, 2);
        this.usedGlyphsIndex.put(2, 2);
        this.usedGlyphsCount++;
    }

    /**
     * Returns the original index of the glyph inside the (non-subset) font's
     * glyph list. This index can be used to access the character width
     * information, for example.
     *
     * @param subsetIndex
     *            the subset index (character selector) to access the glyph
     * @return the original index (or -1 if no glyph index is available for the
     *         subset index)
     */
    public int getGlyphIndexForSubsetIndex(final int subsetIndex) {
        final Integer glyphIndex = this.usedGlyphsIndex.get(subsetIndex);
        if (glyphIndex != null) {
            return glyphIndex.intValue();
        } else {
            return -1;
        }
    }

    /**
     * Returns the Unicode value for a subset index (character selector). If
     * there's no such Unicode value, the "NOT A CHARACTER" (0xFFFF) is
     * returned.
     *
     * @param subsetIndex
     *            the subset index (character selector)
     * @return the Unicode value or "NOT A CHARACTER" (0xFFFF)
     */
    public char getUnicodeForSubsetIndex(final int subsetIndex) {
        final Character mapValue = this.usedCharsIndex.get(subsetIndex);
        if (mapValue != null) {
            return mapValue.charValue();
        } else {
            return CharUtilities.NOT_A_CHARACTER;
        }
    }

    /**
     * Maps a character to a character selector for a font subset. If the
     * character isn't in the subset, yet, it is added and a new character
     * selector returned. Otherwise, the already allocated character selector is
     * returned from the existing map/subset.
     *
     * @param glyphIndex
     *            the glyph index of the character
     * @param unicode
     *            the Unicode index of the character
     * @return the subset index
     */
    public int mapSubsetChar(final int glyphIndex, final char unicode) {
        // Reencode to a new subset font or get the reencoded value
        // IOW, accumulate the accessed characters and build a character map for
        // them
        final Integer subsetCharSelector = this.usedGlyphs.get(glyphIndex);
        if (subsetCharSelector == null) {
            final int selector = this.usedGlyphsCount;
            this.usedGlyphs.put(glyphIndex, selector);
            this.usedGlyphsIndex.put(selector, glyphIndex);
            this.usedCharsIndex.put(selector, Character.valueOf(unicode));
            this.usedGlyphsCount++;
            return selector;
        } else {
            return subsetCharSelector.intValue();
        }
    }

    /**
     * Returns an unmodifiable Map of the font subset. It maps from glyph index
     * to character selector (i.e. the subset index in this case).
     *
     * @return Map Map&lt;Integer, Integer&gt; of the font subset
     */
    public Map<Integer, Integer> getSubsetGlyphs() {
        return Collections.unmodifiableMap(this.usedGlyphs);
    }

    /**
     * Returns a char array containing all Unicode characters that are in the
     * subset.
     *
     * @return a char array with all used Unicode characters
     */
    public char[] getSubsetChars() {
        final char[] charArray = new char[this.usedGlyphsCount];
        for (int i = 0; i < this.usedGlyphsCount; ++i) {
            charArray[i] = getUnicodeForSubsetIndex(i);
        }
        return charArray;
    }

    /**
     * Returns the number of glyphs in the subset.
     *
     * @return the number of glyphs in the subset
     */
    public int getSubsetSize() {
        return this.usedGlyphsCount;
    }

    /**
     * Returns a BitSet with bits set for each available glyph index in the
     * subset.
     *
     * @return a BitSet indicating available glyph indices
     */
    public BitSet getGlyphIndexBitSet() {
        final BitSet bitset = new BitSet();
        final Iterator<Integer> iter = this.usedGlyphsIndex.keySet().iterator();
        while (iter.hasNext()) {
            final Integer cid = iter.next();
            bitset.set(cid.intValue());
        }
        return bitset;
    }

}
