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

/* $Id: RegionBody.java 757256 2009-03-22 21:08:48Z adelmelle $ */

package org.apache.fop.fo.pagination;

// Java
import java.awt.Rectangle;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.FODimension;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.properties.CommonMarginBlock;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_region-body">
 * <code>fo:region-body</code></a> object.
 */
public class RegionBody extends Region {
    // The value of properties relevant for fo:region-body.
    private CommonMarginBlock commonMarginBlock;
    private Numeric columnCount;
    private Length columnGap;

    // End of property values

    /**
     * Create a RegionBody instance that is a child of the given parent
     * {@link FONode}.
     *
     * @param parent
     *            the {@link FONode} that is to be the parent
     */
    public RegionBody(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.commonMarginBlock = pList.getMarginBlockProps();
        this.columnCount = pList.get(PR_COLUMN_COUNT).getNumeric();
        this.columnGap = pList.get(PR_COLUMN_GAP).getLength();

        if (getColumnCount() > 1 && getOverflow() == EN_SCROLL) {
            /*
             * This is an error (See XSL Rec, fo:region-body description). The
             * Rec allows for acting as if "1" is chosen in these cases, but we
             * will need to be able to change Numeric values in order to do
             * this.
             */
            getFOValidationEventProducer()
            .columnCountErrorOnRegionBodyOverflowScroll(this,
                    getName(), getLocator());
        }
    }

    /**
     * Return the {@link CommonMarginBlock} instance attached to this instance.
     *
     * @return the {@link CommonMarginBlock} instance
     */
    public CommonMarginBlock getCommonMarginBlock() {
        return this.commonMarginBlock;
    }

    /**
     * Return the value of the <code>column-count<code> property.
     *
     * @return the "column-count" property.
     */
    public int getColumnCount() {
        return this.columnCount.getValue();
    }

    /**
     * Return the value of the <code>column-gap</code> property.
     *
     * @return the "column-gap" property.
     */
    public int getColumnGap() {
        return this.columnGap.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public Rectangle getViewportRectangle(final FODimension reldims,
            final SimplePageMaster spm) {
        /*
         * Special rules apply to resolving margins in the page context.
         * Contrary to normal margins in this case top and bottom margin are
         * resolved relative to the height. In the property subsystem all margin
         * properties are configured to using BLOCK_WIDTH. That's why we 'cheat'
         * here and setup a context for the height but use the
         * LengthBase.BLOCK_WIDTH. Also the values are resolved relative to the
         * page size and reference orientation.
         */
        final PercentBaseContext pageWidthContext = getPageWidthContext(LengthBase.CONTAINING_BLOCK_WIDTH);
        final PercentBaseContext pageHeightContext = getPageHeightContext(LengthBase.CONTAINING_BLOCK_WIDTH);

        int start;
        int end;
        if (spm.getWritingMode() == EN_LR_TB) { // Left-to-right
            start = this.commonMarginBlock.marginLeft
                    .getValue(pageWidthContext);
            end = this.commonMarginBlock.marginRight.getValue(pageWidthContext);
        } else { // all other supported modes are right-to-left
            start = this.commonMarginBlock.marginRight
                    .getValue(pageWidthContext);
            end = this.commonMarginBlock.marginLeft.getValue(pageWidthContext);
        }
        final int before = this.commonMarginBlock.spaceBefore
                .getOptimum(pageHeightContext).getLength()
                .getValue(pageHeightContext);
        final int after = this.commonMarginBlock.spaceAfter
                .getOptimum(pageHeightContext).getLength()
                .getValue(pageHeightContext);
        return new Rectangle(start, before, reldims.ipd - start - end,
                reldims.bpd - before - after);
    }

    /** {@inheritDoc} */
    @Override
    protected String getDefaultRegionName() {
        return "xsl-region-body";
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "region-body";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_REGION_BODY}
     */
    @Override
    public int getNameId() {
        return FO_REGION_BODY;
    }
}
