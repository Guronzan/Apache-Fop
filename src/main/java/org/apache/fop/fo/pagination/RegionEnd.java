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

/* $Id: RegionEnd.java 757256 2009-03-22 21:08:48Z adelmelle $ */

package org.apache.fop.fo.pagination;

// Java
import java.awt.Rectangle;

import org.apache.fop.datatypes.FODimension;
import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.datatypes.PercentBaseContext;
// FOP
import org.apache.fop.fo.FONode;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_region-end">
 * <code>fo:region-end</code></a> object.
 */
public class RegionEnd extends RegionSE {

    /**
     * Create a RegionEnd instance that is a child of the given parent
     * {@link FONode}.
     * 
     * @param parent
     *            the {@link FONode} that is to be the parent
     */
    public RegionEnd(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public Rectangle getViewportRectangle(final FODimension reldims,
            final SimplePageMaster spm) {
        /*
         * Special rules apply to resolving extent as values are resolved
         * relative to the page size and reference orientation.
         */
        final PercentBaseContext pageWidthContext = getPageWidthContext(LengthBase.CUSTOM_BASE);
        final PercentBaseContext pageHeightContext = getPageHeightContext(LengthBase.CUSTOM_BASE);
        PercentBaseContext neighbourContext;
        Rectangle vpRect;
        if (spm.getWritingMode() == EN_LR_TB
                || spm.getWritingMode() == EN_RL_TB) {
            neighbourContext = pageHeightContext;
            vpRect = new Rectangle(reldims.ipd
                    - getExtent().getValue(pageWidthContext), 0, getExtent()
                    .getValue(pageWidthContext), reldims.bpd);
        } else {
            // Rectangle: x , y (of top left point), width, height
            neighbourContext = pageWidthContext;
            vpRect = new Rectangle(reldims.ipd
                    - getExtent().getValue(pageHeightContext), 0, reldims.bpd,
                    getExtent().getValue(pageHeightContext));
        }
        adjustIPD(vpRect, spm.getWritingMode(), neighbourContext);
        return vpRect;
    }

    /** {@inheritDoc} */
    @Override
    protected String getDefaultRegionName() {
        return "xsl-region-end";
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "region-end";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_REGION_END}
     */
    @Override
    public int getNameId() {
        return FO_REGION_END;
    }
}
