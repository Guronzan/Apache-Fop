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

/* $Id: Inline.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.fo.flow;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.StructurePointerPropertySet;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_inline">
 * <code>fo:inline</code></a> formatting object.
 */
public class Inline extends InlineLevel implements StructurePointerPropertySet {
    // The value of properties relevant for fo:inline.
    // See also superclass InlineLevel
    private Length alignmentAdjust;
    private int alignmentBaseline;
    private Length baselineShift;
    private String ptr; // used for accessibility
    private int dominantBaseline;
    // Unused but valid items, commented out for performance:
    // private CommonRelativePosition commonRelativePosition;
    // private LengthRangeProperty blockProgressionDimension;
    // private Length height;
    // private LengthRangeProperty inlineProgressionDimension;
    // private Length width;
    // private int wrapOption;
    // End of property values
    // used for FO validation
    private boolean blockOrInlineItemFound = false;
    private boolean canHaveBlockLevelChildren = true;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public Inline(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.alignmentAdjust = pList.get(PR_ALIGNMENT_ADJUST).getLength();
        this.alignmentBaseline = pList.get(PR_ALIGNMENT_BASELINE).getEnum();
        this.baselineShift = pList.get(PR_BASELINE_SHIFT).getLength();
        this.dominantBaseline = pList.get(PR_DOMINANT_BASELINE).getEnum();
        this.ptr = pList.get(PR_X_PTR).getString(); // used for accessibility
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();

        /*
         * Check to see if this node can have block-level children. See
         * validateChildNode() below.
         */
        final int lvlLeader = findAncestor(FO_LEADER);
        final int lvlFootnote = findAncestor(FO_FOOTNOTE);
        final int lvlInCntr = findAncestor(FO_INLINE_CONTAINER);

        if (lvlLeader > 0) {
            if (lvlInCntr < 0 || lvlInCntr > 0 && lvlInCntr > lvlLeader) {
                this.canHaveBlockLevelChildren = false;
            }
        } else if (lvlFootnote > 0) {
            if (lvlInCntr < 0 || lvlInCntr > lvlFootnote) {
                this.canHaveBlockLevelChildren = false;
            }
        }

        getFOEventHandler().startInline(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        super.endOfNode();
        getFOEventHandler().endInline(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: marker* (#PCDATA|%inline;|%block;)* <br>
     * <i>Additionally: " An fo:inline that is a descendant of an fo:leader or
     * fo:footnote may not have block-level children, unless it has a nearer
     * ancestor that is an fo:inline-container." (paraphrased)</i>
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("marker")) {
                if (this.blockOrInlineItemFound) {
                    nodesOutOfOrderError(loc, "fo:marker",
                            "(#PCDATA|%inline;|%block;)");
                }
            } else if (!isBlockOrInlineItem(nsURI, localName)) {
                invalidChildError(loc, nsURI, localName);
            } else if (!this.canHaveBlockLevelChildren
                    && isBlockItem(nsURI, localName)) {
                invalidChildError(loc, getParent().getName(), nsURI, getName(),
                        "rule.inlineContent");
            } else {
                this.blockOrInlineItemFound = true;
            }
        }
    }

    /** @return the "alignment-adjust" property */
    public Length getAlignmentAdjust() {
        return this.alignmentAdjust;
    }

    /** @return the "alignment-baseline" property */
    public int getAlignmentBaseline() {
        return this.alignmentBaseline;
    }

    /** @return the "baseline-shift" property */
    public Length getBaselineShift() {
        return this.baselineShift;
    }

    /** @return the "dominant-baseline" property */
    public int getDominantBaseline() {
        return this.dominantBaseline;
    }

    /** {@inheritDoc} */
    @Override
    public String getPtr() {
        return this.ptr;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "inline";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_INLINE}
     */
    @Override
    public int getNameId() {
        return FO_INLINE;
    }
}
