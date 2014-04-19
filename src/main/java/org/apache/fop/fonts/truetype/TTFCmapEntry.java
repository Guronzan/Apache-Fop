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

/* $Id: TTFCmapEntry.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fonts.truetype;

/**
 * The CMap entry contains information of a Unicode range and the the glyph
 * indexes related to the range
 */
public class TTFCmapEntry {

    private int unicodeStart;
    private int unicodeEnd;
    private int glyphStartIndex;

    TTFCmapEntry() {
        this.unicodeStart = 0;
        this.unicodeEnd = 0;
        this.glyphStartIndex = 0;
    }

    TTFCmapEntry(final int unicodeStart, final int unicodeEnd,
            final int glyphStartIndex) {
        this.unicodeStart = unicodeStart;
        this.unicodeEnd = unicodeEnd;
        this.glyphStartIndex = glyphStartIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof TTFCmapEntry) {
            final TTFCmapEntry ce = (TTFCmapEntry) o;
            if (ce.unicodeStart == this.unicodeStart
                    && ce.unicodeEnd == this.unicodeEnd
                    && ce.glyphStartIndex == this.glyphStartIndex) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the glyphStartIndex.
     * 
     * @return int
     */
    public int getGlyphStartIndex() {
        return this.glyphStartIndex;
    }

    /**
     * Returns the unicodeEnd.
     * 
     * @return int
     */
    public int getUnicodeEnd() {
        return this.unicodeEnd;
    }

    /**
     * Returns the unicodeStart.
     * 
     * @return int
     */
    public int getUnicodeStart() {
        return this.unicodeStart;
    }

    /**
     * Sets the glyphStartIndex.
     * 
     * @param glyphStartIndex
     *            The glyphStartIndex to set
     */
    public void setGlyphStartIndex(final int glyphStartIndex) {
        this.glyphStartIndex = glyphStartIndex;
    }

    /**
     * Sets the unicodeEnd.
     * 
     * @param unicodeEnd
     *            The unicodeEnd to set
     */
    public void setUnicodeEnd(final int unicodeEnd) {
        this.unicodeEnd = unicodeEnd;
    }

    /**
     * Sets the unicodeStart.
     * 
     * @param unicodeStart
     *            The unicodeStart to set
     */
    public void setUnicodeStart(final int unicodeStart) {
        this.unicodeStart = unicodeStart;
    }

}
