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

/* $Id: FilledArea.java 721430 2008-11-28 11:13:12Z acumiskey $ */

package org.apache.fop.area.inline;

import java.util.ArrayList;
import java.util.List;

/**
 * Filled area. This inline area contains some inline areas. When the renderer
 * gets the child areas to render the inline areas are repeated to fill the ipd
 * of this inline parent. This extends InlineParent so that the renderer will
 * render this as a normal inline parent.
 */
public class FilledArea extends InlineParent {
    /**
     *
     */
    private static final long serialVersionUID = 8586584705587017474L;
    private int unitWidth;

    /**
     * Create a new filled area.
     */
    public FilledArea() {
    }

    /**
     * Set the unit width for the areas to fill the full width.
     *
     * @param width
     *            the unit width
     */
    public void setUnitWidth(final int width) {
        this.unitWidth = width;
    }

    /**
     * Return the unit width for the areas to fill the full width.
     *
     * @return the unit width
     */
    public int getUnitWidth() {
        return this.unitWidth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBPD() {
        int bpd = 0;
        for (final InlineArea area : getChildAreas()) {
            if (bpd < area.getBPD()) {
                bpd = area.getBPD();
            }
        }
        return bpd;
    }

    /**
     * Get the child areas for this filled area. This copies the references of
     * the inline areas so that it fills the total width of the area a whole
     * number of times for the unit width.
     *
     * @return the list of child areas copied to fill the width
     */
    @Override
    public List<InlineArea> getChildAreas() {
        final int units = getIPD() / this.unitWidth;
        final List<InlineArea> newList = new ArrayList<>();
        for (int count = 0; count < units; ++count) {
            newList.addAll(this.inlines);
        }
        return newList;
    }

    /**
     * Recursively apply the variation factor to all descendant areas
     *
     * @param variationFactor
     *            the variation factor that must be applied to adjustments
     * @param lineStretch
     *            the total stretch of the line
     * @param lineShrink
     *            the total shrink of the line
     * @return true if there is an UnresolvedArea descendant
     */
    @Override
    public boolean applyVariationFactor(final double variationFactor,
            final int lineStretch, final int lineShrink) {
        setIPD(getIPD()
                + this.adjustingInfo.applyVariationFactor(variationFactor));
        return false;
    }
}
