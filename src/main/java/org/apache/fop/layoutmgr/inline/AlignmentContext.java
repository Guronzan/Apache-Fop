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

/* $Id: AlignmentContext.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.layoutmgr.inline;

import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.datatypes.SimplePercentBaseContext;
import org.apache.fop.fo.Constants;
import org.apache.fop.fonts.Font;

/**
 * The alignment context is carried within a LayoutContext and as part of the
 * Knuth Inline elements to facilitate proper line building. All measurements
 * are in mpt.
 */
public class AlignmentContext implements Constants {

    /** The height or BPD of this context */
    private int areaHeight;
    /** The computed line-height property value applicable */
    private final int lineHeight;
    /** The distance in BPD from the top of the box to the alignmentPoint */
    private int alignmentPoint;
    /** The baseline shift value in effect */
    private int baselineShiftValue;
    /** The computed alignment baseline identifier */
    private int alignmentBaselineIdentifier;
    /** The x height */
    private final int xHeight;
    private ScaledBaselineTable scaledBaselineTable = null;
    private ScaledBaselineTable actualBaselineTable = null;
    private AlignmentContext parentAlignmentContext = null;

    /**
     * Creates a new instance of AlignmentContext for graphics areas.
     * 
     * @param height
     *            the total height of the area
     * @param alignmentAdjust
     *            the alignment-adjust property
     * @param alignmentBaseline
     *            the alignment-baseline property
     * @param baselineShift
     *            the baseline-shift property
     * @param dominantBaseline
     *            the dominant-baseline property
     * @param parentAlignmentContext
     *            the parent alignment context
     */
    public AlignmentContext(final int height, final Length alignmentAdjust,
            final int alignmentBaseline, final Length baselineShift,
            final int dominantBaseline,
            final AlignmentContext parentAlignmentContext) {

        this.areaHeight = height;
        this.lineHeight = height;
        this.xHeight = height;
        this.parentAlignmentContext = parentAlignmentContext;
        this.scaledBaselineTable = parentAlignmentContext
                .getScaledBaselineTable();
        setAlignmentBaselineIdentifier(alignmentBaseline,
                parentAlignmentContext.getDominantBaselineIdentifier());
        setBaselineShift(baselineShift);
        int dominantBaselineIdentifier = parentAlignmentContext
                .getDominantBaselineIdentifier();
        boolean newScaledBaselineTableRequired = false;
        if (this.baselineShiftValue != 0) {
            newScaledBaselineTableRequired = true;
        }
        switch (dominantBaseline) {
        case EN_AUTO:
            newScaledBaselineTableRequired = this.baselineShiftValue != 0;
            break;
        case EN_USE_SCRIPT: // TODO
            break;
        case EN_NO_CHANGE:
            break;
        case EN_RESET_SIZE:
            newScaledBaselineTableRequired = true;
            break;
        default:
            newScaledBaselineTableRequired = true;
            dominantBaselineIdentifier = dominantBaseline;
            break;
        }
        this.actualBaselineTable = ScaledBaselineTableFactory
                .makeGraphicsScaledBaselineTable(height,
                        dominantBaselineIdentifier,
                        this.scaledBaselineTable.getWritingMode());
        if (newScaledBaselineTableRequired) {
            this.scaledBaselineTable = ScaledBaselineTableFactory
                    .makeGraphicsScaledBaselineTable(height,
                            dominantBaselineIdentifier,
                            this.scaledBaselineTable.getWritingMode());
        }
        setAlignmentAdjust(alignmentAdjust);
    }

    /**
     * Creates a new instance of AlignmentContext
     * 
     * @param font
     *            the font
     * @param lineHeight
     *            the computed value of the lineHeight property
     * @param alignmentAdjust
     *            the alignment-adjust property
     * @param alignmentBaseline
     *            the alignment-baseline property
     * @param baselineShift
     *            the baseline-shift property
     * @param dominantBaseline
     *            the dominant-baseline property
     * @param parentAlignmentContext
     *            the parent alignment context
     */
    public AlignmentContext(final Font font, final int lineHeight,
            final Length alignmentAdjust, final int alignmentBaseline,
            final Length baselineShift, final int dominantBaseline,
            final AlignmentContext parentAlignmentContext) {
        this.areaHeight = font.getAscender() - font.getDescender();
        this.lineHeight = lineHeight;
        this.parentAlignmentContext = parentAlignmentContext;
        this.scaledBaselineTable = parentAlignmentContext
                .getScaledBaselineTable();
        this.xHeight = font.getXHeight();
        setAlignmentBaselineIdentifier(alignmentBaseline,
                parentAlignmentContext.getDominantBaselineIdentifier());
        setBaselineShift(baselineShift);
        int dominantBaselineIdentifier = parentAlignmentContext
                .getDominantBaselineIdentifier();
        boolean newScaledBaselineTableRequired = false;
        if (this.baselineShiftValue != 0) {
            newScaledBaselineTableRequired = true;
        }
        switch (dominantBaseline) {
        case EN_AUTO:
            newScaledBaselineTableRequired = this.baselineShiftValue != 0;
            break;
        case EN_USE_SCRIPT: // TODO
            break;
        case EN_NO_CHANGE:
            break;
        case EN_RESET_SIZE:
            newScaledBaselineTableRequired = true;
            break;
        default:
            newScaledBaselineTableRequired = true;
            dominantBaselineIdentifier = dominantBaseline;
            break;
        }
        this.actualBaselineTable = ScaledBaselineTableFactory
                .makeFontScaledBaselineTable(font, dominantBaselineIdentifier,
                        this.scaledBaselineTable.getWritingMode());
        if (newScaledBaselineTableRequired) {
            this.scaledBaselineTable = ScaledBaselineTableFactory
                    .makeFontScaledBaselineTable(font,
                            dominantBaselineIdentifier,
                            this.scaledBaselineTable.getWritingMode());
        }
        setAlignmentAdjust(alignmentAdjust);
    }

    /**
     * Creates a new instance of AlignmentContext based simply on the font and
     * the writing mode.
     * 
     * @param font
     *            the font
     * @param lineHeight
     *            the computed value of the lineHeight property
     * @param writingMode
     *            the current writing mode
     */
    public AlignmentContext(final Font font, final int lineHeight,
            final int writingMode) {
        this.areaHeight = font.getAscender() - font.getDescender();
        this.lineHeight = lineHeight;
        this.xHeight = font.getXHeight();
        this.parentAlignmentContext = null;
        this.scaledBaselineTable = ScaledBaselineTableFactory
                .makeFontScaledBaselineTable(font, writingMode);
        this.actualBaselineTable = this.scaledBaselineTable;
        this.alignmentBaselineIdentifier = getDominantBaselineIdentifier();
        this.alignmentPoint = font.getAscender();
        this.baselineShiftValue = 0;
    }

    /**
     * Returns the alignment point for this context. This is the point on the
     * start edge of the area this context applies to measured from the before
     * edge of the area.
     * 
     * @return the default alignment point
     */
    public int getAlignmentPoint() {
        return this.alignmentPoint;
    }

    /**
     * Returns the current value of baseline shift in effect.
     * 
     * @return the baseline shift
     */
    public int getBaselineShiftValue() {
        return this.baselineShiftValue;
    }

    /**
     * Returns the current alignment baseline identifier
     * 
     * @return the alignment baseline identifier
     */
    public int getAlignmentBaselineIdentifier() {
        return this.alignmentBaselineIdentifier;
    }

    /**
     * Sets the current alignment baseline identifer. For alignment-baseline
     * values of "auto" and "baseline" this method does the conversion into the
     * appropriate computed value assuming script is "auto" and the fo is not
     * fo:character.
     * 
     * @param alignmentBaseline
     *            the alignment-baseline property
     * @param parentDominantBaselineIdentifier
     *            the dominant baseline of the parent fo
     */
    private void setAlignmentBaselineIdentifier(final int alignmentBaseline,
            final int parentDominantBaselineIdentifier) {
        switch (alignmentBaseline) {
        case EN_AUTO: // fall through
        case EN_BASELINE:
            this.alignmentBaselineIdentifier = parentDominantBaselineIdentifier;
            break;
        case EN_BEFORE_EDGE:
        case EN_TEXT_BEFORE_EDGE:
        case EN_CENTRAL:
        case EN_MIDDLE:
        case EN_AFTER_EDGE:
        case EN_TEXT_AFTER_EDGE:
        case EN_IDEOGRAPHIC:
        case EN_ALPHABETIC:
        case EN_HANGING:
        case EN_MATHEMATICAL:
            this.alignmentBaselineIdentifier = alignmentBaseline;
            break;
        }
    }

    /**
     * Sets the current alignment baseline identifer. For alignment-baseline
     * values of "auto" and "baseline" this method does the conversion into the
     * appropriate computed value assuming script is "auto" and the fo is not
     * fo:character.
     * 
     * @param alignmentAdjust
     *            the alignment-adjust property
     */
    private void setAlignmentAdjust(final Length alignmentAdjust) {
        final int beforeEdge = this.actualBaselineTable
                .getBaseline(EN_BEFORE_EDGE);
        switch (alignmentAdjust.getEnum()) {
        case EN_AUTO:
            this.alignmentPoint = beforeEdge
                    - this.actualBaselineTable
                            .getBaseline(this.alignmentBaselineIdentifier);
            break;
        case EN_BASELINE:
            this.alignmentPoint = beforeEdge;
            break;
        case EN_BEFORE_EDGE:
        case EN_TEXT_BEFORE_EDGE:
        case EN_CENTRAL:
        case EN_MIDDLE:
        case EN_AFTER_EDGE:
        case EN_TEXT_AFTER_EDGE:
        case EN_IDEOGRAPHIC:
        case EN_ALPHABETIC:
        case EN_HANGING:
        case EN_MATHEMATICAL:
            this.alignmentPoint = beforeEdge
                    - this.actualBaselineTable.getBaseline(alignmentAdjust
                            .getEnum());
            break;
        default:
            this.alignmentPoint = beforeEdge
            + alignmentAdjust
                            .getValue(new SimplePercentBaseContext(null,
                                    LengthBase.ALIGNMENT_ADJUST,
                                    this.lineHeight));
            break;
        }
    }

    /**
     * Return the scaled baseline table for this context.
     * 
     * @return the scaled baseline table
     */
    public ScaledBaselineTable getScaledBaselineTable() {
        return this.scaledBaselineTable;
    }

    /**
     * Return the dominant baseline identifier.
     * 
     * @return the dominant baseline identifier
     */
    public int getDominantBaselineIdentifier() {
        return this.scaledBaselineTable.getDominantBaselineIdentifier();
    }

    /**
     * Return the writing mode.
     * 
     * @return the writing mode
     */
    public int getWritingMode() {
        return this.scaledBaselineTable.getWritingMode();
    }

    /**
     * Calculates the baseline shift value based on the baseline-shift property
     * value.
     * 
     * @param baselineShift
     *            the baseline shift property value
     * @return the computed baseline shift value
     */
    private void setBaselineShift(final Length baselineShift) {
        this.baselineShiftValue = 0;
        final ScaledBaselineTable sbt = null;
        switch (baselineShift.getEnum()) {
        case EN_BASELINE: // Nothing to do
            break;
        case EN_SUB:
            this.baselineShiftValue = Math.round(-(this.xHeight / 2)
                    + this.parentAlignmentContext
                            .getActualBaselineOffset(EN_ALPHABETIC));
            break;
        case EN_SUPER:
            this.baselineShiftValue = Math.round(this.parentAlignmentContext
                    .getXHeight()
                    + this.parentAlignmentContext
                            .getActualBaselineOffset(EN_ALPHABETIC));
            break;
        case 0: // A <length> or <percentage> value
            this.baselineShiftValue = baselineShift
                    .getValue(new SimplePercentBaseContext(null,
                            LengthBase.CUSTOM_BASE, this.parentAlignmentContext
                                    .getLineHeight()));
            break;
        }
    }

    /**
     * Return the parent alignment context.
     * 
     * @return the parent alignment context
     */
    public AlignmentContext getParentAlignmentContext() {
        return this.parentAlignmentContext;
    }

    /**
     * Return the offset between the current dominant baseline and the parent
     * dominant baseline.
     * 
     * @return the offset in shift direction
     */
    public int getBaselineOffset() {
        if (this.parentAlignmentContext == null) {
            return 0;
        }
        return this.parentAlignmentContext.getScaledBaselineTable()
                .getBaseline(this.alignmentBaselineIdentifier)
                - this.scaledBaselineTable.deriveScaledBaselineTable(
                        this.parentAlignmentContext
                                .getDominantBaselineIdentifier()).getBaseline(
                        this.alignmentBaselineIdentifier)
                - this.scaledBaselineTable
                        .getBaseline(this.parentAlignmentContext
                                .getDominantBaselineIdentifier())
                + this.baselineShiftValue;
    }

    /**
     * Return the offset between the current dominant baseline and the outermost
     * parent dominant baseline.
     * 
     * @return the offet in shift direction
     */
    public int getTotalBaselineOffset() {
        int offset = 0;
        if (this.parentAlignmentContext != null) {
            offset = getBaselineOffset()
                    + this.parentAlignmentContext.getTotalBaselineOffset();
        }
        return offset;
    }

    /**
     * Return the offset between the alignment baseline and the outermost parent
     * dominant baseline.
     * 
     * @return the offset in shift direction
     */
    public int getTotalAlignmentBaselineOffset() {
        return getTotalAlignmentBaselineOffset(this.alignmentBaselineIdentifier);
    }

    /**
     * Return the offset between the given alignment baseline and the outermost
     * parent dominant baseline.
     * 
     * @param alignmentBaselineId
     *            the alignment baseline
     * @return the offset
     */
    public int getTotalAlignmentBaselineOffset(final int alignmentBaselineId) {
        int offset = this.baselineShiftValue;
        if (this.parentAlignmentContext != null) {
            offset = this.parentAlignmentContext.getTotalBaselineOffset()
                    + this.parentAlignmentContext.getScaledBaselineTable()
                            .getBaseline(alignmentBaselineId)
                    + this.baselineShiftValue;
        }
        return offset;
    }

    /**
     * Return the offset between the dominant baseline and the given actual
     * baseline
     * 
     * @param baselineIdentifier
     *            the baseline
     * @return the offset
     */
    public int getActualBaselineOffset(final int baselineIdentifier) {
        // This is the offset from the dominant baseline to the alignment
        // baseline
        int offset = getTotalAlignmentBaselineOffset()
                - getTotalBaselineOffset();
        // Add the offset to the actual baseline we want
        offset += this.actualBaselineTable.deriveScaledBaselineTable(
                this.alignmentBaselineIdentifier).getBaseline(
                baselineIdentifier);
        return offset;
    }

    /**
     * Return the offset the outermost parent dominant baseline and the top of
     * this box.
     * 
     * @return the offset
     */
    private int getTotalTopOffset() {
        final int offset = getTotalAlignmentBaselineOffset() + getAltitude();
        return offset;
    }

    /**
     * Return the total height of the context.
     * 
     * @return the height
     */
    public int getHeight() {
        return this.areaHeight;
    }

    /**
     * Return the line height of the context.
     * 
     * @return the height
     */
    public int getLineHeight() {
        return this.lineHeight;
    }

    /**
     * The altitude of the context that is the height above the alignment point.
     * 
     * @return the altitude
     */
    public int getAltitude() {
        return this.alignmentPoint;
    }

    /**
     * The depth of the context that is the height below alignment point.
     * 
     * @return the altitude
     */
    public int getDepth() {
        return getHeight() - this.alignmentPoint;
    }

    /**
     * The x height of the context.
     * 
     * @return the x height
     */
    public int getXHeight() {
        return this.xHeight;
    }

    /**
     * Resizes the line as specified. Assumes that the new alignment point is on
     * the dominant baseline, that is this function should be called for line
     * areas only.
     * 
     * @param newLineHeight
     *            the new height of the line
     * @param newAlignmentPoint
     *            the new alignment point
     */
    public void resizeLine(final int newLineHeight, final int newAlignmentPoint) {
        this.areaHeight = newLineHeight;
        this.alignmentPoint = newAlignmentPoint;
        this.scaledBaselineTable.setBeforeAndAfterBaselines(
                this.alignmentPoint, this.alignmentPoint - this.areaHeight);
    }

    /**
     * Returns the offset from the before-edge of the parent to this context.
     * 
     * @return the offset for rendering
     */
    public int getOffset() {
        int offset = 0;
        if (this.parentAlignmentContext != null) {
            offset = this.parentAlignmentContext.getTotalTopOffset()
                    - getTotalTopOffset();
        } else {
            offset = getAltitude()
                    - this.scaledBaselineTable.getBaseline(EN_TEXT_BEFORE_EDGE);
        }
        return offset;
    }

    /**
     * Returns an indication if we still use the initial baseline table. The
     * initial baseline table is the table generated by the Line LM.
     * 
     * @return true if this is still the initial baseline table
     */
    public boolean usesInitialBaselineTable() {
        return this.parentAlignmentContext == null
                || this.scaledBaselineTable == this.parentAlignmentContext
                        .getScaledBaselineTable()
                && this.parentAlignmentContext.usesInitialBaselineTable();
    }

    private boolean isHorizontalWritingMode() {
        return getWritingMode() == EN_LR_TB || getWritingMode() == EN_RL_TB;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(64);
        sb.append("ah=" + this.areaHeight);
        sb.append(" lp=" + this.lineHeight);
        sb.append(" ap=" + this.alignmentPoint);
        sb.append(" ab=" + this.alignmentBaselineIdentifier);
        sb.append(" bs=" + this.baselineShiftValue);
        return sb.toString();
    }

}
