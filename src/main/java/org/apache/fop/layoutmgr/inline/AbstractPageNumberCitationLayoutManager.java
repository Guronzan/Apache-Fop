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

/* $Id: AbstractPageNumberCitationLayoutManager.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.layoutmgr.inline;

import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Resolvable;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.UnresolvedPageNumber;
import org.apache.fop.fo.flow.AbstractPageNumberCitation;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.TraitSetter;

/**
 * LayoutManager for the fo:page-number-citation(-last) formatting object
 */
public abstract class AbstractPageNumberCitationLayoutManager extends
LeafNodeLayoutManager {

    /** The page number citation object */
    protected AbstractPageNumberCitation fobj;
    /** Font for the page-number-citation */
    protected Font font;

    /**
     * Indicates whether the page referred to by the citation has been resolved
     * yet
     */
    protected boolean resolved = false;

    /**
     * Constructor
     *
     * @param node
     *            the formatting object that creates this area TODO better
     *            retrieval of font info
     */
    public AbstractPageNumberCitationLayoutManager(
            final AbstractPageNumberCitation node) {
        super(node);
        this.fobj = node;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        final FontInfo fi = this.fobj.getFOEventHandler().getFontInfo();
        final FontTriplet[] fontkeys = this.fobj.getCommonFont().getFontState(
                fi);
        this.font = fi.getFontInstance(fontkeys[0],
                this.fobj.getCommonFont().fontSize.getValue(this));
        setCommonBorderPaddingBackground(this.fobj
                .getCommonBorderPaddingBackground());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AlignmentContext makeAlignmentContext(final LayoutContext context) {
        return new AlignmentContext(this.font, this.fobj.getLineHeight()
                .getOptimum(this).getLength().getValue(this),
                this.fobj.getAlignmentAdjust(),
                this.fobj.getAlignmentBaseline(), this.fobj.getBaselineShift(),
                this.fobj.getDominantBaseline(), context.getAlignmentContext());
    }

    /** {@inheritDoc} */
    @Override
    public InlineArea get(final LayoutContext context) {
        this.curArea = getPageNumberCitationInlineArea();
        return this.curArea;
    }

    /**
     * {@inheritDoc} , LayoutContext)
     */
    @Override
    public void addAreas(final PositionIterator posIter,
            final LayoutContext context) {
        super.addAreas(posIter, context);
        if (!this.resolved) {
            getPSLM().addUnresolvedArea(this.fobj.getRefId(),
                    (Resolvable) this.curArea);
        }
    }

    /**
     * If id can be resolved then simply return a word, otherwise return a
     * resolvable area
     *
     * @param parentLM
     *            the parent LayoutManager
     * @return a corresponding InlineArea
     */
    private InlineArea getPageNumberCitationInlineArea() {
        final PageViewport page = getPSLM().getFirstPVWithID(
                this.fobj.getRefId());
        TextArea text;
        if (page != null) {
            final String str = page.getPageNumberString();
            // get page string from parent, build area
            text = new TextArea();
            final int width = getStringWidth(str);
            text.addWord(str, 0);
            text.setIPD(width);
            this.resolved = true;
        } else {
            this.resolved = false;
            text = new UnresolvedPageNumber(this.fobj.getRefId(), this.font);
            final String str = "MMM"; // reserve three spaces for page number
            final int width = getStringWidth(str);
            text.setIPD(width);
        }
        updateTextAreaTraits(text);

        return text;
    }

    /**
     * Updates the traits for the generated text area.
     *
     * @param text
     *            the text area
     */
    protected void updateTextAreaTraits(final TextArea text) {
        TraitSetter.setProducerID(text, this.fobj.getId());
        text.setBPD(this.font.getAscender() - this.font.getDescender());
        text.setBaselineOffset(this.font.getAscender());
        TraitSetter.addFontTraits(text, this.font);
        text.addTrait(Trait.COLOR, this.fobj.getColor());
        TraitSetter.addPtr(text, this.fobj.getPtr()); // used for accessibility
        TraitSetter.addTextDecoration(text, this.fobj.getTextDecoration());
    }

    /**
     * @param str
     *            string to be measured
     * @return width (in millipoints ??) of the string
     */
    protected int getStringWidth(final String str) {
        int width = 0;
        for (int count = 0; count < str.length(); count++) {
            width += this.font.getCharWidth(str.charAt(count));
        }
        return width;
    }

}
