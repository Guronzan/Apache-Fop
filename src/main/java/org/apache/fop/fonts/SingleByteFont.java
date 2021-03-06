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

/* $Id: SingleByteFont.java 777459 2009-05-22 10:38:49Z vhennebert $ */

package org.apache.fop.fonts;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Generic SingleByte font
 */
@Slf4j
public class SingleByteFont extends CustomFont<String, String> {

    private SingleByteEncoding mapping;
    private boolean useNativeEncoding = false;

    private int[] width = null;

    private Map unencodedCharacters;
    // Map<Character, UnencodedCharacter>
    private List additionalEncodings;

    /**
     * Main constructor.
     */
    public SingleByteFont() {
        setEncoding(CodePointMapping.WIN_ANSI_ENCODING);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmbeddable() {
        return !(getEmbedFileName() == null && getEmbedResourceName() == null);
    }

    /** {@inheritDoc} */
    @Override
    public String getEncodingName() {
        return this.mapping.getName();
    }

    /**
     * Returns the code point mapping (encoding) of this font.
     *
     * @return the code point mapping
     */
    public SingleByteEncoding getEncoding() {
        return this.mapping;
    }

    /** {@inheritDoc} */
    @Override
    public int getWidth(final int i, final int size) {
        if (i < 256) {
            final int idx = i - getFirstChar();
            if (idx >= 0 && idx < this.width.length) {
                return size * this.width[i - getFirstChar()];
            }
        } else if (this.additionalEncodings != null) {
            final int encodingIndex = i / 256 - 1;
            final SimpleSingleByteEncoding encoding = getAdditionalEncoding(encodingIndex);
            final int codePoint = i % 256;
            final NamedCharacter nc = encoding.getCharacterForIndex(codePoint);
            final UnencodedCharacter uc = (UnencodedCharacter) this.unencodedCharacters
                    .get(new Character(nc.getSingleUnicodeValue()));
            return size * uc.getWidth();
        }
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int[] getWidths() {
        final int[] arr = new int[this.width.length];
        System.arraycopy(this.width, 0, arr, 0, this.width.length);
        return arr;
    }

    /** {@inheritDoc} */
    @Override
    public char mapChar(final char c) {
        notifyMapOperation();
        char d = this.mapping.mapChar(c);
        if (d != SingleByteEncoding.NOT_FOUND_CODE_POINT) {
            return d;
        }

        // Check unencoded characters which are available in the font by
        // character name
        d = mapUnencodedChar(c);
        if (d != SingleByteEncoding.NOT_FOUND_CODE_POINT) {
            return d;
        }
        warnMissingGlyph(c);
        return Typeface.NOT_FOUND;
    }

    private char mapUnencodedChar(final char ch) {
        if (this.unencodedCharacters != null) {
            final UnencodedCharacter unencoded = (UnencodedCharacter) this.unencodedCharacters
                    .get(new Character(ch));
            if (unencoded != null) {
                if (this.additionalEncodings == null) {
                    this.additionalEncodings = new java.util.ArrayList();
                }
                SimpleSingleByteEncoding encoding = null;
                char mappedStart = 0;
                final int additionalsCount = this.additionalEncodings.size();
                for (int i = 0; i < additionalsCount; ++i) {
                    mappedStart += 256;
                    encoding = getAdditionalEncoding(i);
                    final char alt = encoding.mapChar(ch);
                    if (alt != 0) {
                        return (char) (mappedStart + alt);
                    }
                }
                if (encoding != null && encoding.isFull()) {
                    encoding = null;
                }
                if (encoding == null) {
                    encoding = new SimpleSingleByteEncoding(getFontName()
                            + "EncodingSupp" + (additionalsCount + 1));
                    this.additionalEncodings.add(encoding);
                    mappedStart += 256;
                }
                return (char) (mappedStart + encoding.addCharacter(unencoded
                        .getCharacter()));
            }
        }
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChar(final char c) {
        char d = this.mapping.mapChar(c);
        if (d != SingleByteEncoding.NOT_FOUND_CODE_POINT) {
            return true;
        }
        // Check unencoded characters which are available in the font by
        // character name
        d = mapUnencodedChar(c);
        if (d != SingleByteEncoding.NOT_FOUND_CODE_POINT) {
            return true;
        }
        return false;
    }

    /* ---- single byte font specific setters --- */

    /**
     * Updates the mapping variable based on the encoding.
     *
     * @param encoding
     *            the name of the encoding
     */
    protected void updateMapping(final String encoding) {
        try {
            this.mapping = CodePointMapping.getMapping(encoding);
        } catch (final UnsupportedOperationException e) {
            log.error("Font '" + super.getFontName() + "': " + e.getMessage());
        }
    }

    /**
     * Sets the encoding of the font.
     *
     * @param encoding
     *            the encoding (ex. "WinAnsiEncoding" or "SymbolEncoding")
     */
    public void setEncoding(final String encoding) {
        updateMapping(encoding);
    }

    /**
     * Sets the encoding of the font.
     *
     * @param encoding
     *            the encoding information
     */
    public void setEncoding(final CodePointMapping encoding) {
        this.mapping = encoding;
    }

    /**
     * Controls whether the font is configured to use its native encoding or if
     * it may need to be re-encoded for the target format.
     *
     * @param value
     *            true indicates that the configured encoding is the font's
     *            native encoding
     */
    public void setUseNativeEncoding(final boolean value) {
        this.useNativeEncoding = value;
    }

    /**
     * Indicates whether this font is configured to use its native encoding.
     * This method is used to determine whether the font needs to be re-encoded.
     *
     * @return true if the font uses its native encoding.
     */
    public boolean isUsingNativeEncoding() {
        return this.useNativeEncoding;
    }

    /**
     * Sets a width for a character.
     *
     * @param index
     *            index of the character
     * @param w
     *            the width of the character
     */
    public void setWidth(final int index, final int w) {
        if (this.width == null) {
            this.width = new int[getLastChar() - getFirstChar() + 1];
        }
        this.width[index - getFirstChar()] = w;
    }

    /**
     * Adds an unencoded character (one that is not supported by the primary
     * encoding).
     *
     * @param ch
     *            the named character
     * @param width
     *            the width of the character
     */
    public void addUnencodedCharacter(final NamedCharacter ch, final int width) {
        if (this.unencodedCharacters == null) {
            this.unencodedCharacters = new java.util.HashMap();
        }
        if (ch.hasSingleUnicodeValue()) {
            final UnencodedCharacter uc = new UnencodedCharacter(ch, width);
            this.unencodedCharacters.put(
                    new Character(ch.getSingleUnicodeValue()), uc);
        } else {
            // Cannot deal with unicode sequences, so ignore this character
        }
    }

    /**
     * Makes all unencoded characters available through additional encodings.
     * This method is used in cases where the fonts need to be encoded in the
     * target format before all text of the document is processed (for example
     * in PostScript when resource optimization is disabled).
     */
    public void encodeAllUnencodedCharacters() {
        if (this.unencodedCharacters != null) {
            final Set sortedKeys = new java.util.TreeSet(
                    this.unencodedCharacters.keySet());
            final Iterator iter = sortedKeys.iterator();
            while (iter.hasNext()) {
                final Character ch = (Character) iter.next();
                final char mapped = mapChar(ch.charValue());
                assert mapped != Typeface.NOT_FOUND;
            }
        }
    }

    /**
     * Indicates whether the encoding has additional encodings besides the
     * primary encoding.
     *
     * @return true if there are additional encodings.
     */
    public boolean hasAdditionalEncodings() {
        return this.additionalEncodings != null
                && this.additionalEncodings.size() > 0;
    }

    /**
     * Returns the number of additional encodings this single-byte font
     * maintains.
     *
     * @return the number of additional encodings
     */
    public int getAdditionalEncodingCount() {
        if (hasAdditionalEncodings()) {
            return this.additionalEncodings.size();
        } else {
            return 0;
        }
    }

    /**
     * Returns an additional encoding.
     *
     * @param index
     *            the index of the additional encoding
     * @return the additional encoding
     * @throws IndexOutOfBoundsException
     *             if the index is out of bounds
     */
    public SimpleSingleByteEncoding getAdditionalEncoding(final int index)
            throws IndexOutOfBoundsException {
        if (hasAdditionalEncodings()) {
            return (SimpleSingleByteEncoding) this.additionalEncodings
                    .get(index);
        } else {
            throw new IndexOutOfBoundsException(
                    "No additional encodings available");
        }
    }

    /**
     * Returns an array with the widths for an additional encoding.
     *
     * @param index
     *            the index of the additional encoding
     * @return the width array
     */
    public int[] getAdditionalWidths(final int index) {
        final SimpleSingleByteEncoding enc = getAdditionalEncoding(index);
        final int[] arr = new int[enc.getLastChar() - enc.getFirstChar() + 1];
        for (int i = 0, c = arr.length; i < c; ++i) {
            final NamedCharacter nc = enc.getCharacterForIndex(enc
                    .getFirstChar() + i);
            final UnencodedCharacter uc = (UnencodedCharacter) this.unencodedCharacters
                    .get(new Character(nc.getSingleUnicodeValue()));
            arr[i] = uc.getWidth();
        }
        return arr;
    }

    private static final class UnencodedCharacter {

        private final NamedCharacter character;
        private final int width;

        public UnencodedCharacter(final NamedCharacter character,
                final int width) {
            this.character = character;
            this.width = width;
        }

        public NamedCharacter getCharacter() {
            return this.character;
        }

        public int getWidth() {
            return this.width;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return getCharacter().toString();
        }
    }

}
