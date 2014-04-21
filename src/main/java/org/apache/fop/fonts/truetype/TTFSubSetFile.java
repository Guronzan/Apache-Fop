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

/* $Id: TTFSubSetFile.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fonts.truetype;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

/**
 * Reads a TrueType file and generates a subset that can be used to embed a
 * TrueType CID font. TrueType tables needed for embedded CID fonts are: "head",
 * "hhea", "loca", "maxp", "cvt ", "prep", "glyf", "hmtx" and "fpgm". The
 * TrueType spec can be found at the Microsoft Typography site:
 * http://www.microsoft.com/truetype/
 */
@Slf4j
public class TTFSubSetFile extends TTFFile {

    private byte[] output = null;
    private int realSize = 0;
    private int currentPos = 0;

    /*
     * Offsets in name table to be filled out by table. The offsets are to the
     * checkSum field
     */
    private int cvtDirOffset = 0;
    private int fpgmDirOffset = 0;
    private int glyfDirOffset = 0;
    private int headDirOffset = 0;
    private int hheaDirOffset = 0;
    private int hmtxDirOffset = 0;
    private int locaDirOffset = 0;
    private int maxpDirOffset = 0;
    private int prepDirOffset = 0;

    private int checkSumAdjustmentOffset = 0;
    private int locaOffset = 0;

    /**
     * Initalize the output array
     */
    private void init(final int size) {
        this.output = new byte[size];
        this.realSize = 0;
        this.currentPos = 0;

        // createDirectory()
    }

    private int determineTableCount() {
        int numTables = 4; // 4 req'd tables: head,hhea,hmtx,maxp
        if (isCFF()) {
            throw new UnsupportedOperationException(
                    "OpenType fonts with CFF glyphs are not supported");
        } else {
            numTables += 2; // 1 req'd table: glyf,loca
            if (hasCvt()) {
                numTables++;
            }
            if (hasFpgm()) {
                numTables++;
            }
            if (hasPrep()) {
                numTables++;
            }
        }
        return numTables;
    }

    /**
     * Create the directory table
     */
    private void createDirectory() {
        final int numTables = determineTableCount();
        // Create the TrueType header
        writeByte((byte) 0);
        writeByte((byte) 1);
        writeByte((byte) 0);
        writeByte((byte) 0);
        this.realSize += 4;

        writeUShort(numTables);
        this.realSize += 2;

        // Create searchRange, entrySelector and rangeShift
        final int maxPow = maxPow2(numTables);
        final int searchRange = maxPow * 16;
        writeUShort(searchRange);
        this.realSize += 2;

        writeUShort(maxPow);
        this.realSize += 2;

        writeUShort(numTables * 16 - searchRange);
        this.realSize += 2;

        // Create space for the table entries
        if (hasCvt()) {
            writeString("cvt ");
            this.cvtDirOffset = this.currentPos;
            this.currentPos += 12;
            this.realSize += 16;
        }

        if (hasFpgm()) {
            writeString("fpgm");
            this.fpgmDirOffset = this.currentPos;
            this.currentPos += 12;
            this.realSize += 16;
        }

        writeString("glyf");
        this.glyfDirOffset = this.currentPos;
        this.currentPos += 12;
        this.realSize += 16;

        writeString("head");
        this.headDirOffset = this.currentPos;
        this.currentPos += 12;
        this.realSize += 16;

        writeString("hhea");
        this.hheaDirOffset = this.currentPos;
        this.currentPos += 12;
        this.realSize += 16;

        writeString("hmtx");
        this.hmtxDirOffset = this.currentPos;
        this.currentPos += 12;
        this.realSize += 16;

        writeString("loca");
        this.locaDirOffset = this.currentPos;
        this.currentPos += 12;
        this.realSize += 16;

        writeString("maxp");
        this.maxpDirOffset = this.currentPos;
        this.currentPos += 12;
        this.realSize += 16;

        if (hasPrep()) {
            writeString("prep");
            this.prepDirOffset = this.currentPos;
            this.currentPos += 12;
            this.realSize += 16;
        }
    }

    /**
     * Copy the cvt table as is from original font to subset font
     */
    private boolean createCvt(final FontFileReader in) throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get("cvt ");
        if (entry != null) {
            pad4();
            seekTab(in, "cvt ", 0);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(),
                            (int) entry.getLength()), 0, this.output,
                    this.currentPos, (int) entry.getLength());

            final int checksum = getCheckSum(this.currentPos,
                    (int) entry.getLength());
            writeULong(this.cvtDirOffset, checksum);
            writeULong(this.cvtDirOffset + 4, this.currentPos);
            writeULong(this.cvtDirOffset + 8, (int) entry.getLength());
            this.currentPos += (int) entry.getLength();
            this.realSize += (int) entry.getLength();
            return true;
        } else {
            return false;
            // throw new IOException("Can't find cvt table");
        }
    }

    private boolean hasCvt() {
        return this.dirTabs.containsKey("cvt ");
    }

    private boolean hasFpgm() {
        return this.dirTabs.containsKey("fpgm");
    }

    private boolean hasPrep() {
        return this.dirTabs.containsKey("prep");
    }

    /**
     * Copy the fpgm table as is from original font to subset font
     */
    private boolean createFpgm(final FontFileReader in) throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get("fpgm");
        if (entry != null) {
            pad4();
            seekTab(in, "fpgm", 0);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(),
                            (int) entry.getLength()), 0, this.output,
                    this.currentPos, (int) entry.getLength());
            final int checksum = getCheckSum(this.currentPos,
                    (int) entry.getLength());
            writeULong(this.fpgmDirOffset, checksum);
            writeULong(this.fpgmDirOffset + 4, this.currentPos);
            writeULong(this.fpgmDirOffset + 8, (int) entry.getLength());
            this.currentPos += (int) entry.getLength();
            this.realSize += (int) entry.getLength();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create an empty loca table without updating checksum
     */
    private void createLoca(final int size) {
        pad4();
        this.locaOffset = this.currentPos;
        writeULong(this.locaDirOffset + 4, this.currentPos);
        writeULong(this.locaDirOffset + 8, size * 4 + 4);
        this.currentPos += size * 4 + 4;
        this.realSize += size * 4 + 4;
    }

    /**
     * Copy the maxp table as is from original font to subset font and set num
     * glyphs to size
     */
    private void createMaxp(final FontFileReader in, final int size)
            throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get("maxp");
        if (entry != null) {
            pad4();
            seekTab(in, "maxp", 0);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(),
                            (int) entry.getLength()), 0, this.output,
                    this.currentPos, (int) entry.getLength());
            writeUShort(this.currentPos + 4, size);

            final int checksum = getCheckSum(this.currentPos,
                    (int) entry.getLength());
            writeULong(this.maxpDirOffset, checksum);
            writeULong(this.maxpDirOffset + 4, this.currentPos);
            writeULong(this.maxpDirOffset + 8, (int) entry.getLength());
            this.currentPos += (int) entry.getLength();
            this.realSize += (int) entry.getLength();
        } else {
            throw new IOException("Can't find maxp table");
        }
    }

    /**
     * Copy the prep table as is from original font to subset font
     */
    private boolean createPrep(final FontFileReader in) throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get("prep");
        if (entry != null) {
            pad4();
            seekTab(in, "prep", 0);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(),
                            (int) entry.getLength()), 0, this.output,
                    this.currentPos, (int) entry.getLength());

            final int checksum = getCheckSum(this.currentPos,
                    (int) entry.getLength());
            writeULong(this.prepDirOffset, checksum);
            writeULong(this.prepDirOffset + 4, this.currentPos);
            writeULong(this.prepDirOffset + 8, (int) entry.getLength());
            this.currentPos += (int) entry.getLength();
            this.realSize += (int) entry.getLength();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Copy the hhea table as is from original font to subset font and fill in
     * size of hmtx table
     */
    private void createHhea(final FontFileReader in, final int size)
            throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get("hhea");
        if (entry != null) {
            pad4();
            seekTab(in, "hhea", 0);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(),
                            (int) entry.getLength()), 0, this.output,
                    this.currentPos, (int) entry.getLength());
            writeUShort((int) entry.getLength() + this.currentPos - 2, size);

            final int checksum = getCheckSum(this.currentPos,
                    (int) entry.getLength());
            writeULong(this.hheaDirOffset, checksum);
            writeULong(this.hheaDirOffset + 4, this.currentPos);
            writeULong(this.hheaDirOffset + 8, (int) entry.getLength());
            this.currentPos += (int) entry.getLength();
            this.realSize += (int) entry.getLength();
        } else {
            throw new IOException("Can't find hhea table");
        }
    }

    /**
     * Copy the head table as is from original font to subset font and set
     * indexToLocaFormat to long and set checkSumAdjustment to 0, store offset
     * to checkSumAdjustment in checkSumAdjustmentOffset
     */
    private void createHead(final FontFileReader in) throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get("head");
        if (entry != null) {
            pad4();
            seekTab(in, "head", 0);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(),
                            (int) entry.getLength()), 0, this.output,
                    this.currentPos, (int) entry.getLength());

            this.checkSumAdjustmentOffset = this.currentPos + 8;
            this.output[this.currentPos + 8] = 0; // Set checkSumAdjustment to 0
            this.output[this.currentPos + 9] = 0;
            this.output[this.currentPos + 10] = 0;
            this.output[this.currentPos + 11] = 0;
            this.output[this.currentPos + 50] = 0; // long locaformat
            this.output[this.currentPos + 51] = 1; // long locaformat

            final int checksum = getCheckSum(this.currentPos,
                    (int) entry.getLength());
            writeULong(this.headDirOffset, checksum);
            writeULong(this.headDirOffset + 4, this.currentPos);
            writeULong(this.headDirOffset + 8, (int) entry.getLength());

            this.currentPos += (int) entry.getLength();
            this.realSize += (int) entry.getLength();
        } else {
            throw new IOException("Can't find head table");
        }
    }

    /**
     * Create the glyf table and fill in loca table
     */
    private void createGlyf(final FontFileReader in,
            final Map<Integer, Integer> glyphs) throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get("glyf");
        int size = 0;
        int start = 0;
        int endOffset = 0; // Store this as the last loca
        if (entry != null) {
            pad4();
            start = this.currentPos;

            /*
             * Loca table must be in order by glyph index, so build an array
             * first and then write the glyph info and location offset.
             */
            final int[] origIndexes = new int[glyphs.size()];

            for (final Entry<Integer, Integer> subEntry : glyphs.entrySet()) {
                final Integer origIndex = subEntry.getKey();
                final Integer subsetIndex = subEntry.getValue();
                origIndexes[subsetIndex.intValue()] = origIndex.intValue();
            }

            for (int i = 0; i < origIndexes.length; ++i) {
                int glyphLength = 0;
                int nextOffset = 0;
                final int origGlyphIndex = origIndexes[i];
                if (origGlyphIndex >= this.mtxTab.length - 1) {
                    nextOffset = (int) this.lastLoca;
                } else {
                    nextOffset = (int) this.mtxTab[origGlyphIndex + 1]
                            .getOffset();
                }
                glyphLength = nextOffset
                        - (int) this.mtxTab[origGlyphIndex].getOffset();

                // Copy glyph
                System.arraycopy(in.getBytes((int) entry.getOffset()
                        + (int) this.mtxTab[origGlyphIndex].getOffset(),
                        glyphLength), 0, this.output, this.currentPos,
                        glyphLength);

                // Update loca table
                writeULong(this.locaOffset + i * 4, this.currentPos - start);
                if (this.currentPos - start + glyphLength > endOffset) {
                    endOffset = this.currentPos - start + glyphLength;
                }

                this.currentPos += glyphLength;
                this.realSize += glyphLength;

            }

            size = this.currentPos - start;

            int checksum = getCheckSum(start, size);
            writeULong(this.glyfDirOffset, checksum);
            writeULong(this.glyfDirOffset + 4, start);
            writeULong(this.glyfDirOffset + 8, size);
            this.currentPos += 12;
            this.realSize += 12;

            // Update loca checksum and last loca index
            writeULong(this.locaOffset + glyphs.size() * 4, endOffset);

            checksum = getCheckSum(this.locaOffset, glyphs.size() * 4 + 4);
            writeULong(this.locaDirOffset, checksum);
        } else {
            throw new IOException("Can't find glyf table");
        }
    }

    /**
     * Create the hmtx table by copying metrics from original font to subset
     * font. The glyphs Map contains an Integer key and Integer value that maps
     * the original metric (key) to the subset metric (value)
     */
    private void createHmtx(final FontFileReader in,
            final Map<Integer, Integer> glyphs) throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get("hmtx");

        final int longHorMetricSize = glyphs.size() * 2;
        final int leftSideBearingSize = glyphs.size() * 2;
        final int hmtxSize = longHorMetricSize + leftSideBearingSize;

        if (entry != null) {
            pad4();
            for (final Entry<Integer, Integer> subEntry : glyphs.entrySet()) {
                final Integer origIndex = subEntry.getKey();
                final Integer subsetIndex = subEntry.getValue();

                writeUShort(this.currentPos + subsetIndex.intValue() * 4,
                        this.mtxTab[origIndex.intValue()].getWx());
                writeUShort(this.currentPos + subsetIndex.intValue() * 4 + 2,
                        this.mtxTab[origIndex.intValue()].getLsb());
            }

            final int checksum = getCheckSum(this.currentPos, hmtxSize);
            writeULong(this.hmtxDirOffset, checksum);
            writeULong(this.hmtxDirOffset + 4, this.currentPos);
            writeULong(this.hmtxDirOffset + 8, hmtxSize);
            this.currentPos += hmtxSize;
            this.realSize += hmtxSize;
        } else {
            throw new IOException("Can't find hmtx table");
        }
    }

    /**
     * Returns a List containing the glyph itself plus all glyphs that this
     * composite glyph uses
     */
    private List<Integer> getIncludedGlyphs(final FontFileReader in,
            final int glyphOffset, final Integer glyphIdx) throws IOException {
        final List<Integer> ret = new ArrayList<>();
        ret.add(glyphIdx);
        int offset = glyphOffset
                + (int) this.mtxTab[glyphIdx.intValue()].getOffset() + 10;
        Integer compositeIdx = null;
        int flags = 0;
        boolean moreComposites = true;
        while (moreComposites) {
            flags = in.readTTFUShort(offset);
            compositeIdx = in.readTTFUShort(offset + 2);
            ret.add(compositeIdx);

            offset += 4;
            if ((flags & 1) > 0) {
                // ARG_1_AND_ARG_2_ARE_WORDS
                offset += 4;
            } else {
                offset += 2;
            }

            if ((flags & 8) > 0) {
                offset += 2; // WE_HAVE_A_SCALE
            } else if ((flags & 64) > 0) {
                offset += 4; // WE_HAVE_AN_X_AND_Y_SCALE
            } else if ((flags & 128) > 0) {
                offset += 8; // WE_HAVE_A_TWO_BY_TWO
            }

            if ((flags & 32) > 0) {
                moreComposites = true;
            } else {
                moreComposites = false;
            }
        }

        return ret;
    }

    /**
     * Rewrite all compositepointers in glyphindex glyphIdx
     *
     */
    private void remapComposite(final FontFileReader in,
            final Map<Integer, Integer> glyphs, final int glyphOffset,
            final Integer glyphIdx) throws IOException {
        int offset = glyphOffset
                + (int) this.mtxTab[glyphIdx.intValue()].getOffset() + 10;

        Integer compositeIdx = null;
        int flags = 0;
        boolean moreComposites = true;

        while (moreComposites) {
            flags = in.readTTFUShort(offset);
            compositeIdx = in.readTTFUShort(offset + 2);
            final Integer newIdx = glyphs.get(compositeIdx);
            if (newIdx == null) {
                // This errormessage would look much better
                // if the fontname was printed to
                // log.error("An embedded font "
                // + "contains bad glyph data. "
                // + "Characters might not display "
                // + "correctly.");
                moreComposites = false;
                continue;
            }

            in.writeTTFUShort(offset + 2, newIdx.intValue());

            offset += 4;

            if ((flags & 1) > 0) {
                // ARG_1_AND_ARG_2_ARE_WORDS
                offset += 4;
            } else {
                offset += 2;
            }

            if ((flags & 8) > 0) {
                offset += 2; // WE_HAVE_A_SCALE
            } else if ((flags & 64) > 0) {
                offset += 4; // WE_HAVE_AN_X_AND_Y_SCALE
            } else if ((flags & 128) > 0) {
                offset += 8; // WE_HAVE_A_TWO_BY_TWO
            }

            if ((flags & 32) > 0) {
                moreComposites = true;
            } else {
                moreComposites = false;
            }
        }
    }

    /**
     * Scan all the original glyphs for composite glyphs and add those glyphs to
     * the glyphmapping also rewrite the composite glyph pointers to the new
     * mapping
     */
    private void scanGlyphs(final FontFileReader in,
            final Map<Integer, Integer> glyphs) throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get("glyf");
        Map<Integer, Integer> newComposites = null;
        final Map<Integer, Integer> allComposites = new HashMap<>();

        int newIndex = glyphs.size();

        if (entry != null) {
            while (newComposites == null || !newComposites.isEmpty()) {
                // Inefficient to iterate through all glyphs
                newComposites = new HashMap<>();

                for (final Entry<Integer, Integer> subEntry : glyphs.entrySet()) {
                    final Integer origIndex = subEntry.getKey();
                    if (in.readTTFShort(entry.getOffset()
                            + this.mtxTab[origIndex.intValue()].getOffset()) < 0) {
                        // origIndex is a composite glyph
                        allComposites.put(origIndex, subEntry.getValue());
                        final List<Integer> composites = getIncludedGlyphs(in,
                                (int) entry.getOffset(), origIndex);

                        // Iterate through all composites pointed to
                        // by this composite and check if they exists
                        // in the glyphs map, add them if not.
                        for (final Integer cIdx : composites) {
                            if (glyphs.get(cIdx) == null
                                    && newComposites.get(cIdx) == null) {
                                newComposites.put(cIdx, newIndex);
                                ++newIndex;
                            }
                        }
                    }
                }

                // Add composites to glyphs
                for (final Entry<Integer, Integer> subEntry : newComposites
                        .entrySet()) {
                    glyphs.put(subEntry.getKey(), subEntry.getValue());
                }
            }

            // Iterate through all composites to remap their composite index
            for (final Integer key : allComposites.keySet()) {
                remapComposite(in, glyphs, (int) entry.getOffset(), key);
            }

        } else {
            throw new IOException("Can't find glyf table");
        }
    }

    /**
     * Returns a subset of the original font.
     *
     * @param in
     *            FontFileReader to read from
     * @param name
     *            Name to be checked for in the font file
     * @param glyphs
     *            Map of glyphs (glyphs has old index as (Integer) key and new
     *            index as (Integer) value)
     * @return A subset of the original font
     * @throws IOException
     *             in case of an I/O problem
     */
    public byte[] readFont(final FontFileReader in, final String name,
            final Map<Integer, Integer> glyphs) throws IOException {

        // Check if TrueType collection, and that the name exists in the
        // collection
        if (!checkTTC(in, name)) {
            throw new IOException("Failed to read font");
        }

        // Copy the Map as we're going to modify it
        final Map<Integer, Integer> subsetGlyphs = new HashMap<>(glyphs);

        this.output = new byte[in.getFileSize()];

        readDirTabs(in);
        readFontHeader(in);
        getNumGlyphs(in);
        readHorizontalHeader(in);
        readHorizontalMetrics(in);
        readIndexToLocation(in);

        scanGlyphs(in, subsetGlyphs);

        createDirectory(); // Create the TrueType header and directory

        createHead(in);
        createHhea(in, subsetGlyphs.size()); // Create the hhea table
        createHmtx(in, subsetGlyphs); // Create hmtx table
        createMaxp(in, subsetGlyphs.size()); // copy the maxp table

        boolean optionalTableFound;
        optionalTableFound = createCvt(in); // copy the cvt table
        if (!optionalTableFound) {
            // cvt is optional (used in TrueType fonts only)
            log.debug("TrueType: ctv table not present. Skipped.");
        }

        optionalTableFound = createFpgm(in); // copy fpgm table
        if (!optionalTableFound) {
            // fpgm is optional (used in TrueType fonts only)
            log.debug("TrueType: fpgm table not present. Skipped.");
        }

        optionalTableFound = createPrep(in); // copy prep table
        if (!optionalTableFound) {
            // prep is optional (used in TrueType fonts only)
            log.debug("TrueType: prep table not present. Skipped.");
        }

        createLoca(subsetGlyphs.size()); // create empty loca table
        createGlyf(in, subsetGlyphs); // create glyf table and update loca table

        pad4();
        createCheckSumAdjustment();

        final byte[] ret = new byte[this.realSize];
        System.arraycopy(this.output, 0, ret, 0, this.realSize);

        return ret;
    }

    /**
     * writes a ISO-8859-1 string at the currentPosition updates currentPosition
     * but not realSize
     *
     * @return number of bytes written
     */
    private int writeString(final String str) {
        int length = 0;
        try {
            final byte[] buf = str.getBytes("ISO-8859-1");
            System.arraycopy(buf, 0, this.output, this.currentPos, buf.length);
            length = buf.length;
            this.currentPos += length;
        } catch (final java.io.UnsupportedEncodingException e) {
            // This should never happen!
        }

        return length;
    }

    /**
     * Appends a byte to the output array, updates currentPost but not realSize
     */
    private void writeByte(final byte b) {
        this.output[this.currentPos++] = b;
    }

    /**
     * Appends a USHORT to the output array, updates currentPost but not
     * realSize
     */
    private void writeUShort(final int s) {
        final byte b1 = (byte) (s >> 8 & 0xff);
        final byte b2 = (byte) (s & 0xff);
        writeByte(b1);
        writeByte(b2);
    }

    /**
     * Appends a USHORT to the output array, at the given position without
     * changing currentPos
     */
    private void writeUShort(final int pos, final int s) {
        final byte b1 = (byte) (s >> 8 & 0xff);
        final byte b2 = (byte) (s & 0xff);
        this.output[pos] = b1;
        this.output[pos + 1] = b2;
    }

    /**
     * Appends a ULONG to the output array, updates currentPos but not realSize
     */
    private void writeULong(final int s) {
        final byte b1 = (byte) (s >> 24 & 0xff);
        final byte b2 = (byte) (s >> 16 & 0xff);
        final byte b3 = (byte) (s >> 8 & 0xff);
        final byte b4 = (byte) (s & 0xff);
        writeByte(b1);
        writeByte(b2);
        writeByte(b3);
        writeByte(b4);
    }

    /**
     * Appends a ULONG to the output array, at the given position without
     * changing currentPos
     */
    private void writeULong(final int pos, final int s) {
        final byte b1 = (byte) (s >> 24 & 0xff);
        final byte b2 = (byte) (s >> 16 & 0xff);
        final byte b3 = (byte) (s >> 8 & 0xff);
        final byte b4 = (byte) (s & 0xff);
        this.output[pos] = b1;
        this.output[pos + 1] = b2;
        this.output[pos + 2] = b3;
        this.output[pos + 3] = b4;
    }

    /**
     * Read a signed short value at given position
     */
    private short readShort(final int pos) {
        final int ret = readUShort(pos);
        return (short) ret;
    }

    /**
     * Read a unsigned short value at given position
     */
    private int readUShort(final int pos) {
        int ret = this.output[pos];
        if (ret < 0) {
            ret += 256;
        }
        ret = ret << 8;
        if (this.output[pos + 1] < 0) {
            ret |= this.output[pos + 1] + 256;
        } else {
            ret |= this.output[pos + 1];
        }

        return ret;
    }

    /**
     * Create a padding in the fontfile to align on a 4-byte boundary
     */
    private void pad4() {
        final int padSize = this.currentPos % 4;
        for (int i = 0; i < padSize; ++i) {
            this.output[this.currentPos++] = 0;
            this.realSize++;
        }
    }

    /**
     * Returns the maximum power of 2 <= max
     */
    private int maxPow2(final int max) {
        int i = 0;
        while (Math.pow(2, i) < max) {
            ++i;
        }

        return i - 1;
    }

    private int log2(final int num) {
        return (int) (Math.log(num) / Math.log(2));
    }

    private int getCheckSum(final int start, final int size) {
        return (int) getLongCheckSum(start, size);
    }

    private long getLongCheckSum(final int start, int size) {
        // All the tables here are aligned on four byte boundaries
        // Add remainder to size if it's not a multiple of 4
        final int remainder = size % 4;
        if (remainder != 0) {
            size += remainder;
        }

        long sum = 0;

        for (int i = 0; i < size; i += 4) {
            int l = this.output[start + i] << 24;
            l += this.output[start + i + 1] << 16;
            l += this.output[start + i + 2] << 16;
            l += this.output[start + i + 3] << 16;
            sum += l;
            if (sum > 0xffffffff) {
                sum = sum - 0xffffffff;
            }
        }

        return sum;
    }

    private void createCheckSumAdjustment() {
        final long sum = getLongCheckSum(0, this.realSize);
        final int checksum = (int) (0xb1b0afba - sum);
        writeULong(this.checkSumAdjustmentOffset, checksum);
    }

}
