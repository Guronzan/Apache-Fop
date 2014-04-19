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

/* $Id: BlockContainer.java 738514 2009-01-28 15:33:02Z vhennebert $ */

package org.apache.fop.fo.flow;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.BreakPropertySet;
import org.apache.fop.fo.properties.CommonAbsolutePosition;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_block-container">
 * <code>fo:block-container</code></a> object.
 */
public class BlockContainer extends FObj implements BreakPropertySet {
    // The value of properties relevant for fo:block-container.
    private CommonAbsolutePosition commonAbsolutePosition;
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private CommonMarginBlock commonMarginBlock;
    private LengthRangeProperty blockProgressionDimension;
    private int breakAfter;
    private int breakBefore;
    // private ToBeImplementedProperty clip;
    private int displayAlign;
    private LengthRangeProperty inlineProgressionDimension;
    private KeepProperty keepTogether;
    private KeepProperty keepWithNext;
    private KeepProperty keepWithPrevious;
    private int overflow;
    private Numeric referenceOrientation;
    private int span;
    private int disableColumnBalancing;
    private int writingMode;
    // Unused but valid items, commented out for performance:
    // private int intrusionDisplace;
    // private Numeric zIndex;
    // End of property values

    /** used for FO validation */
    private boolean blockItemFound = false;

    /**
     * Creates a new BlockContainer instance as a child of the given
     * {@link FONode}.
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public BlockContainer(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.commonAbsolutePosition = pList.getAbsolutePositionProps();
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        this.commonMarginBlock = pList.getMarginBlockProps();
        this.blockProgressionDimension = pList.get(
                PR_BLOCK_PROGRESSION_DIMENSION).getLengthRange();
        this.breakAfter = pList.get(PR_BREAK_AFTER).getEnum();
        this.breakBefore = pList.get(PR_BREAK_BEFORE).getEnum();
        // clip = pList.get(PR_CLIP);
        this.displayAlign = pList.get(PR_DISPLAY_ALIGN).getEnum();
        this.inlineProgressionDimension = pList.get(
                PR_INLINE_PROGRESSION_DIMENSION).getLengthRange();
        this.keepTogether = pList.get(PR_KEEP_TOGETHER).getKeep();
        this.keepWithNext = pList.get(PR_KEEP_WITH_NEXT).getKeep();
        this.keepWithPrevious = pList.get(PR_KEEP_WITH_PREVIOUS).getKeep();
        this.overflow = pList.get(PR_OVERFLOW).getEnum();
        this.referenceOrientation = pList.get(PR_REFERENCE_ORIENTATION)
                .getNumeric();
        this.span = pList.get(PR_SPAN).getEnum();
        this.writingMode = pList.get(PR_WRITING_MODE).getEnum();
        this.disableColumnBalancing = pList.get(PR_X_DISABLE_COLUMN_BALANCING)
                .getEnum();
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startBlockContainer(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: marker* (%block;)+ <br>
     * <i><b>BUT</b>: "In addition an fo:block-container that does not generate
     * an absolutely positioned area may have a sequence of zero or more
     * fo:markers as its initial children." The latter refers to
     * block-containers with absolute-position="absolute" or
     * absolute-position="fixed".
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if ("marker".equals(localName)) {
                if (this.commonAbsolutePosition.absolutePosition == EN_ABSOLUTE
                        || this.commonAbsolutePosition.absolutePosition == EN_FIXED) {
                    getFOValidationEventProducer()
                            .markerBlockContainerAbsolutePosition(this,
                            this.locator);
                }
                if (this.blockItemFound) {
                    nodesOutOfOrderError(loc, "fo:marker", "(%block;)");
                }
            } else if (!isBlockItem(FO_URI, localName)) {
                invalidChildError(loc, FO_URI, localName);
            } else {
                this.blockItemFound = true;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (!this.blockItemFound) {
            missingChildElementError("marker* (%block;)+");
        }

        getFOEventHandler().endBlockContainer(this);
    }

    /** @return <code>true</code> (BlockContainer can generate Reference Areas) */
    @Override
    public boolean generatesReferenceAreas() {
        return true;
    }

    /** @return the {@link CommonAbsolutePosition} */
    public CommonAbsolutePosition getCommonAbsolutePosition() {
        return this.commonAbsolutePosition;
    }

    /** @return the {@link CommonMarginBlock} */
    public CommonMarginBlock getCommonMarginBlock() {
        return this.commonMarginBlock;
    }

    /** @return the {@link CommonBorderPaddingBackground} */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /**
     * @return the "block-progression-dimension" property.
     */
    public LengthRangeProperty getBlockProgressionDimension() {
        return this.blockProgressionDimension;
    }

    /** @return the "display-align" property. */
    public int getDisplayAlign() {
        return this.displayAlign;
    }

    /** @return the "break-after" property. */
    @Override
    public int getBreakAfter() {
        return this.breakAfter;
    }

    /** @return the "break-before" property. */
    @Override
    public int getBreakBefore() {
        return this.breakBefore;
    }

    /** @return the "keep-with-next" property. */
    public KeepProperty getKeepWithNext() {
        return this.keepWithNext;
    }

    /** @return the "keep-with-previous" property. */
    public KeepProperty getKeepWithPrevious() {
        return this.keepWithPrevious;
    }

    /** @return the "keep-together" property. */
    public KeepProperty getKeepTogether() {
        return this.keepTogether;
    }

    /** @return the "inline-progression-dimension" property */
    public LengthRangeProperty getInlineProgressionDimension() {
        return this.inlineProgressionDimension;
    }

    /** @return the "overflow" property */
    public int getOverflow() {
        return this.overflow;
    }

    /** @return the "reference-orientation" property */
    public int getReferenceOrientation() {
        return this.referenceOrientation.getValue();
    }

    /** @return the "span" property */
    public int getSpan() {
        return this.span;
    }

    /**
     * @return the "fox:disable-column-balancing" property, one of
     *         {@link Constants#EN_TRUE}, {@link Constants#EN_FALSE}
     */
    public int getDisableColumnBalancing() {
        return this.disableColumnBalancing;
    }

    /** @return the "writing-mode" property */
    public int getWritingMode() {
        return this.writingMode;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "block-container";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_BLOCK_CONTAINER}
     */
    @Override
    public int getNameId() {
        return FO_BLOCK_CONTAINER;
    }
}
