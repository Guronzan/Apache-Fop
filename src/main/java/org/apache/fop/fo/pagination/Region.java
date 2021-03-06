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

/* $Id: Region.java 757256 2009-03-22 21:08:48Z adelmelle $ */

package org.apache.fop.fo.pagination;

import java.awt.Rectangle;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.FODimension;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.xml.sax.Locator;

/**
 * This is an abstract base class for pagination regions.
 */
public abstract class Region extends FObj {
    // The value of properties relevant for fo:region
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    // private ToBeImplementedProperty clip
    private int displayAlign;
    private int overflow;
    private String regionName;
    private Numeric referenceOrientation;
    private int writingMode;
    // End of property values

    private final SimplePageMaster layoutMaster;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    protected Region(final FONode parent) {
        super(parent);
        this.layoutMaster = (SimplePageMaster) parent;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        // clip = pList.get(PR_CLIP);
        this.displayAlign = pList.get(PR_DISPLAY_ALIGN).getEnum();
        this.overflow = pList.get(PR_OVERFLOW).getEnum();
        this.regionName = pList.get(PR_REGION_NAME).getString();
        this.referenceOrientation = pList.get(PR_REFERENCE_ORIENTATION)
                .getNumeric();
        this.writingMode = pList.getWritingMode();

        // regions may have name, or default
        if (this.regionName.equals("")) {
            this.regionName = getDefaultRegionName();
        } else {
            // check that name is OK. Not very pretty.
            if (isReserved(getRegionName())
                    && !getRegionName().equals(getDefaultRegionName())) {
                getFOValidationEventProducer().illegalRegionName(this,
                        getName(), this.regionName, getLocator());
            }
        }

        // TODO do we need context for getBPPaddingAndBorder() and
        // getIPPaddingAndBorder()?
        if (getCommonBorderPaddingBackground().getBPPaddingAndBorder(false,
                null) != 0
                || getCommonBorderPaddingBackground().getIPPaddingAndBorder(
                        false, null) != 0) {
            getFOValidationEventProducer().nonZeroBorderPaddingOnRegion(this,
                    getName(), this.regionName, true, getLocator());
        }
    }

    /**
     * {@inheritDoc} String, String) <br>
     * XSL Content Model: empty
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /**
     * @param pageRefRect
     *            reference dimension of the page area.
     * @param spm
     *            the simple page master this region belongs to.
     * @return the rectangle for the viewport area
     */
    public abstract Rectangle getViewportRectangle(
            final FODimension pageRefRect, final SimplePageMaster spm);

    /**
     * Returns the default region name (xsl-region-before, xsl-region-start,
     * etc.)
     *
     * @return the default region name
     */
    protected abstract String getDefaultRegionName();

    /**
     * Checks to see if a given region name is one of the reserved names
     *
     * @param name
     *            a region name to check
     * @return true if the name parameter is a reserved region name
     */
    protected boolean isReserved(final String name) {
        return name.equals("xsl-region-before")
                || name.equals("xsl-region-start")
                || name.equals("xsl-region-end")
                || name.equals("xsl-region-after")
                || name.equals("xsl-before-float-separator")
                || name.equals("xsl-footnote-separator");
    }

    /**
     * Get the page-width context
     *
     * @param lengthBase
     *            the lengthBase to use for resolving percentages
     * @return context for the width of the page-reference-area
     */
    protected PercentBaseContext getPageWidthContext(final int lengthBase) {
        return this.layoutMaster.getPageWidthContext(lengthBase);
    }

    /**
     * Get the page-width context
     *
     * @param lengthBase
     *            the lengthBase to use for resolving percentages
     * @return context for the width of the page-reference-area
     */
    protected PercentBaseContext getPageHeightContext(final int lengthBase) {
        return this.layoutMaster.getPageHeightContext(lengthBase);
    }

    /** {@inheritDoc} */
    @Override
    public boolean generatesReferenceAreas() {
        return true;
    }

    /**
     * Returns a sibling region for this region.
     *
     * @param regionId
     *            the Constants ID of the FO representing the region
     * @return the requested region
     */
    protected Region getSiblingRegion(final int regionId) {
        // Ask parent for region
        return this.layoutMaster.getRegion(regionId);
    }

    /**
     * @return the Background Properties (border and padding are not used here).
     */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /** @return the "region-name" property. */
    public String getRegionName() {
        return this.regionName;
    }

    /** @return the "writing-mode" property. */
    public int getWritingMode() {
        return this.writingMode;
    }

    /** @return the "overflow" property. */
    public int getOverflow() {
        return this.overflow;
    }

    /** @return the display-align property. */
    public int getDisplayAlign() {
        return this.displayAlign;
    }

    /** @return the "reference-orientation" property. */
    public int getReferenceOrientation() {
        return this.referenceOrientation.getValue();
    }
}
