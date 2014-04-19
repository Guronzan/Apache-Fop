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

/* $Id: BasicScaledBaselineTable.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.layoutmgr.inline;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.Constants;

/**
 * An implementation of the ScaledBaselineTable interface which calculates all
 * baselines given the height above and below the dominant baseline.
 */
@Slf4j
public class BasicScaledBaselineTable implements ScaledBaselineTable, Constants {

    private final int altitude;
    private final int depth;
    private final int xHeight;
    private final int dominantBaselineIdentifier;
    private final int writingMode;
    private final int dominantBaselineOffset;
    private int beforeEdgeOffset;
    private int afterEdgeOffset;

    private static final float HANGING_BASELINE_FACTOR = 0.8f;
    private static final float MATHEMATICAL_BASELINE_FACTOR = 0.5f;

    /**
     *
     * Creates a new instance of BasicScaledBaselineTable for the given
     * altitude, depth, xHeight, baseline and writingmode.
     *
     * @param altitude
     *            the height of the box or the font ascender
     * @param depth
     *            the font descender or 0
     * @param xHeight
     *            the font xHeight
     * @param dominantBaselineIdentifier
     *            the dominant baseline given as an integer constant
     * @param writingMode
     *            the writing mode given as an integer constant
     */
    public BasicScaledBaselineTable(final int altitude, final int depth,
            final int xHeight, final int dominantBaselineIdentifier,
            final int writingMode) {
        this.altitude = altitude;
        this.depth = depth;
        this.xHeight = xHeight;
        this.dominantBaselineIdentifier = dominantBaselineIdentifier;
        this.writingMode = writingMode;
        this.dominantBaselineOffset = getBaselineDefaultOffset(this.dominantBaselineIdentifier);
        this.beforeEdgeOffset = altitude - this.dominantBaselineOffset;
        this.afterEdgeOffset = depth - this.dominantBaselineOffset;
    }

    /**
     * Return the dominant baseline for this baseline table.
     *
     * @return the dominant baseline
     */
    @Override
    public int getDominantBaselineIdentifier() {
        return this.dominantBaselineIdentifier;
    }

    /**
     * Return the writing mode for this baseline table.
     *
     * @return the writing mode
     */
    @Override
    public int getWritingMode() {
        return this.writingMode;
    }

    /**
     * Return the baseline offset measured from the dominant baseline for the
     * given baseline.
     *
     * @param baselineIdentifier
     *            the baseline identifier
     * @return the baseline offset
     */
    @Override
    public int getBaseline(final int baselineIdentifier) {
        int offset = 0;
        if (!isHorizontalWritingMode()) {
            switch (baselineIdentifier) {
            case EN_TOP:
            case EN_TEXT_TOP:
            case EN_TEXT_BOTTOM:
            case EN_BOTTOM:
                log.warn("The given baseline is only supported for horizontal"
                        + " writing modes");
                return 0;
            }
        }
        switch (baselineIdentifier) {
        case EN_TOP: // fall through
        case EN_BEFORE_EDGE:
            offset = this.beforeEdgeOffset;
            break;
        case EN_TEXT_TOP:
        case EN_TEXT_BEFORE_EDGE:
        case EN_HANGING:
        case EN_CENTRAL:
        case EN_MIDDLE:
        case EN_MATHEMATICAL:
        case EN_ALPHABETIC:
        case EN_IDEOGRAPHIC:
        case EN_TEXT_BOTTOM:
        case EN_TEXT_AFTER_EDGE:
            offset = getBaselineDefaultOffset(baselineIdentifier)
                    - this.dominantBaselineOffset;
            break;
        case EN_BOTTOM: // fall through
        case EN_AFTER_EDGE:
            offset = this.afterEdgeOffset;
            break;
        }
        return offset;
    }

    private boolean isHorizontalWritingMode() {
        return this.writingMode == EN_LR_TB || this.writingMode == EN_RL_TB;
    }

    /**
     * Return the baseline offset measured from the font's default baseline for
     * the given baseline.
     *
     * @param baselineIdentifier
     *            the baseline identifier
     * @return the baseline offset
     */
    private int getBaselineDefaultOffset(final int baselineIdentifier) {
        int offset = 0;
        switch (baselineIdentifier) {
        case EN_TEXT_BEFORE_EDGE:
            offset = this.altitude;
            break;
        case EN_HANGING:
            offset = Math.round(this.altitude * HANGING_BASELINE_FACTOR);
            break;
        case EN_CENTRAL:
            offset = (this.altitude - this.depth) / 2 + this.depth;
            break;
        case EN_MIDDLE:
            offset = this.xHeight / 2;
            break;
        case EN_MATHEMATICAL:
            offset = Math.round(this.altitude * MATHEMATICAL_BASELINE_FACTOR);
            break;
        case EN_ALPHABETIC:
            offset = 0;
            break;
        case EN_IDEOGRAPHIC: // Fall through
        case EN_TEXT_AFTER_EDGE:
            offset = this.depth;
            break;
        }
        return offset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBeforeAndAfterBaselines(final int beforeBaseline,
            final int afterBaseline) {
        this.beforeEdgeOffset = beforeBaseline;
        this.afterEdgeOffset = afterBaseline;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScaledBaselineTable deriveScaledBaselineTable(
            final int baselineIdentifier) {
        final BasicScaledBaselineTable bac = new BasicScaledBaselineTable(
                this.altitude, this.depth, this.xHeight, baselineIdentifier,
                this.writingMode);
        return bac;
    }

}
