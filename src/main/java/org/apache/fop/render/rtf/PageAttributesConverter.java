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

/* $Id: PageAttributesConverter.java 681307 2008-07-31 09:06:10Z jeremias $ */

package org.apache.fop.render.rtf;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.expr.NumericOp;
import org.apache.fop.fo.pagination.RegionBA;
import org.apache.fop.fo.pagination.RegionBody;
import org.apache.fop.fo.pagination.SimplePageMaster;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfAttributes;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfPage;

/**
 * Converts simple-page-master attributes into strings as defined in RtfPage.
 *
 * @author Christopher Scott, scottc@westinghouse.com
 * @author Peter Herweg, pherweg@web.de
 */
@Slf4j
final class PageAttributesConverter {

    /**
     * Constructor is private, because it's just a utility class.
     */
    private PageAttributesConverter() {
    }

    /** convert xsl:fo attributes to RTF text attributes */
    static RtfAttributes convertPageAttributes(final SimplePageMaster pagemaster) {
        FOPRtfAttributes attrib = new FOPRtfAttributes();

        try {
            final RegionBA before = (RegionBA) pagemaster
                    .getRegion(Constants.FO_REGION_BEFORE);
            final RegionBody body = (RegionBody) pagemaster
                    .getRegion(Constants.FO_REGION_BODY);
            final RegionBA after = (RegionBA) pagemaster
                    .getRegion(Constants.FO_REGION_AFTER);

            attrib.setTwips(RtfPage.PAGE_WIDTH, pagemaster.getPageWidth());
            attrib.setTwips(RtfPage.PAGE_HEIGHT, pagemaster.getPageHeight());
            // Sets the document level property
            attrib.set(RtfPage.ITAP, "0");

            final Object widthRaw = attrib.getValue(RtfPage.PAGE_WIDTH);
            final Object heightRaw = attrib.getValue(RtfPage.PAGE_HEIGHT);
            if (widthRaw instanceof Integer
                    && heightRaw instanceof Integer
                    && ((Integer) widthRaw).intValue() > ((Integer) heightRaw)
                    .intValue()) {
                attrib.set(RtfPage.LANDSCAPE);
            }

            final Length pageTop = pagemaster.getCommonMarginBlock().marginTop;
            final Length pageBottom = pagemaster.getCommonMarginBlock().marginBottom;
            final Length pageLeft = pagemaster.getCommonMarginBlock().marginLeft;
            final Length pageRight = pagemaster.getCommonMarginBlock().marginRight;

            Length bodyTop = pageTop;
            Length bodyBottom = pageBottom;
            Length bodyLeft = pageLeft;
            Length bodyRight = pageRight;

            if (body != null) {
                // Should perhaps be replaced by full reference-area handling.
                final CommonMarginBlock bodyMargin = body
                        .getCommonMarginBlock();
                bodyTop = (Length) NumericOp.addition(pageTop,
                        bodyMargin.marginTop);
                bodyBottom = (Length) NumericOp.addition(pageBottom,
                        bodyMargin.marginBottom);
                bodyLeft = (Length) NumericOp.addition(pageLeft,
                        bodyMargin.marginLeft);
                bodyRight = (Length) NumericOp.addition(pageRight,
                        bodyMargin.marginRight);
            }

            attrib.setTwips(RtfPage.MARGIN_TOP, bodyTop);
            attrib.setTwips(RtfPage.MARGIN_BOTTOM, bodyBottom);
            attrib.setTwips(RtfPage.MARGIN_LEFT, bodyLeft);
            attrib.setTwips(RtfPage.MARGIN_RIGHT, bodyRight);

            // region-before attributes
            Length beforeTop = pageTop;
            if (before != null) {
                beforeTop = (Length) NumericOp.addition(pageTop,
                        before.getExtent());
            }
            attrib.setTwips(RtfPage.HEADERY, beforeTop);

            // region-after attributes
            Length afterBottom = pageBottom;
            if (after != null) {
                afterBottom = (Length) NumericOp.addition(pageBottom,
                        after.getExtent());
            }
            attrib.setTwips(RtfPage.FOOTERY, afterBottom);
        } catch (final Exception e) {
            log.error("Exception in convertPageAttributes: " + e.getMessage()
                    + "- page attributes ignored");
            attrib = new FOPRtfAttributes();
        }

        return attrib;
    }
}
