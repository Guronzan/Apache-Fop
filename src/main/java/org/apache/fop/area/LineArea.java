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

/* $Id: LineArea.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.area;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.fo.Constants;

/**
 * The line area. This is a line area that contains inline areas.
 */
public class LineArea extends Area {

    /**
     *
     */
    private static final long serialVersionUID = 406291187010424557L;

    /**
     * this class stores information about line width and potential adjustments
     * that can be used in order to re-compute adjustement and / or indents when
     * a page-number or a page-number-citation is resolved
     */
    private class LineAdjustingInfo implements Serializable {
        /**
         *
         */
        private static final long serialVersionUID = 453935351806476066L;
        private final int lineAlignment;
        private int difference;
        private final int availableStretch;
        private final int availableShrink;
        private double variationFactor;
        private boolean bAddedToAreaTree;

        private LineAdjustingInfo(final int alignment, final int diff,
                final int stretch, final int shrink) {
            this.lineAlignment = alignment;
            this.difference = diff;
            this.availableStretch = stretch;
            this.availableShrink = shrink;
            this.variationFactor = 1.0;
            this.bAddedToAreaTree = false;
        }
    }

    private LineAdjustingInfo adjustingInfo = null;

    // this class can contain the dominant char styling info
    // this means that many renderers can optimise a bit

    private final List<InlineArea> inlineAreas = new ArrayList<>();

    /**
     * default constructor: nothing to do
     */
    public LineArea() {
    }

    /**
     * constructor with extra parameters: a new LineAdjustingInfo object is
     * created
     *
     * @param alignment
     *            alignment of this line
     * @param diff
     *            difference between content width and line width
     * @param stretch
     *            the available stretch for any adjustments
     * @param shrink
     *            the available shrink for any adjustments
     */
    public LineArea(final int alignment, final int diff, final int stretch,
            final int shrink) {
        this.adjustingInfo = new LineAdjustingInfo(alignment, diff, stretch,
                shrink);
    }

    /**
     * Add a child area to this line area.
     *
     * @param childArea
     *            the inline child area to add
     */
    @Override
    public void addChildArea(final Area childArea) {
        if (childArea instanceof InlineArea) {
            addInlineArea((InlineArea) childArea);
            // set the parent area for the child area
            ((InlineArea) childArea).setParentArea(this);
        }
    }

    /**
     * Add an inline child area to this line area.
     *
     * @param area
     *            the inline child area to add
     */
    public void addInlineArea(final InlineArea area) {
        this.inlineAreas.add(area);
    }

    /**
     * Get the inline child areas of this line area.
     *
     * @return the list of inline areas
     */
    public List<InlineArea> getInlineAreas() {
        return this.inlineAreas;
    }

    /**
     * Get the start indent of this line area. The start indent is used for
     * offsetting the start of the inline areas for alignment or other indents.
     *
     * @return the start indent value
     */
    public int getStartIndent() {
        if (hasTrait(Trait.START_INDENT)) {
            return getTraitAsInteger(Trait.START_INDENT);
        } else {
            return 0;
        }
    }

    /**
     * Updates the extents of the line area from its children.
     */
    public void updateExtentsFromChildren() {
        int ipd = 0;
        int bpd = 0;
        for (int i = 0, len = this.inlineAreas.size(); i < len; ++i) {
            ipd = Math.max(ipd, this.inlineAreas.get(i).getAllocIPD());
            bpd += this.inlineAreas.get(i).getAllocBPD();
        }
        setIPD(ipd);
        setBPD(bpd);
    }

    /**
     * receive notification about the ipd variation of a descendant area and
     * perform the needed adjustment, according to the alignment; in particular:
     * <ul>
     * <li>left-aligned text needs no adjustement;</li>
     * <li>right-aligned text and centered text are handled locally, adjusting
     * the indent of this LineArea;</li>
     * <li>justified text requires a more complex adjustment, as the variation
     * factor computed on the basis of the total stretch and shrink of the line
     * must be applied in every descendant leaf areas (text areas and leader
     * areas).</li>
     * </ul>
     *
     * @param ipdVariation
     *            the difference between old and new ipd
     */
    public void handleIPDVariation(final int ipdVariation) {
        switch (this.adjustingInfo.lineAlignment) {
        case Constants.EN_START:
            // nothing to do in this case
            break;
        case Constants.EN_CENTER:
            // re-compute indent
            addTrait(Trait.START_INDENT, getStartIndent() - ipdVariation / 2);
            break;
        case Constants.EN_END:
            // re-compute indent
            addTrait(Trait.START_INDENT, getStartIndent() - ipdVariation);
            break;
        case Constants.EN_JUSTIFY:
            // compute variation factor
            this.adjustingInfo.variationFactor *= (float) (this.adjustingInfo.difference - ipdVariation)
            / this.adjustingInfo.difference;
            this.adjustingInfo.difference -= ipdVariation;
            // if the LineArea has already been added to the area tree,
            // call finalize(); otherwise, wait for the LineLM to call it
            if (this.adjustingInfo.bAddedToAreaTree) {
                finalise();
            }
            break;
        default:
            throw new RuntimeException();
        }
    }

    /**
     * apply the variation factor to all descendant areas and destroy the
     * AdjustingInfo object if there are no UnresolvedAreas left
     */
    public void finalise() {
        if (this.adjustingInfo.lineAlignment == Constants.EN_JUSTIFY) {
            // justified line: apply the variation factor
            boolean bUnresolvedAreasPresent = false;
            // recursively apply variation factor to descendant areas
            for (final InlineArea inlineArea : this.inlineAreas) {
                bUnresolvedAreasPresent |= inlineArea.applyVariationFactor(
                        this.adjustingInfo.variationFactor,
                        this.adjustingInfo.availableStretch,
                        this.adjustingInfo.availableShrink);
            }
            if (!bUnresolvedAreasPresent) {
                // there are no more UnresolvedAreas:
                // destroy the AdjustingInfo instance
                this.adjustingInfo = null;
            } else {
                // this method will be called again later:
                // the first time, it is called by the LineLM,
                // afterwards it must be called by the LineArea itself
                if (!this.adjustingInfo.bAddedToAreaTree) {
                    this.adjustingInfo.bAddedToAreaTree = true;
                }
                // reset the variation factor
                this.adjustingInfo.variationFactor = 1.0;
            }
        } else {
            // the line is not justified: the ipd variation has already
            // been handled, modifying the line indent
        }
    }
}
